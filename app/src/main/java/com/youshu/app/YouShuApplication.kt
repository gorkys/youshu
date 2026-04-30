package com.youshu.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.youshu.app.data.repository.ItemRepository
import com.youshu.app.util.ExpiryCheckWorker
import com.youshu.app.util.ImageUtil
import com.youshu.app.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class YouShuApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var itemRepository: ItemRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        ExpiryCheckWorker.schedule(this)
        applicationScope.launch {
            val cutoffTime = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            itemRepository.purgeDeletedItemsOlderThan(cutoffTime).forEach { item ->
                if (item.imagePath.isNotBlank()) {
                    ImageUtil.deleteImage(item.imagePath)
                }
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
