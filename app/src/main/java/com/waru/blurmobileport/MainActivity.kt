package com.waru.blurmobileport

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile

/**
 * Lightweight native launcher shell.
 *
 * Intentionally uses platform Views and shape drawables instead of large image assets,
 * animation libraries, or Compose so it stays small and responsive on entry-level phones.
 * This app does not execute Blur.exe; a compatible runtime is a separate future task.
 */
class MainActivity : Activity() {
    private lateinit var statusTitle: TextView
    private lateinit var statusBody: TextView
    private lateinit var folderPath: TextView
    private lateinit var chooseButton: Button
    private lateinit var forgetButton: Button

    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private var selectedTree: Uri? = null

    private val bg = Color.rgb(10, 14, 22)
    private val panel = Color.rgb(20, 27, 39)
    private val panelAlt = Color.rgb(25, 35, 50)
    private val accent = Color.rgb(49, 145, 255)
    private val accentBright = Color.rgb(91, 180, 255)
    private val textPrimary = Color.rgb(242, 246, 252)
    private val textMuted = Color.rgb(157, 172, 192)
    private val green = Color.rgb(80, 214, 151)
    private val amber = Color.rgb(255, 190, 92)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        buildUi()
        restoreSelection()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(26))
        }

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val mark = TextView(this).apply {
            text = "B"
            textSize = 30f
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(accent, 14)
        }
        brandRow.addView(mark, LinearLayout.LayoutParams(dp(56), dp(56)))
        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), 0, 0, 0)
            addView(label("BLUR", 24f, textPrimary, true))
            addView(label("ANDROID COMPANION", 10f, accentBright, true).apply {
                letterSpacing = 0.13f
            })
        }
        brandRow.addView(brandText, LinearLayout.LayoutParams(0, -2, 1f))
        brandRow.addView(label("v0.1.0", 11f, textMuted, false))
        page.addView(brandRow)

        val hero = card(panelAlt).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(20))
        }
        hero.addView(label("THE RACE STARTS HERE", 11f, accentBright, true).apply {
            letterSpacing = 0.12f
        })
        hero.addView(label("Your Blur library.\nOne place.", 28f, textPrimary, true).apply {
            setPadding(0, dp(10), 0, dp(8))
        })
        hero.addView(label(
            "Choose your own PC game folder and check whether the main executable is present.",
            14f, textMuted, false
        ))
        page.addView(hero, marginTop(24))

        page.addView(sectionTitle("GAME LIBRARY"), marginTop(25))
        val library = card(panel).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val libraryHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        libraryHeader.addView(label("BLUR (2010) · PC", 15f, textPrimary, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        libraryHeader.addView(pill("LOCAL FILES", accent))
        library.addView(libraryHeader)
        folderPath = label("No game folder selected yet.", 12f, textMuted, false).apply {
            setPadding(0, dp(12), 0, dp(8))
        }
        library.addView(folderPath)
        statusTitle = label("Waiting for game folder", 14f, amber, true)
        statusTitle.setPadding(0, dp(5), 0, dp(5))
        library.addView(statusTitle)
        statusBody = label(
            "Select the folder where Blur.exe is located. Your choice is saved on this device.",
            12f, textMuted, false
        )
        library.addView(statusBody)
        page.addView(library, marginTop(10))

        chooseButton = Button(this).apply {
            text = "SELECT GAME FOLDER"
            isAllCaps = false
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.WHITE)
            background = rounded(accent, 14)
            minHeight = dp(54)
            setPadding(dp(16), 0, dp(16), 0)
            setOnClickListener { chooseGameFolder() }
        }
        page.addView(chooseButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(54)
        ).apply { topMargin = dp(14) })

        forgetButton = Button(this).apply {
            text = "Forget saved folder"
            isAllCaps = false
            textSize = 13f
            setTextColor(textMuted)
            background = rounded(panel, 12)
            setOnClickListener { forgetFolder() }
        }
        page.addView(forgetButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(46)
        ).apply { topMargin = dp(8) })

        page.addView(sectionTitle("PERFORMANCE FIRST"), marginTop(26))
        val perf = card(panel).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        perf.addView(label("Lightweight by design", 15f, textPrimary, true))
        perf.addView(label(
            "Native Android views • no background services • no animated wallpaper • no large image packs.",
            13f, textMuted, false
        ).apply { setPadding(0, dp(7), 0, dp(12)) })
        val tags = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tags.addView(pill("LOW RAM", green))
        tags.addView(space(dp(8)))
        tags.addView(pill("LOW STORAGE", accentBright))
        perf.addView(tags)
        page.addView(perf, marginTop(10))

        page.addView(sectionTitle("DEVICE COMPATIBILITY"), marginTop(26))
        val compatibilityCard = card(panel).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        compatibilityCard.addView(label("Automatic hardware detection", 15f, textPrimary, true))
        compatibilityCard.addView(label(
            "Reads CPU ABI, Android version, RAM, OpenGL ES renderer and advertised Vulkan support. It suggests possible paths; it cannot confirm Blur compatibility until the actual runtime is integrated.",
            12f, textMuted, false
        ).apply { setPadding(0, dp(7), 0, dp(10)) })
        val compatibilityDetails = label("Checking device capabilities...", 11f, textMuted, false)
        compatibilityCard.addView(compatibilityDetails)
        page.addView(compatibilityCard, marginTop(10))
        compatibilityDetails.text = DeviceCompatibility.inspect(this).toDisplayText()
        val runtimePreflight = RuntimeBootstrap.inspect(this)
        compatibilityDetails.append("\n\nRUNTIME PREFLIGHT\n")
        compatibilityDetails.append("Wine: ${if (runtimePreflight.winePresent) "detected" else "missing"} · ")
        compatibilityDetails.append("Box64: ${if (runtimePreflight.box64Present) "detected" else "missing"}\n")
        compatibilityDetails.append("DXVK: ${if (runtimePreflight.dxvkPresent) "detected" else "missing"} · ")
        compatibilityDetails.append("WineD3D: ${if (runtimePreflight.wineD3dPresent) "detected" else "missing"}\n")
        compatibilityDetails.append("Selected path: ${runtimePreflight.selectedRenderer.displayName}\n")
        compatibilityDetails.append(runtimePreflight.explanation)

        page.addView(sectionTitle("IMPORTANT"), marginTop(26))
        val notice = card(Color.rgb(35, 29, 22)).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
        }
        notice.addView(label("Game engine not integrated", 14f, amber, true))
        notice.addView(label(
            "This launcher currently checks your files only. It does not run Blur.exe yet. A compatible Windows runtime and Direct3D-to-OpenGL layer (WineD3D candidate) must be integrated and tested before gameplay is possible.",
            12f, Color.rgb(220, 202, 176), false
        ).apply { setPadding(0, dp(7), 0, 0) })
        page.addView(notice, marginTop(10))

        page.addView(label(
            "Blur Mobile Port • Community prototype\nGame files remain on your device.",
            11f, textMuted, false
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(4))
        })

        scroll.addView(page)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun restoreSelection() {
        val raw = prefs.getString(KEY_TREE, null) ?: return
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return
        selectedTree = uri
        refreshFolder(uri)
    }

    private fun chooseGameFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_TREE)
    }

    private fun forgetFolder() {
        selectedTree?.let { uri ->
            runCatching {
                contentResolver.releasePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        selectedTree = null
        prefs.edit().remove(KEY_TREE).apply()
        folderPath.text = "No game folder selected yet."
        statusTitle.text = "Waiting for game folder"
        statusTitle.setTextColor(amber)
        statusBody.text = "Select the folder where Blur.exe is located. Your choice is saved on this device."
    }

    @Deprecated("Use Activity Result APIs in a production app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_TREE || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val readFlags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            contentResolver.takePersistableUriPermission(uri, readFlags)
        } catch (_: SecurityException) {
            statusTitle.text = "Folder access could not be saved"
            statusTitle.setTextColor(amber)
        }
        selectedTree = uri
        prefs.edit().putString(KEY_TREE, uri.toString()).apply()
        refreshFolder(uri)
    }

    private fun refreshFolder(uri: Uri) {
        folderPath.text = uri.lastPathSegment?.substringAfterLast(':')?.ifBlank { "Selected folder" }
            ?: "Selected folder"
        val folder = DocumentFile.fromTreeUri(this, uri)
        val exe = folder?.findFile("Blur.exe")
        if (exe != null && exe.isFile) {
            statusTitle.text = "EXECUTABLE FOUND"
            statusTitle.setTextColor(green)
            statusBody.text = "Blur.exe is present. The launcher is ready for runtime integration; gameplay is not available in this prototype."
        } else {
            statusTitle.text = "EXECUTABLE NOT FOUND"
            statusTitle.setTextColor(amber)
            statusBody.text = "Blur.exe was not found in this folder's top level. Select the actual installation folder."
        }
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            typeface = Typeface.create(
                if (bold) "sans-serif-medium" else "sans-serif",
                if (bold) Typeface.BOLD else Typeface.NORMAL
            )
            includeFontPadding = true
        }

    private fun sectionTitle(value: String) = label(value, 11f, textMuted, true).apply {
        letterSpacing = 0.14f
    }

    private fun card(color: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(color, 18)
    }

    private fun pill(value: String, color: Int) = TextView(this).apply {
        text = value
        textSize = 9f
        letterSpacing = 0.07f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        setTextColor(color)
        gravity = Gravity.CENTER
        setPadding(dp(9), dp(6), dp(9), dp(6))
        background = rounded(Color.argb(28, Color.red(color), Color.green(color), Color.blue(color)), 20)
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun marginTop(value: Int) = LinearLayout.LayoutParams(-1, -2).apply {
        topMargin = dp(value)
    }

    private fun space(widthDp: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(widthDp), 1)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        private const val PREFS = "blur_mobile_port"
        private const val KEY_TREE = "game_tree_uri"
        private const val REQUEST_TREE = 4201
    }
}
