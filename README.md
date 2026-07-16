# Wildlife FieldOps — Android

Native Android application for wildlife-removal field operations.

## Platform scope

This repository's supported product is the Android application under `app/`.

- Android only
- Kotlin
- Jetpack Compose
- Room for local persistence
- Supabase integration for connected services
- GitHub Actions APK builds

Any older JavaScript, Capacitor, or web-oriented files in repository history are legacy material and are not part of the supported V1 Android product.

## V1 appearance

The `dark-theme` branch uses the approved Wildlife FieldOps visual direction:

- Black and graphite surfaces
- Light-gray and silver controls
- White primary text
- Dense sans-serif typography
- Rounded operational cards
- Monochrome dashboard and navigation styling

The app follows the Android device appearance by default through `AppThemeMode.SYSTEM`. The theme engine also supports explicit `LIGHT` and `DARK` modes for a future settings selector.

## Build

```bash
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Every push to `dark-theme` triggers the Android APK workflow. The resulting APK is available from the GitHub Actions run artifacts.

## Configuration

Build-time environment variables may include:

- `XAI_API_KEY` or `LLM_API_KEY`
- `LLM_BASE_URL`
- `LLM_MODEL`
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `OPENWEATHER_API_KEY`
- `GOOGLE_MAPS_API_KEY`

Never commit production secrets.
