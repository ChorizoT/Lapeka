<div align="center">

```
░██                                          ░██                  
░██                                          ░██                  
░██          ░██████   ░████████   ░███████  ░██    ░██ ░██████   
░██               ░██  ░██    ░██ ░██    ░██ ░██   ░██       ░██  
░██          ░███████  ░██    ░██ ░█████████ ░███████   ░███████  
░██         ░██   ░██  ░███   ░██ ░██        ░██   ░██ ░██   ░██  
░██████████  ░█████░██ ░██░█████   ░███████  ░██    ░██ ░█████░██ 
                       ░██                                        
                       ░██                                        
```
</div>

**Lapeka** is a modern Android application built with Kotlin and Jetpack Compose that an app that allows you to install and update apps from APKs.

## Key Features

- **BirdyAuth Integration**: Secure access to your private manifest via BirdyWood's authentication system.
- **Modern UI**: Full Material Design 3 implementation with support for **Dynamic Theming** and **Dark Mode**.
- **Silent Updates**: Uses the `PackageInstaller` session API for seamless updates without confirmation dialogs (Android 12+) (BROKEN).
- **Live Progress Notifications**: Track your app installations with a real-time progress bar in the system tray.
- **Onboarding Flow**: Smooth welcome experience for new users with automated permission handling.
- **Search & Filter**: Quickly find and manage your tracked applications.
- **Pull-to-Refresh**: Easily update your app list with a simple swipe.
- **Background Updates**: Automated version checking via WorkManager.

## How it works

1. **Authentication**: Sign in with your BirdyAuth account to access the BirdyWood's app list (by default).
2. **Manifest Tracking**: The app polls your configured JSON manifest endpoint and compares `versionCode` against installed packages.
3. **Installation**: When you trigger an update or install, Lapeka downloads the APK, verifies the SHA-256 checksum, and initiates a `PackageInstaller` session.

## Manifest JSON Schema



## Configuration

You can set your own manifest endpoint URL in the **Settings** section.

Your endpoint should return a JSON array with the following structure:

```json
[
  {
    "id": "app-id",
    "name": "App Name",
    "desc": "A short description of the app.",
    "packageName": "com.example.app",
    "versionCode": 42,
    "versionName": "1.2.0",
    "apkUrl": "https://your-domain/apks/app-1.2.0.apk",
    "sha256": "sha256-checksum-for-verification",
    "changelog": "https://link-to-changelog-or-text",
    "iconUrl": "https://link-to-app-icon.png",
    "lastUpdate": 1723886400
  }
]
```

If you want to set it to the default value, clear the cache and data of the app.

## Credits

Built with ❤️ by **BirdyWood**.
2026 © BirdyWood
