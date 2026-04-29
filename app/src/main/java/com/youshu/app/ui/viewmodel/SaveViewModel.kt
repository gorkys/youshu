package com.youshu.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.Location
import com.youshu.app.data.repository.CategoryRepository
import com.youshu.app.data.repository.ItemRepository
import com.youshu.app.data.repository.LocationRepository
import com.youshu.app.util.ImageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaveItemState(
    val name: String = "",
    val categoryId: Long? = null,
    val locationId: Long? = null,
    val quantity: Int = 1,
    val unit: String = "个",
    val price: String = "",
    val expireTime: Long? = null,
    val note: String = "",
    val imagePath: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class SaveViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SaveItemState())
    val state: StateFlow<SaveItemState> = _state.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locations: StateFlow<List<Location>> = locationRepository.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initFromPhoto(context: Context, imageUri: Uri, aiName: String?, aiCategory: String?) {
        val savedPath = ImageUtil.saveImageToInternal(context, imageUri)
        _state.value = _state.value.copy(
            name = aiName ?: "",
            imagePath = savedPath ?: ""
        )
        // Try to match AI category
        if (aiCategory != null) {
            viewModelScope.launch {
                categories.value.find { it.name == aiCategory }?.let {
                    _state.value = _state.value.copy(categoryId = it.id)
                }
            }
        }
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name)
    }

    fun updateCategory(categoryId: Long?) {
        _state.value = _state.value.copy(categoryId = categoryId)
    }

    fun updateLocation(locationId: Long?) {
        _state.value = _state.value.copy(locationId = locationId)
    }

    fun updateQuantity(quantity: Int) {
        _state.value = _state.value.copy(quantity = quantity.coerceAtLeast(1))
    }

    fun updateUnit(unit: String) {
        _state.value = _state.value.copy(unit = unit)
    }

    fun updatePrice(price: String) {
        _state.value = _state.value.copy(price = price)
    }

    fun updateExpireTime(time: Long?) {
        _state.value = _state.value.copy(expireTime = time)
    }

    fun updateNote(note: String) {
        _state.value = _state.value.copy(note = note)
    }

    fun save() {
        val current = _state.value
        if (current.name.isBlank() || current.isSaving) return

        _state.value = current.copy(isSaving = true)
        viewModelScope.launch {
            val item = Item(
                name = current.name,
                categoryId = current.categoryId,
                locationId = current.locationId,
                quantity = current.quantity,
                unit = current.unit,
                price = current.price.toDoubleOrNull(),
                expireTime = current.expireTime,
                note = current.note,
                imagePath = current.imagePath
            )
            itemRepository.insert(item)
            _state.value = _state.value.copy(isSaving = false, isSaved = true)
        }
    }

    fun reset() {
        _state.value = SaveItemState()
    }
}
