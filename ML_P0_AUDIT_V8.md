# ML P0 + Kimi V8 — Audit Reconciliation

Branch: `fix/ml-p0-audit-V8` (based on `feature/ml-p0`)
Version: `2.5.3-ml-p0-audit-v8` (versionCode 39)

This branch reconciles the two divergent fix lines that grew out of `ChatGPTv6`:

- `feature/ml-p0` — ML capture features, taxonomy/vision pipeline, the Compose BOM hover fix, CameraX 16 KB fix, and the partial audit set from `4369db5` (DataStore, theme, navigation, inspector search).
- `Kimi_V7` — the full 30-fix audit (round 1: `dc04548`, round 2: `da79f81`).

## What was applied from Kimi_V7

Both audit commits were cherry-picked and conflicts resolved in favor of the ml-p0 implementation where both sides fixed the same thing (theirs was equal or better):

| Area | Resolution |
|---|---|
| DataStore consolidation | ml-p0's kept (shared `AppSettingsKeys`, stronger) |
| Theme persistence | ml-p0's kept (`setDarkTheme` updates `ThemeController` + DataStore) |
| Navigation wiring | ml-p0's kept (`Screen.Contract` route object, `MlKitCamera`, `FieldCapture`); my duplicate declarations removed |
| Voice dictation result | **mine kept** — transcription now returns via `savedStateHandle`; ml-p0 discarded it |
| Room migrations | **conflict-resolved, see below** |
| Everything else (GPS, signature pad, route optimizer, sync queue, warranty persistence, scheduler, ML Kit camera, locale fixes, etc.) | applied cleanly — ml-p0 had no competing changes |

**Skipped deliberately:**
- `87fdcc5` (Activity-level `ACTION_HOVER_EXIT` workaround) — redundant: ml-p0's BOM upgrade to `2024.12.01` (compose-ui 1.7.6) is the proper fix.
- `README_KIMI_V7.md` — superseded by this document.

## Room version conflict — resolved

Both lines had independently bumped the DB to **version 4 with different schemas**:

- `Kimi_V7` v4: `ALTER TABLE jobs ADD COLUMN warrantyMonths`
- `feature/ml-p0` v4: capture tables (`vision_predictions`, `training_labels`, `capture_sessions`, photo ML columns)

Same version number + different schemas = guaranteed crash when installing one line's build over the other. Resolution on this branch:

- ml-p0's v4 (capture schema) stays as `MIGRATION_3_4` — unchanged, so existing ml-p0 installs (DB v4 with capture tables) keep working.
- `warrantyMonths` moved to **`MIGRATION_4_5`**; DB version is now **5**.
- `MIGRATION_1_2` (conditional `inspections.notes` add, handles both v1 flavors in the wild) is now registered — the v1-upgrade startup crash is fixed on this line too.
- All four migrations registered in `AppModule`: `1_2, 2_3, 3_4, 4_5`.

**Note for upgrades:** a device that ran a `Kimi_V7` build (DB v4 with `warrantyMonths` but no capture tables) cannot migrate cleanly to this branch's v5 (`MIGRATION_3_4` would try to re-add capture tables and the v4→v5 step only adds `warrantyMonths`, which already exists). If any Kimi_V7 build was ever installed on a test device, uninstall it (or clear app data) before installing this build. Devices coming from ml-p0 v4 or any v1–v3 install migrate cleanly.

## Fixes this brings to the ml-p0 line (previously missing)

Crash/data-loss class:
- `MIGRATION_1_2` — startup crash for v1-DB upgrades
- `error("Job not found")` crash when a job is deleted mid-edit
- GPS my-location layer enabled without permission (`SecurityException`)
- Save-then-pop race could cancel inspection writes (`persistenceScope`)

Wrong-data class:
- Dashboard "today" in local time, not UTC
- GPS distance in feet (was meters labeled ft); stale point after Clear/Stop
- Job search + status filter now compose; route optimizer excludes cancelled/invoiced jobs, keeps optimized order, camera follows route
- Contract warranty persisted (`jobs.warrantyMonths`); scheduler keeps address in notes and filters to upcoming
- Sync queue wired end-to-end (failed pushes recorded, retried first on next sync, cleared on success)

UX class:
- Signature strokes render while drawing
- ML Kit shutter button wired to `ImageCapture`; analyzer executor leak fixed
- Voice dictation returns text to job notes (mic button on Notes field)
- Contract PDF result snackbar; duplicate-job guard; loading state in Sync Queue
- All 39 `DefaultLocale` warnings (`Locale.US` on every `String.format`)

## Verification

- `./gradlew :app:assembleDebug :app:lintDebug` — see build result in the commit message / CI.
- Hover crash: covered by ml-p0's BOM `2024.12.01` (compose-ui 1.7.6).
