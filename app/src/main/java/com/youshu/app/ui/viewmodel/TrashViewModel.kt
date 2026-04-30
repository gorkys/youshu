package com.youshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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

    fun restore(itemId: Long) {
        viewModelScope.launch {
            itemRepository.restoreFromTrash(itemId)
        }
    }
}
