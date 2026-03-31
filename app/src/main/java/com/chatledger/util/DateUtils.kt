package com.chatledger.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MM月dd日", Locale.getDefault())
    private val fullFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy年MM月", Locale.getDefault())

    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun formatFull(timestamp: Long): String = fullFormat.format(Date(timestamp))

    fun formatMonth(timestamp: Long): String = monthFormat.format(Date(timestamp))

    fun isToday(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp }
        val cal2 = Calendar.getInstance()
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun formatSmartTime(timestamp: Long): String {
        return if (isToday(timestamp)) {
            formatTime(timestamp)
        } else {
            "${formatDate(timestamp)} ${formatTime(timestamp)}"
        }
    }

    fun formatCurrency(amount: Double, currency: String = "¥"): String {
        return "$currency%.2f".format(amount)
    }
}
