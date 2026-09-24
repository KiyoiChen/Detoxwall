# DetoxWall v2 — Design

## 1. Architecture

```
                         ┌───────────────────────── process: com.aistudio.detoxwall ─────────────────────────┐
  Android system         │                                                                                     │
  ──────────────         │   AppContainer (manual DI, created in DetoxApplication)                             │
  UsageStatsManager ◀────┼── UsageReader ──▶ UsageCalculator (pure)                                            │
  Window events ─────────┼─▶ DetoxAccessibilityService ──▶ Enforcer.decide() (pure) ──▶ LockOverlayController  │
  Key events ────────────┼─▶        │  ▲                          ▲                        (TYPE_ACCESSIBILITY_ │
  Window content ────────┼─▶ ProtectedSurfaceDetector (pure)      │                          OVERLAY + Compose) │
                         │          │  │                          │                                          │
                         │          ▼  │                 SettingsStore (prefs, StateFlow)                    │
                         │   SirenPlayer                 PinManager (PBKDF2 + lockout)                       │
                         │                               DetoxRepository ──▶ Room (limits, schedules, events)│
                         │                                                                                     │
  BOOT_COMPLETED ────────┼─▶ BootReceiver ──▶ GuardianService (sticky foreground, notification)              │
                         │   MainActivity ──▶ MainViewModel ──▶ Compose UI (Onboarding / Gate / 5 tabs)       │
                         └─────────────────────────────────────────────────────────────────────────────────────┘
```

**The accessibility service is where enforcement happens.** The system binds it, so swiping the app from Recents doesn't kill it. Its overlay windows sit above every app and above Recents, and no Activity lifecycle can remove them. That is what fixes P1.

## 2. Key decisions

| Decision | Choice | Why |
|---|---|---|
| Block surface | `TYPE_ACCESSIBILITY_OVERLAY` window with a `ComposeView` | Can't be dismissed by Home/Back/Recents, needs no overlay permission |
| Block UX | Kick to Home (`GLOBAL_ACTION_HOME`) and then show the card | Pauses the blocked app so its usage stops counting and its media stops |
| Input on overlay | Custom on-screen PIN pad; window is `FLAG_NOT_FOCUSABLE` | No IME needed, can't be dismissed through the keyboard |
| PIN storage | PBKDF2-HMAC-SHA256, 120k iterations, salt, constant-time compare, in private prefs | Standard KDF, needs no network, and `allowBackup=false` keeps it off backups |
| Usage source | `UsageStatsManager.queryEvents` since midnight → `UsageCalculator` | Accurate for the session still in progress; the daily buckets from `queryUsageStats` lag |
| Evaluation | A 5 s ticker **and** every window change | Fixes P4 |
| Decision logic | Pure `Enforcer.decide(EnforcementInput)` | Fully unit-testable decision table |
| DI | Manual `AppContainer` | Small app, no Hilt/Koin overhead |
| Min SDK | 26 | Needed for PBKDF2-HMAC-SHA256 and adaptive notifications |
| DB | Room v3, destructive migration | Schema rewritten; v1 data was mostly seed data |

## 3. Enforcement decision table (`Enforcer.decide`)

Checked top to bottom; the first match wins. The *foreground* is the last app package from a window event (DetoxWall, SystemUI and input methods are ignored).

| # | Condition | Decision |
|---|---|---|
| 1 | Child Mode ∧ tamper protection ∧ no maintenance window ∧ the screen is protected | `Tamper` |
| 2 | Foreground is the default dialer | `None` (emergency, R13) |
| 3 | Child Mode ∧ (manual lock ∨ blackout used ≥ allowance ∨ active *lock-device* schedule) | `LockDevice(reason)` |
| 4 | Foreground app is limited ∧ enabled ∧ focus active | `BlockApp(Focus)` |
| 5 | Foreground app is limited ∧ enabled ∧ active *block-apps* schedule | `BlockApp(Schedule)` |
| 6 | Foreground app is limited ∧ enabled ∧ used ≥ limit + granted extra time | `BlockApp(Limit)` |
| 7 | otherwise | `None` |

