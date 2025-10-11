#include <jni.h>
#include <android/log.h>
#include <unistd.h>

#include <cerrno>
#include <cstring>
#include <new>
#include <string>
#include <thread>
#include <vector>

namespace {

constexpr const char *kTag = "RsyncBridge";

extern "C" int rsync_android_entry(int argc, char *argv[]);

class ScopedFd {
public:
    ScopedFd() = default;
    explicit ScopedFd(int fd) : fd_(fd) {}
    ~ScopedFd() { close(); }

    ScopedFd(const ScopedFd &) = delete;
    ScopedFd &operator=(const ScopedFd &) = delete;

    ScopedFd(ScopedFd &&other) noexcept : fd_(other.fd_) { other.fd_ = -1; }
    ScopedFd &operator=(ScopedFd &&other) noexcept {
        if (this != &other) {
            close();
            fd_ = other.fd_;
            other.fd_ = -1;
        }
        return *this;
    }

    int get() const { return fd_; }

    int release() {
        int fd = fd_;
        fd_ = -1;
        return fd;
    }

    void reset(int fd = -1) {
        if (fd_ != fd) {
            close();
            fd_ = fd;
        }
    }

private:
    void close() {
        if (fd_ >= 0) {
            ::close(fd_);
            fd_ = -1;
        }
    }

    int fd_ = -1;
};

std::string ReadFully(int fd) {
    std::string buffer;
    char temp[4096];
    ssize_t count;
    while ((count = ::read(fd, temp, sizeof(temp))) > 0) {
        buffer.append(temp, static_cast<std::size_t>(count));
    }
    return buffer;
}

void ThrowIOException(JNIEnv *env, const char *message) {
    jclass exception = env->FindClass("java/io/IOException");
    if (exception != nullptr) {
        env->ThrowNew(exception, message);
    }
}

std::vector<std::string> CollectArgs(JNIEnv *env, jobjectArray argsArray) {
    std::vector<std::string> result;
    if (argsArray == nullptr) {
        return result;
    }

    jsize count = env->GetArrayLength(argsArray);
    result.reserve(static_cast<std::size_t>(count));

    for (jsize index = 0; index < count; ++index) {
        auto element = static_cast<jstring>(env->GetObjectArrayElement(argsArray, index));
        if (element == nullptr) {
            result.emplace_back();
            continue;
        }

        const char *utfChars = env->GetStringUTFChars(element, nullptr);
        if (utfChars == nullptr) {
            env->DeleteLocalRef(element);
            throw std::bad_alloc();
        }

        result.emplace_back(utfChars);
        env->ReleaseStringUTFChars(element, utfChars);
        env->DeleteLocalRef(element);
    }

    return result;
}

}  // namespace

extern "C" JNIEXPORT jobject JNICALL
Java_com_linminitools_myrsync_rsync_RsyncBridge_runRsyncNative(
        JNIEnv *env, jclass /*clazz*/, jobjectArray javaArgs) {
    std::vector<std::string> argsStorage;
    try {
        argsStorage = CollectArgs(env, javaArgs);
    } catch (const std::bad_alloc &) {
        ThrowIOException(env, "Unable to allocate argument buffer for rsync invocation");
        return nullptr;
    }

    std::vector<char *> argv;
    argv.reserve(argsStorage.size() + 1);
    for (auto &arg : argsStorage) {
        argv.push_back(arg.empty() ? const_cast<char *>("") : arg.data());
    }
    argv.push_back(nullptr);
    int argc = static_cast<int>(argsStorage.size());

    int stdoutPipe[2];
    int stderrPipe[2];
    if (pipe(stdoutPipe) != 0 || pipe(stderrPipe) != 0) {
        ThrowIOException(env, "Unable to create pipe for rsync output capture");
        return nullptr;
    }

    ScopedFd stdoutRead(stdoutPipe[0]);
    ScopedFd stdoutWrite(stdoutPipe[1]);
    ScopedFd stderrRead(stderrPipe[0]);
    ScopedFd stderrWrite(stderrPipe[1]);

    ScopedFd stdoutBackup(::dup(STDOUT_FILENO));
    ScopedFd stderrBackup(::dup(STDERR_FILENO));
    if (stdoutBackup.get() < 0 || stderrBackup.get() < 0) {
        ThrowIOException(env, "Unable to duplicate stdout/stderr for rsync invocation");
        return nullptr;
    }

    if (::dup2(stdoutWrite.get(), STDOUT_FILENO) < 0 || ::dup2(stderrWrite.get(), STDERR_FILENO) < 0) {
        ThrowIOException(env, "Unable to redirect stdout/stderr for rsync invocation");
        return nullptr;
    }

    stdoutWrite.reset();
    stderrWrite.reset();

    std::string stdoutBuffer;
    std::string stderrBuffer;

    std::thread stdoutThread([&stdoutBuffer](int fd) {
        stdoutBuffer = ReadFully(fd);
        ::close(fd);
    }, stdoutRead.release());

    std::thread stderrThread([&stderrBuffer](int fd) {
        stderrBuffer = ReadFully(fd);
        ::close(fd);
    }, stderrRead.release());

    int exitCode = rsync_android_entry(argc, argv.data());

    ::fflush(stdout);
    ::fflush(stderr);

    if (stdoutBackup.get() >= 0 && ::dup2(stdoutBackup.get(), STDOUT_FILENO) < 0) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "Failed to restore stdout: %s", std::strerror(errno));
    }
    if (stderrBackup.get() >= 0 && ::dup2(stderrBackup.get(), STDERR_FILENO) < 0) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "Failed to restore stderr: %s", std::strerror(errno));
    }

    stdoutBackup.reset();
    stderrBackup.reset();

    if (stdoutThread.joinable()) {
        stdoutThread.join();
    }
    if (stderrThread.joinable()) {
        stderrThread.join();
    }

    jclass resultClass = env->FindClass("com/linminitools/myrsync/rsync/RsyncResult");
    if (resultClass == nullptr) {
        ThrowIOException(env, "Unable to locate com.linminitools.myrsync.rsync.RsyncResult");
        return nullptr;
    }

    jmethodID ctor = env->GetMethodID(resultClass, "<init>", "(ILjava/lang/String;Ljava/lang/String;)V");
    if (ctor == nullptr) {
        ThrowIOException(env, "Missing RsyncResult constructor");
        return nullptr;
    }

    jstring stdoutString = env->NewStringUTF(stdoutBuffer.c_str());
    jstring stderrString = env->NewStringUTF(stderrBuffer.c_str());
    if (stdoutString == nullptr || stderrString == nullptr) {
        ThrowIOException(env, "Failed to allocate result strings");
        return nullptr;
    }

    jobject result = env->NewObject(resultClass, ctor, exitCode, stdoutString, stderrString);
    env->DeleteLocalRef(stdoutString);
    env->DeleteLocalRef(stderrString);

    return result;
}
