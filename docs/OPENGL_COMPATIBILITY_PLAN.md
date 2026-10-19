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