*Blackout used* = today's total screen time (all apps except DetoxWall and launchers) − the baseline recorded at the last PIN unlock today.

## 4. Protected screen detection (`ProtectedSurfaceDetector`)

Input: foreground package + up to 400 visible texts from `rootInActiveWindow`.

- Package-installer packages (`com.android.packageinstaller`, `com.google.android.packageinstaller`, `com.samsung.android.packageinstaller`, …) whose text contains the app label → `UNINSTALL`.
- Settings-like packages (`com.android.settings`, `com.samsung.android.settings…`, `com.miui.securitycenter`, `com.coloros.safecenter`, …) whose text contains the app label → `SETTINGS`.
- Anything else → not protected.

It is checked on `WINDOW_STATE_CHANGED` and throttled on `WINDOW_CONTENT_CHANGED` (at most every 400 ms, and only for those packages).

## 5. Overlay states

```kotlin
sealed interface OverlayState {
  data class DeviceLock(reason)                        // full black, PIN pad, emergency call
  data class AppBlocked(pkg, label, reason, strict)    // card: countdown → snooze / ask parent (PIN → +time)
  data object Tamper                                   // "Protected setting" + PIN → maintenance window
}
```

`LockOverlayController` adds the window the first time it is needed, animates the content in and out (`AnimatedContent`), and removes the window 350 ms after the state becomes `null`. The `ComposeView` gets its own `LifecycleOwner` and `SavedStateRegistryOwner`.

## 6. Persistence

- **Room `detoxwall_v3.db`:** `app_limits(packageName PK, appName, dailyLimitMinutes, enabled, addedAt)`, `schedules(id, name, startMinute, endMinute, daysMask, lockDevice, enabled)`, `tamper_events(id, timestamp, type, detail)`.
- **`SettingsStore` (`detox_settings` prefs, exposed as `StateFlow<Settings>`):** onboarding done, child mode, tamper protection, blackout enabled/limit/baseline/date, manual lock, siren enabled/volume, theme, focus end/label, maintenance-until, heartbeat. Grants and snoozes are stored per `date:pkg` key and pruned on each new day.
- **`PinManager` (`detox_pin` prefs):** hash, salt, iterations, failed count, lockout-until.

## 7. Anti-tamper mechanisms

| Threat | Countermeasure |
|---|---|
| Home / Recents / Back on the block screen | The overlay isn't tied to an activity. The bypass is detected (launcher shows while locked, outside our own 1.5 s action window) → logged + siren |
| Notification shade / quick settings while locked | `GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE` (API 31+) / `BACK` |
| Force stop / clear data / disable accessibility | Tamper detection on Settings screens that mention DetoxWall |
| Uninstall | Tamper detection on the installer + Device Admin |
| Swipe from Recents | Enforcement runs in the system-bound accessibility service; the guardian FGS uses `stopWithTask=false` |
| Reboot | Lock state is persisted; `BootReceiver` restarts the guardian; the accessibility service auto-binds |
| Muting the siren | Volume keys consumed via `flagRequestFilterKeyEvents`; alarm stream boosted |
| PIN guessing | Exponential lockout, persisted |
| Changing limits inside the app | Parent Gate in Child Mode, re-locks on `onStop` |
| Changing the clock | `TIME_SET` / `TIMEZONE_CHANGED` logged as tamper events |

## 8. UI

- **Theme:** "Obsidian" dark (default) and "Porcelain" light color schemes. Brand colors: Mint `#2DD4BF` (primary), Indigo `#818CF8` (secondary), Rose `#FB7185` (error/alarm), Amber `#FBBF24` (warning). All text uses `colorScheme` roles, never hardcoded white.
- **Motion kit (`ui/motion`):** `Modifier.pressScale()` (spring), `AnimatedCounter`, `ProgressRing` (animated sweep), staggered `animateItem()`, `PinPad` with dot pop and shake-on-error, pulsing shield, bars that grow in sequence.
- **Navigation:** a single `DetoxApp` composable → `Onboarding` | `ParentGate` | `MainScaffold(5 tabs)`, with `AnimatedContent` transitions between tabs.
