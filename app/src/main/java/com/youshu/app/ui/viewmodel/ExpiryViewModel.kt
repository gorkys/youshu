package com.youshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ExpiryViewModel @Inject constructor(
    itemRepository: ItemRepository
) : ViewModel() {

    val expirableItems: StateFlow<List<ItemDetail>> = itemRepository.getActiveItems()
        .map { items -> items.filter { it.item.expireTime != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
