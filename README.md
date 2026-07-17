# Wildlife FieldOps — Version 1.5

**Stable native Android field-operations app for wildlife-removal work.**

Branch: `Wildlife-field-Ops-version-1.5`

Version 1.5 is the practical testing line. Version 2 develops the larger commercial platform architecture.

## Platform

- Native Android only
- Kotlin and Jetpack Compose
- Room local persistence
- Hilt dependency injection
- WorkManager background work
- Supabase-connected services
- GitHub Actions APK builds
- Android 10+ (`minSdk 29`)

The supported app lives under `app/`. There is no supported web, PWA, Vite, Capacitor, or Vercel application.

## Implemented features

### Field operations

- Operations dashboard
- Job list, job creation, editing, and job details
- Customer records and customer forms
- Inspection list and inspection forms
- Scheduling
- GPS capture and high-accuracy location support
- Google Maps job display
- Photo gallery and job-photo workflows
- Route optimizer
- Offline mode and automatic synchronization controls

### Business records

- Estimates
- Invoices
- Expenses
- Inventory
- Visits
- Repairs
- Trap logs
- Reminders

### AI and diagnostics

- AI field assistant
- Configurable AI endpoint and model
- Supabase diagnostics
- Google Maps diagnostics
- Network, synchronization, offline-mode, and GPS status
- Optional weather configuration

### Android experience

- Black and graphite surfaces
- Silver and light-gray controls
- White primary text
- Rounded operational cards
- Bottom navigation and navigation drawer
- Android splash screen
- Runtime permission handling
- System, light, and dark appearance modes

## Local data models

- Jobs
- Customers
- Inspections
- Visits
- Photos
- Repairs
- Trap logs
- Expenses
- Invoices
- Reminders
- Inventory items

## Build configuration

Connected builds can use:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `GOOGLE_MAPS_API_KEY`
- `XAI_API_KEY` or `LLM_API_KEY`
- `LLM_BASE_URL`
- `LLM_MODEL`
- `OPENWEATHER_API_KEY`

A new APK must be built after repository secret changes. Older APKs retain the configuration present when they were compiled.

## Build and download

```bash
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions builds APK artifacts on supported branch pushes and through manual workflow dispatch.

## Version 1.5 role

Use this branch for device testing, field feedback, integration testing, stability work, and regression comparisons with Version 2.

## Best next upgrades

1. Shareable PDF inspection reports, estimates, and invoices.
2. Voice-to-structured job and inspection notes.
3. Sync queue visibility, retry controls, and conflict reporting.
4. Notifications for visits, traps, callbacks, and overdue invoices.
5. Photo annotations for entry points, damage, measurements, and repairs.
6. Barcode or QR inventory scanning and low-stock alerts.
7. Signed APK and Android App Bundle tester builds.
8. Automated tests for Room migrations, navigation, estimates, and synchronization.

## Version 2

Version 2 lives on branch `fix/v2-full-working`. It shares the Android foundation and adds commercial-platform models and replaceable service contracts. Version 1.5 remains the simpler stable testing line.