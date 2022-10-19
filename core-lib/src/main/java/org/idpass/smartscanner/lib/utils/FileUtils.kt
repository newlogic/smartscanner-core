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
import android.content.res.AssetManager
import android.os.Environment
import android.util.Log
import java.io.*


object FileUtils {
    private val TAG = FileUtils::class.java.simpleName
    val directory : String = "${Environment.getExternalStorageDirectory()?.absolutePath}/SmartScanner"

    fun createSmartScannerDirs() = File(directory).also { if (!it.exists()) it.mkdirs() }
    fun copyAssets(context: Context,path: String, outPath: String) {
        val assetManager: AssetManager = context.assets
        val assets: Array<String>?
        try {
            assets = assetManager.list(path)
            if (assets!!.isEmpty()) {
                copyFile(context, path, outPath)
            } else {
                val fullPath = "$outPath/$path"
                val dir = File(fullPath)
                if (!dir.exists()) if (!dir.mkdir()) Log.e(
                    TAG,
                    "No create external directory: $dir"
                )
                for (asset in assets) {
                    copyAssets(context,"$path/$asset", outPath)
                }
            }
        } catch (ex: IOException) {
            Log.e(TAG, "I/O Exception", ex)
        }
    }

    private fun copyFile(context: Context, filename: String, outPath: String) {
        val assetManager: AssetManager = context.assets
        val `in`: InputStream
        val out: OutputStream
        try {
            `in` = assetManager.open(filename)
            val newFileName = "$outPath/$filename"
            out = FileOutputStream(newFileName)
            val buffer = ByteArray(1024)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            `in`.close()
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}