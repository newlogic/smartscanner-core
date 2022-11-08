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

package org.newlogic.smartscanner.result

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import android.view.View.GONE
import android.view.View.VISIBLE
import org.newlogic.smartscanner.R
import org.newlogic.smartscanner.databinding.ActivityRawResultBinding

class RawResultActivity : AppCompatActivity() {

    companion object {
        const val HEADER = "SCAN_HEADER_RESULT"
        const val RESULT = "SCAN_RESULT"
        const val PAYLOAD = "SCAN_PAYLOAD"
    }

    private lateinit var binding: ActivityRawResultBinding

    private var resultHeader: String? = null
    private var resultRaw: String? = null
    private var payload: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRawResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        overridePendingTransition(R.anim.slide_in_up, android.R.anim.fade_out)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)

        resultHeader = intent.getStringExtra(HEADER)
        resultRaw = intent.getStringExtra(RESULT)
        payload = intent.getStringExtra(PAYLOAD)

        binding.layoutHeader.visibility = GONE
        binding.layoutPayload.visibility = GONE
        binding.layoutRaw.visibility = GONE
    }

    override fun onStart() {
        super.onStart()

        if (resultRaw != null) {
            binding.tvRawValue.text = resultRaw
            binding.layoutRaw.visibility = VISIBLE
        }

        if (resultHeader != null) {
            binding.tvHeaderValue.text = formatString(resultHeader)
            binding.layoutHeader.visibility = VISIBLE
        }

        if (payload != null) {
            binding.tvPayloadValue.text = formatString(payload)
            binding.layoutPayload.visibility = VISIBLE
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }

        return super.onOptionsItemSelected(item)
    }


    private fun formatString(text: String?): String {

        if (text == null) {
            return ""
        }


        val json = StringBuilder()
        var indentString = ""
        for (element in text) {
            when (element) {
                '{', '[' -> {
                    json.append(
                        """
                        
                        $indentString$element
                        
                        """.trimIndent()
                    )
                    indentString += "\t"
                    json.append(indentString)
                }
                '}', ']' -> {
                    indentString = indentString.replaceFirst("\t".toRegex(), "")
                    json.append(
                        """
                        
                        $indentString$element
                        """.trimIndent()
                    )
                }
                ',' -> json.append(
                    """
                    $element
                    $indentString
                    """.trimIndent()
                )
                else -> json.append(element)
            }
        }
        return json.toString()
    }

}