package com.newlogic.mlkitlib.newlogic.utils

import android.content.Context
import java.io.*


object FileUtils {
    private val TAG = FileUtils::class.java.simpleName

    private fun copyFileOrDir(path: String, context: Context) {
        val assetManager = context.assets
        val assets: Array<String>?
        try {
            assets = assetManager.list(path)
            if (assets!!.isEmpty()) {
                copyFile(path, context)
            } else {
                val fullPath = context.getExternalFilesDir(null).toString() + "/" + path
                val dir = File(fullPath)
                if (!dir.exists()
                    && !path.startsWith("fallback-locales") && !path.startsWith("stored-locales")
                    && !path.startsWith("images") && !path.startsWith("public")
                    && !path.startsWith("sounds") && !path.startsWith("webkit")
                )

                for (i in assets.indices) {
                    val p = if (path == "") "" else "$path/"
                    if (!path.startsWith("fallback-locales") && !path.startsWith(
                            "stored" +
                                    "-locales"
                        ) &&
                        !path.startsWith("images") && !path.startsWith("public") &&
                        !path.startsWith("sounds") && !path.startsWith("webkit")
                    ) copyFileOrDir(p + assets[i], context)
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    private fun copyFile(filename: String, context: Context) {
        val assetManager = context.assets
        val `in`: InputStream?
        val out: OutputStream?
        val newFileName: String?
        try {
            `in` = assetManager.open(filename)
            newFileName =
                if (filename.endsWith(".jpg")) // extension was added to avoid compression on APK file
                    context.getExternalFilesDir(null).toString() + "/" + filename.substring(
                        0,
                        filename.length - 4
                    ) else context.getExternalFilesDir(null).toString() + "/" + filename
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

