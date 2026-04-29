package com.youshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedFilter = MutableStateFlow(LibraryStatusFilter.ALL)
    val selectedFilter: StateFlow<LibraryStatusFilter> = _selectedFilter.asStateFlow()

    val allItems: StateFlow<List<ItemDetail>> = itemRepository.getAllItems()
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
        _selectedFilter
    ) { items, query, filter ->
        items.filter { itemDetail ->
            matchesFilter(itemDetail, filter) && matchesQuery(itemDetail, query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(query: String) {
        _query.value = query
    }

    fun selectFilter(filter: LibraryStatusFilter) {
        _selectedFilter.value = filter
    }

    private fun matchesQuery(itemDetail: ItemDetail, query: String): Boolean {
        if (query.isBlank()) return true
        return listOf(
            itemDetail.item.name,
            itemDetail.categoryName.orEmpty(),
            itemDetail.locationName.orEmpty(),
            itemDetail.item.note
        ).any { it.contains(query, ignoreCase = true) }
    }

    private fun matchesFilter(itemDetail: ItemDetail, filter: LibraryStatusFilter): Boolean {
        val item = itemDetail.item
        return when (filter) {
            LibraryStatusFilter.ALL -> true
            LibraryStatusFilter.USED_UP -> item.status == Item.STATUS_USED_UP
            LibraryStatusFilter.PENDING_REVIEW -> {
                item.status == Item.STATUS_USED_UP && item.rating == null
            }
            LibraryStatusFilter.REVIEWED -> {
                item.status == Item.STATUS_USED_UP && item.rating != null
            }
        }
    }
}
