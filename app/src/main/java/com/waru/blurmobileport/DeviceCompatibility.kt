package com.waru.blurmobileport

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Best-effort device capability probe for choosing a renderer after a real runtime is bundled.
 *
 * This deliberately reports capabilities, not guaranteed game compatibility. It does not
 * claim that DXVK, WineD3D, Turnip, VirGL or Vortek is installed merely because Android
 * advertises Vulkan/OpenGL support.
 */
data class DeviceCompatibility(
    val supportedAbis: List<String>,
    val isArm64: Boolean,
    val androidApi: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val glesVersion: String?,
    val glVendor: String?,
    val glRenderer: String?,
    val vulkanFeaturePresent: Boolean,
    val vulkan11FeaturePresent: Boolean,
    val suggestedRenderers: List<String>,
    val notes: List<String>
) {
    companion object {
        fun inspect(activity: Activity): DeviceCompatibility {
            val pm = activity.packageManager
            val memory = ActivityManager.MemoryInfo()
            (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .getMemoryInfo(memory)

            val abis = Build.SUPPORTED_ABIS.toList()
            val arm64 = abis.any { it.equals("arm64-v8a", ignoreCase = true) }
            val vulkan = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
            val vulkan11 = if (Build.VERSION.SDK_INT >= 24) {
                runCatching {
                    pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION) &&
                        (pm.getSystemAvailableFeatures().any { feature ->
                            feature.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION &&
                                feature.version >= 0x00401000
                        })
                }.getOrDefault(false)
            } else false

            val gl = probeOpenGl(activity)
            val renderers = mutableListOf<String>()
            if (vulkan) renderers += "DXVK + system Vulkan (runtime must include DXVK)"
            if (gl.first != null) renderers += "WineD3D + OpenGL (runtime must include WineD3D)"
            if (renderers.isEmpty()) renderers += "No supported hardware renderer confirmed"
            val notes = mutableListOf<String>()
            if (!arm64) notes += "ARM64 ABI was not detected; the selected runtime must support this CPU ABI."
            if (memory.totalMem < 3L * 1024L * 1024L * 1024L) {
                notes += "Low RAM class: use conservative resolution and close background apps."
            }
            if (!vulkan) notes += "Android did not advertise Vulkan hardware support; Vulkan rendering is not recommended."
            if (vulkan && !vulkan11) notes += "Vulkan 1.1 was not confirmed; test only a compatible legacy graphics path."
            notes += "Renderer choice is provisional until the bundled runtime and Blur are smoke-tested."
            return DeviceCompatibility(
                supportedAbis = abis,
                isArm64 = arm64,
                androidApi = Build.VERSION.SDK_INT,
                totalRamMb = memory.totalMem / (1024L * 1024L),
                availableRamMb = memory.availMem / (1024L * 1024L),
                glesVersion = gl.first,
                glVendor = gl.second,
                glRenderer = gl.third,
                vulkanFeaturePresent = vulkan,
                vulkan11FeaturePresent = vulkan11,
                suggestedRenderers = renderers,
                notes = notes
            )
        }

        private fun probeOpenGl(activity: Activity): Triple<String?, String?, String?> {
            val latch = CountDownLatch(1)
            var version: String? = null
            var vendor: String? = null
            var renderer: String? = null
            val view = GLSurfaceView(activity)
            view.setEGLContextClientVersion(2)
            view.setRenderer(object : GLSurfaceView.Renderer {
                override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                    version = GLES20.glGetString(GLES20.GL_VERSION)
                    vendor = GLES20.glGetString(GLES20.GL_VENDOR)
                    renderer = GLES20.glGetString(GLES20.GL_RENDERER)
                    latch.countDown()
                }
                override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) = Unit
                override fun onDrawFrame(gl: GL10?) = Unit
            })
            // A hidden, temporary view can fail on some vendor implementations. Treat that
            // as unknown rather than crashing the launcher.
            runCatching {
                activity.addContentView(view, android.view.ViewGroup.LayoutParams(1, 1))
                view.visibility = android.view.View.INVISIBLE
                latch.await(2, TimeUnit.SECONDS)
                view.onPause()
                (view.parent as? android.view.ViewGroup)?.removeView(view)
            }
            return Triple(version, vendor, renderer)
        }
    }

    fun toDisplayText(): String = buildString {
        appendLine("CPU ABI: ${supportedAbis.joinToString().ifBlank { "unknown" }}")
        appendLine("Android API: $androidApi")
        appendLine("RAM: ${totalRamMb} MB total · ${availableRamMb} MB available")
        appendLine("OpenGL ES: ${glesVersion ?: "not detected"}")
        appendLine("GPU: ${glVendor ?: "unknown"} · ${glRenderer ?: "unknown"}")
        appendLine("Vulkan: ${if (vulkanFeaturePresent) "available" else "not advertised"}")
        appendLine("Vulkan 1.1: ${if (vulkan11FeaturePresent) "confirmed" else "not confirmed"}")
        appendLine("Candidate paths:")
        suggestedRenderers.forEach { appendLine("• $it") }
        notes.forEach { appendLine("• $it") }
    }
}
