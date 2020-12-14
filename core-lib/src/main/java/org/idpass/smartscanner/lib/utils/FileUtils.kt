package org.idpass.smartscanner.lib.utils

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import java.io.*


object FileUtils {
    private val TAG = FileUtils::class.java.simpleName

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