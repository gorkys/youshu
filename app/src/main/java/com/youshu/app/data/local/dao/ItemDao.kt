package com.youshu.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.ItemDetail
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query(
        """
        SELECT items.*,
               categories.name AS categoryName,
               locations.name AS locationName
        FROM items
        LEFT JOIN categories ON items.categoryId = categories.id
        LEFT JOIN locations ON items.locationId = locations.id
        WHERE items.status = 0
          AND items.deletedAt IS NULL
        ORDER BY items.createdAt DESC
        """
    )
    fun getActiveItems(): Flow<List<ItemDetail>>

    @Query(
        """
        SELECT items.*,
               categories.name AS categoryName,
               locations.name AS locationName
        FROM items
        LEFT JOIN categories ON items.categoryId = categories.id
        LEFT JOIN locations ON items.locationId = locations.id
        WHERE items.deletedAt IS NULL
        ORDER BY items.createdAt DESC
        """
    )
    fun getAllItems(): Flow<List<ItemDetail>>

    @Query(
        """
        SELECT items.*,
               categories.name AS categoryName,
               locations.name AS locationName
        FROM items
        LEFT JOIN categories ON items.categoryId = categories.id
        LEFT JOIN locations ON items.locationId = locations.id
        WHERE items.id = :id
          AND items.deletedAt IS NULL
        """
    )
    fun getItemDetailById(id: Long): Flow<ItemDetail?>

    @Query(
        """
        SELECT items.*,
               categories.name AS categoryName,
               locations.name AS locationName
        FROM items
        LEFT JOIN categories ON items.categoryId = categories.id
        LEFT JOIN locations ON items.locationId = locations.id
        WHERE items.status = 0
          AND items.deletedAt IS NULL
          AND items.expireTime IS NOT NULL
          AND items.expireTime <= :thresholdTime
          AND items.expireTime > 0
        ORDER BY items.expireTime ASC
        """
    )
    fun getExpiringItems(thresholdTime: Long): Flow<List<ItemDetail>>

    @Query(
        """
        SELECT items.*,
               categories.name AS categoryName,
               locations.name AS locationName
        FROM items
        LEFT JOIN categories ON items.categoryId = categories.id
        LEFT JOIN locations ON items.locationId = locations.id
        WHERE items.status = 0
          AND items.deletedAt IS NULL
          AND (
              items.name LIKE '%' || :query || '%'
              OR categories.name LIKE '%' || :query || '%'
              OR locations.name LIKE '%' || :query || '%'
              OR items.note LIKE '%' || :query || '%'
          )
        ORDER BY items.createdAt DESC
        """
    )
    fun searchItems(query: String): Flow<List<ItemDetail>>

    @Query(
        """
        SELECT items.*,
               categories.name AS categoryName,
               locations.name AS locationName
        FROM items
        LEFT JOIN categories ON items.categoryId = categories.id
        LEFT JOIN locations ON items.locationId = locations.id
        WHERE items.status = 0
          AND items.deletedAt IS NULL
          AND items.categoryId = :categoryId
        ORDER BY items.createdAt DESC
        """
    )
    fun getItemsByCategory(categoryId: Long): Flow<List<ItemDetail>>

    @Query(
        """
        SELECT items.*,
               categories.name AS categoryName,
               locations.name AS locationName
        FROM items
        LEFT JOIN categories ON items.categoryId = categories.id
        LEFT JOIN locations ON items.locationId = locations.id
        WHERE items.status = 0
          AND items.deletedAt IS NULL
          AND items.locationId = :locationId
        ORDER BY items.createdAt DESC
        """
    )
    fun getItemsByLocation(locationId: Long): Flow<List<ItemDetail>>

    @Query("SELECT * FROM items WHERE id = :id AND deletedAt IS NULL")
    suspend fun getItemById(id: Long): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("UPDATE items SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedAt: Long)

    @Query("UPDATE items SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("UPDATE items SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int)

    @Query(
        """
        UPDATE items
        SET status = :status,
            rating = :rating,
            ratedAt = :ratedAt
        WHERE id = :id
        """
    )
    suspend fun updateStatusAndRating(id: Long, status: Int, rating: Int?, ratedAt: Long?)

    @Query("UPDATE items SET rating = :rating, ratedAt = :ratedAt WHERE id = :id")
    suspend fun updateRating(id: Long, rating: Int, ratedAt: Long)

    @Query(
        """
        SELECT * FROM items
        WHERE status = 0
          AND deletedAt IS NULL
          AND expireTime IS NOT NULL
          AND expireTime > 0
        """
    )
    suspend fun getActiveItemsSync(): List<Item>

    @Query("SELECT COUNT(*) FROM items WHERE status = 0 AND deletedAt IS NULL")
    fun getActiveCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM items
        WHERE status = 0
          AND deletedAt IS NULL
          AND expireTime IS NOT NULL
          AND expireTime <= :thresholdTime
          AND expireTime > 0
        """
    )
    fun getExpiringCount(thresholdTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE deletedAt IS NULL")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE status = 1 AND deletedAt IS NULL")
    fun getUsedUpCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(price * quantity), 0.0) FROM items WHERE status = 0 AND deletedAt IS NULL")
    fun getTotalValue(): Flow<Double>

    @Query("SELECT COUNT(*) FROM items WHERE status = 0 AND deletedAt IS NULL AND categoryId = :categoryId")
    fun getCountByCategory(categoryId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE status = 0 AND deletedAt IS NULL AND locationId = :locationId")
    fun getCountByLocation(locationId: Long): Flow<Int>

    @Query(
        """
        SELECT items.*,
               categories.name AS categoryName,
               locations.name AS locationName
        FROM items
        LEFT JOIN categories ON items.categoryId = categories.id
        LEFT JOIN locations ON items.locationId = locations.id
        WHERE items.deletedAt IS NOT NULL
          AND items.deletedAt >= :cutoffTime
        ORDER BY items.deletedAt DESC
        """
    )
    fun getRecycleItems(cutoffTime: Long): Flow<List<ItemDetail>>

    @Query("SELECT COUNT(*) FROM items WHERE deletedAt IS NOT NULL AND deletedAt >= :cutoffTime")
    fun getRecycleCount(cutoffTime: Long): Flow<Int>

    @Query("SELECT * FROM items WHERE deletedAt IS NOT NULL AND deletedAt < :cutoffTime")
    suspend fun getDeletedItemsBefore(cutoffTime: Long): List<Item>

    @Query("DELETE FROM items WHERE deletedAt IS NOT NULL AND deletedAt < :cutoffTime")
    suspend fun purgeDeletedBefore(cutoffTime: Long)

    @Query(
        """
        SELECT items.*,
               categories.name AS categoryName,
               locations.name AS locationName
        FROM items
        LEFT JOIN categories ON items.categoryId = categories.id
        LEFT JOIN locations ON items.locationId = locations.id
        WHERE items.status = 0
          AND items.deletedAt IS NULL
          AND items.expireTime IS NOT NULL
          AND items.expireTime <= :thresholdTime
          AND items.expireTime > 0
          AND items.expireTime > :currentTime
        ORDER BY items.expireTime ASC
        """
    )
    fun getExpiringItemsInRange(currentTime: Long, thresholdTime: Long): Flow<List<ItemDetail>>
}
