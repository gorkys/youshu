package com.youshu.app.ui.viewmodel

import android.util.Log
import com.youshu.app.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.ai.AiCallResult
import com.youshu.app.data.ai.AiRepository
import com.youshu.app.data.local.dao.CategoryDao
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.local.entity.Location
import com.youshu.app.data.repository.ItemRepository
import com.youshu.app.data.repository.LocationRepository
import com.youshu.app.data.search.LocalSearchParser
import com.youshu.app.data.search.SearchCriteria
import com.youshu.app.data.search.SearchEngine
import com.youshu.app.data.search.SearchIntentKind
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

    private val searchEngine = SearchEngine()
    private val localSearchParser = LocalSearchParser()

    private val _searchCriteria = MutableStateFlow<SearchCriteria?>(null)
    val searchCriteria: StateFlow<SearchCriteria?> = _searchCriteria.asStateFlow()

    private val _aiSearchMessage = MutableStateFlow<String?>(null)
    val aiSearchMessage: StateFlow<String?> = _aiSearchMessage.asStateFlow()

    private val _isAiSearching = MutableStateFlow(false)
    val isAiSearching: StateFlow<Boolean> = _isAiSearching.asStateFlow()

    val searchResults: StateFlow<List<ItemDetail>> = combine(
        allItems,
        locations,
        _searchQuery,
        _searchCriteria
    ) { items, locations, query, criteria ->
        buildSearchResults(
            query = query,
            items = items,
            locations = locations,
            criteria = criteria
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _searchCriteria.value = null
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
            val localCriteria = localSearchParser.parse(query)
            if (localCriteria.kind != SearchIntentKind.PLAIN_TEXT) {
                _searchCriteria.value = localCriteria
                _aiSearchMessage.value = localCriteria.summary ?: "已按本地规则解析搜索条件"
                _isAiSearching.value = false
                return@launch
            }

            when (val result = aiRepository.parseSearchCriteria(query)) {
                is AiCallResult.Success -> {
                    if (result.data.isExecutableAiPlan()) {
                        _searchCriteria.value = result.data
                        _aiSearchMessage.value = result.data.summary ?: "AI 已解析搜索条件"
                    } else {
                        _searchCriteria.value = localCriteria
                        _aiSearchMessage.value = "AI 没有给出可执行搜索条件，已使用普通搜索"
                    }
                }

                is AiCallResult.Error -> {
                    _searchCriteria.value = localCriteria
                    _aiSearchMessage.value = "${result.message}，已使用普通搜索"
                }
            }
            _isAiSearching.value = false
        }
    }

    private fun buildSearchResults(
        query: String,
        items: List<ItemDetail>,
        locations: List<Location>,
        criteria: SearchCriteria?
    ): List<ItemDetail> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return emptyList()
        val resolvedCriteria = criteria ?: localSearchParser.parse(normalizedQuery)
        val results = searchEngine.filter(items, locations, resolvedCriteria)
        val finalResults = if (results.isEmpty() && resolvedCriteria.canFallbackToPlainText()) {
            searchEngine.filter(items, locations, SearchCriteria(text = normalizedQuery))
        } else {
            results
        }
        if (BuildConfig.DEBUG) {
            Log.d(
                SEARCH_UI_LOG_TAG,
                "query=$normalizedQuery items=${items.size} criteria=$resolvedCriteria results=${finalResults.size}"
            )
        }
        return finalResults
    }

    fun searchResults(items: List<ItemDetail>): List<ItemDetail> {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return emptyList()
        val sourceItems = allItems.value.ifEmpty { items }
        val criteria = _searchCriteria.value ?: localSearchParser.parse(query)
        val results = searchEngine.filter(sourceItems, locations.value, criteria)
        return if (results.isEmpty() && criteria.canFallbackToPlainText()) {
            searchEngine.filter(sourceItems, locations.value, SearchCriteria(text = query))
        } else {
            results
        }
    }

    private fun SearchCriteria.canFallbackToPlainText(): Boolean {
        return kind == SearchIntentKind.PLAIN_TEXT || kind == SearchIntentKind.FIND_ITEM
    }

    private fun SearchCriteria.isExecutableAiPlan(): Boolean {
        return itemIds.isNotEmpty() ||
            categoryIds.isNotEmpty() ||
            locationIds.isNotEmpty() ||
            !itemName.isNullOrBlank() ||
            !categoryName.isNullOrBlank() ||
            !locationName.isNullOrBlank() ||
            semanticTerms.isNotEmpty() ||
            hasStructuredFilters()
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

    private companion object {
        const val SEARCH_UI_LOG_TAG = "AiSearchUi"
    }
}
