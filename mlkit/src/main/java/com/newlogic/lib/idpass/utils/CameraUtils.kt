package com.newlogic.lib.idpass.utils

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

object CameraUtils {
    private val TAG = CameraUtils::class.java.simpleName

    fun isLedFlashAvailable(context: Context): Boolean {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return true
        }
       val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (id in cameraManager.cameraIdList) {
            if (cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                return true
            }
        }
        return false
    }
}