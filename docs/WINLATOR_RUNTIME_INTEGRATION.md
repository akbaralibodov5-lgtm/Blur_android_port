# Winlator runtime integration plan for Blur Android Port

## Goal

Use the proven Blur-on-Winlator configuration as the compatibility reference, then integrate a reproducible runtime into this launcher. This document is an implementation plan, not a claim that runtime binaries are bundled or that the launcher can run Blur yet.

## Important architecture decision

Do not copy arbitrary files from an installed Winlator container into `app/src/main/jniLibs`. A working setup consists of a compatible Linux userspace/root filesystem, Wine and its Windows DLL trees, Box64, graphics translation DLLs, GPU/Vulkan userspace components as appropriate, process environment, and a launch bridge. Android JNI libraries are only one part of that system.

Recommended first milestone: create a runtime package that can be installed/extracted into the app's private files directory, and launch a diagnostic executable before launching Blur. Keep the game files user-selected and external.

## Runtime layout proposal

The runtime installer should produce this private app-data layout (exact paths must match the chosen runtime build):

```text
files/
  runtimes/
    winlator-blur/
      runtime-manifest.json
      rootfs/                 # Linux userspace and libraries
      usr/bin/box64           # or the chosen build's actual path
      wine/                   # Wine loader, DLLs, WoW64/x64 support as required
      graphics/
        dxvk/                 # x64 and/or x86 Windows DLLs matching the game
        wined3d/               # WineD3D files from the same compatible Wine build
      licenses/
      tests/
```

This is a logical layout only. Do not assume these names match upstream packages; record the real paths after selecting and auditing a runtime build.

## Required components / decisions

1. **Runtime base:** choose a specific Winlator source/release and commit. Record upstream URL, version, commit/tag, license, build instructions, and hashes. Prefer building from source or using redistributable packages whose licenses permit redistribution. Do not repackage another APK blindly.
2. **Linux userspace:** include the matching libc/loader and required runtime libraries. Box64 generally expects a compatible Linux userspace; Android's bionic environment is not a drop-in replacement for glibc.
3. **Wine:** choose a build matching the selected Box64 and userspace. Preserve Wine DLLs, registry/prefix setup, fonts and supporting files required by the game.
4. **Box64:** use a tested ARM64 Android-compatible build and matching configuration. Verify executable permissions and shared-library dependencies in the extracted private directory.
5. **Graphics:** DXVK requires a working Vulkan path and matching 32-/64-bit D3D DLLs for the game's DirectX version. WineD3D is an alternative path and must use the Wine build's compatible files. Do not install both into the same prefix simultaneously without a deliberate DLL override plan.
6. **GPU userspace:** use only drivers/components that are compatible with the device and redistribution terms. Vulkan feature detection alone is not proof DXVK works. Some devices need vendor-specific userspace drivers; do not bundle kernel drivers.
7. **Launcher bridge:** implement a process launcher that prepares environment variables, prefix, working directory, game path, graphics DLL overrides, logs, audio/input/display integration, and clean shutdown. Android's normal `ProcessBuilder` cannot by itself supply the full Linux runtime or a graphical X/Wayland session.

## Reproducible package manifest

Create `runtime-manifest.json` for each runtime package with at least:

- schemaVersion
- runtimeId and runtimeVersion
- upstream repository and immutable source revision
- package license and license-notice directory
- target Android ABI(s)
- Box64 path and version
- Wine loader path and version
- rootfs path and architecture
- graphics options available (DXVK, WineD3D)
- relative paths for x86 and x64 Direct3D DLLs
- SHA-256 hashes for all shipped archives/binaries
- known limitations and tested device/GPU list

Never include fake marker files to make preflight pass. Validate the manifest, file hashes, executable permissions and required files before reporting a runtime as installed.

## Implementation milestones

### M0 — capture the working Winlator configuration

On the phone where Blur works, record:
- Winlator version/release and container settings;
- Wine version, Box64 version/preset, DXVK version or WineD3D selection;
- graphics driver and Vulkan/OpenGL versions;
- Windows version/prefix settings, resolution, environment variables and command line;
- whether the game reaches menu and a full race;
- log output after a successful launch.

Do not upload proprietary Blur game files, serials, or personal data to GitHub.

### M1 — build or obtain a redistributable runtime package

- Pin upstream source revisions.
- Build/test each ABI and architecture combination in CI where possible.
- Generate a package archive plus SHA-256 manifest.
- Add all required third-party license notices.
- Keep large runtime archives out of Git history; use CI artifacts or a release/package hosting solution with integrity verification.
- Confirm package redistribution is permitted before publishing.

### M2 — install and validate

- Extract to app-private storage, not `jniLibs`.
- Reject path traversal entries and verify hashes before extraction.
- Check all required files and executable bits.
- Run a harmless Linux diagnostic, then a Wine version check, then a minimal Direct3D test.
- Keep logs in app-private storage and offer an export/share option that redacts personal paths.

### M3 — renderer selection and fallback

- Try the last-known-good renderer first.
- Use DXVK only if Vulkan and the runtime/driver test pass.
- Otherwise try WineD3D if its OpenGL path passes.
- Persist the renderer profile only after the Direct3D smoke test succeeds.
- On crash or timeout, capture logs and offer the alternate path; don't silently claim compatibility from GPU feature flags.

### M4 — launch Blur

- Ask the user to select the PC game directory and validate the executable/data files.
- Use the runtime launch bridge with the correct working directory and prefix.
- Verify menu, gameplay, audio, controls, pause/resume, memory use, thermals and repeated launches on physical devices.
- Mark a device/runtime combination as supported only after these tests pass.

## Acceptance checklist

- [ ] Runtime source/version/commit and licenses are documented.
- [ ] Package archive hashes validate.
- [ ] Installer safely extracts to private storage.
- [ ] Box64 and Wine diagnostics execute successfully.
- [ ] DXVK Direct3D smoke test passes on the device, or WineD3D smoke test passes.
- [ ] Blur reaches menu and a complete race.
- [ ] Input/audio/lifecycle tested.
- [ ] CI builds the launcher and tests manifest validation.
- [ ] No game binaries or proprietary game data committed to this repository.

## Current project status

The current `feature/renderer-auto-detect` branch is diagnostic scaffolding only. Its GitHub Actions APK build can succeed without Wine/Box64/DXVK/WineD3D being integrated. Runtime integration and Blur compatibility remain unverified until the milestones above are completed.
