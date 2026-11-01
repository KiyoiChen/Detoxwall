# Child-selected one-time play session — DetoxWall fork

Feature branch: `feature/child-play-session`. Existing DetoxWall child mode, parent PIN and accessibility lock remain in place. This iteration adds a forced accessibility-overlay picker after screen wake.

## Usage

1. Parent installs the custom build, configures PIN and accessibility + usage permissions.
2. Parent enables **Child Mode** and the new **讓孩子選擇單次遊戲時間** toggle in Settings.
3. Child sees a full-screen **5 / 10 / 15 / 30-minute picker** covering whichever app is open, including the launcher, and selects a round. It can also appear immediately after activating Child Mode.
4. The choice saves a fixed deadline to device settings and survives app restarts. Switching apps does not restart the round.
5. Until a duration is chosen, the accessibility overlay blocks access to the launcher, DetoxWall and ordinary apps. Emergency dialer remains accessible; parent PIN temporarily opens maintenance mode.
6. After the deadline, DetoxWall uses its existing **whole-device accessibility lock**. The child must switch the screen off and on again; an expired round is then reset to READY, displaying the picker above the foreground app (normally after system unlock).
7. No parent PIN is needed for each normal round. Parent PIN still temporarily unlocks the phone for 15 minutes and grants access to settings. A parent may also explicitly reset the round.

## IMPORTANT

- This is an experimental MVP, **not yet compiled or real-device tested** on ROG Phone 1.
- Time is counted from tapping the duration, including time with the screen off. It is not gaming-foreground-only time.
- The picker is a full-screen accessibility overlay, not a true Android device-owner lock; Android system keyguard, battery restrictions and OEM gestures may behave differently. Verify screen-off/on and Home/Back/Recents on the physical ROG Phone 1.
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
- [ ] Next screen-off/on after expiration automatically shows picker, without parent PIN
- [ ] Child cannot reach launcher, settings, notification-shade games or other apps before choosing
- [ ] Parent PIN is still required to enter settings or explicitly reset a round
- [ ] Emergency dialer remains accessible
