package com.youshu.app.data.ai

import com.youshu.app.data.local.entity.AiModelConfig

interface AiClient {
    suspend fun recognizeItem(
        config: AiModelConfig,
        prompt: String,
        imageBase64List: List<String>
    ): AiCallResult<AiItemSuggestion>

    suspend fun parseSearchIntent(
        config: AiModelConfig,
        prompt: String
    ): AiCallResult<AiSearchIntent>

    suspend fun testConnection(config: AiModelConfig): AiCallResult<Unit>
}
