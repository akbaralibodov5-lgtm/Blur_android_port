# OpenGL compatibility plan for Blur (2010)

## Objective
Test whether the PC version of Blur can run on Android using an OpenGL-backed Direct3D translation path.

## Preferred experiment: WineD3D
WineD3D translates supported Direct3D calls to OpenGL. It is a candidate for testing, not a guarantee of compatibility or performance.

### Phase 1 — inspect the game
- Identify the exact Blur PC build and executable architecture.
- Identify the DirectX version and required Windows APIs.
- Keep the game installation external; never bundle game data in this repository.

### Phase 2 — runtime proof of concept
- Select a compatible Wine build and x86/x64 execution layer for the target device ABI.
- Build/package dependencies with licenses and notices preserved.
- Configure WineD3D to use the Android device's supported OpenGL ES capabilities through the chosen runtime's supported path.
- Confirm the runtime can start a minimal Windows Direct3D application before testing Blur.

### Phase 3 — Blur compatibility
Test separately:
1. Process starts and reaches the menu.
2. Textures, shaders, lighting, shadows and post-processing render correctly.
3. A full race can start and finish.
4. Audio, controller/touch input and pause/resume work.
5. Repeated runs do not crash or corrupt saves.

Record logs and device/GPU details for each test. Do not claim support until these checks pass.

### Phase 4 — low-end device optimization
- Start with conservative resolution and graphics settings where the game supports them.
- Avoid unnecessary launcher background work, animations and large bundled assets.
- Measure CPU/GPU load, memory use, heat and sustained frame rate; optimize from measurements.
- Keep a fallback option if WineD3D compatibility or speed is inadequate.

## Current status
**Planning only.** The current Android app selects a game folder and checks for `Blur.exe`. It does not include Wine, Box64, WineD3D or a game execution bridge. No working Blur-on-Android result has been verified.


## Automatic device detection implementation

The launcher now contains a best-effort capability probe that reports:
- supported CPU ABIs and whether `arm64-v8a` is present;
- Android API level and total/available RAM;
- OpenGL ES version, vendor and renderer when the temporary EGL context succeeds;
- whether Android advertises Vulkan hardware support and Vulkan 1.1;
- candidate rendering paths and caution notes.

This is a diagnostic foundation, **not yet an automatic runtime selector**. A reliable selector must query the actual bundled runtime components and driver compatibility, run a small renderer smoke test, save the last known-good profile, and fall back after a failed launch. Android's feature flags alone cannot prove DXVK or WineD3D will work with Blur. No renderer/runtime binaries are bundled by this change.


## Runtime preflight status

The launcher now calls a conservative runtime preflight. It checks for explicit runtime component files/markers before selecting a renderer; it will not infer that DXVK or WineD3D is installed from Vulkan/OpenGL support alone. On the current project branch, Wine, Box64, DXVK and WineD3D are not bundled, so the correct status is **not ready to launch**.

This is intentionally not described as full runtime integration. The actual engine integration still requires choosing a legally distributable, reproducible Android runtime build; packaging native components for supported ABIs; bridging process startup, filesystem access, input and audio; and validating Blur on physical devices. Do not add placeholder marker assets to make preflight pass.
