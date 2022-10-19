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
package org.idpass.smartscanner.lib.utils

import android.content.Context
import android.content.res.Configuration
import java.util.*

object LanguageUtils {
    private val TAG = LanguageUtils::class.java.simpleName

    fun changeLanguage(context: Context, language: String) {
        val locale = Locale(language)
        val config = Configuration()
        val resources = context.resources
        Locale.setDefault(locale)
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    /**
     * Determine if String s is right-to-left or RTL.
     * Example of RTL languages: Arabic, Hebrew, etc
     */
    fun isRTL(language: String?): Boolean {
        if (language != null) {
            for (element in language) {
                val d = Character.getDirectionality(element)
                if (d == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                        || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
                        || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
                        || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE) {
                    return true
                }
            }
            return false
        } else {
            return false
        }
    }
}