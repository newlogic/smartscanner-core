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
package org.idpass.smartscanner.lib.nfc

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonParser
import org.idpass.smartscanner.lib.BuildConfig
import org.idpass.smartscanner.lib.R
import org.idpass.smartscanner.lib.databinding.ActivityNfcBinding
import org.idpass.smartscanner.lib.nfc.passport.Passport
import org.idpass.smartscanner.lib.nfc.passport.PassportDetailsFragment
import org.idpass.smartscanner.lib.nfc.passport.PassportPhotoFragment
import org.idpass.smartscanner.lib.platform.utils.FileUtils
import org.idpass.smartscanner.lib.platform.utils.LoggerUtils
import org.jmrtd.lds.icao.MRZInfo


class NFCActivity : androidx.fragment.app.FragmentActivity(), NFCFragment.NfcFragmentListener, PassportDetailsFragment.PassportDetailsFragmentListener, PassportPhotoFragment.PassportPhotoFragmentListener {

    companion object {
        private val TAG = NFCActivity::class.java.simpleName

        private val TAG_NFC = "TAG_NFC"
        private val TAG_PASSPORT_DETAILS = "TAG_PASSPORT_DETAILS"
        private val TAG_PASSPORT_PICTURE = "TAG_PASSPORT_PICTURE"

        const val RESULT = "NFC_RESULT"
        const val RESULT_FOR_LOG = "NFC_RESULT_FOR_LOG"
    }
    private val REQUEST_CODE_PERMISSIONS = 11
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private var mrzInfo: MRZInfo? = null
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    private lateinit var binding: ActivityNfcBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNfcBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val mrz = intent.getStringExtra(RESULT)?.let {
            JsonParser.parseString(it).asJsonObject["mrz"].asString
        } ?: run {
            intent.getStringExtra(RESULT_FOR_LOG)
        }
        mrzInfo = MRZInfo(mrz)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(this, 0,
                Intent(this, this.javaClass)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

        if (null == savedInstanceState) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, NFCFragment.newInstance(mrzInfo!!), TAG_NFC)
                    .commit()
        }

        if (!allPermissionsGranted()) {
            setupLogs()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    public override fun onResume() {
        super.onResume()
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onNewIntent(intent: Intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            // drop NFC events
            handleIntent(intent)
        }else{
            super.onNewIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent) {
        val fragmentByTag = supportFragmentManager.findFragmentByTag(TAG_NFC)
        if (fragmentByTag is NFCFragment) {
            fragmentByTag.handleNfcTag(intent)
        }
    }


    /////////////////////////////////////////////////////
    //
    //  NFC Fragment events
    //
    /////////////////////////////////////////////////////
    override fun onEnableNfc() {
        nfcAdapter?.let {
            if (!it.isEnabled) showWirelessSettings()
            it.enableForegroundDispatch(this, pendingIntent, null, null)

        }
    }

    override fun onDisableNfc() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onPassportRead(passport: Passport?) {
        showFragmentDetails(passport!!)
    }

    override fun onCardException(cardException: Exception?) {
        cardException?.printStackTrace()
    }

    private fun showWirelessSettings() {
        Toast.makeText(this, getString(R.string.warning_enable_nfc), Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        startActivity(intent)
    }


    private fun showFragmentDetails(passport: Passport) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, PassportDetailsFragment.newInstance(passport))
                .addToBackStack(TAG_PASSPORT_DETAILS)
                .commit()
    }

    private fun showFragmentPhoto(bitmap: Bitmap) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, PassportPhotoFragment.newInstance(bitmap))
                .addToBackStack(TAG_PASSPORT_PICTURE)
                .commit()
    }


    override fun onImageSelected(bitmap: Bitmap?) {
        showFragmentPhoto(bitmap!!)
    }

    private fun setupLogs() {
        if (BuildConfig.DEBUG) LoggerUtils.writeLogToFile(FileUtils.directory, identifier = "NFC")
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setupLogs()
            } else {
                val snackBar: Snackbar = Snackbar.make(binding.container, R.string.required_perms_not_given, Snackbar.LENGTH_INDEFINITE)
                snackBar.setAction(R.string.settings) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                snackBar.show()
            }
        }
    }
}