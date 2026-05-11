package com.youshu.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_model_configs",
    indices = [Index("isDefault"), Index("provider")]
)
data class AiModelConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alias: String,
    val provider: String,
    val endpoint: String,
    val modelName: String,
    val apiKey: String = "",
    val supportsVision: Boolean = true,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
