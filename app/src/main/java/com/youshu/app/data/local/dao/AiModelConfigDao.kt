package com.youshu.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.youshu.app.data.local.entity.AiModelConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface AiModelConfigDao {

    @Query("SELECT * FROM ai_model_configs ORDER BY isDefault DESC, updatedAt DESC")
    fun getAllConfigs(): Flow<List<AiModelConfig>>

    @Query("SELECT * FROM ai_model_configs WHERE id = :id")
    suspend fun getConfigById(id: Long): AiModelConfig?

    @Query("SELECT * FROM ai_model_configs WHERE isDefault = 1 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getDefaultConfig(): AiModelConfig?

    @Query("SELECT * FROM ai_model_configs ORDER BY id ASC")
    suspend fun getAllConfigsSnapshot(): List<AiModelConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: AiModelConfig): Long

    @Update
    suspend fun update(config: AiModelConfig)

    @Delete
    suspend fun delete(config: AiModelConfig)

    @Query("UPDATE ai_model_configs SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE ai_model_configs SET isDefault = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDefault(id: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM ai_model_configs")
    suspend fun getCount(): Int

    @Query("DELETE FROM ai_model_configs")
    suspend fun deleteAll()
}
