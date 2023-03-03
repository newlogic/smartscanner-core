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

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import net.sf.scuba.smartcards.CardServiceException
import net.sf.scuba.smartcards.ISO7816
import org.idpass.smartscanner.lib.R
import org.idpass.smartscanner.lib.nfc.details.IntentData
import org.idpass.smartscanner.lib.nfc.details.NFCDocumentTag
import org.idpass.smartscanner.lib.nfc.passport.Passport
import org.idpass.smartscanner.lib.scanner.config.Language
import org.idpass.smartscanner.lib.utils.DateUtils
import org.idpass.smartscanner.lib.utils.DateUtils.BIRTH_DATE_THRESHOLD
import org.idpass.smartscanner.lib.utils.DateUtils.EXPIRY_DATE_THRESHOLD
import org.idpass.smartscanner.lib.utils.DateUtils.formatStandardDate
import org.idpass.smartscanner.lib.utils.KeyStoreUtils
import org.idpass.smartscanner.lib.utils.LanguageUtils
import org.jmrtd.*
import org.jmrtd.lds.icao.MRZInfo
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.util.*


class NFCFragment : Fragment() {

    private var mrzInfo: MRZInfo? = null
    private var nfcFragmentListener: NfcFragmentListener? = null
    private var textViewPassportNumber: TextView? = null
    private var textViewNfcTitle: TextView? = null
    private var textViewDateOfBirth: TextView? = null
    private var textViewDateOfExpiry: TextView? = null
    private var progressBar: ProgressBar? = null
    private var label: String? = null
    private var language: String? = null
    private var locale: String? = null
    private var withPhoto: Boolean = true
    private var captureLog: Boolean = false

