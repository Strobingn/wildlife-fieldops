# Wildlife Whisperer FieldOps Rockstar - GitHub APK Build

This package includes a GitHub Actions workflow that builds a debug Android APK in the cloud.

## Fast Phone-Only GitHub Build

### 1. Create a new GitHub repo

Go to GitHub and create a new repository named:

```text
wildlife-fieldops
```

Keep it empty. Do not add a README from GitHub.

### 2. Upload these files

Upload everything inside this folder to the repo.

Important: make sure this file exists in GitHub:

```text
.github/workflows/build-android.yml
```

### 3. Run the APK build

In GitHub:

```text
Repo → Actions → Build Android APK → Run workflow
```

Or push to `main`; it will run automatically.

### 4. Download the APK

When the workflow finishes:

```text
Actions → latest successful run → Artifacts → Wildlife-FieldOps-debug-apk
```

Download and unzip the artifact. Inside is the debug `.apk`.

### 5. Install on Android

On your Android phone:

```text
Files → APK → Install
```

You may need to allow installation from unknown sources.

## Notes

This creates a debug APK for field testing.

For production/release APK:

- create Android signing key
- add GitHub encrypted secrets
- use `assembleRelease`
- sign the APK/AAB
- optionally publish to Google Play

## Included App Features

- GPS capture and job pins
- AI-style field assistant/species suggestions
- Voice dictation
- Smart estimator
- Property history
- Digital contract text and signatures
- Offline-first storage
- Sync queue
- Cloud sync endpoint hook
- Capacitor Android scaffold
