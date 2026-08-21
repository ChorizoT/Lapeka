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

**Lapeka** is a modern Android application built with Kotlin and Jetpack Compose that serves as a self-hosted app manager. It tracks your own APKs against a remote manifest and handles downloads and installations seamlessly.

## Key Features

- 🔐 **BirdyAuth Integration**: Secure access to your private manifest via BirdyWood's authentication system.
- 📱 **Modern UI**: Full Material 3 implementation with support for **Dynamic Theming** and **Dark Mode**.
- 🚀 **Silent Updates**: Uses the `PackageInstaller` session API for seamless updates without confirmation dialogs (Android 12+) (BROKEN).
- 🔔 **Live Progress Notifications**: Track your app installations with a real-time progress bar in the system tray.
- ✨ **Onboarding Flow**: Smooth welcome experience for new users with automated permission handling.
- 🔍 **Search & Filter**: Quickly find and manage your tracked applications.
- 🔄 **Pull-to-Refresh**: Easily update your app list with a simple swipe.
- 🛠️ **Background Updates**: Automated version checking via WorkManager. (DEPRECATED)

## How it works

1. **Authentication**: Sign in with your BirdyAuth account to access your personalized app list.
2. **Manifest Tracking**: The app polls your configured JSON manifest endpoint and compares `versionCode` against installed packages.
3. **Installation**: When you trigger an update or install, Lapeka downloads the APK, verifies the SHA-256 checksum, and initiates a `PackageInstaller` session.

## Manifest JSON Schema

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

## Configuration

Set your manifest endpoint URL in the **Settings** section. You can also manage:
- Dark Mode preferences (System, Light, Dark).
- Dynamic Theming toggle.
- System notification settings.
- Manual cache refresh.

## Building

Open the project in **Android Studio (Ladybug or newer)**.

From CLI:
```bash
./gradlew assembleDebug
```

## Future Improvements

- **Local Caching (Room)**: Implement a local database to store the app manifest, allowing for instant loading and offline browsing.
- **Dependency Injection**: Refactor the project to use **Hilt** for better modularity and easier unit testing.
- **Detailed App Pages**: Create dedicated detail screens with screenshots, version history, and comprehensive permission lists.
- **Adaptive Layouts**: Optimize the UI for larger screens (tablets, foldables, and desktop) using Navigation 3 scenes.
- **Silent Update Fixes**: Refine the "Installer of Record" logic to improve the reliability of silent updates on Android 12+.
- **Enhanced Background Worker**: Modernize the background update check to be more energy-efficient and provide better user notifications.
- **Multi-Account Support**: Allow users to configure multiple manifest endpoints and switch between different BirdyAuth accounts.

## Credits

Built with ❤️ by **BirdyWood**.
2026 © BirdyWood
