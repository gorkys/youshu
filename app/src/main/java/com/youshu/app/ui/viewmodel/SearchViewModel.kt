package com.youshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.repository.ItemRepository
import com.youshu.app.data.repository.LocationRepository
import com.youshu.app.data.search.SearchCriteria
import com.youshu.app.data.search.SearchEngine
import com.youshu.app.data.search.SearchItemStatus
import com.youshu.app.data.search.SearchRatingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryStatusFilter(val label: String) {
    ALL("全部物品"),
    USED_UP("已用完"),
    PENDING_REVIEW("待评价"),
    REVIEWED("已评价")
}

data class LibraryStatusCount(
    val filter: LibraryStatusFilter,
    val count: Int
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    locationRepository: LocationRepository
) : ViewModel() {

    private val searchEngine = SearchEngine()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedFilter = MutableStateFlow(LibraryStatusFilter.ALL)
    val selectedFilter: StateFlow<LibraryStatusFilter> = _selectedFilter.asStateFlow()

    val allItems: StateFlow<List<ItemDetail>> = itemRepository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allLocations = locationRepository.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statusCounts: StateFlow<List<LibraryStatusCount>> = allItems
        .combine(_selectedFilter) { items, _ ->
            LibraryStatusFilter.entries.map { filter ->
                LibraryStatusCount(filter, items.count { matchesFilter(it, filter) })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val results: StateFlow<List<ItemDetail>> = combine(
        allItems,
        _query,
        _selectedFilter,
        allLocations
    ) { items, query, filter, locations ->
        searchEngine.filter(items, locations, filter.toSearchCriteria(query))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(query: String) {
        _query.value = query
    }

    fun selectFilter(filter: LibraryStatusFilter) {
        _selectedFilter.value = filter
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

    private fun matchesFilter(itemDetail: ItemDetail, filter: LibraryStatusFilter): Boolean {
        val item = itemDetail.item
        return when (filter) {
            LibraryStatusFilter.ALL -> true
            LibraryStatusFilter.USED_UP -> item.status == Item.STATUS_USED_UP
            LibraryStatusFilter.PENDING_REVIEW -> item.isPendingReview()
            LibraryStatusFilter.REVIEWED -> item.hasReview()
        }
    }

    private fun LibraryStatusFilter.toSearchCriteria(query: String): SearchCriteria {
        return when (this) {
            LibraryStatusFilter.ALL -> SearchCriteria(text = query)
            LibraryStatusFilter.USED_UP -> SearchCriteria(
                text = query,
                status = SearchItemStatus.USED_UP
            )
            LibraryStatusFilter.PENDING_REVIEW -> SearchCriteria(
                text = query,
                status = SearchItemStatus.USED_UP,
                ratingState = SearchRatingState.PENDING
            )
            LibraryStatusFilter.REVIEWED -> SearchCriteria(
                text = query,
                status = SearchItemStatus.USED_UP,
                ratingState = SearchRatingState.REVIEWED
            )
        }
    }
}
