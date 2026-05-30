# Wildlife Whisperer FieldOps Rockstar

Included:
- GPS capture and offline GPS pins
- AI-style field assistant/species suggestions
- Voice dictation
- Smart estimator
- Property history
- Digital contract text and signatures
- Offline-first service worker
- Persistent local storage
- Sync queue and Cloudflare Worker sync stub
- Capacitor Android scaffold

Run on Android Termux:
```bash
termux-setup-storage
pkg install -y python unzip
cd ~
cp ~/storage/downloads/wildlife-fieldops-rockstar.zip .
unzip wildlife-fieldops-rockstar.zip
cd wildlife-fieldops-rockstar
python -m http.server 5173
```
Open:
```text
http://127.0.0.1:5173
```

Build APK later on desktop/cloud machine with Android Studio:
```bash
npm install
npx cap add android
npx cap sync android
npx cap open android
```

Cloud sync:
Paste `cloudflare-worker-sync-stub.js` into a Cloudflare Worker and use that Worker URL in the app.
For production, connect the Worker to D1/PostgreSQL/Supabase and R2/S3 for photos/contracts/signatures.


## GitHub APK Build

See `GITHUB_APK_BUILD.md`. The workflow is at `.github/workflows/build-android.yml`.
