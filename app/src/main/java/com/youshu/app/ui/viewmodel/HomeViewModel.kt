package com.youshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.ai.AiCallResult
import com.youshu.app.data.ai.AiPromptBuilder
import com.youshu.app.data.ai.AiRatingState
import com.youshu.app.data.ai.AiRepository
import com.youshu.app.data.ai.AiSearchIntent
import com.youshu.app.data.ai.AiSearchSort
import com.youshu.app.data.ai.AiSearchStatus
import com.youshu.app.data.local.dao.CategoryDao
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.local.entity.Location
import com.youshu.app.data.repository.ItemRepository
import com.youshu.app.data.repository.LocationRepository
import com.youshu.app.util.LegacyTextNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryDao: CategoryDao,
    private val locationRepository: LocationRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L

    val activeItems: StateFlow<List<ItemDetail>> = itemRepository.getActiveItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allItems: StateFlow<List<ItemDetail>> = itemRepository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiringItems: StateFlow<List<ItemDetail>> = itemRepository
        .getExpiringItems(System.currentTimeMillis() + sevenDaysMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiringCount: StateFlow<Int> = itemRepository
        .getExpiringCount(System.currentTimeMillis() + sevenDaysMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categories: StateFlow<List<Category>> = itemRepository.getActiveItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        .let { activeItemsFlow ->
            combine(
                activeItemsFlow,
                categoryDao.getAllCategories()
            ) { items, categories ->
                val activeCategoryIds = items.mapNotNull { it.item.categoryId }.toSet()
                categories.filter { it.id in activeCategoryIds }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locations: StateFlow<List<Location>> = locationRepository.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _aiSearchIntent = MutableStateFlow<AiSearchIntent?>(null)
    val aiSearchIntent: StateFlow<AiSearchIntent?> = _aiSearchIntent.asStateFlow()

    private val _aiSearchMessage = MutableStateFlow<String?>(null)
    val aiSearchMessage: StateFlow<String?> = _aiSearchMessage.asStateFlow()

    private val _isAiSearching = MutableStateFlow(false)
    val isAiSearching: StateFlow<Boolean> = _isAiSearching.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _aiSearchIntent.value = null
        _aiSearchMessage.value = null
    }

    fun rememberSearch(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        _searchHistory.value = listOf(normalized) +
            _searchHistory.value.filterNot { it.equals(normalized, ignoreCase = true) }
                .take(7)
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    fun runAiSearch() {
        val query = _searchQuery.value.trim()
        if (query.isBlank() || _isAiSearching.value) return

        rememberSearch(query)
        _isAiSearching.value = true
        _aiSearchMessage.value = "AI 正在理解搜索条件..."
        viewModelScope.launch {
            when (val result = aiRepository.parseSearchIntent(query)) {
                is AiCallResult.Success -> {
                    _aiSearchIntent.value = result.data
                    _aiSearchMessage.value = result.data.summary ?: "AI 已解析搜索条件"
                }

                is AiCallResult.Error -> {
                    _aiSearchIntent.value = null
                    _aiSearchMessage.value = "${result.message}，已使用普通搜索"
                }
            }
            _isAiSearching.value = false
        }
    }

    fun searchResults(items: List<ItemDetail>): List<ItemDetail> {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return emptyList()
        val intent = _aiSearchIntent.value
        val matched = if (intent == null) {
            items.filter { it.matchesPlainQuery(query) }
        } else {
            val aiMatched = items.filter { it.matchesIntent(intent) }
            aiMatched.ifEmpty { items.filter { it.matchesPlainQuery(query) } }
        }
        return when (intent?.sort) {
            AiSearchSort.EXPIRE_ASC -> matched.sortedBy { it.item.expireTime ?: Long.MAX_VALUE }
            AiSearchSort.NAME_ASC -> matched.sortedBy { it.item.name }
            else -> matched.sortedByDescending { it.item.createdAt }
        }
    }

    fun moveToTrash(item: Item) {
        viewModelScope.launch {
            itemRepository.moveToTrash(item)
        }
    }

    fun markAsUsed(id: Long) {
        viewModelScope.launch {
            itemRepository.markAsUsed(id)
        }
    }

    fun rateUsedItem(id: Long, rating: Int) {
        viewModelScope.launch {
            itemRepository.rateUsedItem(id, rating)
        }
    }

    private fun ItemDetail.matchesPlainQuery(query: String): Boolean {
        val relatedLocationNames = locationNamesForItem()
        return listOf(
            item.name,
            categoryName.orEmpty(),
            locationName.orEmpty(),
            relatedLocationNames.joinToString(" "),
            item.note
        ).any { it.contains(query, ignoreCase = true) }
    }

    private fun ItemDetail.matchesIntent(intent: AiSearchIntent): Boolean {
        val now = System.currentTimeMillis()
        val statusMatched = when (intent.status ?: AiSearchStatus.IN_USE) {
            AiSearchStatus.IN_USE -> item.status == Item.STATUS_IN_USE
            AiSearchStatus.USED_UP -> item.status == Item.STATUS_USED_UP
            AiSearchStatus.ALL -> true
        }
        val ratingMatched = when (intent.ratingState ?: AiRatingState.ANY) {
            AiRatingState.PENDING -> item.status == Item.STATUS_USED_UP && item.rating == null
            AiRatingState.REVIEWED -> item.status == Item.STATUS_USED_UP && item.rating != null
            AiRatingState.ANY -> true
        }
        val categoryMatched = intent.categoryName?.let { requested ->
            namesMatch(categoryName, requested)
        } ?: true
        val locationMatched = intent.locationName?.let { requested ->
            locationNamesForItem().any { namesMatch(it, requested) }
        } ?: true
        val expiringMatched = intent.expiringWithinDays?.let { days ->
            val expireTime = item.expireTime ?: return@let false
            expireTime in now..(now + days * 24L * 60L * 60L * 1000L)
        } ?: true
        val hasStructuredFilters = intent.categoryName != null ||
            intent.locationName != null ||
            intent.expiringWithinDays != null ||
            intent.ratingState != null ||
            intent.status != null
        val keywordsMatched = hasStructuredFilters || intent.keywords.all { keyword ->
            AiPromptBuilder.buildItemDigest(this).contains(keyword, ignoreCase = true)
        }

        return statusMatched && ratingMatched && categoryMatched && locationMatched && expiringMatched && keywordsMatched
    }

    private fun ItemDetail.locationNamesForItem(): List<String> {
        val allLocations = locations.value
        val currentLocationId = item.locationId ?: return listOfNotNull(locationName)
        val result = mutableListOf<String>()
        var cursor = allLocations.firstOrNull { it.id == currentLocationId }
        if (cursor == null) {
            locationName?.let(result::add)
        }
        while (cursor != null) {
            result += cursor.name
            val parentId = cursor.parentId
            cursor = parentId?.let { id -> allLocations.firstOrNull { it.id == id } }
        }
        return result.distinct()
    }

    private fun namesMatch(actual: String?, requested: String): Boolean {
        return LegacyTextNormalizer.namesMatch(actual, requested)
    }
}
