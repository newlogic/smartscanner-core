package com.newlogic.mlkitlib.newlogic.utils

import android.content.Context
import android.content.pm.PackageManager

object CameraUtils {
    private val TAG = CameraUtils::class.java.simpleName

    fun isLedFlashAvailable(context: Context): Boolean {
        // method 1
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return true
        }
        // method 2
       /* val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (id in cameraManager.cameraIdList) {
            if (cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                return true
            }
        }*/
        return false
    }
}

