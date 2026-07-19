# Kimi V7 — Audit & Fix Notes

Branch: `Kimi_V7` (based on `ChatGPTv6`)

This document breaks down the full audit of the `ChatGPTv6` branch and every fix applied in `Kimi_V7`.

- **Round 1** (`dc04548`): audit + 30 runtime/logic fixes across 28 files.
- **Round 2**: closed the remaining feature gaps (offline sync queue, voice dictation entry point) and cleared all 39 `DefaultLocale` lint warnings.
- **Round 3**: workaround for the Compose framework crash `IllegalStateException: The ACTION_HOVER_EXIT event was not cleared` (`AndroidComposeView.sendHoverExitEvent`), seen in device logcat when a mouse/stylus hover sequence is interrupted (e.g. crossing the Compose ↔ embedded Google Map boundary). `MainActivity.dispatchGenericMotionEvent` now consumes hover-exit events before they reach the broken Compose code path; hover enter/move and all touch input are unaffected. The permanent fix is a Compose BOM upgrade — deferred, since it cascades through the whole UI stack and needs device testing.

## How the audit was run

1. **Compile check** — `:app:assembleDebug`: passed clean. No compile errors existed on `ChatGPTv6`.
2. **Lint check** — `:app:lintDebug`: 0 errors, 97 warnings (non-blocking: `DefaultLocale`, `UnusedResources`, etc.).
3. **JS syntax check** — all root/tooling JS files parsed with `node --check`.
4. **Full code review** — all 43 Kotlin files changed vs `main` were read end-to-end and cross-checked against the files they touch (DAOs, entities, NavHost, manifest, resources, gradle). Every finding below was verified against the actual code (and git history where relevant) before being fixed.

Result: the branch compiled, but had **30 runtime/logic bugs** — crashes, silent data loss, and dead features.

---

## Crash fixes

| # | Bug | Fix | Files |
|---|-----|-----|-------|
| 1 | **Settings screen crashed on every open.** Two separate `preferencesDataStore(name = "settings")` delegates existed (`data/preferences/AppSettings.kt` and a private one in `SettingsViewModel`). The app activates the first at startup, so constructing `SettingsViewModel` threw `IllegalStateException: multiple DataStores active for the same file`. | Deleted the private delegate; `SettingsViewModel` now uses the shared `settingsDataStore`. | `ui/viewmodel/SettingsViewModel.kt` |
| 2 | **Startup crash upgrading from DB v1.** Room was at version 3 with only `MIGRATION_2_3` registered and no destructive fallback — any device with a v1 database crashed on first DB access. Git history showed v1 shipped in **two flavors**: early builds without `inspections.notes`, and later builds with it (added in `90b7dc4` without a version bump). | Added `MIGRATION_1_2` that checks `PRAGMA table_info(inspections)` and only adds the `notes` column when missing, so both v1 flavors migrate safely. Registered in `AppModule`. | `data/local/AppDatabase.kt`, `di/AppModule.kt` |
| 3 | **Crash when a job was deleted mid-edit.** `JobsViewModel.updateJobDetailsNow` used `?: error("Job not found")` inside a SupervisorJob+IO coroutine with no exception handler — the exception hit the uncaught-exception handler and killed the process. | Replaced with a safe early `return`. | `ui/viewmodel/JobsViewModel.kt` |
| 4 | **GPS screen could crash without location permission.** `isMyLocationEnabled = true` was set unconditionally (`SecurityException` for users who denied/revoked location), the declared `hasLocationPermission` state was never set, and the `SecurityException` from `requestLocationUpdates` was swallowed so tracking silently never started. | Permission is now actually checked (FINE or COARSE), the my-location layer/button is gated on it, and Start Tracking launches a runtime permission request when needed. | `ui/screens/GPSScreen.kt` |

## Data & logic fixes

