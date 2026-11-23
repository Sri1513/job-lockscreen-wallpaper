# Job Lockscreen Wallpaper

Android live wallpaper that shows the countdown at
jobs.sriomkar.com/lockscreen on your phone/tablet lock screen.

## Change the job (no rebuild)

1. Open the **Mission Lockscreen** app.
2. Paste the new lockscreen link:
   `https://jobs.sriomkar.com/lockscreen?job_id=<job id>&deadline=<ISO date with offset, e.g. 2026-12-15T23:59:00-05:00>`
3. Tap **Save**. The wallpaper picks it up the next time your lock
   screen shows. **Reset to Pax8** restores the original link.

## Auto-follow (follows the current dream job)

Every time the lock screen shows, the wallpaper asks the server
(`GET https://jobs.sriomkar.com/api/lockscreen/current`) which dream
job is current. When the server answers with a different lockscreen
link, the wallpaper switches to it automatically — when one job
closes, the next dream job in line takes its place with no tap.
The link saved above stays the fallback: if the server is
unreachable or answers with no job, the saved link keeps showing,
and it is also the first-launch link. A server link must pass the
same check as a manual one (https + contains `lockscreen`).

## Build

Actions → Build APK → Run workflow. The APK is uploaded as the
`MissionLockscreen-APK` artifact. Install `app-debug.apk`, then pick
**De-Job-Intelligence Lockscreen** under Settings → Wallpaper → Live Wallpapers.
