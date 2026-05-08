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

data class EditItemState(
    val itemId: Long = 0,
    val name: String = "",
    val categoryId: Long? = null,
    val locationId: Long? = null,
    val quantity: Int = 1,
    val unit: String = "件",
    val price: String = "",
    val expireTime: Long? = null,
    val note: String = "",
    val imagePaths: List<String> = emptyList(),
    val rating: Int? = null,
    val ratedAt: Long? = null,
    val status: Int = Item.STATUS_IN_USE,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isLoaded: Boolean = false
)

@HiltViewModel
class EditViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditItemState())
    val state: StateFlow<EditItemState> = _state.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locations: StateFlow<List<Location>> = locationRepository.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadItem(itemId: Long) {
        if (_state.value.isLoaded && _state.value.itemId == itemId) return
        viewModelScope.launch {
            val item = itemRepository.getItemById(itemId) ?: return@launch
            _state.value = EditItemState(
                itemId = item.id,
                name = item.name,
                categoryId = item.categoryId,
                locationId = item.locationId,
                quantity = item.quantity,
                unit = item.unit,
                price = item.price?.toString() ?: "",
                expireTime = item.expireTime,
                note = item.note,
                imagePaths = item.imagePathList(),
                rating = item.rating,
                ratedAt = item.ratedAt,
                status = item.status,
                isLoaded = true
            )
        }
    }

    fun appendPhotos(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val savedPaths = uris.mapNotNull { ImageUtil.saveImageToInternal(context, it) }
        if (savedPaths.isEmpty()) return
        _state.value = _state.value.copy(
            imagePaths = (_state.value.imagePaths + savedPaths).distinct()
        )
    }

    fun replacePrimaryPhoto(context: Context, uri: Uri) {
        val savedPath = ImageUtil.saveImageToInternal(context, uri) ?: return
        val currentPaths = _state.value.imagePaths.toMutableList()
        currentPaths.firstOrNull()?.let(ImageUtil::deleteImage)
        if (currentPaths.isEmpty()) {
            currentPaths += savedPath
        } else {
            currentPaths[0] = savedPath
        }
        _state.value = _state.value.copy(imagePaths = currentPaths.distinct())
    }

    fun removePhoto(index: Int) {
        val currentPaths = _state.value.imagePaths.toMutableList()
        val removed = currentPaths.getOrNull(index) ?: return
        currentPaths.removeAt(index)
        ImageUtil.deleteImage(removed)
        _state.value = _state.value.copy(imagePaths = currentPaths)
    }

    fun movePhoto(fromIndex: Int, toIndex: Int) {
        val currentPaths = _state.value.imagePaths.toMutableList()
        if (fromIndex !in currentPaths.indices || toIndex !in currentPaths.indices || fromIndex == toIndex) return
        val moved = currentPaths.removeAt(fromIndex)
        currentPaths.add(toIndex, moved)
        _state.value = _state.value.copy(imagePaths = currentPaths)
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
            val original = itemRepository.getItemById(current.itemId)
            val normalizedImages = current.imagePaths.distinct()
            val item = Item(
                id = current.itemId,
                name = current.name,
                categoryId = current.categoryId,
                locationId = current.locationId,
                quantity = current.quantity,
                unit = current.unit,
                price = current.price.toDoubleOrNull(),
                expireTime = current.expireTime,
                status = current.status,
                rating = current.rating,
                ratedAt = current.ratedAt,
                deletedAt = original?.deletedAt,
                note = current.note,
                imagePath = normalizedImages.firstOrNull().orEmpty(),
                imagePaths = Item.encodeImagePaths(normalizedImages),
                createdAt = original?.createdAt ?: System.currentTimeMillis()
            )
            itemRepository.update(item)
            _state.value = _state.value.copy(isSaving = false, isSaved = true)
        }
    }
}
