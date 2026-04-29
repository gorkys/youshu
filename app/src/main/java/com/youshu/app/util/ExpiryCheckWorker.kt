package com.youshu.app.util

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.youshu.app.data.local.dao.ItemDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ExpiryCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val itemDao: ItemDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val thresholdTime = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
        // Use a simple query approach
        val items = itemDao.getActiveItemsSync()
        items.forEach { item ->
            item.expireTime?.let { expireTime ->
                if (expireTime <= thresholdTime && expireTime > 0) {
                    val daysLeft = DateUtil.daysUntil(expireTime)
                    NotificationHelper.showExpiryNotification(
                        context = applicationContext,
                        itemId = item.id,
                        itemName = item.name,
                        daysLeft = daysLeft
                    )
                }
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "expiry_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(
                12, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
