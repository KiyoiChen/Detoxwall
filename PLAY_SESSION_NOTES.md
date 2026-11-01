# Child-selected one-time play session — DetoxWall fork

Feature branch: `feature/child-play-session`. Existing DetoxWall child mode, parent PIN and accessibility lock remain in place.

## Usage

1. Parent installs the custom build, configures PIN and accessibility + usage permissions.
2. Parent enables **Child Mode** and the new **讓孩子選擇單次遊戲時間** toggle in Settings.
3. Child opens DetoxWall and picks 5, 10, 15 or 30 minutes.
4. The choice saves a fixed deadline to device settings and survives app restarts. Switching apps does not restart the round.
5. Until a duration is chosen, the app intercepts other apps. The launcher, DetoxWall and emergency dialer remain available.
6. After the deadline, DetoxWall uses its existing **whole-device accessibility lock**.
7. Parent PIN temporarily unlocks the phone for 15 minutes through the existing lock screen. Only a parent-PIN-confirmed reset creates a new round.

## IMPORTANT

- This is an experimental MVP, **not yet compiled or real-device tested** on ROG Phone 1.
- Time is counted from tapping the duration, including time with the screen off. It is not gaming-foreground-only time.
- It does not automatically show the picker when a game launches; children must first open DetoxWall.
- The existing **daily screen time** limit may lock the phone early; configure it accordingly.
- Accessibility blocking isn't a system-level, tamper-proof child control. Keep a backup emergency access plan.
- Emergency dialer remains exempt.
- Do not assume the original APK can be updated in place: signing keys may differ, requiring uninstall and reinstall. Uninstalling erases app settings.
- Do not merge into main or rely on this as a family safety control until verified.

## Local verification

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## ROG Phone 1 manual checks

- [ ] Settings + PIN are configured before Child Mode is enabled
- [ ] Opening games without selecting minutes is blocked
- [ ] Selection of 5 minutes works and starts a countdown
- [ ] Opening other apps cannot restart the round
- [ ] At the deadline, Home, Back, Recent apps cannot dismiss the lock
- [ ] Reboot retains an expired lock
- [ ] Parent PIN grants temporary access but doesn't reset a completed round
- [ ] Parent PIN is required to explicitly reset to READY
- [ ] Emergency dialer remains accessible
