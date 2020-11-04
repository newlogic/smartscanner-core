package com.newlogic.mlkitlib.newlogic.extension

import java.text.SimpleDateFormat
import java.util.*

fun String.Companion.empty() = ""

fun String.Companion.space() = " "

fun String.Companion.formatToSimpleDate(date : Date): String = SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(date)