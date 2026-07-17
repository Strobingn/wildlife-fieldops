# Wildlife FieldOps

**Native Android field-operations platform for wildlife-removal businesses.**

This repository contains two active Android product lines.

## Active versions

### Version 1.5 — stable testing line

Branch: `Wildlife-field-Ops-version-1.5`

Use Version 1.5 for daily device testing, field feedback, integration validation, stability fixes, and regression testing.

Current foundation includes:

- Dashboard
- Jobs and job details
- Customers
- Inspections
- Scheduling
- GPS and Google Maps
- Photos
- Estimates and invoices
- Expenses
- Inventory
- Route optimization
- AI field assistant
- Supabase-connected services
- Developer diagnostics
- Offline and synchronization controls
- System, light, and dark appearance modes

### Version 2 — commercial platform line

Branch: `fix/v2-full-working`

Version 2 keeps the working Android foundation and adds platform models and replaceable service contracts for future commercial modules.

Version 2 architecture includes:

- Business snapshots
- Inspection findings
- Smart estimates
- Technician scorecards
- Compliance rules
- AI inspection contracts
- Reporting contracts
- Messaging automation contracts
- Payment contracts
- Route optimization contracts
- Compliance-service contracts
- Business-intelligence contracts

Not every advanced Version 2 module is fully implemented or wired into navigation yet. Each branch README separates implemented features from scaffolded and planned work.

## Supported stack

- Kotlin
- Jetpack Compose
- Room
- Hilt
- WorkManager
- Supabase
- Google Maps
- Configurable AI services
- GitHub Actions APK builds

## Android only

The supported product is the native Android app under `app/`.

The repository no longer supports a web app, PWA, Vite frontend, Capacitor wrapper, Vercel deployment, service worker, browser UI, or JavaScript application runtime.

## Build locally

```bash
./gradlew assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build-time configuration

Connected builds can use:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `GOOGLE_MAPS_API_KEY`
- `XAI_API_KEY` or `LLM_API_KEY`
- `LLM_BASE_URL`
- `LLM_MODEL`
- `OPENWEATHER_API_KEY`

Repository secret changes require a new APK build. Existing APKs keep the configuration they were compiled with.

## Development rule

Build and prove new commercial modules on Version 2. Backport selected stable features to Version 1.5 only after they are tested.