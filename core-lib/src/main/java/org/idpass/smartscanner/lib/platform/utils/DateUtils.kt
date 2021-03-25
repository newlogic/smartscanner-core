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
package org.idpass.smartscanner.lib.platform.utils

import android.annotation.SuppressLint
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.ROOT)

    fun isValidDate(inDate: String): Boolean {
        dateFormat.isLenient = false
        try {
            dateFormat.parse(inDate.trim { it <= ' ' })
        } catch (pe: ParseException) {
            return false
        }
        return true
    }
    fun formatDate(date: Date) : String = dateFormat.format(date)

    @SuppressLint("SimpleDateFormat")
    fun formatStandardDate(dateString: String?, pattern : String = "yyMMdd"): String? {
        val date = stringToDate(dateString, SimpleDateFormat(pattern)) ?: return null
        return dateToString(date, SimpleDateFormat("MM/dd/yyyy"))
    }

    private fun stringToDate(dateStr: String?, dateFormat: DateFormat): Date? {
        if (dateStr == null) return null
        var date: Date? = null
        try {
            date = dateFormat.parse(dateStr)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return date
    }

    private fun dateToString(date: Date, dateFormat: DateFormat): String? {
        return dateFormat.format(date)
    }

    fun toAdjustedDate(date: String?) : String? {
        val parts: Array<String>? = date?.split("/")?.toTypedArray()
        // Convert 2 digit date to 4 digits
        if (parts?.size == 3 && parts[2].length == 2) {
            var year: Int = Integer.valueOf(parts[2])
            // Allow 5 years in the future for a 2 digit date
            year = if (year + 100 > Date().year + 5) {
                year + 1900
            } else {
                year + 2000
            }
            return parts[0] + "/" + parts[1] + "/" + year.toString()
        }
        return date
    }

    fun toReadableDate(date: String?) : String? {
        val parts: Array<String>? = date?.split("/")?.toTypedArray()
        // Convert 2 digit date to 4 digits
        if (parts?.size == 3 && parts[2].length == 2) {
            var year: Int = Integer.valueOf(parts[2])
            year += 2000
            return parts[0] + "/" + parts[1] + "/" + year.toString()
        }
        return date
    }
}