| # | Bug | Fix | Files |
|---|-----|-----|-------|
| 5 | **Dark theme choice never persisted.** The Settings toggle wrote only to the in-memory `ThemeController` (always starts dark), while the ViewModel's `dark_theme` DataStore plumbing was dead code. Light theme reverted on every cold start. | Toggle now writes DataStore via `viewModel.setDarkTheme` *and* updates `ThemeController`; the app restores the persisted value at startup. | `ui/screens/SettingsScreen.kt`, `WildlifeFieldOpsApp.kt` |
| 6 | **"Jobs today" computed in UTC.** `dayStart = now - (now % 86400000)` is UTC midnight — for US timezones the "today" window was off by 4–8 hours. | Day boundaries now use `LocalDate.now().atStartOfDay(ZoneId.systemDefault())`. | `ui/viewmodel/DashboardViewModel.kt` |
| 7 | **GPS distance shown in the wrong unit.** `Location.distanceBetween` returns meters; the total was displayed labeled "ft" (~3.3× too large). | Accumulate `meters * 3.28084`; label stays "ft". | `ui/screens/GPSScreen.kt` |
| 8 | **Stale location inflated distance after Clear/Stop.** Reset paths cleared points/distance but not `currentLocation`, so the next fix measured from the stale point. | `currentLocation = null` in both reset paths. | `ui/screens/GPSScreen.kt` |
| 9 | **Job status filter ignored while searching.** `when { query -> search; status -> getByStatus; else -> getAll }` let search win; the UI shows search and status chips together. | Search/all from the DAO, then apply the status filter on top — both compose. | `ui/viewmodel/JobsViewModel.kt` |
| 10 | **Inspection search ignored inspector names**, despite the placeholder "Search customer, species or inspector". | Added `inspectorName` to the filter. | `ui/viewmodel/InspectionsViewModel.kt` |
| 11 | **Route optimizer routed cancelled jobs** (filter excluded COMPLETED and PAID only; screen text says only *open* jobs can be routed). | Also excludes `CANCELLED` and `INVOICED`. | `ui/viewmodel/RouteOptimizerViewModel.kt` |
| 12 | **Route map camera never moved to the route** — it was initialized once, while the job list was still empty, and stayed on the hardcoded fallback location. | `LaunchedEffect(firstPoint)` moves the camera when the first stop changes. | `ui/screens/RouteOptimizerScreen.kt` |
| 13 | **Optimized route silently reset on any DB write** — `LaunchedEffect(jobs)` re-fired on every Room re-emission (e.g. background sync), discarding the optimized order. | Effect is keyed on the job IDs instead of the list instance. | `ui/screens/RouteOptimizerScreen.kt` |
| 14 | **Inspections created from a job lost the job link.** `JobDetailScreen` passed the job id, but `MainActivity` dropped it and the route had no `jobId` argument — `jobId` was always saved as `""`, permanently breaking job↔inspection queries. | `inspection_form` route takes an optional `jobId` param, passed through from Job Detail to the form's `prefilledJobId`. | `navigation/Screen.kt`, `MainActivity.kt`, `ui/screens/InspectionFormScreen.kt` |
| 15 | **Scheduler dialog discarded the typed address** (no address column on `Inspection`). | Address is merged into the inspection's notes field. | `ui/screens/InspectionSchedulerScreen.kt` |
| 16 | **"Upcoming Inspections" showed past inspections, oldest first**; a dead `selectedDate` state existed with a stale comment. | List filters to `inspectionDate >= now`; dead state removed. | `ui/screens/InspectionSchedulerScreen.kt` |
| 17 | **Contract warranty silently dropped on save** — the screen collected "Warranty (mo)" but `saveContract` never persisted it (the `Job` entity had no column). | Added `Job.warrantyMonths` (DB v3→v4, `MIGRATION_3_4`), persisted in `saveContract`, and the contract form prefills it from the job. | `data/model/Job.kt`, `data/local/AppDatabase.kt`, `ui/viewmodel/ContractViewModel.kt`, `ui/screens/ContractScreen.kt` |
| 18 | **Voice dictation result discarded** — the NavHost handler popped back without using the transcription. | Transcription is handed back via the previous back-stack entry's `savedStateHandle` (`"voice_transcription"`). | `MainActivity.kt` |
| 19 | **Cancellation turned into retry.** `SyncWorker` (and `SyncRepository.syncAll`) caught `Exception`/`Throwable` including `CancellationException`, so WorkManager-cancelled syncs kept running and ended in `Result.retry()`. | `CancellationException` is rethrown in both. | `data/sync/SyncWorker.kt`, `data/repository/SyncRepository.kt` |
| 20 | **Full-resolution photo decode on the main thread** in species ID (`InputImage.fromFilePath` inside `viewModelScope`) — ANR risk. | Moved onto `Dispatchers.IO`. | `ui/viewmodel/SpeciesIdViewModel.kt` |
| 21 | **PDF generation on the main thread** — iText layout + file I/O froze the UI for the whole render. | Wrapped in `withContext(Dispatchers.IO)`. | `ui/viewmodel/ContractViewModel.kt` |
| 22 | **Save-then-pop race on inspections** — the form popped back immediately after calling save, clearing the screen-scoped ViewModel and racing the Room write. | `InspectionsViewModel` now uses the same app-surviving `persistenceScope` pattern `JobsViewModel` already had. | `ui/viewmodel/InspectionsViewModel.kt` |
| 23 | **New Room query on every recomposition** — `getJobById(jobId)` / `getInspectionById(...)` were called inline in composition, so every keystroke restarted the DB query (Estimate screen, Inspection form). | Flows are wrapped in `remember(id)`. | `ui/screens/EstimateScreen.kt`, `ui/screens/InspectionFormScreen.kt` |

