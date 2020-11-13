package com.newlogic.mlkitlib.newlogic.config

import android.os.Parcelable
import com.newlogic.mlkitlib.newlogic.extension.empty
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Config(
        val background: String? = String.empty(),
        val font: String? = String.empty(),
        val imageResultType: String? = ImageResultType.PATH.value,
        val label: String? = String.empty(),
        val isManualCapture: Boolean? = false,
        val branding: Boolean? = false,
) : Parcelable {
    companion object {
        val default = Config( String.empty(),String.empty(), String.empty(), ImageResultType.PATH.value, isManualCapture = true, branding = true)
    }
}