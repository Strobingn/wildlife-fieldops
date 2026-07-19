# ChatGPTv6 Audit Report

**Branch audited:** `ChatGPTv6`  
**Fix branch:** `fix/chatgptv6-audit`  
**Date:** 2026-07-19  
**Status:** Fixes applied and pushed

---

## What was checked

- GitHub CI history for `ChatGPTv6` (debug APK build was already green)
- Local `./gradlew :app:compileDebugKotlin` (successful after wrapper fix)
- Navigation wiring, settings storage, theme, Room/Hilt, release R8, git hygiene

---

## Critical fixes

| Issue | Risk | Fix |
|-------|------|-----|
| `gradlew.bat` corrupted to GitHub "429 Too Many Requests" text | Windows local builds completely broken | Restored valid Gradle wrapper script |
| Two DataStores for the same `"settings"` file (`AppSettings.kt` + `SettingsViewModel.kt`) | Runtime crash: *multiple DataStores active for the same file* | Single shared `settingsDataStore` + `AppSettingsKeys` |
| Dark theme toggle only updated memory (`ThemeController`) | Theme reset every app restart | Persist via DataStore; restore on app start; Settings uses ViewModel |
| Job → Inspect dropped `jobId` | New inspections not linked to the job | Inspection form route accepts `jobId`; Job Detail passes it through |

## High / medium fixes

| Issue | Fix |
|-------|-----|
| Release R8 crash risk on AGP 8.5.2 (`ConcurrentModificationException`) | Pin R8 `8.6.27` in root `build.gradle.kts` (same fix as kimiswarmV5) |
| Sync Queue / Inspection Scheduler / Voice Dictation / AI Camera unreachable from menu | Added to drawer `Screen` routes |
| Contract screen existed but no entry point | Job Detail **Contract** action + typed `Screen.Contract` route |
| Version still labeled `chatgpt-v4` | Bumped to `2.4.2-chatgpt-v6-audit` (versionCode 35) |
| Weak `.gitignore` (could track secrets/build noise) | Expanded ignores for `local.properties`, APKs, build dirs, keys |
| Manifest `package=` warning on AGP 8+ | Removed obsolete `package` attribute (namespace remains in Gradle) |
| Drawer version string hard-coded | Shows `BuildConfig.VERSION_NAME` |

## CI note

- Previous `ChatGPTv6` debug workflow run succeeded on GitHub Actions (Linux uses `./gradlew`, so the broken `.bat` did not fail CI).
- New branch `fix/chatgptv6-audit` is included in `build-android.yml` push triggers.

## Remaining notes (not blocking)

- Many screens still hardcode dark palette colors (`BackgroundDark`, `TextPrimary`, …). Light theme works on redesigned Home/Inspections/Settings, but older screens stay dark-styled. Full MaterialTheme migration is a follow-up.
- Room `exportSchema = true` with no checked-in schemas: schema JSON is generated at build time under `app/schemas` (now gitignored patterns cover build output; consider committing schemas later for migration safety).
- Compile produces many deprecation warnings (AutoMirrored icons, statusBarColor setters). Cleanup only.

## Verify

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

On Windows, `gradlew.bat` must be the real wrapper (not a 200-byte rate-limit text file).
