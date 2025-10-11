#include <stdlib.h>

extern int main(int argc, char *argv[]);

int rsync_android_entry(int argc, char *argv[]) {
    return main(argc, argv);
}
