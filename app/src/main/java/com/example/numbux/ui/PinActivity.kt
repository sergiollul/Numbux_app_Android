package com.example.numbux.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.numbux.R
import com.example.numbux.control.BlockManager
import android.util.Log

class PinActivity : Activity() {

    private val correctPin = "1234" // Puedes vincular esto a Firebase después

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val input = findViewById<EditText>(R.id.editTextPin)
        val btn = findViewById<Button>(R.id.buttonUnlock)
        val appPackage = intent.getStringExtra("app_package")

        btn.setOnClickListener {
            if (input.text.toString() == correctPin) {
                if (!appPackage.isNullOrEmpty()) {
                    BlockManager.allowTemporarily(appPackage)
                    Toast.makeText(this, "App desbloqueada temporalmente", Toast.LENGTH_SHORT).show()
                }
                setResult(Activity.RESULT_OK)
                BlockManager.isShowingPin = false
                finish()
            } else {
                Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("Numbux", "🛑 PinActivity -> onPause")
        BlockManager.isShowingPin = false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Numbux", "💀 PinActivity -> onDestroy")
        BlockManager.isShowingPin = false

        // Si no se desbloqueó correctamente, recordar que fue rechazado
        val appPackage = intent.getStringExtra("app_package")
        if (!appPackage.isNullOrEmpty()) {
            BlockManager.dismissUntilAppChanges(appPackage)
            Log.d("Numbux", "❌ PIN rechazado para $appPackage (temporalmente ignorado)")
        }
    }

    override fun onBackPressed() {
        // No permitir cerrar con back
    }
}
