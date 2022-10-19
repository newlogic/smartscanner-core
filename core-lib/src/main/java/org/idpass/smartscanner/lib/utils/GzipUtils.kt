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

import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GzipUtils {
    @Throws(IOException::class)
    fun compress(jsonString: String) : ByteArray {
        val os = ByteArrayOutputStream(jsonString.length)
        val gos = GZIPOutputStream(os)
        gos.write(jsonString.toByteArray())
        gos.close()
        val compressed: ByteArray = os.toByteArray()
        os.close()
        return compressed
    }

    @Throws(IOException::class)
    fun decompress(bytes: ByteArray) : String {
        val BUFFER_SIZE = 32
        val inputStream = ByteArrayInputStream(bytes)
        val gis = GZIPInputStream(inputStream, BUFFER_SIZE)
        val sb = StringBuilder()
        val data = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        while (gis.read(data).also { bytesRead = it } != -1) {
            sb.append(String(data, 0, bytesRead))
        }
        gis.close()
        inputStream.close()
        return sb.toString()
    }

    fun isGZipped(inputStream: InputStream): Boolean {
        var input: InputStream = inputStream
        if (!input.markSupported()) {
            input = BufferedInputStream(input)
        }
        input.mark(2)
        var magic = 0
        try {
            magic = input.read() and 0xff or (input.read() shl 8 and 0xff00)
            input.reset()
        } catch (e: IOException) {
            e.printStackTrace(System.err)
            return false
        }
        return magic == GZIPInputStream.GZIP_MAGIC
    }
}