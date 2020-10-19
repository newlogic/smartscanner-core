package com.newlogic.mlkitlib.newlogic.config

import android.os.Parcelable
import com.newlogic.mlkitlib.newlogic.extension.empty
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Config(
        val background: String,
        val font: String,
        val label: String,
        val imageResultType: String,
) : Parcelable {
    companion object {
        fun empty() = Config(String.empty(),String.empty(), String.empty(), String.empty())
    }
}