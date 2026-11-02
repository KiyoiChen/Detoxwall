# DetoxWall — automatic rest and parent temporary-use MVP

Branch: `feature/rest-break-parent-mode` (independent of `main` and the previously working `feature/child-play-session`).

## Child flow (no manual reset)

1. Child Mode and play sessions enabled, parent PIN set, accessibility service enabled.
2. On screen wake (after Android keyguard), an accessibility overlay requires choosing 5 / 10 / 15 / 30 minutes before using the phone.
3. A fixed deadline is saved; the round continues counting even if apps or the screen are switched off.
4. At round end, a **30-minute rest** starts. The full-screen overlay stays visible during rest, with a countdown and an emergency dialer button. Screen off/on cannot reset the rest timer.
5. When rest ends, the time-picker automatically returns on screen wake or the next enforcement check. No parent action required.
6. Daily **60-minute gaming allowance** is separate from DetoxWall's optional generic screen-time limit. Choosing a round **books its full chosen duration** against today's allowance.
7. When remaining daily allowance is less than five minutes, no more choices are available. It resets when the phone's local date changes.

## Parent interruption during rest

- Enter parent PIN in the rest overlay to see TWO options:
  - **Temporary parent use (15 minutes)**: grants parent access; the original rest deadline keeps running and daily gaming budget does not reset. A notification has **結束家長模式**, and the app also shows an End Parent Mode button. On exit or the 15-minute expiry the normal rest/picker/daily lock resumes automatically.
  - **End rest early**: immediately open the next picker, but still subject to daily limit.
- PIN verification is required; missing PIN cannot be used to bypass the lock.
- Emergency dialer remains accessible without parent PIN.

## Parent setup
- Child Mode ON, 讓孩子選擇單次遊戲時間 ON.
- In Settings, change rest duration (default 30min) and daily gaming cap (default 60min).
- Enabling the new play-session setting turns off the old generic daily screen quota to avoid two independent counters unexpectedly locking the phone. Parent can re-enable it if desired.
- The old manual parent reset remains an explicit override, not part of routine use.

## Important limitations
- The Android accessibility overlay is not a tamper-proof Device Owner or true system lock. ROG Phone 1 has not been real-device tested with this branch. Verify Home/Back/Recents, emergency calls, battery optimizations, and reboot.
- Parent notification actions require notifications to remain enabled.
- System clock changes can affect wall-clock deadlines; confirm Android date/time is automatic.
- If the phone is rebooted, the persisted deadlines remain, but the accessibility service must restart.
- Test APK uses a NEW debug application ID suffix `.childtimer3`, so it can coexist with the previously installed `.childtimer2` test. Enable **only one** accessibility service at a time.
- As with the previous version, parent PIN and daily settings are stored locally.

## Build and test
```bash
./gradlew testDebugUnitTest assembleDebug
```

## Device checks
- [ ] Selecting the round starts a deadline and covers Home/Back/Recents before selection
- [ ] At expiry full-screen rest appears without child input
- [ ] Screen off/on or force-stopping the app does not reset 30-minute rest
- [ ] Rest timeout automatically offers next duration
- [ ] Parent PIN presents both choices; temporary use doesn't reset rest/daily budget
- [ ] App/notification End Parent Mode action immediately restores child restrictions
- [ ] Parent 15-minute temporary use automatically expires
- [ ] Daily 60-minute quota locks all games for the day; midnight reopens
- [ ] Android lock screen, emergency dialer and device reset paths remain usable
