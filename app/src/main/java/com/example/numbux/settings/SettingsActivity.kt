package com.example.numbux.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity              // ← AppCompatActivity has supportFragmentManager
import androidx.preference.PreferenceFragmentCompat
import com.example.numbux.R                               // ← import your app’s R class

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
