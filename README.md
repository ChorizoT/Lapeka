# Lapeka

Android app (Kotlin + Jetpack Compose) that tracks other APKs — SimpMusic,
your own BirdyWood apps — against a manifest endpoint you host, and
installs/updates them.

## How it works

1. You host an API endpoint returning a JSON array (see schema below).
2. The app polls it (on open + every 6h via WorkManager) and compares
   `versionCode` against what's installed on the device via `PackageManager`.
3. On install/update, it downloads the APK, verifies SHA-256 if provided,
   and installs via the `PackageInstaller` session API.

## Silent install behavior

- **First install of any package**: always shows one system confirmation
  dialog. This is an Android platform requirement, not something the app
  can bypass on arbitrary (non-Device-Owner) devices.
- **Subsequent updates, Android 12+ (API 31+)**: silent, no dialog — as
  long as this app was the one that performed the original install
  (checked via `PackageManager.getInstallSourceInfo`).
- **Android 11 and below**: every install/update shows the dialog.

## Manifest JSON schema

```json
[
  {
    "id": "simpmusic",
    "name": "SimpMusic",
    "packageName": "com.example.simpmusic",
    "versionCode": 42,
    "versionName": "1.4.2",
    "apkUrl": "https://your-domain/apks/simpmusic-1.4.2.apk",
    "sha256": "optional-but-recommended",
    "changelog": "optional",
    "iconUrl": "optional"
  }
]
```

Set the endpoint URL in-app via the settings (gear icon) on first launch.

## Building

Open in Android Studio (Koala+ recommended) and run, or from CLI:

```
./gradlew assembleDebug
```

(Requires the Gradle wrapper — run `gradle wrapper` once if `gradlew` is
missing, or open in Android Studio which generates it automatically.)

## Notes / next steps

- No Room/DataStore for caching the app list yet — every refresh re-hits
  the manifest endpoint. Fine for a personal-scale list; add caching if
  the list grows large or the endpoint gets slow.
- `QUERY_ALL_PACKAGES` is declared because tracked package names are
  dynamic (come from your manifest, not known at compile time). This is
  fine for sideloaded distribution; would need Play Store justification
  if ever published there.
- The launcher icon is a placeholder vector — swap in real artwork
  whenever you're ready.
