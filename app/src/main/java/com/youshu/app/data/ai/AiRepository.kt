package com.youshu.app.data.ai

import com.youshu.app.data.local.entity.AiModelConfig
import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.Location
import com.youshu.app.data.repository.AiModelConfigRepository
import com.youshu.app.data.repository.CategoryRepository
import com.youshu.app.data.repository.LocationRepository
import com.youshu.app.util.LegacyTextNormalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val modelConfigRepository: AiModelConfigRepository,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: LocationRepository,
    private val clientFactory: AiClientFactory,
    private val imageEncoder: AiImageEncoder
) {

    suspend fun recognizeItem(
        imagePaths: List<String>,
        userHint: String? = null
    ): AiCallResult<AiItemSuggestion> {
        val config = modelConfigRepository.getDefaultConfig()
            ?: return AiCallResult.Error("请先在 AI 模型管理中配置默认模型")
        val client = clientFactory.create(config)
            ?: return AiCallResult.Error("暂不支持当前 Provider：${config.provider}")
        val categories = categoryRepository.getAllCategoriesSnapshot()
        val locations = locationRepository.getAllLocationsSnapshot()
        val prompt = AiPromptBuilder.buildItemRecognitionPrompt(categories, locations, userHint)
        val encodedImages = imageEncoder.encodeImages(imagePaths)
        if (imagePaths.isNotEmpty() && encodedImages.isEmpty()) {
            return AiCallResult.Error("图片读取失败，无法发送给 AI 识别")
        }

        return client.recognizeItem(config, prompt, encodedImages)
            .normalizeItemSuggestion(categories, locations)
    }

    suspend fun parseSearchIntent(query: String): AiCallResult<AiSearchIntent> {
        if (query.isBlank()) return AiCallResult.Error("搜索内容不能为空")
        val config = modelConfigRepository.getDefaultConfig()
            ?: return AiCallResult.Error("请先在 AI 模型管理中配置默认模型")
        val client = clientFactory.create(config)
            ?: return AiCallResult.Error("暂不支持当前 Provider：${config.provider}")
        val categories = categoryRepository.getAllCategoriesSnapshot()
        val locations = locationRepository.getAllLocationsSnapshot()
        val prompt = AiPromptBuilder.buildSearchIntentPrompt(query, categories, locations)

        return client.parseSearchIntent(config, prompt)
            .normalizeSearchIntent(categories, locations)
    }

    suspend fun testConnection(config: AiModelConfig): AiCallResult<Unit> {
        val client = clientFactory.create(config)
            ?: return AiCallResult.Error("暂不支持当前 Provider：${config.provider}")
        return client.testConnection(config)
    }

    private fun AiCallResult<AiItemSuggestion>.normalizeItemSuggestion(
        categories: List<Category>,
        locations: List<Location>
    ): AiCallResult<AiItemSuggestion> {
        return when (this) {
            is AiCallResult.Error -> this
            is AiCallResult.Success -> {
                val categoryName = data.categoryName.matchExistingName(categories.map { it.name })
                val locationName = data.locationName.matchExistingName(locations.map { it.name })
                AiCallResult.Success(
                    data.copy(
                        name = data.name?.trim()?.takeIf { it.isNotBlank() },
                        categoryName = categoryName,
                        locationName = locationName,
                        quantity = data.quantity?.coerceAtLeast(1),
                        unit = data.unit?.trim()?.takeIf { it.isNotBlank() },
                        confidence = data.confidence?.coerceIn(0.0, 1.0)
                    )
                ).rejectEmptyItemSuggestion()
            }
        }
    }

    private fun AiCallResult<AiSearchIntent>.normalizeSearchIntent(
        categories: List<Category>,
        locations: List<Location>
    ): AiCallResult<AiSearchIntent> {
        return when (this) {
            is AiCallResult.Error -> this
            is AiCallResult.Success -> AiCallResult.Success(
                data.copy(
                    keywords = data.keywords.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
                    categoryName = data.categoryName.matchExistingName(categories.map { it.name }),
                    locationName = data.locationName.matchExistingName(locations.map { it.name }),
                    expiringWithinDays = data.expiringWithinDays?.coerceIn(1, 365)
                )
            )
        }
    }

    private fun String?.matchExistingName(candidates: List<String>): String? {
        val normalized = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return candidates.firstOrNull { candidate ->
            LegacyTextNormalizer.namesMatch(candidate, normalized)
        }
            ?: candidates.firstOrNull { normalized.contains(it, ignoreCase = true) }
            ?: candidates.firstOrNull { it.contains(normalized, ignoreCase = true) }
    }

    private fun AiCallResult<AiItemSuggestion>.rejectEmptyItemSuggestion(): AiCallResult<AiItemSuggestion> {
        return when (this) {
            is AiCallResult.Error -> this
            is AiCallResult.Success -> {
                val suggestion = data
                val hasName = !suggestion.name.isNullOrBlank()
                val hasUsefulField = hasName ||
                    !suggestion.categoryName.isNullOrBlank() ||
                    !suggestion.locationName.isNullOrBlank() ||
                    suggestion.expireDate != null ||
                    !suggestion.note.isNullOrBlank()
                val confidence = suggestion.confidence ?: 0.0
                if (!hasUsefulField || (!hasName && confidence <= 0.0)) {
                    AiCallResult.Error("AI 未识别出有效物品信息，请确认模型支持图片识别，或换一张更清晰的图片")
                } else {
                    this
                }
            }
        }
    }
}
