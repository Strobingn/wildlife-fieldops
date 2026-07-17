# Wildlife FieldOps

**Native Android field-operations platform for wildlife-removal businesses.**

This repository contains two separately installable Android product lines.

## Active versions

### Version 1.5 — stable testing line

Branch: `Wildlife-field-Ops-version-1.5`

Application ID: `com.strobingn.wildlifefieldops`

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

Branch: `phase-1-2-3-integration`

Application ID: `com.strobingn.wildlifefieldops.next`

Display name: **Wildlife FieldOps Next**

V2 installs beside V1.5 on the same Android device and uses its own package identity.

## V2 implementation status

### Phase 1 — platform foundation

Implemented:

- Separate V2 Android application identity
- Supabase authentication gate
- Organization creation and selection
- Persistent tenant context
- Organization IDs in remote DTOs
- Tenant-aware row-level-security migration
- Offline Room persistence
- Customer, job, inspection, and invoice synchronization
- Protection for unsynced local edits during cloud pulls
- Partial-sync failure reporting
- SMS/email job import with local parsing and offline job creation
- Connected-provider status screen
- GitHub Actions debug APK build

### Phase 2 — operational collaboration

In progress:

- **Organization team management backend**
  - Organization invitations
  - Seven-day invitation expiration
  - Owner/admin authorization checks
  - Invitation acceptance
  - Member role updates
  - Member removal with owner protection
  - Supported roles: owner, admin, dispatcher, technician, member
  - Android repository for listing members and invitations, inviting users, accepting invitations, changing roles, and removing members

Next Phase 2 work:

- Team-management Compose screens
- Job assignment workflows
- Technician availability and workload views
- Offline conflict detection and resolution
- Attachment and media synchronization
- Detailed sync-status dashboard
- Organization administration settings
- Provider connection actions and validation

### Phase 3 — intelligence and automation

Planned after Phase 2 reaches a tested, green-build state:

- AI-assisted field reporting
- Predictive inspection and maintenance recommendations
- Analytics dashboards
- Advanced reporting and exports
- Operational notifications
- Technician and business performance metrics
- Configurable automation rules

## V1.5 audit track

The V1.5 line is audited in parallel for:

- Build and dependency failures
- Compiler warnings and errors
- Null-safety defects
- Repository consistency
- Room migration safety
- Authentication regressions
- Synchronization reliability
- Performance bottlenecks
- Security weaknesses
- Technical debt requiring backport or isolation

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

The repository does not support a web app, PWA, Vite frontend, Capacitor wrapper, Vercel deployment, service worker, browser UI, or JavaScript application runtime.

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

## Database migrations

Apply migrations in `supabase/migrations/` in filename order. Phase 2 team management depends on the Phase 1 organization migration being applied first.

## Development rule

Every implemented feature must include a matching README status update. Build and prove commercial modules on V2, keep CI green, and backport selected stable fixes to V1.5 only after device testing.
