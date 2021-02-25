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
package org.idpass.smartscanner.lib.nfc.passport

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.idpass.smartscanner.lib.databinding.FragmentPhotoBinding
import org.idpass.smartscanner.lib.nfc.details.IntentData


class PassportPhotoFragment : androidx.fragment.app.Fragment() {

    private var passportPhotoFragmentListener: PassportPhotoFragmentListener? = null

    private var bitmap: Bitmap? = null
    private lateinit var binding : FragmentPhotoBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val arguments = arguments
        if (arguments!!.containsKey(IntentData.KEY_IMAGE)) {
            bitmap = arguments.getParcelable<Bitmap>(IntentData.KEY_IMAGE)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData(bitmap)
    }

    private fun refreshData(bitmap: Bitmap?) {
        if (bitmap == null) return

        binding.image.setImageBitmap(bitmap)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        if (activity is PassportPhotoFragmentListener) {
            passportPhotoFragmentListener = activity
        }
    }

    override fun onDetach() {
        passportPhotoFragmentListener = null
        super.onDetach()

    }

    interface PassportPhotoFragmentListener

    companion object {
        fun newInstance(bitmap: Bitmap): PassportPhotoFragment {
            val myFragment = PassportPhotoFragment()
            val args = Bundle()
            args.putParcelable(IntentData.KEY_IMAGE, bitmap)
            myFragment.arguments = args
            return myFragment
        }
    }
}