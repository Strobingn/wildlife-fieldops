# Job Detail House Map — Design Spec

**Date:** 2026-07-19  
**Repo:** `Strobingn/wildlife-fieldops`  
**Branch target:** `feature/ml-p0` (or current main work branch)  
**Status:** Approved for implementation planning  

## Goal

When a user opens a job (Job Detail), show a **small Google Map** of the **house / service location** so techs can orient themselves without leaving the job page. Tap expands the in-app map; a button opens the full Google Maps app for turn-by-turn.

## Decisions (locked)

| Decision | Choice |
|----------|--------|
| Placement | **A — Inline map card** always visible under Customer Information on Job Detail |
| Missing coords | **Geocode** the job address string when lat/lng are null |
| Tap behavior | **Both** — tap expands the in-app map; separate **Open in Google Maps** button |
| Tech stack | **Maps Compose** (`GoogleMap` + marker), same as existing Property Map (`MapScreen`) |

## Non-goals

- Route optimization or multi-stop routing from this card  
- Editing job location from the map (use Job Form / GPS flows for that)  
- Reverse-geocode-only jobs with no address (still show pin if coords exist)  
- Static Maps image API or WebView embed  

## UI

### Location on Job Detail

Insert a **House location** card immediately after the existing **Customer Information** card (`JobDetailScreen`).

```
[ Title / status / priority / type ]
[ Customer Information ]
[ House location ]          ← NEW
[ Job Details ]
[ Notes / AI / actions … ]
```

### Card contents

1. **Title:** “House location”
2. **Map viewport**
   - Collapsed height: **180.dp**
   - Expanded height: **320.dp**
   - Full width inside card padding
   - Rounded corners consistent with other cards (`RoundedCornerShape(12.dp)` or map clip)
   - Map type: **Hybrid** (match `MapScreen`) or Normal if hybrid is too dark in card — prefer Hybrid for house roofs
3. **Marker:** single pin at resolved house `LatLng`
4. **Camera:** center on pin, zoom **16.5f** collapsed / **17.5f** expanded
5. **Address caption** under map (job.address, or “Location pin only” if address blank but coords exist)
6. **Actions row**
   - Primary text button: **Open in Google Maps**
   - When expanded: secondary **Collapse** control (or tap map toggles expand/collapse)
7. **Empty / error states**
   - No address and no coords → message: “No location on this job” (no map)
   - Geocoding in progress → map placeholder + CircularProgressIndicator
   - Geocode failure → show address text + **Open in Google Maps** (address query intent); no live map
   - Maps API key missing → soft message; still offer Open in Google Maps intent

### Interaction

| Gesture | Behavior |
|---------|----------|
| Tap map body | Toggle expand / collapse |
| “Open in Google Maps” | External intent (see below) |
| Pinch zoom / pan | Enabled when **expanded** only; disabled or minimal when collapsed to avoid scroll conflicts |

Scroll note: Job Detail is a vertical `Column` + `verticalScroll`. Collapsed map should use `MapUiSettings` with scroll/rotate gestures **off** so the page scrolls cleanly. Expanded may enable scroll gestures.

## Data resolution

Resolve a display `LatLng` in this order:

1. If `job.latitude` and `job.longitude` are both non-null → use them (source = `coords`)
2. Else if `job.address` is non-blank → **forward geocode** address (source = `geocode`)
3. Else → no location

### Geocoding

- Use Android `android.location.Geocoder` (already used in `MapViewModel` and `FieldCaptureViewModel`)
- Prefer existing patterns: check `Geocoder.isPresent()`, run on `Dispatchers.IO`, limit 1 result
- On API 33+, use the async `Geocoder.getFromLocationName` callback API if the project already targets that; otherwise match current `MapViewModel` style for consistency
- Cache result per job id in the composable/`remember` + ViewModel state for the screen lifetime (do not re-geocode on every recomposition)
- **Do not** require writing geocoded lat/lng back to Room in this PR (optional follow-up). Display-only is enough for P0

## Open in Google Maps

Build an `Intent` that works with or without the Google Maps package:

1. Prefer `geo:lat,lng?q=lat,lng(label)` when coords known  
2. Else `geo:0,0?q=Uri.encode(address)`  
3. `Intent.ACTION_VIEW` + `resolveActivity` / `startActivity`; if nothing handles it, toast “No maps app found”

Label = `job.customerName` or first line of address.

## Architecture / components

### New composable

`ui/components/JobLocationMapCard.kt` (or similar)

```text
JobLocationMapCard(
  address: String,
  latitude: Double?,
  longitude: Double?,
  customerLabel: String = "",
  modifier: Modifier = Modifier
)
```

Responsibilities:

- Local UI state: `expanded`, geocode loading/error, resolved `LatLng?`
- Host `GoogleMap` with `CameraPositionState` updated when resolved point changes
- Open-Maps button via `LocalContext`
- Self-contained so Job Detail stays readable

### JobDetailScreen

After Customer Information `InfoCard`, call:

```kotlin
JobLocationMapCard(
  address = currentJob.address,
  latitude = currentJob.latitude,
  longitude = currentJob.longitude,
  customerLabel = currentJob.customerName
)
```

Optional: hide GPS raw lat/lng `InfoRow` if redundant once map is present (keep if useful for support; **keep** for P0).

### Dependencies

Already present — no new libraries:

- `com.google.android.gms:play-services-maps`
- `com.google.maps.android:maps-compose`
- Manifest `com.google.android.geo.API_KEY` via `GOOGLE_MAPS_API_KEY`
- INTERNET permission already declared

### Permissions

- Map display of a fixed lat/lng does **not** require runtime location permission
- Do **not** enable `isMyLocationEnabled` on this mini-map (avoids permission prompts on Job Detail)

## Error handling

| Case | UX |
|------|-----|
| No data | Static empty card message |
| Geocode throws / empty list | Caption + Open in Maps with address query |
| Map fails to render (key) | Gray placeholder + Open in Maps |
| Intent fails | Snackbar / Toast |

## Testing

1. **Manual**
   - Job with lat/lng → pin correct, expand/collapse works, Open in Maps opens app
   - Job with address only → geocode places pin (online device)
   - Job with neither → empty state
   - Scroll Job Detail with collapsed map without gesture fights
2. **Unit (light)**
   - Pure helper: `resolveLocationSource(lat, lng, address)` priority logic if extracted
3. **CI**
   - Existing `assembleDebug` / unit tests still green; maps key may be empty in CI (card must not crash)

## File touch list (expected)

| File | Change |
|------|--------|
| `ui/components/JobLocationMapCard.kt` | **New** mini-map card |
| `ui/screens/JobDetailScreen.kt` | Insert card under customer info |
| Optional small util for geo intent | If not inlined |

No Room schema change. No navigation change.

## Acceptance criteria

- [ ] Opening any job with location shows a small Google Map with a house pin  
- [ ] Missing coords but present address geocodes and shows pin when Geocoder succeeds  
- [ ] Tap toggles ~180dp ↔ ~320dp  
- [ ] “Open in Google Maps” launches external maps to that house  
- [ ] No location data does not crash; shows clear empty state  
- [ ] Page scroll still works with collapsed map  
- [ ] Debug build installs and Job Detail remains usable without Maps key (degraded UX OK)

## Out of scope / follow-ups

- Persist geocoded coordinates onto `Job` after first success  
- Cluster / multi-job mini-map  
- Directions from current GPS to house inside FieldOps  

## Implementation note

Next step after user confirms this written spec: implementation plan (`writing-plans`), then code on `feature/ml-p0`.
