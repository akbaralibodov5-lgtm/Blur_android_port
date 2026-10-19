package com.waru.blurmobileport

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/**
 * Runtime preflight and conservative renderer selection.
 *
 * This class intentionally never pretends a renderer is usable just because a GPU advertises
 * Vulkan/OpenGL. A candidate is selectable only when the corresponding runtime marker/assets
 * exist in this app's private files or packaged assets. Actual game compatibility still requires
 * launching a renderer smoke test through a real Wine/Box64 runtime.
 */
object RuntimeBootstrap {
    enum class Renderer(val id: String, val displayName: String) {
        DXVK_VULKAN("dxvk-vulkan", "DXVK + Vulkan"),
        WINED3D_OPENGL("wined3d-opengl", "WineD3D + OpenGL"),
        NONE("none", "No integrated renderer")
    }

    data class Preflight(
        val runtimePresent: Boolean,
        val winePresent: Boolean,
        val box64Present: Boolean,
        val dxvkPresent: Boolean,
        val wineD3dPresent: Boolean,
        val vulkanAdvertised: Boolean,
        val openGlAvailable: Boolean,
        val selectedRenderer: Renderer,
        val readyToLaunch: Boolean,
        val explanation: String
    )

    fun inspect(activity: Activity): Preflight {
        val nativeDir = activity.applicationInfo.nativeLibraryDir?.let(::File)
        val assets = runCatching { activity.assets.list("runtime")?.toSet().orEmpty() }
            .getOrDefault(emptySet())

        // Runtime files are not currently shipped. These checks deliberately require explicit
        // marker files rather than guessing from the GPU. Packaging a marker alone is not enough
        // for release: validate the binary and its license before adding it.
        val wine = hasRuntimeComponent(nativeDir, assets, "wine")
        val box64 = hasRuntimeComponent(nativeDir, assets, "box64")
        val dxvk = hasRuntimeComponent(nativeDir, assets, "dxvk")
        val wineD3d = hasRuntimeComponent(nativeDir, assets, "wined3d")

        val pm = activity.packageManager
        val vulkan = if (Build.VERSION.SDK_INT >= 24) {
            runCatching {
                pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
            }.getOrDefault(false)
        } else false
        val gles = runCatching {
            pm.systemAvailableFeatures.any { it.name == PackageManager.FEATURE_OPENGLES_EXTENSION_PACK }
        }.getOrDefault(false) || Build.VERSION.SDK_INT >= 21

        val canRunWindows = wine && box64 && Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }
        val renderer = when {
            canRunWindows && dxvk && vulkan -> Renderer.DXVK_VULKAN
            canRunWindows && wineD3d && gles -> Renderer.WINED3D_OPENGL
            else -> Renderer.NONE
        }
        val ready = canRunWindows && renderer != Renderer.NONE
        val why = when {
            !Build.SUPPORTED_ABIS.any { it == "arm64-v8a" } ->
                "ARM64 Android runtime support is not detected."
            !wine || !box64 ->
                "Wine/Box64 runtime components are not integrated in this APK yet."
            renderer == Renderer.NONE ->
                "No complete renderer package was found. Hardware API detection alone is insufficient."
            renderer == Renderer.DXVK_VULKAN ->
                "DXVK/Vulkan is the first candidate; a renderer smoke test is still required."
            else ->
                "WineD3D/OpenGL is the fallback candidate; a renderer smoke test is still required."
        }
        return Preflight(
            runtimePresent = canRunWindows,
            winePresent = wine,
            box64Present = box64,
            dxvkPresent = dxvk,
            wineD3dPresent = wineD3d,
            vulkanAdvertised = vulkan,
            openGlAvailable = gles,
            selectedRenderer = renderer,
            readyToLaunch = ready,
            explanation = why
        )
    }

    private fun hasRuntimeComponent(nativeDir: File?, assets: Set<String>, component: String): Boolean {
        val marker = "runtime-$component.ready"
        if (marker in assets) return true
        val dir = nativeDir ?: return false
        val possible = when (component) {
            "wine" -> listOf("libwine.so", "wine")
            "box64" -> listOf("libbox64.so", "box64")
            "dxvk" -> listOf("libdxvk.so", "dxvk")
            "wined3d" -> listOf("libwined3d.so", "wined3d")
            else -> emptyList()
        }
        return possible.any { File(dir, it).exists() }
    }
}
