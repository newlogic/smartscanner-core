package org.idpass.smartscanner.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.ROOT)

    fun isValidDate(inDate: String): Boolean {
        dateFormat.isLenient = false
        try {
            dateFormat.parse(inDate.trim { it <= ' ' })
        } catch (pe: ParseException) {
            return false
        }
        return true
    }
    fun formatDate(date : Date) : String = dateFormat.format(date)
}