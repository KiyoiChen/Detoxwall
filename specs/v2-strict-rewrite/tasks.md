# DetoxWall v2 — Tasks

Legend: `[ ]` todo · `[x]` done. Each task lists the requirements it satisfies.

## Phase A — Foundation
- [x] **A1** Build cleanup: remove Clerk, Firebase, Retrofit/OkHttp/Moshi and the Secrets plugin; minSdk 26; drop INTERNET/SYSTEM_ALERT_WINDOW; add BOOT/battery permissions and `<queries>` (R4, G2)
- [x] **A2** `AppContainer` + `DetoxApplication` manual DI
- [x] **A3** `PinHasher` (PBKDF2) + `LockoutPolicy` + `PinManager` (R1–R4)
- [x] **A4** `SettingsStore` with `StateFlow<Settings>`, grants/snoozes, daily pruning
- [x] **A5** Room v3: `AppLimit`, `Schedule` (overnight-correct matching), `TamperEvent`, DAOs, `DetoxRepository` (R17, R18)
- [x] **A6** `UsageCalculator` (pure) + `UsageReader` + `InstalledApps` + `AppIconCache` (R14, R23)

## Phase B — Enforcement engine
- [ ] **B1** `Enforcer.decide()` pure decision table (R7, R14–R17)
- [ ] **B2** `ProtectedSurfaceDetector` (R9)
- [ ] **B3** `SirenPlayer` (synth siren, vibration, alarm-stream boost) (R8, R12)
- [ ] **B4** `LockOverlayController` + overlay lifecycle owner (R6)
- [ ] **B5** Overlay Compose content: DeviceLock / AppBlocked / Tamper (R7, R13, R15)
- [ ] **B6** `DetoxAccessibilityService`: event handling, 5 s ticker, bypass detection, shade dismissal, volume-key capture, time-change log, heartbeat (R6–R12, R14)
- [ ] **B7** `GuardianService` (sticky FGS) + `BootReceiver` + `DetoxDeviceAdminReceiver` (minimal policy) (R10, R11)

## Phase C — UI
- [ ] **C1** Theme (dark/light), motion kit, shared components (PinPad, ProgressRing, AppIcon, cards) (R25)
- [ ] **C2** `MainViewModel` + `DetoxApp` root (Onboarding → Gate → Tabs) (R5, R19, R20)
- [ ] **C3** Onboarding: welcome, create PIN, permission checklist (R19)
- [x] **C4** Home screen (R21)
- [x] **C5** Apps screen + picker + edit sheet (R22)
- [x] **C6** Schedules screen + editor (R17)
- [x] **C7** Insights screen (R23)
- [ ] **C8** Protect screen: child mode, blackout, siren, tamper, device admin, PIN change, permission health, log, theme (R24)

## Phase D — Quality
- [ ] **D1** Unit tests: PinHasher, LockoutPolicy, UsageCalculator, Schedule, Enforcer, ProtectedSurfaceDetector
- [ ] **D2** Robolectric: PinManager persistence/lockout; screenshot test of PinPad
- [x] **D3** `assembleDebug` + `testDebugUnitTest` green
- [ ] **D4** README rewritten for v2

## Status notes (2026-09-24)
- **Built so far:** Home, Apps, Schedules, Insights (monthly chart + per-day apps), Settings (theme, parent PIN), Permissions screen.
- **Tests:** 20 unit/Robolectric tests pass (PIN hashing + lockout, PinManager persistence, usage calculator, schedule matching).
- **Not built:** the blocking engine (Phase B services/overlay), onboarding, the parent gate. Limits, schedules and focus sessions are saved but **do not block apps yet**.
