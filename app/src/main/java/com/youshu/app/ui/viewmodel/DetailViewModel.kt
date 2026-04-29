package com.youshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.repository.ItemRepository
import com.youshu.app.util.ImageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    val items: StateFlow<List<ItemDetail>> = itemRepository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAsUsed(id: Long, rating: Int? = null) {
        viewModelScope.launch {
            itemRepository.markAsUsed(id, rating)
        }
    }

    fun rateUsedItem(id: Long, rating: Int) {
        viewModelScope.launch {
            itemRepository.rateUsedItem(id, rating)
        }
    }

    fun markAsDiscarded(id: Long) {
        viewModelScope.launch {
            itemRepository.markAsDiscarded(id)
        }
    }

    fun delete(item: Item) {
        viewModelScope.launch {
            itemRepository.delete(item)
            if (item.imagePath.isNotEmpty()) {
                ImageUtil.deleteImage(item.imagePath)
            }
        }
    }
}
