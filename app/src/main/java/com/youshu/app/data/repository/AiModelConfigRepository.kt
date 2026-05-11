package com.youshu.app.data.repository

import com.youshu.app.data.local.dao.AiModelConfigDao
import com.youshu.app.data.local.entity.AiModelConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiModelConfigRepository @Inject constructor(
    private val aiModelConfigDao: AiModelConfigDao
) {
    fun getAllConfigs(): Flow<List<AiModelConfig>> = aiModelConfigDao.getAllConfigs()

    suspend fun getDefaultConfig(): AiModelConfig? = aiModelConfigDao.getDefaultConfig()

    suspend fun getAllConfigsSnapshot(): List<AiModelConfig> = aiModelConfigDao.getAllConfigsSnapshot()

    suspend fun upsert(config: AiModelConfig): Long {
        val normalized = config.copy(updatedAt = System.currentTimeMillis())
        return if (normalized.id == 0L) {
            aiModelConfigDao.insert(normalized)
        } else {
            aiModelConfigDao.update(normalized)
            normalized.id
        }
    }

    suspend fun delete(config: AiModelConfig) {
        aiModelConfigDao.delete(config)
    }

    suspend fun setDefault(id: Long) {
        aiModelConfigDao.clearDefault()
        aiModelConfigDao.markDefault(id, System.currentTimeMillis())
    }

    suspend fun replaceAll(configs: List<AiModelConfig>) {
        aiModelConfigDao.deleteAll()
        configs.forEach { aiModelConfigDao.insert(it) }
    }
}
