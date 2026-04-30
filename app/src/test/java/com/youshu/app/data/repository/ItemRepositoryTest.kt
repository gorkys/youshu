package com.youshu.app.data.repository

import com.youshu.app.data.local.dao.ItemDao
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.ItemDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItemRepositoryTest {

    @Test
    fun restoreFromTrash_restoresOnlySelectedItems() = runTest {
        val deletedAt = 1_000L
        val dao = FakeItemDao(
            mutableListOf(
                testItem(id = 1L, deletedAt = deletedAt),
                testItem(id = 2L, deletedAt = deletedAt),
                testItem(id = 3L, deletedAt = null)
            )
        )
        val repository = ItemRepository(dao)

        repository.restoreFromTrash(listOf(1L, 2L))

        val items = dao.snapshot()
        assertNull(items.first { it.id == 1L }.deletedAt)
        assertNull(items.first { it.id == 2L }.deletedAt)
        assertNull(items.first { it.id == 3L }.deletedAt)
    }

    @Test
    fun permanentlyDeleteFromTrash_removesOnlyDeletedItems() = runTest {
        val deletedAt = 2_000L
        val dao = FakeItemDao(
            mutableListOf(
                testItem(id = 1L, deletedAt = deletedAt),
                testItem(id = 2L, deletedAt = null),
                testItem(id = 3L, deletedAt = deletedAt)
            )
        )
        val repository = ItemRepository(dao)

        val deletedItems = repository.permanentlyDeleteFromTrash(listOf(1L, 2L, 3L))

        assertEquals(listOf(1L, 3L), deletedItems.map { it.id })
        assertEquals(listOf(2L), dao.snapshot().map { it.id })
    }

    @Test
    fun purgeDeletedItemsOlderThan_returnsPurgedItems() = runTest {
        val dao = FakeItemDao(
            mutableListOf(
                testItem(id = 1L, deletedAt = 100L),
                testItem(id = 2L, deletedAt = 300L),
                testItem(id = 3L, deletedAt = null)
            )
        )
        val repository = ItemRepository(dao)

        val purgedItems = repository.purgeDeletedItemsOlderThan(cutoffTime = 200L)

        assertEquals(listOf(1L), purgedItems.map { it.id })
        assertEquals(listOf(2L, 3L), dao.snapshot().map { it.id })
    }

    private fun testItem(id: Long, deletedAt: Long?) = Item(
        id = id,
        name = "Item $id",
        deletedAt = deletedAt,
        imagePath = if (deletedAt != null) "image-$id.jpg" else ""
    )
}

private class FakeItemDao(
    private val items: MutableList<Item>
) : ItemDao {

    fun snapshot(): List<Item> = items.toList()

    override fun getActiveItems(): Flow<List<ItemDetail>> = flowOf(emptyList())

    override fun getAllItems(): Flow<List<ItemDetail>> = flowOf(emptyList())

    override fun getRecycleItems(cutoffTime: Long): Flow<List<ItemDetail>> = flowOf(
        items.filter { it.deletedAt != null && it.deletedAt >= cutoffTime }.map { ItemDetail(item = it) }
    )

    override fun getItemDetailById(id: Long): Flow<ItemDetail?> = flowOf(null)

    override fun getExpiringItems(thresholdTime: Long): Flow<List<ItemDetail>> = flowOf(emptyList())

    override fun searchItems(query: String): Flow<List<ItemDetail>> = flowOf(emptyList())

    override fun getItemsByCategory(categoryId: Long): Flow<List<ItemDetail>> = flowOf(emptyList())

    override fun getItemsByLocation(locationId: Long): Flow<List<ItemDetail>> = flowOf(emptyList())

    override suspend fun getItemById(id: Long): Item? = items.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun insert(item: Item): Long {
        items += item
        return item.id
    }

    override suspend fun update(item: Item) {
        replace(item)
    }

    override suspend fun delete(item: Item) {
        items.removeAll { it.id == item.id }
    }

    override suspend fun moveToTrash(id: Long, deletedAt: Long) {
        items.replaceAll { item -> if (item.id == id) item.copy(deletedAt = deletedAt) else item }
    }

    override suspend fun restoreFromTrash(id: Long) {
        items.replaceAll { item -> if (item.id == id) item.copy(deletedAt = null) else item }
    }

    override suspend fun restoreItemsFromTrash(ids: List<Long>) {
        val idSet = ids.toSet()
        items.replaceAll { item -> if (item.id in idSet) item.copy(deletedAt = null) else item }
    }

    override suspend fun updateStatus(id: Long, status: Int) {
        items.replaceAll { item -> if (item.id == id) item.copy(status = status) else item }
    }

    override suspend fun updateStatusAndRating(id: Long, status: Int, rating: Int?, ratedAt: Long?) {
        items.replaceAll { item ->
            if (item.id == id) item.copy(status = status, rating = rating, ratedAt = ratedAt) else item
        }
    }

    override suspend fun updateRating(id: Long, rating: Int, ratedAt: Long) {
        items.replaceAll { item ->
            if (item.id == id) item.copy(rating = rating, ratedAt = ratedAt) else item
        }
    }

    override suspend fun getActiveItemsSync(): List<Item> =
        items.filter { it.status == Item.STATUS_IN_USE && it.deletedAt == null }

    override fun getActiveCount(): Flow<Int> = flowOf(items.count { it.status == Item.STATUS_IN_USE && it.deletedAt == null })

    override fun getExpiringCount(thresholdTime: Long): Flow<Int> = flowOf(0)

    override fun getTotalCount(): Flow<Int> = flowOf(items.count { it.deletedAt == null })

    override fun getUsedUpCount(): Flow<Int> = flowOf(items.count { it.status == Item.STATUS_USED_UP && it.deletedAt == null })

    override fun getTotalValue(): Flow<Double> = flowOf(0.0)

    override fun getCountByCategory(categoryId: Long): Flow<Int> = flowOf(0)

    override fun getCountByLocation(locationId: Long): Flow<Int> = flowOf(0)

    override fun getRecycleCount(cutoffTime: Long): Flow<Int> =
        flowOf(items.count { it.deletedAt != null && it.deletedAt >= cutoffTime })

    override suspend fun getDeletedItemsBefore(cutoffTime: Long): List<Item> =
        items.filter { it.deletedAt != null && it.deletedAt < cutoffTime }

    override suspend fun getDeletedItemsByIds(ids: List<Long>): List<Item> {
        val idSet = ids.toSet()
        return items.filter { it.id in idSet && it.deletedAt != null }
    }

    override suspend fun purgeDeletedBefore(cutoffTime: Long) {
        items.removeAll { it.deletedAt != null && it.deletedAt < cutoffTime }
    }

    override suspend fun deleteDeletedItemsByIds(ids: List<Long>) {
        val idSet = ids.toSet()
        items.removeAll { it.id in idSet && it.deletedAt != null }
    }

    override fun getExpiringItemsInRange(currentTime: Long, thresholdTime: Long): Flow<List<ItemDetail>> =
        flowOf(emptyList())

    private fun replace(item: Item) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            items[index] = item
        }
    }
}
