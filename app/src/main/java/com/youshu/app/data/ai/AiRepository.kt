package com.youshu.app.data.ai

import android.util.Log
import com.youshu.app.BuildConfig
import com.youshu.app.data.local.entity.AiModelConfig
import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.local.entity.Location
import com.youshu.app.data.repository.AiModelConfigRepository
import com.youshu.app.data.repository.CategoryRepository
import com.youshu.app.data.repository.ItemRepository
import com.youshu.app.data.repository.LocationRepository
import com.youshu.app.data.search.SearchCriteria
import com.youshu.app.data.search.SearchEngine
import com.youshu.app.data.search.SearchIntentKind
import com.youshu.app.data.search.SearchItemStatus
import com.youshu.app.data.search.SearchRatingState
import com.youshu.app.util.LegacyTextNormalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val modelConfigRepository: AiModelConfigRepository,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: LocationRepository,
    private val itemRepository: ItemRepository,
    private val clientFactory: AiClientFactory,
    private val imageEncoder: AiImageEncoder
) {
    private val searchEngine = SearchEngine()

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
        val context = when (val result = buildSearchContext(query)) {
            is AiCallResult.Error -> return result
            is AiCallResult.Success -> result.data
        }
        return requestSearchIntent(
            context = context,
            query = query,
            prompt = AiPromptBuilder.buildSearchIntentPrompt(
                query = query,
                categories = context.categories,
                locations = context.locations,
                items = context.items
            )
        )
    }

    suspend fun parseSearchCriteria(query: String): AiCallResult<SearchCriteria> {
        val context = when (val result = buildSearchContext(query)) {
            is AiCallResult.Error -> return result
            is AiCallResult.Success -> result.data
        }
        val initialIntent = when (
            val result = requestSearchIntent(
                context = context,
                query = query,
                prompt = AiPromptBuilder.buildSearchIntentPrompt(
                    query = query,
                    categories = context.categories,
                    locations = context.locations,
                    items = context.items
                )
            )
        ) {
            is AiCallResult.Error -> return result
            is AiCallResult.Success -> result.data
        }
        val initialCriteria = AiSearchIntentNormalizer.toSearchCriteria(query, initialIntent)
        val initialEvaluation = evaluateSearchPlan(initialCriteria, context)
        logSearchPlan("initial", query, initialIntent, initialCriteria, initialEvaluation)
        return AiCallResult.Success(initialCriteria)
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

    private suspend fun buildSearchContext(query: String): AiCallResult<SearchContext> {
        if (query.isBlank()) return AiCallResult.Error("搜索内容不能为空")
        val config = modelConfigRepository.getDefaultConfig()
            ?: return AiCallResult.Error("请先在 AI 模型管理中配置默认模型")
        val client = clientFactory.create(config)
            ?: return AiCallResult.Error("暂不支持当前 Provider：${config.provider}")
        return AiCallResult.Success(
            SearchContext(
                config = config,
                client = client,
                categories = categoryRepository.getAllCategoriesSnapshot(),
                locations = locationRepository.getAllLocationsSnapshot(),
                items = itemRepository.getAllItemsSnapshot().filter { it.deletedAt == null }
            )
        )
    }

    private suspend fun requestSearchIntent(
        context: SearchContext,
        query: String,
        prompt: String
    ): AiCallResult<AiSearchIntent> {
        return AiSearchIntentNormalizer.normalize(
            result = context.client.parseSearchIntent(context.config, prompt),
            query = query,
            items = context.items,
            categories = context.categories,
            locations = context.locations
        )
    }

    private fun evaluateSearchPlan(
        criteria: SearchCriteria,
        context: SearchContext
    ): List<ItemDetail> {
        return searchEngine.filter(
            items = context.items.toItemDetails(context.categories, context.locations),
            locations = context.locations,
            criteria = criteria
        )
    }

    private fun logSearchPlan(
        stage: String,
        query: String,
        intent: AiSearchIntent,
        criteria: SearchCriteria,
        matchedItems: List<ItemDetail>
    ) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            SEARCH_LOG_TAG,
            "stage=$stage query=$query intent=$intent criteria=$criteria matched=${matchedItems.size} " +
                "items=${matchedItems.joinToString(limit = 12) { "${it.item.id}:${it.item.name}" }}"
        )
    }

    private fun SearchCriteria.isExecutablePlan(): Boolean {
        return itemIds.isNotEmpty() ||
            categoryIds.isNotEmpty() ||
            locationIds.isNotEmpty() ||
            !itemName.isNullOrBlank() ||
            !categoryName.isNullOrBlank() ||
            !locationName.isNullOrBlank() ||
            semanticTerms.isNotEmpty() ||
            status != SearchItemStatus.ANY ||
            ratingState != SearchRatingState.ANY ||
            expiringWithinDays != null
    }

    private fun List<Item>.toItemDetails(
        categories: List<Category>,
        locations: List<Location>
    ): List<ItemDetail> {
        val categoryNames = categories.associate { it.id to it.name }
        val locationNames = locations.associate { it.id to it.name }
        return map { item ->
            ItemDetail(
                item = item,
                categoryName = item.categoryId?.let(categoryNames::get),
                locationName = item.locationId?.let(locationNames::get)
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

    private data class SearchContext(
        val config: AiModelConfig,
        val client: AiClient,
        val categories: List<Category>,
        val locations: List<Location>,
        val items: List<Item>
    )

    private companion object {
        const val SEARCH_LOG_TAG = "AiSearch"
    }
}
