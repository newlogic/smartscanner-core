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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.idpass.smartscanner.MainActivity
import org.idpass.smartscanner.R
import java.util.*


class SettingsActivity : AppCompatActivity() {
    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_languages)

        val engilshpic = findViewById<ImageView>(R.id.engilshpic)
        val arabicpic = findViewById<ImageView>(R.id.arabicpic)
        val arabicLayout = findViewById<LinearLayout>(R.id.arabiclayout)
        val backspace = findViewById<ImageView>(R.id.backspace)
        val preference = getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
        val editor = preference.edit()
        val currentLanguage = resources.configuration.locale.displayLanguage

        if (currentLanguage == "English") {
            arabicpic.visibility = View.INVISIBLE
            engilshpic.visibility = View.VISIBLE
        } else {
            arabicpic.visibility = View.VISIBLE
            engilshpic.visibility = View.INVISIBLE
        }

        // Arabic language
        arabicLayout.setOnClickListener {
            val locale = Locale("ar")
            Locale.setDefault(locale)
            val config = Configuration()
            config.locale = locale
            baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
            Toast.makeText(applicationContext, Locale.getDefault().language, Toast.LENGTH_SHORT).show()

            arabicpic.visibility = View.VISIBLE
            engilshpic.visibility = View.INVISIBLE
            editor.putString("name", "ar")
            editor.apply()
            startActivity(Intent(this, MainActivity::class.java))
        }

        // English language
        val englishLayout = findViewById<LinearLayout>(R.id.engilshlayout)
        englishLayout.setOnClickListener {
            val locale = Locale("en")
            Locale.setDefault(locale)
            val config = Configuration()
            config.locale = locale
            baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
            arabicpic.visibility = View.INVISIBLE
            engilshpic.visibility = View.VISIBLE
            editor.putString("name", "en")
            editor.apply()
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Go Back
        backspace.setOnClickListener { onBackPressed() }
    }
}