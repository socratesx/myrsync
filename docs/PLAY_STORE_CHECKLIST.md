# Play Store release checklist (myRSync)

Use this when preparing a paid or free listing for `com.linminitools.myrsync`.

## Limited vs paid — how Play actually works

Play does **not** give you two special APK types (“limited” and “paid”). You choose a **monetization model** and a **distribution track**. Same codebase can feed all of them via AAB upload.

### Distribution tracks (“limited” access)

| Track | Who can install | Good for |
| --- | --- | --- |
| Internal testing | Up to 100 email addresses | You + a few devices |
| Closed testing | Testers you invite (often required before production) | Beta / policy testing |
| Open testing | Anyone with the opt-in link | Public beta |
| Production | Everyone in selected countries | Real release |

All of these use the **same** signed AAB; “limited” means who is on the tester list, not a different binary format.

### Monetization models (pick one before production)

Important: if the listing is already **Free**, you **cannot** later flip it to **Paid to download**. You *can* add in-app purchases to a free app.

| Model | What users get | Maintenance | Notes |
| --- | --- | --- | --- |
| **A. Paid app** | Pay once on Play → full app | Lowest | One listing, no Billing code. Niche utilities still do this. |
| **B. Free + one-time unlock (recommended for reach)** | Free install; Pro unlocks limits | Medium | One APK + Play Billing Library; create a non-consumable product in Console. |
| **C. Two apps (free + paid)** | `…myrsync` paid and `…myrsync.free` lite | Highest | Two listings, two updates; avoid unless you need completely separate products. |

**Selected model: B — Free + one-time Pro unlock** (implemented in-app).

| Tier | Limits |
| --- | --- |
| Free | 1 configuration, manual run only, no schedules, no Tasker |
| Pro (`myrsync_pro`) | Unlimited configs, schedules, Tasker / Locale |

Same package id and same AAB. No second APK.

### Play Console — create the product

1. App must be **Free** to download (IAP unlocks Pro).
2. Monetize → In-app products → Create **one-time** product  
   - Product ID: **`myrsync_pro`** (must match `BuildConfig.PRO_PRODUCT_ID`)  
   - Name: e.g. “myRSync Pro”  
   - Status: Active  
3. License testing: add your Google accounts under Settings → License testing so you can buy without charge.
4. Upload a build to Internal/Closed testing that includes Billing, then buy/restore on a test account.

### Local debug without Play

Debug builds show **Settings → Debug: unlock Pro** to exercise Free/Pro gates without Billing.

## Before you upload

- [ ] Host the privacy policy (see `docs/privacy-policy.html`)
  - Recommended: GitHub Pages from `/docs` →  
    `https://socratesx.github.io/myrsync/privacy-policy.html`
  - Paste that URL in Play Console **App content → Privacy policy**
  - Confirm Settings → Privacy policy opens the same URL in the app
- [ ] Create / verify a **release keystore** (never commit it)
  - Run: `./scripts/create-release-keystore.sh`  
    (or copy `keystore.properties.example` → `keystore.properties` and fill in)
  - Build AAB: `./gradlew :app:bundleRelease`
  - Prefer AAB upload + Play App Signing in Console
- [ ] Bump `versionCode` / `versionName` in `app/build.gradle` for each upload
- [ ] Smoke-test the release APK/AAB on a clean device:
  - Create config, manual run, scheduled run, Tasker action
  - Grant All files access + Alarms & reminders when prompted

## Play Console declarations

### Privacy policy
Required. Use the hosted `privacy-policy.html` URL.

### Data safety form (typical answers for this app)
- Collects / shares personal data with developer: **No** (no analytics, no account, no backend operated by you)
- Data handled on device / by user-configured remote servers only
- Encryption in transit: depends on user config (SSH vs plain rsync daemon) — describe honestly
- Users can delete data by clearing app data / uninstall

### All files access (`MANAGE_EXTERNAL_STORAGE`)
Core use case fits **Backup and restore** / file sync tool:

Suggested declaration wording:

> myRSync’s core purpose is scheduled and on-demand backup/sync of user-selected folders to a remote rsync/SSH server using the native rsync binary. The app must read and write arbitrary filesystem paths the user configures (not only MediaStore media). Storage Access Framework alone is insufficient because rsync requires stable filesystem paths and unattended scheduled access without re-prompting the user to pick folders each run.

Also prepare:
- Short screencast: pick local path → save config → run → files sync
- Store listing text that mentions backup/sync of local folders

### Alarms & reminders (`SCHEDULE_EXACT_ALARM`)
Declare if Console asks: used so user-defined schedules fire near the chosen time.

### Sensitive permissions / exported components
- Tasker/Locale plugin activity + receiver are intentionally exported (plugin protocol)
- `BOOT_COMPLETED` receiver re-arms schedules after reboot

## Store listing (draft)

**Short description (≤80 chars)**  
Schedule rsync backups from your phone to your own server.

**Full description (draft)**  
myRSync lets you create rsync configurations and run them manually or on a schedule. Sync local folders to an rsync daemon or over SSH so your backups stay on infrastructure you control.

Features:
- Daemon and SSH transports
- Schedules with exact-alarm support
- Overview of next/last run status
- Tasker / Locale plugin action
- Optional root access for restricted paths

Requires Android storage access for the folders you choose to sync.

## GitHub Pages (privacy URL)

From a machine with `gh` access to `socratesx/myrsync`, after `docs/privacy-policy.html` is on the branch you publish from:

```bash
# Enable Pages from /docs on the branch that contains docs/
gh api -X POST repos/socratesx/myrsync/pages \
  -f build_type=legacy \
  -f 'source[branch]=modernize-application' \
  -f 'source[path]=/docs'
```

Then open: `https://socratesx.github.io/myrsync/privacy-policy.html`

If the API returns 409 (already exists), update the source branch/path in repo Settings → Pages instead.

## After approval
- [ ] Turn on managed publishing / staged rollout if desired
- [ ] Keep privacy policy URL stable when you move hosting
