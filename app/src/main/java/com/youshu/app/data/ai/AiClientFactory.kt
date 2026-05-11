package com.youshu.app.data.ai

import com.youshu.app.data.local.entity.AiModelConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiClientFactory @Inject constructor(
    private val openAiCompatibleClient: OpenAiCompatibleClient
) {
    fun create(config: AiModelConfig): AiClient? {
        return when (config.provider) {
            AiProviders.OPENAI_COMPATIBLE,
            AiProviders.OLLAMA -> openAiCompatibleClient
            else -> null
        }
    }
}
