package com.youshu.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtil {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun daysUntil(timestamp: Long): Long {
        val now = System.currentTimeMillis()
        val diff = timestamp - now
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    fun isExpiringSoon(timestamp: Long, days: Int = 7): Boolean {
        val daysLeft = daysUntil(timestamp)
        return daysLeft in 0..days.toLong()
    }

    fun isExpired(timestamp: Long): Boolean {
        return daysUntil(timestamp) < 0
    }

    fun expiryText(timestamp: Long): String {
        val days = daysUntil(timestamp)
        return when {
            days < 0 -> "已过期"
            days == 0L -> "今天过期"
            days <= 7 -> "${days}天后过期"
            days <= 30 -> "${days}天后过期"
            else -> formatDate(timestamp)
        }
    }
}
