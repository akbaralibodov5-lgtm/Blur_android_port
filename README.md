# Blur Android Port — starter

This repository currently contains a **starter launcher**, not a completed port of Blur (2010).

## Current prototype
- Android folder picker for a user-owned PC installation.
- Checks for `Blur.exe` in the selected folder.
- GitHub Actions workflow to compile a debug APK.

## Important limitation
Selecting `Blur.exe` does not make a Windows PC game run on Android. A compatible Windows runtime (for example Wine/Box64) and DirectX translation layer would need to be integrated and tested with Blur. This repository does **not** yet include that runtime and does not claim the game works.

## Build APK
Open this project in Android Studio, allow Gradle sync, then choose **Build > Build APK(s)**. Or push a commit and download the debug APK from the GitHub Actions workflow artifact.

## Test the current prototype
1. Install the generated debug APK.
2. Tap **Select Blur game folder**.
3. Choose the directory containing `Blur.exe`.
4. The app reports whether the executable was found.

Do not upload or redistribute copyrighted game files. Select a copy you are entitled to use.
