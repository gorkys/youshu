package com.youshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.local.dao.ItemDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    itemDao: ItemDao
) : ViewModel() {

    private val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000

    val totalCount: StateFlow<Int> = itemDao.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeCount: StateFlow<Int> = itemDao.getActiveCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val usedUpCount: StateFlow<Int> = itemDao.getUsedUpCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L

    val expiringCount: StateFlow<Int> = itemDao
        .getExpiringCount(System.currentTimeMillis() + sevenDaysMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalValue: StateFlow<Double> = itemDao.getTotalValue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val trashCount: StateFlow<Int> = itemDao
        .getRecycleCount(System.currentTimeMillis() - thirtyDaysMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
