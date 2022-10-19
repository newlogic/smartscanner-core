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

import java.io.File


object LoggerUtils {
    fun writeLogToFile(identifier : String = "default") {
        try {
            val directory = FileUtils.directory
            if (!File(directory).exists()) {
                FileUtils.createSmartScannerDirs()
            }
            val fileName = "logcat-$identifier.txt"
            val file = File(directory, fileName)
            if (!file.exists()) {
                file.createNewFile()
            } else {
                file.delete()
            }
            //"logcat -f *:S NFCScannerActivity:D SmartScannerActivity:D"
            val command = "logcat -f " + file.absolutePath
            Runtime.getRuntime().exec(command)

        } catch (e : Exception) {
            e.printStackTrace()
        }
    }
}