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
package org.idpass.smartscanner.lib.mrz

import org.idpass.smartscanner.lib.platform.extension.empty
import org.idpass.smartscanner.lib.platform.extension.noValue
import java.util.*


object MrzUtils {

    fun getImageOnly(image: String?) : MRZResult {
        return MRZResult(
                image = image,
                String.empty(),
                Int.noValue().toShort(),
                Int.noValue().toShort(),
                String.empty(),
                String.empty(),
                String.empty(),
                String.empty(),
                String.empty(),
                String.empty(),
                String.empty(),
                String.empty(),
                String.empty(),
                String.empty()
        )
    }

    fun formatToAdjustedMrzDate(date: String?) : String {
        val parts: Array<String> = date?.split("/")!!.toTypedArray()
        // Convert 2 digit date to 4 digits
        if (parts.size == 3 && parts[2].length == 2) {
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

   fun formatToReadableMrzDate(date: String?) : String {
        val parts: Array<String> = date?.split("/")!!.toTypedArray()
        // Convert 2 digit date to 4 digits
        if (parts.size == 3 && parts[2].length == 2) {
            var year: Int = Integer.valueOf(parts[2])
            year += 2000
            return parts[0] + "/" + parts[1] + "/" + year.toString()
        }
        return date
    }
}