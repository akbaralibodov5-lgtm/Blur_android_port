# Blur Android Port — OpenGL compatibility prototype

This repository currently contains a lightweight Android launcher prototype, **not a completed port of Blur (2010)**.

## Goal
Investigate running a user's own Blur PC installation on Android, preferring an **OpenGL translation path** for Direct3D rather than requiring a Vulkan-only graphics path.

## Proposed compatibility architecture
- **Android launcher:** selects and remembers the user's game folder; checks for `Blur.exe`.
- **Windows compatibility runtime:** Wine plus an appropriate x86/x64 execution layer such as Box64, built and configured for the target Android ABI.
- **Graphics translation:** test WineD3D (Direct3D-to-OpenGL) first. Blur's exact DirectX API requirements and rendering behavior must be measured. If WineD3D is too slow or incompatible, compare alternatives; do not assume OpenGL support alone guarantees gameplay.
- **Input/audio/filesystem:** map touch controls to game input, route audio, and grant access to game files without copying copyrighted game assets into the APK.
- **Device validation:** test startup, menus, a full race, rendering glitches, audio, thermals, and sustained frame rate on real hardware.

## What is implemented now
- Native Android folder picker and saved folder permission.
- A check for `Blur.exe` at the selected directory's top level.
- A GitHub Actions workflow to compile a debug APK.

## What is NOT implemented
The Wine/Box64 runtime, WineD3D libraries/configuration, game execution, touch controls, audio bridge, and performance tuning are **not integrated yet**. Therefore this app currently does not launch Blur and must not be described as a working port.

## Why OpenGL is not a one-line change
Blur is a Windows game that uses DirectX. The game does not become an OpenGL game just because the Android launcher uses OpenGL. A compatible runtime and Direct3D-to-OpenGL translation layer must be packaged or provided, configured for the game, and tested. Runtime licensing and redistribution requirements must be respected.

## Build the current launcher
Open this project in Android Studio, allow Gradle sync, then choose **Build > Build APK(s)**. Or push a commit and download the debug APK from the GitHub Actions workflow artifact.

## Test the current prototype
1. Install the generated debug APK.
2. Tap **Select Game Folder**.
3. Choose the directory containing `Blur.exe`.
4. Confirm the launcher detects the executable.

Do not upload or redistribute copyrighted Blur game files. Select a copy you are entitled to use.
