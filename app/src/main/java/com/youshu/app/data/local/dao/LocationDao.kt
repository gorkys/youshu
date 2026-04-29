package com.youshu.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.youshu.app.data.local.entity.Location
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations ORDER BY name ASC")
    fun getAllLocations(): Flow<List<Location>>

    @Query("SELECT * FROM locations WHERE parentId IS NULL ORDER BY name ASC")
    fun getRootLocations(): Flow<List<Location>>

    @Query("SELECT * FROM locations WHERE parentId = :parentId ORDER BY name ASC")
    fun getSubLocations(parentId: Long): Flow<List<Location>>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getLocationById(id: Long): Location?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: Location): Long

    @Update
    suspend fun update(location: Location)

    @Delete
    suspend fun delete(location: Location)

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun getCount(): Int
}
