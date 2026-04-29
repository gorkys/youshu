package com.youshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.repository.ItemRepository
import com.youshu.app.util.ImageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _itemDetail = MutableStateFlow<ItemDetail?>(null)
    val itemDetail: StateFlow<ItemDetail?> = _itemDetail.asStateFlow()

    fun loadItem(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            itemRepository.getItemDetailById(id).collect { detail ->
                _itemDetail.value = detail
            }
        }
    }

    fun markAsUsed(id: Long) {
        viewModelScope.launch {
            itemRepository.markAsUsed(id)
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
