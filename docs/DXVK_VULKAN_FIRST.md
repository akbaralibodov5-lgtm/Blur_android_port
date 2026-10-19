# DXVK + Vulkan first integration plan

## Decision

The first graphics path for Blur Mobile Port is DXVK + Vulkan. This is a priority for testing, not a claim of compatibility. Android Vulkan feature reporting does not prove that the vendor driver exposes everything DXVK needs or that Blur will render correctly.

## Current facts

- The launcher reports Android API 36 and ABI `arm64-v8a` on the device shown by the user.
- It reports Vulkan 1.1, but OpenGL ES renderer detection returned "not detected"; that needs separate diagnosis.
- The app currently has no Wine, Box64, DXVK or WineD3D runtime bundled.
- The user reports that Blur launches in Winlator. The exact working Winlator version/container settings have not yet been captured.

## Integration sequence

### 1. Capture a known-good reference

From the working Winlator installation, record:
- Winlator app version and release/source;
- container architecture and Windows version;
- Wine version/build;
- Box64 version and preset;
- DXVK version and whether DXVK is enabled;
- graphics driver name/version;
- resolution and relevant environment variables;
- whether the Blur main menu and a full race both work.

Use screenshots of the container settings and the app's about/version screen. Do not export proprietary game data or private tokens.

### 2. Select a legally distributable runtime

Pin an immutable upstream revision and audit its license/redistribution terms. A complete runtime needs more than the four named components: it may need a compatible Linux userspace/rootfs, loader and shared libraries, Wine prefix setup, fonts, audio/input/display bridge and graphics userspace integration. Do not copy arbitrary files from the Winlator APK into `jniLibs`.

### 3. Package outside the APK native-library directory

Install a verified archive into app-private storage, for example:

```text
files/runtimes/blur-arm64/
  manifest.json
  rootfs/
  wine/
  usr/bin/box64
  graphics/dxvk/x32/
  graphics/dxvk/x64/
  licenses/
```

The exact structure must follow the selected runtime build; this is a proposed layout, not a promise that files from upstream have these paths. Keep the runtime archive out of Git history. Verify SHA-256 hashes, reject path-traversal entries, preserve license notices and validate required files before activation.

### 4. DXVK smoke test before Blur

Launch in stages:
1. Runtime/loader diagnostic.
2. Wine version and prefix diagnostic.
3. Minimal Direct3D test through DXVK.
4. Blur menu.
5. A complete race, then audio/input/pause/resume/relaunch tests.

Store logs in app-private storage and offer a way to export them. Only mark DXVK usable after its smoke test passes.

### 5. Renderer policy

- If the DXVK + Vulkan smoke test passes, save that as the last-known-good renderer.
- If Vulkan is advertised but the test fails, report the failure and log details; do not assume Vulkan alone is sufficient.
- Keep WineD3D as a later fallback experiment. It should use a compatible Wine build and a separately tested prefix/override setup.
- Do not silently switch DLLs inside a single prefix without recording the configuration.

## Code tasks after the reference runtime is chosen

- Replace placeholder component checks with manifest + hash + required-file validation.
- Add a safe runtime installer/extractor.
- Add a process-launch bridge that prepares rootfs, environment, prefix, working directory and game path.
- Add timeout, process exit status and log capture for renderer smoke tests.
- Add persistent last-known-good profile and explicit failure fallback.
- Keep `Blur.exe` and game data user-supplied; never commit them.

## Acceptance criteria

- [ ] Exact Winlator reference settings recorded.
- [ ] Runtime version, source commit and licenses reviewed.
- [ ] Package hashes verified and safe extraction tested.
- [ ] DXVK Direct3D smoke test passes on physical device.
- [ ] Blur menu and complete race verified.
- [ ] CI validates the manifest and builds the Android launcher.
- [ ] No compatibility claim based only on GPU feature flags.

## Status

Planning and launcher diagnostics only. No runtime binary is bundled by this document; no Blur execution is implemented or verified yet.
