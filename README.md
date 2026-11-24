# Daily Lucky Draw

An Android app built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Firebase Authentication (Google Sign-In)**, and **Cloud Firestore**.

## Project Structure

- `app/` — Android application module (`applicationId = "com.aistudio.dailyluckydraw.qkmxtr"`, `namespace = "com.example"`)
  - `app/src/main/java/com/example/` — Kotlin Compose UI, ViewModel, and Firestore repository
  - `app/src/main/res/` — App resources, drawables, strings, and Firebase config
  - `app/google-services.json` — Firebase project configuration
- `firestore.rules` & `firebase-blueprint.json` — Cloud Firestore security rules and schema blueprint
- `firestore.test.js` — Emulator unit tests for Firestore security rules
- `.github/workflows/build-apk.yml` — GitHub Actions workflow that automatically builds `app-debug.apk` on pushes to `main`

## Building the APK on GitHub (No Computer Required)

Every push to `main` (or manual run) triggers the **Build Debug APK** GitHub Actions workflow:

1. Open your repository on GitHub (in a phone browser or the GitHub app).
2. Tap the **Actions** tab and select **Build Debug APK**.
3. Open the latest completed run and scroll down to **Artifacts**.
4. Download **daily-lucky-draw-debug-apk** (contains `app-debug.apk`).

### Optional Admin Email Secret

To override the default admin email during GitHub Actions builds, add a repository secret in **Settings → Secrets and variables → Actions**:

- `ADMIN_EMAIL` — Google account email granted admin controls
