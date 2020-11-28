package com.newlogic.lib.idpass.config

import android.os.Parcelable
import com.newlogic.lib.idpass.extension.empty
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Config(
    val background: String? = null,
    val font: String? = null,
    val imageResultType: String? = null,
    val label: String? = null,
    val isManualCapture: Boolean? = null,
    val branding: Boolean? = null,
) : Parcelable {
    companion object {
        val default = Config(
            background = String.empty(),
            font = String.empty(),
            imageResultType = ImageResultType.PATH.value,
            label = String.empty(),
            isManualCapture = false,
            branding = true
        )
    }
}
