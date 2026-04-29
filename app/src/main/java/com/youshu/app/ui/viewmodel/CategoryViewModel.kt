package com.youshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.local.entity.Location
import com.youshu.app.data.repository.CategoryRepository
import com.youshu.app.data.repository.ItemRepository
import com.youshu.app.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val locationRepository: LocationRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rootLocations: StateFlow<List<Location>> = locationRepository.getRootLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _selectedLocationId = MutableStateFlow<Long?>(null)
    val selectedLocationId: StateFlow<Long?> = _selectedLocationId.asStateFlow()

    val filteredItems: StateFlow<List<ItemDetail>> =
        combine(_selectedCategoryId, _selectedLocationId) { catId, locId -> catId to locId }
            .flatMapLatest { (catId, locId) ->
                when {
                    catId != null -> itemRepository.getItemsByCategory(catId)
                    locId != null -> itemRepository.getItemsByLocation(locId)
                    else -> itemRepository.getActiveItems()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(id: Long?) {
        _selectedCategoryId.value = id
        _selectedLocationId.value = null
    }

    fun selectLocation(id: Long?) {
        _selectedLocationId.value = id
        _selectedCategoryId.value = null
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.insert(Category(name = name))
        }
    }

    fun addLocation(name: String, parentId: Long? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            locationRepository.insert(Location(name = name, parentId = parentId))
        }
    }
}
