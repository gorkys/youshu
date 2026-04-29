package com.youshu.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

object DateUtil {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.CHINA)
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    fun daysUntil(timestamp: Long): Long {
        val today = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zoneId).toLocalDate()
        val target = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
        return ChronoUnit.DAYS.between(today, target)
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
            days == 0L -> "今天到期"
            days <= 30 -> "${days}天后到期"
            else -> formatDate(timestamp)
        }
    }

    fun expiryCountdownText(timestamp: Long): String {
        val days = daysUntil(timestamp)
        return when {
            days < 0 -> "已过期"
            days == 0L -> "今天到期"
            else -> "剩余${days}天"
        }
    }

    fun formatCurrency(price: Double): String {
        return currencyFormatter.format(price)
    }

    fun daysFromNow(days: Long): Long {
        return LocalDate.now(zoneId)
            .plusDays(days)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun monthsFromNow(months: Long): Long {
        return LocalDate.now(zoneId)
            .plusMonths(months)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
