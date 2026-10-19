package com.waru.blurmobileport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 32, 28, 28)
        }
        content.addView(TextView(this).apply {
            text = "BLUR MOBILE PORT"
            textSize = 26f
            setPadding(0, 8, 0, 16)
        })
        content.addView(TextView(this).apply {
            text = "Choose the folder containing your legally obtained Blur (2010) PC installation."
            textSize = 16f
            setPadding(0, 0, 0, 18)
        })
        content.addView(Button(this).apply {
            text = "Select Blur game folder"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
                startActivityForResult(intent, REQUEST_TREE)
            }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        status = TextView(this).apply {
            text = "No game folder selected."
            textSize = 15f
            setPadding(0, 20, 0, 18)
        }
        content.addView(status)
        content.addView(TextView(this).apply {
            text = "This prototype checks files only; it does not run Blur.exe yet. A compatible Windows runtime and graphics translation layer still need to be integrated and tested."
            textSize = 14f
        })

        val scroll = ScrollView(this)
        scroll.addView(content)
        setContentView(scroll)
    }

    @Deprecated("Use Activity Result APIs in a production app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_TREE || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val flags = data.flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        try {
            contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
        }

        val folder = DocumentFile.fromTreeUri(this, uri)
        val exe = folder?.findFile("Blur.exe")
        status.text = if (exe != null && exe.isFile) {
            "Blur.exe found. The game engine is not integrated yet.\n\nSelected folder: $uri"
        } else {
            "Blur.exe was not found at the top level. Choose the actual game installation folder.\n\nSelected folder: $uri"
        }
    }

    companion object {
        private const val REQUEST_TREE = 4201
    }
}
