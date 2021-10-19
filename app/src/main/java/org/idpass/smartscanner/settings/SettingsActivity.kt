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
package org.idpass.smartscanner.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.idpass.smartscanner.BuildConfig
import org.idpass.smartscanner.MainActivity
import org.idpass.smartscanner.R
import org.idpass.smartscanner.SmartScannerApplication
import org.idpass.smartscanner.lib.platform.utils.LanguageUtils
import org.idpass.smartscanner.lib.scanner.config.Language


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_languages)

        val arabicpic = findViewById<ImageView>(R.id.arabicpic)
        val englishpic = findViewById<ImageView>(R.id.englishpic)
        val arabicLayout = findViewById<LinearLayout>(R.id.arabiclayout)
        val englishLayout = findViewById<LinearLayout>(R.id.englishLayout)
        val backspace = findViewById<ImageView>(R.id.backspace)
        val versionText = findViewById<TextView>(R.id.version_text)

        val preference = getSharedPreferences(SmartScannerApplication.SHARED, Context.MODE_PRIVATE)
        val editor = preference.edit()
        val currentLanguage = resources.configuration.locale.displayLanguage

        if (currentLanguage == "English") {
            arabicpic.visibility = View.INVISIBLE
            englishpic.visibility = View.VISIBLE
        } else {
            arabicpic.visibility = View.VISIBLE
            englishpic.visibility = View.INVISIBLE
        }

        // Arabic language
        arabicLayout.setOnClickListener {
            arabicpic.visibility = View.VISIBLE
            englishpic.visibility = View.INVISIBLE
            saveLanguage(editor = editor, language = Language.AR)
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        // English language
        englishLayout.setOnClickListener {
            arabicpic.visibility = View.INVISIBLE
            englishpic.visibility = View.VISIBLE
            saveLanguage(editor = editor, language = Language.EN)
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }

        // Go Back
        backspace.setOnClickListener {
            onBackPressed()
            this.finish()
        }

        // Display version
        val version = BuildConfig.VERSION_NAME
        val versionLabel = if (BuildConfig.DEBUG) version else version.split("-").first()
        versionText.text = getString(R.string.label_version, versionLabel)
    }

    private fun saveLanguage(editor: SharedPreferences.Editor, language : String) {
        // Remove previous set language
        editor.remove(Language.NAME).apply()
        // Set new language
        LanguageUtils.changeLanguage(this, language)
        // Save new language to sharedPrefs
        editor.putString(Language.NAME, language)
        editor.apply()
    }
}