package com.youshu.app.data.repository

import com.youshu.app.data.local.dao.ItemDao
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.ItemDetail
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: ItemDao
) {
    fun getActiveItems(): Flow<List<ItemDetail>> = itemDao.getActiveItems()

    fun getAllItems(): Flow<List<ItemDetail>> = itemDao.getAllItems()

    fun getItemDetailById(id: Long): Flow<ItemDetail?> = itemDao.getItemDetailById(id)

    fun getExpiringItems(thresholdTime: Long): Flow<List<ItemDetail>> =
        itemDao.getExpiringItems(thresholdTime)

    fun searchItems(query: String): Flow<List<ItemDetail>> = itemDao.searchItems(query)

    fun getItemsByCategory(categoryId: Long): Flow<List<ItemDetail>> =
        itemDao.getItemsByCategory(categoryId)

    fun getItemsByLocation(locationId: Long): Flow<List<ItemDetail>> =
        itemDao.getItemsByLocation(locationId)

    suspend fun getItemById(id: Long): Item? = itemDao.getItemById(id)

    suspend fun insert(item: Item): Long = itemDao.insert(item)

    suspend fun update(item: Item) = itemDao.update(item)

    suspend fun delete(item: Item) = itemDao.delete(item)

    suspend fun markAsUsed(id: Long) = itemDao.updateStatus(id, Item.STATUS_USED_UP)

    suspend fun markAsDiscarded(id: Long) = itemDao.updateStatus(id, Item.STATUS_DISCARDED)

    fun getActiveCount(): Flow<Int> = itemDao.getActiveCount()

    fun getExpiringCount(thresholdTime: Long): Flow<Int> = itemDao.getExpiringCount(thresholdTime)
}
