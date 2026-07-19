# Wildlife FieldOps — ChatGPTv6 Audit Fix Branch

**Branch name:** `fix/chatgptv6-audit`  
**Based on:** `ChatGPTv6`  
**Date:** July 19, 2026  

This branch is a **bug-fix pass** on the ChatGPTv6 redesign.  
It does **not** redesign the whole app again.  
It only fixes real problems found in an audit.

---

## What this app is (simple)

Wildlife FieldOps is a phone app for wildlife-removal field work.

You can:

- See jobs on a home screen  
- Open jobs, customers, and inspections  
- Use maps and GPS  
- Make estimates and invoices  
- Use AI helpers when a key is set  
- Work offline, then sync when you have internet  

Stack: **Kotlin + Jetpack Compose + Room + Hilt** (Android only).

---

## What was wrong on ChatGPTv6

### 1. Windows build file was broken
`gradlew.bat` was not a real build script.  
It was a GitHub “too many requests” error message.  
So Windows builds failed. Linux CI still worked because it uses `./gradlew`.

### 2. Settings could crash the app
Two different “settings storage” systems pointed at the **same file**.  
Android does not allow that.  
It can crash with: *multiple DataStores active for the same file*.

### 3. Dark / light mode forgot your choice
The theme switch only changed memory.  
Restart the app → back to dark.  
Now the choice is saved and restored.

### 4. Job → Inspect lost the job link
From a job, “Inspect” opened a new inspection form **without** the job id.  
So the inspection was not tied to that job.  
Now the job id is passed and saved.

### 5. Some tools were hard to reach
Screens existed (sync queue, scheduler, voice dictation, AI camera, contract)  
but were missing from the side menu or job actions.  
They are wired up now.

### 6. Release builds could fail minify
Same R8 crash risk fixed on other branches.  
R8 is pinned to a safe version for signed release builds.

### 7. Housekeeping
- Version label updated to ChatGPTv6 audit  
- Safer `.gitignore` (secrets, APKs, local SDK paths)  
- Manifest package warning cleaned up  
- CI trigger added for this fix branch  

---

## What files changed (main ones)

| Area | Files |
|------|--------|
| Windows build | `gradlew.bat` |
| Settings storage | `AppSettings.kt`, `SettingsViewModel.kt` |
| Theme restore | `WildlifeFieldOpsApp.kt`, `SettingsScreen.kt` |
| Navigation | `Screen.kt`, `MainActivity.kt`, `JobDetailScreen.kt` |
| Release safety | `build.gradle.kts` (R8 pin) |
| Version | `app/build.gradle.kts` → `2.4.2-chatgpt-v6-audit` |
| Git hygiene | `.gitignore` |
| CI | `.github/workflows/build-android.yml` |
| Full audit notes | `CHATGPTV6_AUDIT.md` |

---

## How to build

### On a computer with Android SDK

```bash
# From repo root
./gradlew :app:assembleDebug
```

Windows:

```bat
gradlew.bat :app:assembleDebug
```

APK path:

```
app/build/outputs/apk/debug/
```

### On GitHub

Push to `fix/chatgptv6-audit` runs **Build Native Android APK (Debug)**.  
Download the APK from the Actions run artifacts.

### Secrets (optional, for cloud / maps / AI)

Set in GitHub Actions or local env when building:

- `SUPABASE_URL` / `SUPABASE_ANON_KEY` (or `VITE_*` secrets already used in CI)  
- `GOOGLE_MAPS_API_KEY`  
- `OPENWEATHER_API_KEY`  
- `XAI_API_KEY` or `LLM_API_KEY`  

Without them, the app still runs with offline / limited features.

---

## How to test the fixes (quick checklist)

1. **Install debug APK** on a phone.  
2. Open **Settings → Dark theme** — flip it, kill app, reopen. Theme should stick.  
3. Open a **job → Inspect** — save an inspection. Confirm it is linked to that job.  
4. Open the **side menu** — Sync Queue, Inspection Scheduler, Voice Dictation, AI Camera should open.  
5. On a job, tap **Contract** — contract screen should open for that job.  
6. On Windows, run `gradlew.bat :app:compileDebugKotlin` — should work (not the old 429 error).

---

## What we did *not* fully fix (later work)

- Many older screens still use hard-coded dark colors. Light mode looks best on Home / Inspections / Settings first.  
- Full light-theme polish across every screen is a follow-up.  
- Room schema JSON is generated at build; committing schemas for migration history is optional later.

---

## Branch map

| Branch | Role |
|--------|------|
| `main` | Default / older line |
| `ChatGPTv6` | Redesign source that was audited |
| **`fix/chatgptv6-audit`** | **This branch — audit fixes** |
| `kimiswarmV5` / `chatGPTV5` | Other feature lines |

---

## Simple summary

**We looked at ChatGPTv6, found real bugs, fixed them, and put the fixes on a new branch.**

Build works on Windows again.  
Settings should not crash.  
Theme remembers you.  
Inspections keep their job.  
Hidden tools are reachable.  
Release minify is safer.

See **`CHATGPTV6_AUDIT.md`** for the technical table of every issue and fix.
