package com.youshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.repository.ItemRepository
import com.youshu.app.util.ImageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val retentionWindowMs = 30L * 24 * 60 * 60 * 1000
    private val cutoffTime = System.currentTimeMillis() - retentionWindowMs

    val deletedItems: StateFlow<List<ItemDetail>> = itemRepository
        .getRecycleItems(cutoffTime)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    fun toggleSelection(itemId: Long) {
        _selectedIds.update { current ->
            if (itemId in current) current - itemId else current + itemId
        }
    }

    fun selectAll(itemIds: Set<Long>) {
        _selectedIds.value = itemIds
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun pruneSelection(validIds: Set<Long>) {
        _selectedIds.update { current -> current.intersect(validIds) }
    }

    fun restore(itemId: Long) {
        viewModelScope.launch {
            itemRepository.restoreFromTrash(itemId)
            _selectedIds.update { it - itemId }
        }
    }

    fun restoreSelected() {
        val itemIds = _selectedIds.value.toList()
        if (itemIds.isEmpty()) return

        viewModelScope.launch {
            itemRepository.restoreFromTrash(itemIds)
            _selectedIds.value = emptySet()
        }
    }

    fun permanentlyDelete(itemIds: List<Long>) {
        if (itemIds.isEmpty()) return

        viewModelScope.launch {
            itemRepository.permanentlyDeleteFromTrash(itemIds).forEach { item ->
                if (item.imagePath.isNotBlank()) {
                    ImageUtil.deleteImage(item.imagePath)
                }
            }
            _selectedIds.update { it - itemIds.toSet() }
        }
    }
}
