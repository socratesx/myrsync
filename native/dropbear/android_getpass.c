/*
 * Minimal getpass() for Android libc (which does not provide it).
 * myrsync always supplies DROPBEAR_PASSWORD, so Dropbear's getpass_or_cancel()
 * returns before calling this. The stub exists only to satisfy the linker.
 */
#include <stddef.h>

char *getpass(const char *prompt) {
    (void)prompt;
    return NULL;
}
