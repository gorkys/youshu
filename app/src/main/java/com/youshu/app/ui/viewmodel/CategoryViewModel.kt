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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val locationRepository: LocationRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLocations: StateFlow<List<Location>> = locationRepository.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rootLocations: StateFlow<List<Location>> = locationRepository.getRootLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeItems: StateFlow<List<ItemDetail>> = itemRepository.getActiveItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _selectedLocationId = MutableStateFlow<Long?>(null)
    val selectedLocationId: StateFlow<Long?> = _selectedLocationId.asStateFlow()

    val filteredItems: StateFlow<List<ItemDetail>> = combine(
        activeItems,
        allLocations,
        _selectedCategoryId,
        _selectedLocationId
    ) { items, locations, categoryId, locationId ->
        val descendants = buildLocationDescendants(locations)
        items.filter { itemDetail ->
            val categoryMatched = categoryId == null || itemDetail.item.categoryId == categoryId
            val locationMatched = if (locationId == null) {
                true
            } else {
                itemDetail.item.locationId in descendants[locationId].orEmpty()
            }
            categoryMatched && locationMatched
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(id: Long?) {
        _selectedCategoryId.value = id
        _selectedLocationId.value = null
    }

    fun selectLocation(id: Long?) {
        _selectedLocationId.value = id
        _selectedCategoryId.value = null
    }

    fun addCategory(name: String) {
        val normalized = name.trim()
        if (normalized.isBlank()) return
        viewModelScope.launch {
            categoryRepository.insert(Category(name = normalized))
        }
    }

    fun deleteCategory(categoryId: Long?) {
        val id = categoryId ?: return
        viewModelScope.launch {
            categories.value.firstOrNull { it.id == id }?.let { category ->
                categoryRepository.delete(category)
                if (_selectedCategoryId.value == id) {
                    _selectedCategoryId.value = null
                }
            }
        }
    }

    fun addLocation(name: String, parentId: Long? = null) {
        val normalized = name.trim()
        if (normalized.isBlank()) return
        viewModelScope.launch {
            locationRepository.insert(Location(name = normalized, parentId = parentId))
        }
    }

    fun deleteLocation(locationId: Long?) {
        val id = locationId ?: return
        viewModelScope.launch {
            allLocations.value.firstOrNull { it.id == id }?.let { location ->
                locationRepository.delete(location)
                if (_selectedLocationId.value == id) {
                    _selectedLocationId.value = null
                }
            }
        }
    }

    fun getSubLocations(parentId: Long): Flow<List<Location>> {
        return locationRepository.getSubLocations(parentId)
    }

    private fun buildLocationDescendants(locations: List<Location>): Map<Long, Set<Long>> {
        val childrenMap = locations.groupBy { it.parentId }

        fun collectIds(locationId: Long): Set<Long> {
            val childIds = childrenMap[locationId].orEmpty().flatMap { collectIds(it.id) }
            return setOf(locationId) + childIds
        }

        return locations.associate { it.id to collectIds(it.id) }
    }
}
