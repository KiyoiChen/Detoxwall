# DetoxWall v2 — Requirements

Status: **Approved for implementation** · Baseline commit: `564bd0d`

## 1. Problem statement

v1 of DetoxWall looks like a parental-control app, but it's easy to get around:

| # | v1 weakness | Impact |
|---|---|---|
| P1 | The black screen is an `Activity`. Home / Recents / swipe gestures just leave it. | A child "closes" the black screen in one gesture. |
| P2 | Nothing protects the app itself. Settings → App info → **Force stop**, uninstalling, or turning off the accessibility service all work. | All protection gone in 10 seconds. |
| P3 | Opening DetoxWall itself needs no PIN. A child can turn off Child Mode or raise the limits. | Protection is only for show. |
| P4 | Limits are only checked when you switch apps, so an app left open is never blocked. | Unlimited use if you never switch. |
| P5 | The "accumulated child minutes" used for blackout are never actually counted. | The blackout timer never fires. |
| P6 | Parent login depends on Clerk (network) and "succeeds" offline without a password. | False sense of security; the build fails. |
| P7 | PIN is a single SHA-256 hash with no attempt limit. | A 4-digit PIN can be brute-forced quickly. |
| P8 | Overnight schedules check the wrong weekday after midnight. | Bedtime rules miss nights. |
| P9 | The UI hardcodes white text (light theme broken) and seeds fake usage data. | Looks broken and misleading. |

## 2. Goals

- **G1 Strict:** once Child Mode is on, the child can't get past a block with gestures, buttons, killing the app, or Settings.
- **G2 Local-only:** no accounts, no network. The parent PIN on the device is the only credential.
- **G3 Correct:** limits, schedules and blackout use real usage data, checked continuously.
- **G4 Delightful:** a modern, smooth, animated UI that works in both dark and light themes.

## 3. Non-goals / platform limits (documented, not solvable without device-owner mode)

- Safe-mode boot, factory reset, or ADB can always remove a normal app.
- The power menu / reboot can't be blocked. Protection comes back automatically after boot.
- A true kiosk lock (`startLockTask`) needs device-owner provisioning, which is out of scope.

## 4. Functional requirements

### Parent PIN (local)
- **R1** A parent PIN (4–8 digits) is created during onboarding and stored **only on the device**.
- **R2** The PIN is hashed with PBKDF2-HMAC-SHA256 (≥100k iterations, random 16-byte salt) and compared in constant time.
- **R3** After 5 wrong attempts, PIN entry locks out for 30 s, doubling each time up to 15 min. The lockout survives app restarts.
- **R4** The parent can change the PIN (needs the current PIN). Clerk and all network auth are removed.

### Strict (Child) Mode
- **R5** While Child Mode is on, opening DetoxWall shows a **Parent Gate** (PIN pad) before any settings. It re-locks whenever the app goes to the background.
- **R6** Block screens are drawn as an **accessibility overlay window** that belongs to the service, not an Activity. Home, Back, Recents and swiping DetoxWall out of Recents don't remove them.
- **R7** A **device blackout** covers the whole screen until the parent PIN is entered. It triggers on: the total screen-time allowance running out (5 min – 5 h), a "Lock device" schedule, or "Lock now" from the parent.
- **R8** While locked, pulling down the notification shade is dismissed automatically. Pressing Home or Recents counts as a **bypass attempt** (logged, siren if on).
- **R9** **Tamper protection:** in Child Mode, these screens get kicked to Home and show a "Protected — parent PIN required" overlay:
  - any system Settings screen that mentions DetoxWall (app info / force stop / clear storage / accessibility toggle / device-admin deactivation / battery)
  - the package-installer uninstall dialog for DetoxWall

  A correct PIN opens a 5-minute maintenance window.
- **R10** The optional **Device Admin** activation blocks uninstalling DetoxWall. Deactivating it is itself a protected screen (R9).
- **R11** Protection survives: swiping from Recents (the enforcement service is system-bound), reboot (boot receiver + persisted lock state), and process death (sticky foreground guardian service). If protection was interrupted (e.g. the service was killed), that gets logged for the parent.
- **R12** While the siren is playing, the hardware volume keys are consumed so the child can't mute it.
- **R13** Emergency: the blackout screen has an **Emergency call** button, and the default dialer is never blocked.

### Limits, focus, schedules
- **R14** Per-app daily limits (5–240 min), using today's real foreground time from `UsageEvents`. They're checked **every few seconds while an app is open**, not only when switching.
- **R15** When a limit is reached, DetoxWall sends the user Home and shows a block card:
  - Adult mode: a 5-second mindful pause, then up to 3 "+5 min" snoozes per app per day.
  - Child Mode: "Ask a parent", where the PIN grants +15/+30/+60 min for today.
- **R16** Focus session (15m / 30m / 1h / 2h): all limited apps are blocked until it ends. In Child Mode, ending it early needs the parent (via the gate).
- **R17** Schedules: name, start/end (overnight supported, and the after-midnight part belongs to the **start** day), weekdays, and a type: *block limited apps* or *lock whole device* (Child Mode only).
- **R18** Tamper and bypass events are stored in a local log that the parent can view and clear.

### UI / UX
- **R19** First run: onboarding (welcome → create PIN → permission checklist with live status, including Android 13 "restricted settings" guidance).
- **R20** Five tabs: **Home**, **Apps**, **Schedules**, **Insights**, **Protect**, with animated tab transitions.
- **R21** Home: animated screen-time ring, protection health, quick focus, lock-now, top apps, and a warning banner when permissions are missing.
- **R22** Apps: real app icons, animated progress, add-app picker with search, edit sheet with presets.
- **R23** Insights: animated 7-day bar chart and per-day app breakdown, built from real data. No fake seed data.
- **R24** Protect: Child Mode, blackout allowance, siren, tamper protection, device admin, change PIN, permission health, tamper log, theme.
- **R25** Motion everywhere it helps: spring press feedback, PIN dot fill and shake on error, staggered list entry, animated counters and progress. Light and dark themes are both fully supported.

## 5. Acceptance criteria (manual, on a device with Child Mode on)

| AC | Steps | Expected |
|---|---|---|
| AC1 | Use up the blackout allowance, then press Home / Recents / Back / swipe | Black screen stays; bypass logged; siren plays |
| AC2 | Settings → Apps → DetoxWall | Kicked Home, PIN overlay shown |
| AC3 | Long-press icon → Uninstall | Kicked Home, PIN overlay shown |
| AC4 | Swipe DetoxWall away in Recents, open a blocked app | Still blocked |
| AC5 | Reboot while locked | Black screen returns after unlock/boot |
| AC6 | Keep a limited app open past its limit without switching | Blocked within ~10 s |
| AC7 | Open DetoxWall | Parent Gate shown; 5 wrong PINs → lockout timer |
| AC8 | Siren playing, press volume down | Volume doesn't change |

Automated: unit tests cover PIN hashing and lockout, the usage calculator, schedule matching, the enforcement decision table, and the protected-screen detector.
