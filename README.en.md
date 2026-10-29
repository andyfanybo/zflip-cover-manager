# Z Flip Cover Manager

[简体中文](README.md) · English

Manage the MultiStar app list from the Galaxy Z Flip5 cover screen. After one-time authorization, add newly installed apps without opening the inner display.

**[Download APK](https://github.com/andyfanybo/zflip-cover-manager/releases/latest)** · [Technical notes](docs/implementation.md) · [MIT License](LICENSE)

## Interface

<img src="docs/images/english.png" width="340" alt="Cover Manager in English">

The screenshot is filtered to this tool. Clear the search to see all apps.

## Installation

### Shizuku

1. Install and start [Shizuku](https://shizuku.rikka.app/) v13 or newer.
2. Install the APK and open Cover Manager.
3. Choose **Setup → Authorize with Shizuku and add entry**, then allow the request.

Shizuku does not need to keep running afterward. You must be able to open this tool for the first time. If the inner display is broken and your existing cover launcher cannot open it, use ADB below.

### ADB

Download the APK and `setup.ps1` from the release page. On Windows with Android Platform Tools installed, run:

```powershell
.\setup.ps1 -Device 'SERIAL_OR_IP:PORT' -ApkPath '.\CoverManager-v0.3.1.apk'
```

The script installs the app, grants permission, backs up the configuration and adds the cover entry. Backups are saved to `.covermanager-backups` in the current directory.

Or install and authorize manually:

```sh
adb -s DEVICE install -r CoverManager-v0.3.1.apk
adb -s DEVICE shell pm grant local.covermanager android.permission.WRITE_SECURE_SETTINGS
adb -s DEVICE shell am start --display 1 -n local.covermanager/.MainActivity
```

Then choose **Setup → ADB already granted: add / repair entry**. Unlock the phone first. Display ID `1` is the cover screen on the test device.

## Usage

- Tap **Refresh** after installing an app, then search and select it. Clear its checkbox to remove it.
- If MultiStar icons have not refreshed, switch cover widgets and back.
- **Undo** restores the last list change, unless another app has since changed the configuration.
- The app uses the system font. Chinese system locales use Chinese; all others use English. You can also select a language in Setup.

There is no MultiStar version restriction. Unrecognized configuration formats are read-only. Tested on a Z Flip5 SC-54D with Android 15 / One UI 7 and MultiStar 9.1.09. Minimum Android version: 11.

## Build

Requires JDK 17, Android SDK Platform 35 and Build Tools 35.0.0:

```powershell
.\build.ps1 -Jdk 'C:\path\to\jdk-17' -AndroidSdk 'C:\path\to\Android\Sdk'
```

Output: `build/CoverManager-v0.3.1.apk`. The script downloads and checks the Shizuku dependencies, runs tests and creates a development signing key. Its signature differs from the Release. Keep your own key for future updates.

Report issues in [Issues](https://github.com/andyfanybo/zflip-cover-manager/issues), including the device, system and MultiStar versions. Do not publish complete configuration backups or personal app lists.

[MIT License](LICENSE). Shizuku attribution: [third-party license](docs/SHIZUKU-LICENSE.txt).
