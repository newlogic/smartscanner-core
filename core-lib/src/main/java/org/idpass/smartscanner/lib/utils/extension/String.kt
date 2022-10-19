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
package org.idpass.smartscanner.lib.utils.extension

import android.util.Patterns
import android.webkit.URLUtil
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


private val hexArray = "0123456789ABCDEF".toCharArray()

fun String.Companion.empty() = ""

fun String.Companion.space() = " "

fun String.Companion.formatToSimpleDate(date: Date): String = SimpleDateFormat(
    "yyyyMMddHHmmss",
    Locale.ROOT
).format(date)

fun ByteArray.bytesToHex(): String {
    val hexChars = CharArray(this.size * 2)
    for (j in this.indices) {
        val v = this[j].toInt() and 0xFF
        hexChars[j * 2] = hexArray[v.ushr(4)]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
}

fun List<String>.arrayToString(): String {
    var temp = ""
    val iterator = this.iterator()
    while (iterator.hasNext()) {
        temp += iterator.next() + "\n"
    }
    if (temp.endsWith("\n")) {
        temp = temp.substring(0, temp.length - "\n".length)
    }
    return temp
}

fun String.isValidUrl() : Boolean {
    return URLUtil.isValidUrl(this) && Patterns.WEB_URL.matcher(this).matches()
}

fun String.isJSONValid(): Boolean {
    return try {
        val o = JSONObject(this)
        true
    } catch (e: JSONException) {
        false
    }
}