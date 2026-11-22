# Job Lockscreen Wallpaper

Android live wallpaper that shows the countdown at
jobs.sriomkar.com/lockscreen on your phone/tablet lock screen.

## Change the job (no rebuild)

1. Open the **Mission Lockscreen** app.
2. Paste the new lockscreen link:
   `https://jobs.sriomkar.com/lockscreen?job_id=<job id>&deadline=<ISO date with offset, e.g. 2026-12-15T23:59:00-05:00>`
3. Tap **Save**. The wallpaper picks it up the next time your lock
   screen shows. **Reset to Pax8** restores the original link.

## Build

Actions → Build APK → Run workflow. The APK is uploaded as the
`MissionLockscreen-APK` artifact. Install `app-debug.apk`, then pick
**De-Job-Intelligence Lockscreen** under Settings → Wallpaper → Live Wallpapers.
