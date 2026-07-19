# ML P0 — Field Capture (implementation)

**Branch:** `feature/ml-p0`  
**Version:** `2.5.0-ml-p0` (versionCode 36)  
**Base:** `fix/chatgptv6-audit`

## What shipped (PR-01 … PR-06)

| PR | Deliverable |
|----|-------------|
| 01 | Room v4: `VisionPrediction`, `TrainingLabel`, `CaptureSession` |
| 02 | Taxonomy + ML Kit `VisionAnalyzer` |
| 03 | Voice parsers + deterministic fusion |
| 04 | Transactional capture commit |
| 05 | Field Capture UI + navigation |
| 06 | Training label JSONL export + diagnostics + CI |

## In-app entry

- Drawer → **Field Capture**
- Home → Field Capture card / camera FAB
- Settings → Developer diagnostics → **ML / Field Capture** section + export buttons

## Design pack (full architecture)

Authoritative design docs (local F: drive):

`F:\wildlife-fieldops\design\ml-p0\`

- `DESIGN.md`, `PR-PLAN.md`, `DATA-MODELS.md`, `LABEL-TAXONOMY.md`, `IMPLEMENTATION-PLAN.md`

## Export format

JSONL lines (`TrainingLabelExportLine`):

```json
{"id":"...","photoId":"...","target":"SPECIES","labelId":"raccoon","source":"MODEL_ACCEPTED",...}
```

Share via Android share sheet (FileProvider, cache `exports/`).

## Flags

| BuildConfig | Default | Meaning |
|-------------|---------|---------|
| `ML_TFLITE_ENABLED` | false | Custom TFLite path reserved |
| `ML_CLOUD_VLM_ENABLED` | false | Cloud vision escalate reserved |

## Build

```bash
./gradlew :app:assembleDebug
```

## Next (not P0)

- Quote/margin model, trap ranker, entry BOM (P1)
- TFLite custom model + OTA (P3)