## UI / UX fixes

| # | Bug | Fix | Files |
|---|-----|-----|-------|
| 24 | **Signature pad didn't render strokes while drawing** — points were appended by mutating a plain list, invisible to Compose snapshots; the pad lagged one stroke behind. | Strokes now use snapshot-observable lists; the in-progress stroke renders live; Clear/Capture include it. | `ui/components/SignaturePad.kt` |
| 25 | **ML Kit camera shutter button did nothing** (`onClick = { /* Capture photo */ }`, `onPhotoCaptured` never invoked). | Wired a real `ImageCapture` use case; the shutter saves a photo and invokes `onPhotoCaptured(path, labels, objects)`. | `ui/screens/MLKitCameraScreen.kt` |
| 26 | **Analyzer executor leaked per camera bind** (`Executors.newSingleThreadExecutor()` created in the bind path, never shut down). | One remembered executor, shut down in `DisposableEffect`. | `ui/screens/MLKitCameraScreen.kt` |
| 27 | **Duplicate jobs on double-tap** — "Create Job" stayed enabled after a successful create. | Button is disabled once created. | `ui/screens/VoiceJobScreen.kt` |
| 28 | **No feedback after generating a contract PDF** — result state was produced but never collected. | Snackbar shows the saved path; the button disables and shows progress while generating. | `ui/screens/ContractScreen.kt` |
| 29 | **Dead loading state in Sync Queue** (`isLoading` hardcoded `false`, so the empty state flashed before the first DB emission). | Loading now starts true and clears on first emission. | `ui/viewmodel/SyncQueueViewModel.kt` |
| 30 | **Unreachable features**: Contract, Sync Queue, Inspection Scheduler, and AI Camera were registered in the NavHost but nothing navigated to them. | "Contract" action button on Job Detail; "Sync Queue" row in Settings; "AI Camera" and "Inspection Scheduler" drawer items. | `MainActivity.kt`, `navigation/Screen.kt`, `ui/screens/JobDetailScreen.kt`, `ui/screens/SettingsScreen.kt` |

## Tooling fix

- `tools/patch-main-js-ai.js` was not valid JavaScript: template-literal backslashes were double-escaped (`\\\`` + backtick terminates the literal early). Fixed the 4 escape sequences; `node --check` passes.

---

## Verification

- `./gradlew :app:assembleDebug :app:lintDebug` → **BUILD SUCCESSFUL**, 0 lint errors.
- All fixes verified by re-reading the final files and reviewing the complete diff (28 files, +311/−142).

---

## Round 2 — Gap closure

### Offline sync queue wired end-to-end

The `pending_operations` table, DAO, retry/backoff machinery, and Sync Queue screen existed but were never populated — sync went straight to Supabase and failures were invisible. Now (`data/repository/SyncRepository.kt`, `data/local/PendingOperationDao.kt`):

- **On push failure**: each entity in the failed batch is recorded as a `PendingOperation` (deduped by entity type + id, preserving retry history), visible in Settings → Sync Queue with the error message.
- **On the next sync**: queued operations are processed first, before the regular push/pull. Each re-reads the entity from Room (freshest data), upserts it, marks it synced, and leaves the queue on success. Failures bump the retry count (`markFailed`); stuck `isProcessing` flags from killed runs are reset each pass.
- **Manual retry**: the screen's Retry button (shown after 5 exhausted retries) resets the count so the next sync re-attempts.
- Successful regular pushes also clear any queued operations for those entities.
- Supports JOB, CUSTOMER, INSPECTION, INVOICE — the four entity types that have push paths.

### Voice dictation entry point

`voice_dictation` was registered in the NavHost but unreachable, and its result was discarded. Now:

- The Job form's **Notes** field has a mic button that opens the dictation screen (`ui/screens/JobFormScreen.kt`, `MainActivity.kt`).
- The transcription returns via the back-stack `savedStateHandle` (round 1 fix) and is appended to Notes; the handle is cleared after consumption so it doesn't re-apply.
- Mic permission is requested by `VoiceDictationScreen` itself.

### Lint: `DefaultLocale` cleared

All 39 locale-less `String.format(...)` calls across 14 files now pass `Locale.US` explicitly (default-locale formatting breaks numbers in comma-decimal locales). Lint warnings went **97 → 56**.

## Remaining warnings & notes (deliberate scope)

- **56 lint warnings remain**, all non-blocking: 27 `GradleDependency` (newer library versions available — deferred, upgrades need testing), 10 `UnusedResources`, 7 launcher-icon cosmetics, 2 `ObsoleteSdkInt`, 2 informational.
- **`EstimateViewModel` is unused** (EstimateScreen works through `JobsViewModel`/`JobAiViewModel`). Harmless dead code, kept as-is.
