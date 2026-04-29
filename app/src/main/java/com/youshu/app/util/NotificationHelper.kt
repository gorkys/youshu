package com.youshu.app.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.youshu.app.R
import java.time.LocalDate

object NotificationHelper {

    private const val CHANNEL_ID = "expiry_reminder"
    private const val CHANNEL_NAME = "到期提醒"
    private const val PREFS_NAME = "expiry_notification_state"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "用于提醒即将到期或已到期的物品"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showExpiryNotification(
        context: Context,
        itemId: Long,
        itemName: String,
        daysLeft: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (!shouldNotify(context, itemId, daysLeft)) {
            return
        }

        val text = when {
            daysLeft < 0 -> "「$itemName」已过期，请尽快处理。"
            daysLeft == 0L -> "「$itemName」今天到期，建议优先使用。"
            else -> "「$itemName」将在 ${daysLeft} 天后到期。"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("有数到期提醒")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(itemId.toInt(), notification)
    }

    private fun shouldNotify(context: Context, itemId: Long, daysLeft: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stateKey = "state_$itemId"
        val dayKey = "day_$itemId"
        val today = LocalDate.now().toString()
        val state = when {
            daysLeft < 0 -> "expired"
            else -> "d$daysLeft"
        }

        val lastDay = prefs.getString(dayKey, null)
        val lastState = prefs.getString(stateKey, null)
        if (lastDay == today && lastState == state) {
            return false
        }

        prefs.edit()
            .putString(dayKey, today)
            .putString(stateKey, state)
            .apply()
        return true
    }
}
