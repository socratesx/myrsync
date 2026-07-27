/*
 * Android client build for myrsync.
 * Password auth is required for DROPBEAR_PASSWORD / non-interactive rsync -e.
 */

/* Server pieces are unused; crypt() is unavailable on Android. */
#define DROPBEAR_SVR_PASSWORD_AUTH 0

/* Enable client password auth (also enables keyboard-interactive via sysoptions). */
#define DROPBEAR_CLI_PASSWORD_AUTH 1

/* Read password from DROPBEAR_PASSWORD (no TTY available under rsync -e). */
#define DROPBEAR_USE_PASSWORD_ENV 1

/* Prefer speed over size; matches typical Android Dropbear builds. */
#define DROPBEAR_SMALL_CODE 0

/* Non-interactive only for our wrapper path; keep askpass off (noexec /data). */
#define DROPBEAR_CLI_ASKPASS_HELPER 0
