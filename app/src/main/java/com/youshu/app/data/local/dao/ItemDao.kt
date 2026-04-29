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
        WHERE items.status = 0 AND items.categoryId = :categoryId
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
        WHERE items.status = 0 AND items.locationId = :locationId
        ORDER BY items.createdAt DESC
        """
    )
    fun getItemsByLocation(locationId: Long): Flow<List<ItemDetail>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

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
          AND expireTime IS NOT NULL
          AND expireTime > 0
        """
    )
    suspend fun getActiveItemsSync(): List<Item>

    @Query("SELECT COUNT(*) FROM items WHERE status = 0")
    fun getActiveCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM items
        WHERE status = 0
          AND expireTime IS NOT NULL
          AND expireTime <= :thresholdTime
          AND expireTime > 0
        """
    )
    fun getExpiringCount(thresholdTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM items")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE status = 1")
    fun getUsedUpCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(price * quantity), 0.0) FROM items WHERE status = 0")
    fun getTotalValue(): Flow<Double>

    @Query("SELECT COUNT(*) FROM items WHERE status = 0 AND categoryId = :categoryId")
    fun getCountByCategory(categoryId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE status = 0 AND locationId = :locationId")
    fun getCountByLocation(locationId: Long): Flow<Int>

    @Query(
        """
        SELECT items.*,
               categories.name AS categoryName,
               locations.name AS locationName
        FROM items
        LEFT JOIN categories ON items.categoryId = categories.id
        LEFT JOIN locations ON items.locationId = locations.id
        WHERE items.status = 0
          AND items.expireTime IS NOT NULL
          AND items.expireTime <= :thresholdTime
          AND items.expireTime > 0
          AND items.expireTime > :currentTime
        ORDER BY items.expireTime ASC
        """
    )
    fun getExpiringItemsInRange(currentTime: Long, thresholdTime: Long): Flow<List<ItemDetail>>
}
