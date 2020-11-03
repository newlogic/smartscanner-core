package com.newlogic.mlkitlib.newlogic.config

import android.os.Parcelable
import com.newlogic.mlkitlib.newlogic.extension.empty
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Config(
        val branding: Boolean,
        val background: String,
        val font: String,
        val imageResultType: String,
        val label: String,
        val isManualCapture: Boolean,
) : Parcelable {
    companion object {
        val default = Config(false, String.empty(),String.empty(), String.empty(), ImageResultType.PATH.value, false)
    }
}