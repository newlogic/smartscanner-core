/*
 * Copyright (C) 2020 Newlogic Pte. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package org.newlogic.smartscanner.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonParser
import org.idpass.smartscanner.lib.SmartScannerActivity
import org.idpass.smartscanner.lib.scanner.config.*
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.CONFIG_PROFILE_NAME
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.CONFIG_PUB_KEY
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.OP_SCANNER
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.ORIENTATION
import org.idpass.smartscanner.lib.scanner.config.Orientation.LANDSCAPE
import org.idpass.smartscanner.lib.scanner.config.Orientation.PORTRAIT
import org.idpass.smartscanner.lib.utils.LanguageUtils
import org.newlogic.smartscanner.BuildConfig
import org.newlogic.smartscanner.MainActivity
import org.newlogic.smartscanner.R
import org.newlogic.smartscanner.databinding.ActivitySettingsBinding
import org.newlogic.smartscanner.result.ResultActivity


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var preference: SharedPreferences? = null
    private var isConfigUpdated: Boolean = false

    companion object {
        const val CONFIG_UPDATED = "CONFIG_UPDATED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isConfigUpdated = intent.getBooleanExtra(CONFIG_UPDATED, false)

        preference = getSharedPreferences(Config.SHARED, Context.MODE_PRIVATE)
    }

    override fun onStart() {
        super.onStart()
        // Language (English/Arabic)
        val currentLanguage = resources.configuration.locale.displayLanguage
        if (currentLanguage == "English") {
            binding.arabicpic.visibility = View.INVISIBLE
            binding.englishpic.visibility = View.VISIBLE
        } else {
            binding.arabicpic.visibility = View.VISIBLE
            binding.englishpic.visibility = View.INVISIBLE
        }
        // Orientation (Portrait/Landscape)
        val orientation = preference?.getString(ORIENTATION, PORTRAIT.value)
        if (orientation == PORTRAIT.value) {
            binding.portraitCheck.visibility = View.VISIBLE
            binding.landscapeCheck.visibility = View.INVISIBLE
        } else {
            binding.portraitCheck.visibility = View.INVISIBLE
            binding.landscapeCheck.visibility = View.VISIBLE
        }
        // Configuration Profile
        if (
            preference?.getString(CONFIG_PROFILE_NAME, null) == null ||
            preference?.getString(CONFIG_PUB_KEY, null) == null
        ) {
            binding.layoutConfigEmpty.visibility = View.VISIBLE
            binding.layoutConfigLoaded.visibility = View.GONE
        } else {
            binding.layoutConfigEmpty.visibility = View.GONE
            binding.layoutConfigLoaded.visibility = View.VISIBLE
            binding.tvConfigName.text = preference?.getString(CONFIG_PROFILE_NAME, "")
            binding.tvConfigPubKey.text = preference?.getString(CONFIG_PUB_KEY, "")
        }
        // Display version
        val version = BuildConfig.VERSION_NAME
        val versionLabel = if (BuildConfig.DEBUG) version else version.split("-").first()
        binding.versionText.text = getString(R.string.label_version, versionLabel)

        // Display Toast message once a flag for config update's true
        if (isConfigUpdated) {
            Toast.makeText(
                this@SettingsActivity,
                getString(R.string.config_loaded),
                Toast.LENGTH_LONG)
                .show()
        }

        // Setup click listeners
        setupViewListeners(preference)
    }

    private fun setupViewListeners(preference: SharedPreferences?) {
        val editor = preference?.edit()
        // Language (English/Arabic)
        binding.arabiclayout.setOnClickListener {
            binding.arabicpic.visibility = View.VISIBLE
            binding.englishpic.visibility = View.INVISIBLE
            saveLanguage(editor = editor, language = Language.AR)
            startActivity(
                Intent(
                    this,
                    MainActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
        binding.englishLayout.setOnClickListener {
            binding.arabicpic.visibility = View.INVISIBLE
            binding.englishpic.visibility = View.VISIBLE
            saveLanguage(editor = editor, language = Language.EN)
            startActivity(
                Intent(
                    this,
                    MainActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }

        // Orientation (Portrait/Landscape)
        binding.landscapeLayout.setOnClickListener {
            binding.portraitCheck.visibility = View.INVISIBLE
            binding.landscapeCheck.visibility = View.VISIBLE
            saveToPreference(key = ORIENTATION, value = LANDSCAPE.value)
            startActivity(
                Intent(
                    this,
                    MainActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
        binding.portraitLayout.setOnClickListener {
            binding.portraitCheck.visibility = View.VISIBLE
            binding.landscapeCheck.visibility = View.INVISIBLE
            saveToPreference(key = ORIENTATION, value = PORTRAIT.value)
            startActivity(
                Intent(
                    this,
                    MainActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }

        // Go Back
        binding.backspace.setOnClickListener {
            onBackPressed()
            this.finish()
        }

        binding.btnConfigReset.setOnClickListener {
            // Reset and remove config profile name and public key
            preference?.edit()?.remove(CONFIG_PUB_KEY)?.apply()
            preference?.edit()?.remove(CONFIG_PROFILE_NAME)?.apply()
            binding.tvConfigName.text = ""
            binding.tvConfigPubKey.text = ""

            binding.layoutConfigEmpty.visibility = View.VISIBLE
            binding.layoutConfigLoaded.visibility = View.GONE
        }
    }

    private fun saveLanguage(editor: SharedPreferences.Editor?, language: String) {
        // Set new language
        saveToPreference(Language.NAME, language)
        LanguageUtils.changeLanguage(this, language)
    }

    private fun saveToPreference(key: String, value: String?) {
        // Remove previous set
        val editor = preference?.edit()
        editor?.remove(key)?.apply()
        editor?.putString(key, value)
        editor?.apply()
    }

}