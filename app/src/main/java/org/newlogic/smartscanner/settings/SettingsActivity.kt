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

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.idpass.smartscanner.lib.platform.utils.LanguageUtils
import org.idpass.smartscanner.lib.scanner.config.Language
import org.idpass.smartscanner.lib.scanner.config.Orientation
import org.newlogic.smartscanner.BuildConfig
import org.newlogic.smartscanner.MainActivity
import org.newlogic.smartscanner.R
import org.newlogic.smartscanner.SmartScannerApplication
import org.newlogic.smartscanner.databinding.ActivitySettingsBinding


class SettingsActivity : AppCompatActivity() {

    companion object {
      val ORIENTATION = "Orientation"
    }

    private lateinit var binding : ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setupViews()
    }

    private fun setupViews() {
        val preference = getSharedPreferences(SmartScannerApplication.SHARED, Context.MODE_PRIVATE)
        val editor = preference.edit()
        val currentLanguage = resources.configuration.locale.displayLanguage
        if (currentLanguage == "English") {
            binding.arabicpic.visibility = View.INVISIBLE
            binding.englishpic.visibility = View.VISIBLE
        } else {
            binding.arabicpic.visibility = View.VISIBLE
            binding.englishpic.visibility = View.INVISIBLE
        }

        // Arabic language
        binding.arabiclayout.setOnClickListener {
            binding.arabicpic.visibility = View.VISIBLE
            binding.englishpic.visibility = View.INVISIBLE
            saveLanguage(editor = editor, language = Language.AR)
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        // English language
        binding.englishLayout.setOnClickListener {
            binding.arabicpic.visibility = View.INVISIBLE
            binding.englishpic.visibility = View.VISIBLE
            saveLanguage(editor = editor, language = Language.EN)
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }

        // Orientation (Portrait/Landscape)
        val orientation = preference.getString(ORIENTATION, Orientation.LANDSCAPE.value)
        if (orientation == Orientation.LANDSCAPE.value) {
            binding.portraitCheck.visibility = View.INVISIBLE
            binding.landscapeCheck.visibility = View.VISIBLE
        } else {
            binding.portraitCheck.visibility = View.VISIBLE
            binding.landscapeCheck.visibility = View.INVISIBLE
        }

        // Landscape
        binding.landscapeLayout.setOnClickListener {
            binding.portraitCheck.visibility = View.INVISIBLE
            binding.landscapeCheck.visibility = View.VISIBLE
            saveToPreference(editor = editor, key = ORIENTATION, value = Orientation.LANDSCAPE.value)
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        // Portrait
        binding.portraitLayout.setOnClickListener {
            binding.portraitCheck.visibility = View.VISIBLE
            binding.landscapeCheck.visibility = View.INVISIBLE
            saveToPreference(editor = editor, key = ORIENTATION, value = Orientation.PORTRAIT.value)
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }

        // Display version
        val version = BuildConfig.VERSION_NAME
        val versionLabel = if (BuildConfig.DEBUG) version else version.split("-").first()
        binding.versionText.text = getString(R.string.label_version, versionLabel)

        // Go Back
        binding.backspace.setOnClickListener {
            onBackPressed()
            this.finish()
        }
    }

    private fun saveLanguage(editor: SharedPreferences.Editor, language : String) {
        // Set new language
        LanguageUtils.changeLanguage(this, language)
        // Save new language to sharedPrefs
        saveToPreference(editor, Language.NAME, language)
    }

    private fun saveToPreference( editor: SharedPreferences.Editor, key: String, value : String) {
        // Remove previous set language
        editor.remove(key).apply()
        editor.putString(key, value)
        editor.apply()
    }
}