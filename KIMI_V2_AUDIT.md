# Wildlife FieldOps — KimiV2 Branch

**Branch:** `KimiV2`  
**Base:** `main` (3437c927)  
**Audited & Fixed by:** ok-computer (AI Assistant)  
**Date:** 2026-07-17

---

## What Was Fixed

### Critical Build Fixes
| Fix | File | Before | After |
|-----|------|--------|-------|
| Compose Compiler | `app/build.gradle.kts` | `1.5.15` (non-existent) | `1.5.8` (valid) |
| Hilt Version | `app/build.gradle.kts` | `2.50` | `2.51.1` (aligned with plugin) |
| Gradle Refresh | `.github/workflows/build-android.yml` | Missing | `--refresh-dependencies` added |
| WorkManager | `app/build.gradle.kts` | Missing | `work-runtime-ktx:2.9.0` added |
| Hilt Worker | `app/build.gradle.kts` | Missing | `hilt-work:1.1.0` + `hilt-compiler:1.1.0` |

### New Features Added
| Feature | File | Description |
|---------|------|-------------|
| Inspection Scheduler | `ui/screens/InspectionSchedulerScreen.kt` | Full calendar view with date picker, type filters, severity badges, FAB |
| Background Sync | `data/sync/SyncWorker.kt` | WorkManager-based periodic sync (15 min intervals) |
| App Lifecycle | `WildlifeFieldOpsApp.kt` | HiltWorkerFactory + periodic sync scheduling |
| ProGuard Rules | `app/proguard-rules.pro` | Obfuscation rules for Room, Hilt, Supabase, Compose |
| Release Signing | `app/build.gradle.kts` | Release build type with keystore from env vars |
| Release CI | `.github/workflows/build-release.yml` | Signed APK generation workflow |

### Navigation Wiring
| Route | Screen | Status |
|-------|--------|--------|
| `dashboard` | `DashboardScreen` | ✅ Existing |
| `schedule` | `ScheduleScreen` | ✅ Existing (job calendar) |
| `inspection_scheduler` | `InspectionSchedulerScreen` | ✅ **NEW** — wired into `MainActivity.kt` |
| `ai_assistant` | `AIAssistantScreen` | ✅ Existing |
| `inspections` | `InspectionListScreen` | ✅ Existing |

### ViewModel Updates
| ViewModel | Method Added | Purpose |
|-----------|-------------|---------|
| `InspectionsViewModel` | `scheduleInspection(Inspection)` | Insert inspection from scheduler UI |

---

## What Still Needs Work

### P1 (High Priority)
- [ ] **Digital Contract & Signature** — Canvas signature pad, PDF generation, Supabase Storage upload
- [ ] **Smart Estimator Logic** — Species-based pricing matrix, regional pricing rules, quote PDF
- [ ] **Offline Sync Queue** — `PendingOperation` entity, conflict resolution, queue UI
- [ ] **Voice Dictation** — `SpeechRecognizer` integration, `RECORD_AUDIO` permission

### P2 (Medium Priority)
- [ ] **ML Kit Image Analysis** — CameraX integration, species identification, object detection
- [ ] **GPS/Map Full Implementation** — `FusedLocationProviderClient`, offline map tiles, property boundaries
- [ ] **Adaptive Icons** — Foreground/background layers, notification icons
- [ ] **Unit/UI Tests** — Compose testing, ViewModel unit tests, repository tests

### P3 (Low Priority)
- [ ] **Play Store Upload** — AAB generation, Google Play Console integration
- [ ] **Analytics** — Firebase Crashlytics, performance monitoring
- [ ] **Accessibility** — TalkBack support, content descriptions, focus order

---

## Required GitHub Secrets

For the debug build to work:
```
VITE_SUPABASE_URL
VITE_SUPABASE_ANON_KEY
VITE_GOOGLE_MAPS_API_KEY
VITE_OPENWEATHER_API_KEY
XAI_API_KEY (or LLM_API_KEY as fallback)
```

For the release build to work (additional):
```
KEYSTORE_BASE64 (base64-encoded .keystore file)
KEYSTORE_PASSWORD
KEY_ALIAS
KEY_PASSWORD
```

---

## How to Build

### Debug APK (Local)
```bash
./gradlew :app:assembleDebug
```

### Debug APK (CI)
Push to `KimiV2` branch. The `build-android.yml` workflow triggers automatically.

### Release APK (CI)
Push to `KimiV2` branch. The `build-release.yml` workflow triggers automatically.

### Release APK (Local)
```bash
export KEYSTORE_PATH=/path/to/wildlife.keystore
export KEYSTORE_PASSWORD=your_password
export KEY_ALIAS=wildlife
export KEY_PASSWORD=your_password
./gradlew :app:assembleRelease
```

---

## Branch Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable production code |
| `KimiV2` | **This branch** — AI-audited fixes + new features |
| `KimiV1.5` | Same fixes applied to V1.5 base |
| `Wildlife-field-Ops-version-1.5` | Original V1.5 (stale) |
| `platform-v2` | Redundant — identical to V1.5 |
| `fix/v2-full-working` | Redundant — identical to V1.5 |

**Recommendation:** Delete `platform-v2` and `fix/v2-full-working` after verifying `KimiV2` is stable.

---

*End of KimiV2 Documentation*