    private var mHandler = Handler(Looper.getMainLooper())
    private var disposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nfc, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val arguments = arguments
        if (arguments?.containsKey(IntentData.KEY_MRZ_INFO) == true) {
            mrzInfo = arguments.getSerializable(IntentData.KEY_MRZ_INFO) as MRZInfo?
        }
        if (arguments?.containsKey(IntentData.KEY_LABEL) == true) {
            label = arguments.getString(IntentData.KEY_LABEL)
        }
        if (arguments?.containsKey(IntentData.KEY_LOCALE) == true) {
            locale = arguments.getString(IntentData.KEY_LOCALE)
        }
        if (arguments?.containsKey(IntentData.KEY_LANGUAGE) == true) {
            language = arguments.getString(IntentData.KEY_LANGUAGE) ?: Language.EN
        }
        if (arguments?.containsKey(IntentData.KEY_WITH_PHOTO) == true) {
            withPhoto = arguments.getBoolean(IntentData.KEY_WITH_PHOTO)
        }
        if (arguments?.containsKey(IntentData.KEY_CAPTURE_LOG) == true) {
            captureLog = arguments.getBoolean(IntentData.KEY_CAPTURE_LOG)
        }
        textViewNfcTitle = view.findViewById(R.id.tv_nfc_title)
        textViewPassportNumber = view.findViewById(R.id.value_passport_number)
        textViewDateOfBirth = view.findViewById(R.id.value_DOB)
        textViewDateOfExpiry = view.findViewById(R.id.value_expiration_date)
        progressBar = view.findViewById(R.id.progressBar)

    }

    fun handleNfcTag(intent: Intent?) {
        if (intent == null || intent.extras == null) {
            return
        }
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        val cscaInputStream = context!!.assets.open("csca.ks")
        val keyStore = KeyStoreUtils().readKeystoreFromFile(cscaInputStream)

        val mrtdTrustStore = MRTDTrustStore()
        if (keyStore != null) {
            val certStore = KeyStoreUtils().toCertStore(keyStore = keyStore)
            mrtdTrustStore.addAsCSCACertStore(certStore)
        }
        // if withPhoto is true, readDG2 is enabled and photo is added to NFC result
        val subscribe = NFCDocumentTag(withPhoto, captureLog).handleTag(requireContext(), tag, mrzInfo!!, mrtdTrustStore, object : NFCDocumentTag.PassportCallback {

            override fun onPassportReadStart() {
                onNFCSReadStart()
            }

            override fun onPassportReadFinish() {
                onNFCReadFinish()
            }

            override fun onPassportRead(passport: Passport?) {
                this@NFCFragment.onPassportRead(passport)
            }

            override fun onAccessDeniedException(exception: AccessDeniedException) {
                Toast.makeText(context, getString(R.string.warning_authentication_failed), Toast.LENGTH_SHORT).show()
                exception.printStackTrace()
                this@NFCFragment.onCardException(exception)
            }

            override fun onBACDeniedException(exception: BACDeniedException) {
                Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show()
                this@NFCFragment.onCardException(exception)
            }

            override fun onPACEException(exception: PACEException) {
                Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show()
                this@NFCFragment.onCardException(exception)
            }

            override fun onCardException(exception: CardServiceException) {
                val sw = exception.sw.toShort()
                when (sw) {
                    ISO7816.SW_CLA_NOT_SUPPORTED -> {
                        Toast.makeText(context, getString(R.string.warning_cla_not_supported), Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
                this@NFCFragment.onCardException(exception)
            }

            override fun onGeneralException(exception: Exception?) {
                Toast.makeText(context, exception?.toString(), Toast.LENGTH_SHORT).show()
                this@NFCFragment.onCardException(exception)
            }
        })

        disposable.add(subscribe)

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        if (activity is NfcFragmentListener) {
            nfcFragmentListener = activity
        }
    }

    override fun onDetach() {
        nfcFragmentListener = null
        super.onDetach()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // Display proper language
        LanguageUtils.changeLanguage(requireContext(), if (language == Language.EN) Language.EN else Language.AR)
        // Display MRZ details
        textViewNfcTitle?.text = if (label != null) label else getString(R.string.nfc_title)
        textViewPassportNumber?.text = getString(R.string.doc_number, mrzInfo?.documentNumber)
        textViewDateOfBirth?.text = getString(R.string.doc_dob, DateUtils.toAdjustedDate(formatStandardDate(mrzInfo?.dateOfBirth, threshold = BIRTH_DATE_THRESHOLD)))
        textViewDateOfExpiry?.text = getString(R.string.doc_expiry, DateUtils.toReadableDate(formatStandardDate(mrzInfo?.dateOfExpiry, threshold = EXPIRY_DATE_THRESHOLD)))

        if (nfcFragmentListener != null) {
            nfcFragmentListener?.onEnableNfc()
        }
    }

    override fun onPause() {
        super.onPause()
        if (nfcFragmentListener != null) {
            nfcFragmentListener?.onDisableNfc()
        }
    }

    override fun onDestroyView() {
        if (!disposable.isDisposed) {
            disposable.dispose()
        }
        super.onDestroyView()
    }

    private fun onNFCSReadStart() {
        Log.d(TAG, "onNFCSReadStart")
        mHandler.post { progressBar?.visibility = View.VISIBLE }

    }

    private fun onNFCReadFinish() {
        Log.d(TAG, "onNFCReadFinish")
        mHandler.post { progressBar?.visibility = View.GONE }
    }

    private fun onCardException(cardException: Exception?) {
        mHandler.post {
            if (nfcFragmentListener != null) {
                nfcFragmentListener?.onCardException(cardException)
            }
        }
    }

    private fun onPassportRead(passport: Passport?) {
        mHandler.post {
            if (nfcFragmentListener != null) {
                nfcFragmentListener?.onPassportRead(passport)
            }
        }
    }

    interface NfcFragmentListener {
        fun onEnableNfc()
        fun onDisableNfc()
        fun onPassportRead(passport: Passport?)
        fun onCardException(cardException: Exception?)
    }

    companion object {
        private val TAG = NFCFragment::class.java.simpleName

        init {
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }

        fun newInstance(mrzInfo: MRZInfo?, label: String?, language: String?, locale: String?, withPhoto: Boolean, captureLog: Boolean): NFCFragment {
            val myFragment = NFCFragment()
            val args = Bundle()
            args.putSerializable(IntentData.KEY_MRZ_INFO, mrzInfo)
            args.putString(IntentData.KEY_LABEL, label)
            args.putString(IntentData.KEY_LANGUAGE, language)
            args.putString(IntentData.KEY_LOCALE, locale)
            args.putBoolean(IntentData.KEY_WITH_PHOTO, withPhoto)
            args.putBoolean(IntentData.KEY_CAPTURE_LOG, captureLog)
            myFragment.arguments = args
            return myFragment
        }
    }
}