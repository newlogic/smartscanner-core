package org.idpass.smartscanner.lib.platform.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GzipUtils {
    @Throws(IOException::class)
    fun compress(jsonString : String) : ByteArray {
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
        val `is` = ByteArrayInputStream(bytes)
        val gis = GZIPInputStream(`is`, BUFFER_SIZE)
        val string = StringBuilder()
        val data = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        while (gis.read(data).also { bytesRead = it } != -1) {
            string.append(String(data, 0, bytesRead))
        }
        gis.close()
        `is`.close()
        return string.toString()
    }
}