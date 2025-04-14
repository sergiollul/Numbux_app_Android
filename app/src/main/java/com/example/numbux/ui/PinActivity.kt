package com.example.numbux.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.numbux.R
import com.example.numbux.control.BlockManager

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
                finish()
            } else {
                Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        BlockManager.isShowingPin = false
    }

    override fun onDestroy() {
        super.onDestroy()
        BlockManager.isShowingPin = false
    }

}
