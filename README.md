# myRSync

Android rsync client for creating configurations and running them on demand or on a schedule. Aimed at people who want backups under their own control (rsync daemon or SSH).

Google Play (beta): [com.linminitools.myrsync](https://play.google.com/store/apps/details?id=com.linminitools.myrsync)

## App tabs

1. **Overview** — Next/last run status for scheduled jobs  
2. **Configurations** — Create and edit rsync jobs; attach a schedule when saving  
3. **Log** — rsync output for recent runs  

Automation: Tasker / Locale plugin action can fire a saved configuration.

## How to use

1. Create an rsync configuration (daemon or SSH).  
2. Run it manually, or enable a schedule on the config.  
3. Grant **All files access** (Android 11+) and **Alarms & reminders** when prompted so paths and schedules work.  
4. Check Overview and Log for status.

## Free vs Pro

One Play listing (`com.linminitools.myrsync`), free to install:

| Free | Pro (one-time IAP `myrsync_pro`) |
| --- | --- |
| 1 configuration | Unlimited configurations |
| Manual run | Schedules + Tasker / Locale |

See [docs/PLAY_STORE_CHECKLIST.md](docs/PLAY_STORE_CHECKLIST.md) for Play Console product setup.

## Privacy

Privacy policy (host via GitHub Pages from `docs/`):  
[docs/privacy-policy.html](docs/privacy-policy.html)

## License

GPLv3 — see [LICENSE](LICENSE).
