# DetoxWall

**Calm. Simple. Protective.** — an Android digital-detox and parental-control app.

DetoxWall helps adults cut down on distracting apps and lets parents put firm screen-time limits on a child's phone. It watches which app is open. When a limit is reached, a schedule is active, or a focus session is running, it covers the app with a full-screen "wall". In Child Mode that wall is a black screen that only a parent PIN can unlock, and trying to get around it sets off a loud siren.

All limits, schedules and usage data stay **on the device** (Room database + SharedPreferences). The only network feature is the optional parent login through Clerk.

---

## Table of contents

1. [Features](#features)
2. [How it works](#how-it-works)
3. [Screens](#screens)
4. [Tech stack](#tech-stack)
5. [Project structure](#project-structure)
6. [Data model](#data-model)
7. [Permissions](#permissions)
8. [Getting started](#getting-started)
9. [Testing](#testing)
10. [Known limitations / notes](#known-limitations--notes)

---

## Features

| Feature | What it does |
|---|---|
| **Per-app daily limits** | Set a daily limit in minutes for any installed app. Presets are 15/30/45/60/90/120 min, or use the slider. Instagram, TikTok, YouTube, X, Facebook, Reddit, Snapchat, Netflix and Chrome are suggested first. |
| **Real usage tracking** | Reads Android's `UsageStatsManager` and syncs today's minutes into the database every 30 s from a foreground service. |
| **Adult soft block** | When an adult goes over a limit, a "Time's Up" wall appears with a **5-second mindfulness countdown**. After it, they can close the app or "Proceed with Awareness". |
| **Child Mode** | Turns on strict protection for a child profile (built-in profiles: *Default Kid* and *Teen Profile*). Over-limit apps get a **pitch-black screen** that only the parent PIN unlocks. |
| **Screen Blackout timer** | Blacks out the screen after a total amount of child screen time, adjustable from **5 minutes to 5 hours** (presets 5m, 15m, 30m, 45m, 1h, 2h, 3h, 5h). |
| **Siren alarm deterrent** | Pressing Back or leaving the black screen plays a loud generated emergency siren (650–1350 Hz sweep), the system alarm ringtone, a tone generator and vibration together. Volume is adjustable from 10–100%. Each attempt is counted as a *bypass attempt* on the profile. |
| **Parent PIN** | A 4-digit PIN, stored as a salted SHA-256 hash. You can set, change or remove it from the Home screen or the menu. Removing the PIN also turns off Child Mode. |
| **Focus sessions** | One-tap focus blocks (15m / 30m / 1h, or "Block For Next 2 Hours"). While one is running, every restricted app is blocked, and only the parent PIN ends it early. |
| **Schedules** | Named, repeating time windows ("School Hours Lock" 08:30–15:00 Mon–Fri, "Bedtime Detox" 21:30–07:00 every day) that block restricted apps automatically. Supports overnight windows and any mix of weekdays. |
| **Usage insights** | Daily screen-time chart and a "Most Used Apps" list. You can step back and forward through days. |
| **Parent account (Clerk)** | Optional email/password sign-in and sign-up through Clerk. If there's no network or no real key, it falls back to a local parent account. The publishable key can be changed inside the app. |
| **Themes** | Light, Dark or System theme (Dark by default). |
| **Test tools** | "Test Black Screen Now", "Test Siren Alarm Sound", "Test 5s Wall" and a "+5m Demo" usage button, so you can try the block screens without waiting for a real limit. |

---

## How it works

```
 ┌──────────────────────┐      every 30 s       ┌───────────────────────┐
 │ DetoxMonitoringService│ ───────────────────▶ │ UsageStatsManager      │
 │ (foreground service)  │  syncUsageWithSystem │ (today's minutes/app)  │
 └──────────┬───────────┘                       └───────────────────────┘
            │ writes currentUsageMinutes
            ▼
 ┌──────────────────────┐
 │ Room DB (detoxwall_  │◀───── UI (Compose) via DetoxViewModel / DetoxRepository
 │ database)            │
 └──────────┬───────────┘
            │ reads limits / schedules
            ▼
 ┌──────────────────────────┐  app switched   ┌─────────────────────────┐
 │ DetoxAccessibilityService │◀─────────────── │ Android window events   │
 └──────────┬───────────────┘                 └─────────────────────────┘
            │ decides to block
            ▼
 ┌──────────────────────────┐
 │ OverlayBlockActivity      │  → Adult wall / Child blackout / Focus lock
 └──────────────────────────┘
```

### Enforcement logic (`DetoxAccessibilityService`)

Every time the foreground app changes (`TYPE_WINDOW_STATE_CHANGED`), the service ignores DetoxWall itself, system UI, launchers and Settings. It waits 1.5 s before checking the same app again. Then it checks, **in this order**:

1. **Focus session active** and the app is restricted → **Focus lock** (`MODE_FOCUS`)
2. **A schedule is active now** and the app is restricted → **Blackout** in Child Mode, otherwise the **Adult wall**
3. **Child Mode on** and (the app's limit is exceeded **or** total child minutes ≥ the blackout limit) → **Blackout** (`MODE_BLACKOUT`)
4. **Adult mode** and the app's limit is exceeded → **Adult soft wall** (`MODE_ADULT`)

### Anti-bypass loop

- The black screen and focus screens hide the system bars (immersive mode), keep the screen on and show over the lock screen.
- **Back** doesn't close them. It logs a bypass attempt and plays the siren.
- If the child goes to Recents or Home, `onPause` logs a bypass attempt. The accessibility service then sees the blocked app come back and shows the wall again.
- Only a correct parent PIN closes the black screen. Unlocking also resets the child's total minutes.

---

## Screens

The app has a bottom navigation bar with 4 tabs, plus a top bar with a Login/Account pill, an *Adult Mode / Child Mode* badge and a menu.

| Tab | File | Contents |
|---|---|---|
| **Home** | `ui/screens/ChildModeScreen.kt` | Child Protection on/off (PIN-gated), Screen Blackout toggle + 5 min–5 hr slider & presets, siren toggle + volume, test buttons, parent PIN management, parent account, "Why DetoxWall?" |
| **App Limits** | `ui/screens/LimitsScreen.kt` | Focus Session card with a countdown, the list of protected apps with progress bars, change-limit dialog, on/off switch per app, "Add Protected App" picker, demo/test buttons |
| **Usage** | `ui/screens/UsageScreen.kt` | "Usage Insights": day picker, total screen time, chart, most-used apps (real `UsageStats` data) |
| **Schedules** | `ui/screens/SchedulesScreen.kt` | "Automated Schedules" list with add/edit/delete/toggle, weekday picker, "Active now" badge, and a **System Permissions** panel with *Grant* buttons |

**Menu (bottom sheet):** parent account, parent PIN, theme picker, feature list.

**Block screen** (`ui/blocker/OverlayBlockActivity.kt`): `ChildBlackoutScreen`, `AdultSoftBlockScreen`, `FocusLockOverlayScreen`.

---

## Tech stack

- **Language:** Kotlin (JVM target 11)
- **UI:** Jetpack Compose + Material 3, Material Icons Extended
- **Architecture:** MVVM: `DetoxViewModel` (StateFlow) → `DetoxRepository` → Room DAOs / helpers
- **Storage:** Room 2.7 (KSP), SharedPreferences
- **Async:** Kotlin Coroutines & Flow
- **Auth:** Clerk Android SDK (`com.clerk:clerk-android-api` 1.1.4)
- **Build:** Gradle (Kotlin DSL) + version catalog, AGP 9.1.1, Kotlin 2.4.10, Secrets Gradle Plugin (reads `.env`)
- **SDK:** `minSdk 24`, `targetSdk / compileSdk 36`
- **Testing:** JUnit 4, Robolectric, Roborazzi (screenshot tests), Compose UI test, Espresso
- Also included from the AI Studio template but not used by the app yet: Firebase AI / App Check, Retrofit, OkHttp, Moshi

---

## Project structure

```
app/src/main/java/com/example/
├── DetoxApplication.kt            # Application class – initializes Clerk
├── MainActivity.kt                # Scaffold, top bar, bottom nav, main menu sheet, dialogs
├── auth/
│   └── ClerkAuthManager.kt        # Clerk sign-in/up/out, local-parent fallback, cached session
├── data/
│   ├── model/                     # Room entities: AppLimit, ChildProfile, ScheduleBlock, UsageLog
│   ├── db/                        # DAOs + DetoxDatabase (v2, destructive migration)
│   └── repository/
│       └── DetoxRepository.kt     # Single source of truth, seeds default data
├── service/
│   ├── DetoxAccessibilityService.kt  # Real-time app-switch detection & blocking
│   └── DetoxMonitoringService.kt     # Foreground service, 30 s usage sync
├── receiver/
│   └── DetoxDeviceAdminReceiver.kt   # Device admin (uninstall protection in Child Mode)
├── ui/
│   ├── blocker/OverlayBlockActivity.kt  # Full-screen block walls
│   ├── components/                # AppPickerModal, EditLimitDialog, PinManagementDialogs,
│   │                              # ClerkAuthDialog, DetoxWallLogo
│   ├── screens/                   # ChildMode (Home), Limits, Usage, Schedules
│   ├── theme/                     # Colors (ObsidianBg, DetoxMint, LimitRose…), Theme, Type
│   └── viewmodel/DetoxViewModel.kt
└── util/
    ├── SecurityManager.kt         # PIN hashing, Child Mode flags, blackout/siren/theme settings
    ├── FocusSessionManager.kt     # Focus session timer (SharedPreferences)
    ├── UsageStatsHelper.kt        # UsageStats queries, permission checks, app list, seed data
    └── AlertSoundManager.kt       # Siren synthesis (AudioTrack), ringtone, tones, vibration

app/src/main/res/xml/
├── accessibility_service_config.xml
├── device_admin.xml
└── backup_rules.xml / data_extraction_rules.xml

app/src/test/java/com/example/     # Unit & Robolectric tests
```

---

## Data model

**Room database `detoxwall_database` (version 2)**

| Table | Entity | Key fields |
|---|---|---|
| `app_limits` | `AppLimit` | `packageName` (PK), `appName`, `dailyLimitMinutes`, `currentUsageMinutes`, `isRestricted`, `category`. Computed: `isLimitExceeded`, `remainingMinutes`, `progressPercent` |
| `usage_logs` | `UsageLog` | `id`, `packageName`, `dateString`, `minutesSpent`, `dayOfWeek`, `timestamp` |
| `child_profiles` | `ChildProfile` | `id`, `name`, `dailyCapMinutes`, `continuousLimitMinutes`, `bypassAttempts`, `isChildModeActive` |
| `schedule_blocks` | `ScheduleBlock` | `id`, `title`, start/end hour & minute, `daysOfWeek` bitmask (bit0 = Mon … bit6 = Sun), `isEnabled` |

**SharedPreferences**

- `detoxwall_secure_prefs`: PIN hash + salt, Child Mode flag, active profile, blackout limit (5–300 min, default 30), blackout on/off, child total minutes, siren on/off, siren volume (default 80%), theme
- `detoxwall_focus_prefs`: focus end time, duration, tag
- `detoxwall_clerk_auth_prefs`: Clerk publishable key, cached signed-in user

On first launch the repository **seeds demo data**: 5 app limits (TikTok is already at its limit, so you can see the wall right away), 7 days of sample usage logs, 2 child profiles and 2 schedules.

---

## Permissions

| Permission | Why |
|---|---|
| `PACKAGE_USAGE_STATS` (Usage Access) | Read how long each app was used today |
| `BIND_ACCESSIBILITY_SERVICE` | Detect which app is in the foreground right now |
| `SYSTEM_ALERT_WINDOW` (Display over other apps) | Show the block wall on top of other apps |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the monitoring service running ("DetoxWall Shield Active" notification) |
| `POST_NOTIFICATIONS` | Show the monitoring notification |
| `VIBRATE` | Vibrate with the siren |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Clerk authentication |
| Device Admin (`device_admin.xml`) | Uninstall protection during Child Mode |

Usage Access, Accessibility and Overlay are special permissions that the user must turn on in system settings. The **Schedules → System Permissions** panel has *Grant* buttons that open the right settings page, and the app re-checks them every time you come back (`onResume`).

---

## Getting started

### Requirements
- Android Studio (a recent version that supports AGP 9.x)
- JDK 17+ (the Gradle toolchain is resolved automatically through foojay)
- An Android device or emulator, API 24+

### Build & run
```bash
# Windows
gradlew.bat assembleDebug
gradlew.bat installDebug

# macOS / Linux
./gradlew assembleDebug
./gradlew installDebug
```
Or open the project in Android Studio and press **Run**.

### First-time setup on the device
1. Open DetoxWall → **Schedules** tab → **System Permissions** → grant **Usage Access**, **Accessibility** (enable the *DetoxWall* service) and **Display over other apps**.
2. **Home** → set a **Parent PIN**.
3. **App Limits** → add apps and set their daily limits.
4. **Home** → turn on **Child Protection** to use Child Mode (optional).

### Configuration
- **Clerk:** the app ships with a placeholder development key. To use real accounts, enter your own `pk_test_…` / `pk_live_…` key in *Login → Clerk API Key Settings*, or change `DEFAULT_PUBLISHABLE_KEY` in `ClerkAuthManager.kt`.
- **Secrets:** the Secrets Gradle Plugin reads `.env` (template: `.env.example`). `GEMINI_API_KEY` there is left over from the AI Studio template and isn't used right now.
- **Release signing:** set the `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD` environment variables (key alias `upload`). If there's a `debug.keystore` in the project root, debug builds use it.

---

## Testing

```bash
gradlew.bat test          # unit + Robolectric tests
gradlew.bat connectedAndroidTest   # instrumented tests (device required)
```

| Test file | Covers |
|---|---|
| `SecurityManagerTest` | PIN set/verify/change/remove, salting, blackout/siren/theme settings (34 tests) |
| `AppLimitTest` | `isLimitExceeded`, `remainingMinutes`, `progressPercent` edge cases |
| `FocusSessionManagerTest` | Start/stop, remaining time, expiry |
| `ScheduleBlockTest` | Day bitmask & summaries |
| `ChildProfileTest` | Default profile values |
| `GreetingScreenshotTest` | Roborazzi screenshot sample |

---

## Known limitations / notes

- **Demo data:** the seeded limits and the 7 days of usage logs are sample values. Real numbers take over once Usage Access is granted and the sync runs.
- **Database migrations** use `fallbackToDestructiveMigration()`, so any schema change wipes local data.
- **Clerk fallback:** if Clerk can't be reached, or the default placeholder key is in use, sign-in/sign-up **succeeds as a local account without checking the password**. Treat the parent account as a convenience feature, not security. The real lock is the parent PIN.
- **Device admin** is declared in the manifest, but the app doesn't yet ask the user to activate it, and `device_admin.xml` requests many more policies than uninstall protection needs.
- **Package name** is still `com.example` (application ID `com.aistudio.detoxwall.qpmxzk`), left over from the AI Studio template.
- A savvy user can still get around the block by turning off the Accessibility service in Settings. Settings is deliberately never blocked.
