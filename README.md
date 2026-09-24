# DetoxWall

**Calm. Simple. Protective.** A local-only Android app for understanding and planning your screen time.

DetoxWall shows how much you use each app, lets you set daily limits and schedules, run focus sessions, and browse a month of usage history. Everything stays **on the phone**: no account, no network, no uploads.

> **Status (v2, in progress):** the screens, data and parent PIN are built and tested. **Blocking is not built yet**: limits, schedules and focus sessions are saved and displayed, but apps aren't blocked when time runs out. See [Roadmap](#roadmap).

---

## Features

| Screen | What it does |
|---|---|
| **Home** | Animated ring with today's real screen time against your goal; focus session timer (15m / 30m / 1h / 2h) with a live countdown; top 5 apps today; a banner when a permission is missing |
| **Apps** | Your limited apps with real icons, time left and colored progress bars; add apps from a searchable picker (most-used first); edit limits with a slider (5 min – 4 h) and presets; pause or remove a limit |
| **Schedules** | Named routines with start/end times, weekday chips and on/off switches; overnight times supported; "Active now" indicator; one-tap starters (School hours, Bedtime, Homework) |
| **Insights** | Bar chart for every day of the month, month-to-month navigation, monthly total and daily average; tap a day to see each app's usage that day |
| **Onboarding** | First run: animated welcome → create a parent PIN (optional) → permissions |
| **PIN screen** | Asks for the parent PIN when the app opens and after 30 s in the background; live countdown during lockout |
| **Settings** | Light / Dark / System theme; set or change the parent PIN; "Ask for PIN when opening" switch; permissions |
| **Permissions** | Usage access, notifications and battery, each with a live status; tap to open the right system page |

**Parent PIN:** 4–8 digits, stored only on the device as a PBKDF2-HMAC-SHA256 hash (120k iterations, random salt, constant-time check). After 5 wrong tries it locks out for 30 s, doubling each time up to 15 min. The lockout survives restarts. Backups are disabled.

**Motion:** tab slides, cards that shrink when pressed, staggered list entry, rolling numbers, growing chart bars, a PIN pad whose dots fill in and shake on a wrong PIN, and a pulsing "active" dot.

---

## How it works

```
UsageStatsManager ──▶ UsageReader ──▶ UsageCalculator (pure) ──▶ ViewModels ──▶ Compose screens
                                                                   ▲
Room DB (limits, schedules, event log) ─▶ DetoxRepository ─────────┤
SettingsStore (prefs → StateFlow) ─────────────────────────────────┤
PinManager (PBKDF2 + lockout) ─────────────────────────────────────┘
```

- **Usage:** today's time per app comes from Android usage *events* (accurate, including the app open right now). Older days fall back to Android's aggregated daily totals when events have expired, which on many phones happens after about a week.
- **Schedules:** overnight windows count the after-midnight part toward the day they **started** (so Monday's 21:30–07:00 is active at 02:00 on Tuesday).
- **DI:** a small manual `AppContainer` created in `DetoxApplication`.

---

## Project structure

```
app/src/main/java/com/example/
├── DetoxApplication.kt / AppContainer.kt   # app entry + manual DI
├── MainActivity.kt                        # 5-tab scaffold, permissions screen
├── data/        # Room entities & DAOs, DetoxRepository, SettingsStore
├── security/    # PinHasher (PBKDF2, lockout policy), PinManager
├── usage/       # UsageCalculator, UsageReader, InstalledApps, Permissions
├── enforcement/ # decision table, detector, siren (not wired into the app yet)
└── ui/
    ├── theme/       # dark + light color schemes, type, shapes
    ├── components/  # motion helpers, ProgressRing, SectionCard, AppIcon, PinPad
    ├── home/ apps/ schedules/ insights/ settings/ permissions/
specs/v2-strict-rewrite/   # requirements, design, task list
```

---

## Getting started

**Requirements:** Android Studio (recent, with AGP 9 support), and a device or emulator running Android 8.0+ (API 26).

1. **Open** `D:\Video\Desktop\Detoxwall-` in Android Studio and wait for Gradle Sync.
2. **Pick a device.**
   - A phone: turn on Developer options → USB debugging and plug it in.
   - An emulator: Device Manager → create a Pixel on Android 14+.
3. **Run** with ▶ (Shift+F10).
4. **Grant Usage access:** Settings tab → Permissions → Usage access → DetoxWall → allow. Without it, all usage numbers stay at 0.

Command line (use Android Studio's bundled JDK if `java` isn't on your PATH):
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew assembleDebug        # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # unit + Robolectric tests
```

---

## Tests

20 tests, all passing:

| Test | Covers |
|---|---|
| `PinSecurityTest` | PBKDF2 hash and verify, salts, PIN validation, lockout durations |
| `PinManagerTest` | PIN persistence; lockout after 5 tries survives a new instance and expires |
| `UsageCalculatorTest` | sessions, app switches, the app open right now, the app open since before midnight, screen off |
| `ScheduleTest` | daytime and overnight windows, day masks, summaries, time formatting |
| `ExampleRobolectricTest` | resources load |

---

## Roadmap

- [x] First-run onboarding (welcome → create PIN → permissions)
- [x] Parent PIN screen when opening the app (and after 30 s in the background; can be turned off in Settings)
- [ ] Blocking when a limit, schedule or focus session is active (not started)
- [ ] Screenshot tests for key components

For strong, hard-to-bypass parental control today, Android's built-in **Digital Wellbeing** and Google **Family Link** are the recommended tools. DetoxWall doesn't replace them.

---

## Changes from v1

- Removed the Clerk login and all network code: no `INTERNET` permission.
- The PIN moved from a single SHA-256 hash to PBKDF2 with a lockout.
- Fake seeded usage data removed; all numbers are real.
- Light theme fixed (v1 hardcoded white text).
- The overnight-schedule weekday bug is fixed.
