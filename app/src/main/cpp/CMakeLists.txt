cmake_minimum_required(VERSION 3.22.1)
project(myrsync_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_library(myrsync-jni SHARED
        rsync_jni.cpp)

add_library(rsync-android SHARED IMPORTED)
set_target_properties(rsync-android PROPERTIES
        IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/librsync-android.so")

target_include_directories(myrsync-jni PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR})

target_link_libraries(myrsync-jni
        android
        log
        rsync-android)
