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

    fun getRecycleItems(cutoffTime: Long): Flow<List<ItemDetail>> = itemDao.getRecycleItems(cutoffTime)

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

    suspend fun moveToTrash(item: Item) = itemDao.moveToTrash(
        id = item.id,
        deletedAt = System.currentTimeMillis()
    )

    suspend fun restoreFromTrash(id: Long) = itemDao.restoreFromTrash(id)

    suspend fun restoreFromTrash(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            itemDao.restoreItemsFromTrash(ids)
        }
    }

    suspend fun permanentlyDeleteFromTrash(ids: List<Long>): List<Item> {
        if (ids.isEmpty()) return emptyList()
        val deletedItems = itemDao.getDeletedItemsByIds(ids)
        if (deletedItems.isNotEmpty()) {
            itemDao.deleteDeletedItemsByIds(deletedItems.map { it.id })
        }
        return deletedItems
    }

    suspend fun markAsUsed(id: Long, rating: Int? = null) {
        val ratedAt = rating?.let { System.currentTimeMillis() }
        itemDao.updateStatusAndRating(id, Item.STATUS_USED_UP, rating, ratedAt)
    }

    suspend fun rateUsedItem(id: Long, rating: Int) =
        itemDao.updateRating(id, rating, System.currentTimeMillis())

    suspend fun markAsDiscarded(id: Long) = itemDao.updateStatus(id, Item.STATUS_DISCARDED)

    fun getActiveCount(): Flow<Int> = itemDao.getActiveCount()

    fun getExpiringCount(thresholdTime: Long): Flow<Int> = itemDao.getExpiringCount(thresholdTime)

    fun getTotalValue(): Flow<Double> = itemDao.getTotalValue()

    fun getRecycleCount(cutoffTime: Long): Flow<Int> = itemDao.getRecycleCount(cutoffTime)

    fun getCountByCategory(categoryId: Long): Flow<Int> = itemDao.getCountByCategory(categoryId)

    fun getCountByLocation(locationId: Long): Flow<Int> = itemDao.getCountByLocation(locationId)

    fun getExpiringItemsInRange(currentTime: Long, thresholdTime: Long): Flow<List<ItemDetail>> =
        itemDao.getExpiringItemsInRange(currentTime, thresholdTime)

    suspend fun purgeDeletedItemsOlderThan(cutoffTime: Long): List<Item> {
        val expiredItems = itemDao.getDeletedItemsBefore(cutoffTime)
        if (expiredItems.isNotEmpty()) {
            itemDao.purgeDeletedBefore(cutoffTime)
        }
        return expiredItems
    }
}
