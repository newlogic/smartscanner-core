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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import org.idpass.smartscanner.lib.scanner.config.*
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.CONFIG_PROFILE_NAME
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.CONFIG_PUB_KEY
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.HEIGHT_GUIDE
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.ORIENTATION
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.REGEX
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.WIDTH_GUIDE
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.X_GUIDE
import org.idpass.smartscanner.lib.scanner.config.Config.Companion.Y_GUIDE
import org.idpass.smartscanner.lib.scanner.config.Orientation.LANDSCAPE
import org.idpass.smartscanner.lib.scanner.config.Orientation.PORTRAIT
import org.idpass.smartscanner.lib.utils.LanguageUtils
import org.newlogic.smartscanner.BuildConfig
import org.newlogic.smartscanner.R
import org.newlogic.smartscanner.databinding.ActivitySettingsBinding


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var preference: SharedPreferences? = null
    private var isConfigUpdated: Boolean = false
    private var isOcrSettingsCall: Boolean = false
    private var extras: ScannerOptions? = null

    companion object {
        const val CONFIG_UPDATED = "CONFIG_UPDATED"
        const val OCR_SETTINGS_CALL = "OCR_SETTINGS_CALL"
        const val SCANNER_INTENT_EXTRAS = "SCANNER_INTENT_EXTRAS"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isConfigUpdated = intent.getBooleanExtra(CONFIG_UPDATED, false)

        isOcrSettingsCall = intent.getBooleanExtra(OCR_SETTINGS_CALL, false)

        extras = intent.getParcelableExtra(SCANNER_INTENT_EXTRAS)

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

        if (isOcrSettingsCall) {
            animateCollapsableUi()
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
                Toast.LENGTH_LONG
            ).show()
        }

        // Setup click listeners
        setupViewListeners(preference)

        //set ocr settings values based on saved values
        binding.regex.setText(preference?.getString(REGEX, "") ?: "")
        binding.widthGuide.setText((preference?.getInt(WIDTH_GUIDE, 0) ?: 0).toString())
        binding.heightGuide.setText((preference?.getInt(HEIGHT_GUIDE, 0) ?: 0).toString())
        binding.x.progress = ((preference?.getFloat(X_GUIDE, 0f) ?: 0f) * 100f).toInt()
        binding.y.progress = ((preference?.getFloat(Y_GUIDE, 0f) ?: 0f) * 100f).toInt()
    }

    private fun setupViewListeners(preference: SharedPreferences?) {
        // Language (English/Arabic)
        binding.arabiclayout.setOnClickListener {
            binding.arabicpic.visibility = View.VISIBLE
            binding.englishpic.visibility = View.INVISIBLE
            saveLanguage(language = Language.AR)
            binding.backspace.callOnClick()
        }
        binding.englishLayout.setOnClickListener {
            binding.arabicpic.visibility = View.INVISIBLE
            binding.englishpic.visibility = View.VISIBLE
            saveLanguage(language = Language.EN)
            binding.backspace.callOnClick()
        }

        // Orientation (Portrait/Landscape)
        binding.landscapeLayout.setOnClickListener {
            binding.portraitCheck.visibility = View.INVISIBLE
            binding.landscapeCheck.visibility = View.VISIBLE
            saveToPreference(key = ORIENTATION, value = LANDSCAPE.value)
            binding.backspace.callOnClick()
        }
        binding.portraitLayout.setOnClickListener {
            binding.portraitCheck.visibility = View.VISIBLE
            binding.landscapeCheck.visibility = View.INVISIBLE
            saveToPreference(key = ORIENTATION, value = PORTRAIT.value)
            binding.backspace.callOnClick()
        }

        // Go Back
        binding.backspace.setOnClickListener {
            saveOCRValues()

            if (extras != null) {
                val data = Intent()
                data.putExtra(SCANNER_INTENT_EXTRAS, extras)
                setResult(Activity.RESULT_OK, data)
            }
            finish()
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

        binding.ocrCollapseButton.setOnClickListener {
            animateCollapsableUi()
        }

        binding.scrollView.post {
            binding.scrollView.scrollTo(0, binding.ocrCollapseButton.top)
        }

        binding.x.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val percentageString = "$p1%"
                binding.xValue.text = percentageString
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        binding.y.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val percentageString = "$p1%"
                binding.yValue.text = percentageString
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })
    }

    private fun saveOCRValues() {
        val regex = binding.regex.text?.toString()
        saveRegex(regex)

        val widthString = binding.widthGuide.text?.toString()
        val width = if (widthString?.isNotEmpty() == true) widthString.toInt() else 0
        saveWidthGuide(width)


        val heightString = binding.heightGuide.text?.toString()
        val height = if (heightString?.isNotEmpty() == true) heightString.toInt() else 0
        saveHeightGuide(height)

        val x = binding.x.progress.toFloat() / 100f
        saveXGuide(x)

        val y = binding.y.progress.toFloat() / 100f
        saveYGuide(y)

        val enableOCRGuide = width != 0 && height != 0

        if (extras?.mode != Modes.OCR.value) return
        extras = extras?.copy(
            ocrOptions = extras?.ocrOptions?.copy(
                regex = regex
            ),
            config = extras?.config?.copy(
                showOcrGuide = enableOCRGuide,
                widthGuide = width,
                heightGuide = height,
                yGuide = y,
                xGuide = x
            )
        )
    }

    private fun saveLanguage(language: String) {
        // Set new language
        saveToPreference(Language.NAME, language)
        LanguageUtils.changeLanguage(this, language)

        if (extras != null) {
            extras = extras?.copy(
                language = language
            )
        }
    }

    private fun saveRegex(regex: String?) {
        // Set new language
        saveToPreference(REGEX, regex)
    }

    private fun saveWidthGuide(width: Int) {
        // Set new language
        saveIntToPreference(WIDTH_GUIDE, width)
    }

    private fun saveHeightGuide(height: Int) {
        // Set new language
        saveIntToPreference(HEIGHT_GUIDE, height)
    }

    private fun saveXGuide(x: Float) {
        // Set new language
        saveFloatToPreference(X_GUIDE, x)
    }

    private fun saveYGuide(y: Float) {
        // Set new language
        saveFloatToPreference(Y_GUIDE, y)
    }

    private fun saveToPreference(key: String, value: String?) {
        // Remove previous set
        val editor = preference?.edit()
        editor?.remove(key)?.apply()
        editor?.putString(key, value)
        editor?.apply()
    }

    private fun saveFloatToPreference(key: String, value: Float) {
        // Remove previous set
        val editor = preference?.edit()
        editor?.remove(key)?.apply()
        editor?.putFloat(key, value)
        editor?.apply()
    }

    private fun saveIntToPreference(key: String, value: Int) {
        // Remove previous set
        val editor = preference?.edit()
        editor?.remove(key)?.apply()
        editor?.putInt(key, value)
        editor?.apply()
    }

    private fun animateCollapsableUi() {
        val isExpanded = binding.ocrSettingsOptionsLayout.isVisible
        binding.ocrSettingsArrow.rotation = (if (isExpanded) 0f else -180f)
        binding.ocrSettingsOptionsLayout.visibility = if (isExpanded) View.GONE else View.VISIBLE
    }

}