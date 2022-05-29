package com.github.libretube

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class RouterActivity : AppCompatActivity() {
    val TAG = "RouterActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    handleSendText(intent) // Handle text being sent
                }
            }
        }
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            Log.i(TAG, it)
            val pm: PackageManager = this.packageManager
            val intent = pm.getLaunchIntentForPackage(this.packageName)
            intent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent?.data = Uri.parse(it)
            this.startActivity(intent)
        }
    }
}
