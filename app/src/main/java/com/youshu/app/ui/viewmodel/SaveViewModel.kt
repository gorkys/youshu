package com.youshu.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.ai.AiCallResult
import com.youshu.app.data.ai.AiRepository
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
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class SaveItemState(
    val name: String = "",
    val categoryId: Long? = null,
    val locationId: Long? = null,
    val quantity: Int = 1,
    val unit: String = "件",
    val price: String = "",
    val expireTime: Long? = null,
    val note: String = "",
    val imagePaths: List<String> = emptyList(),
    val aiStatusMessage: String? = null,
    val isAiRecognizing: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class SaveViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: LocationRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SaveItemState())
    val state: StateFlow<SaveItemState> = _state.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locations: StateFlow<List<Location>> = locationRepository.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initFromPhoto(context: Context, imageUri: Uri?, aiName: String?, aiCategory: String?) {
        val current = _state.value
        if (imageUri == null || current.imagePaths.isNotEmpty()) return

        val savedPath = ImageUtil.saveImageToInternal(context, imageUri)
        _state.value = current.copy(
            name = aiName ?: "",
            imagePaths = savedPath?.let(::listOf).orEmpty()
        )
        if (aiCategory != null) {
            viewModelScope.launch {
                categories.value.find { it.name == aiCategory }?.let { category ->
                    _state.value = _state.value.copy(categoryId = category.id)
                }
            }
        }
        recognizeWithAiIfPossible()
    }

    fun initFromPhotos(context: Context, imageUris: List<Uri>, aiName: String?, aiCategory: String?) {
        val current = _state.value
        if (imageUris.isEmpty() || current.imagePaths.isNotEmpty()) return

        val savedPaths = imageUris.mapNotNull { ImageUtil.saveImageToInternal(context, it) }.distinct()
        _state.value = current.copy(
            name = aiName ?: current.name,
            imagePaths = savedPaths
        )
        if (aiCategory != null) {
            viewModelScope.launch {
                categories.value.find { it.name == aiCategory }?.let { category ->
                    _state.value = _state.value.copy(categoryId = category.id)
                }
            }
        }
        recognizeWithAiIfPossible()
    }

    fun appendPhotos(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val savedPaths = uris.mapNotNull { ImageUtil.saveImageToInternal(context, it) }
        if (savedPaths.isEmpty()) return
        _state.value = _state.value.copy(
            imagePaths = (_state.value.imagePaths + savedPaths).distinct()
        )
        recognizeWithAiIfPossible()
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
        recognizeWithAiIfPossible()
    }

    fun recognizeWithAiIfPossible() {
        val current = _state.value
        if (current.isAiRecognizing || current.imagePaths.isEmpty()) return

        _state.value = current.copy(
            isAiRecognizing = true,
            aiStatusMessage = "AI 正在识别图片..."
        )
        viewModelScope.launch {
            when (val result = aiRepository.recognizeItem(current.imagePaths, current.note.takeIf { it.isNotBlank() })) {
                is AiCallResult.Success -> applyAiSuggestion(result.data)
                is AiCallResult.Error -> {
                    _state.value = _state.value.copy(
                        isAiRecognizing = false,
                        aiStatusMessage = result.message
                    )
                }
            }
        }
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

    fun consumeAiStatusMessage() {
        _state.value = _state.value.copy(aiStatusMessage = null)
    }

    fun save() {
        val current = _state.value
        if (current.name.isBlank() || current.isSaving) return

        _state.value = current.copy(isSaving = true)
        viewModelScope.launch {
            val normalizedImages = current.imagePaths.distinct()
            val item = Item(
                name = current.name,
                categoryId = current.categoryId,
                locationId = current.locationId,
                quantity = current.quantity,
                unit = current.unit,
                price = current.price.toDoubleOrNull(),
                expireTime = current.expireTime,
                note = current.note,
                imagePath = normalizedImages.firstOrNull().orEmpty(),
                imagePaths = Item.encodeImagePaths(normalizedImages)
            )
            itemRepository.insert(item)
            _state.value = _state.value.copy(isSaving = false, isSaved = true)
        }
    }

    fun reset() {
        _state.value = SaveItemState()
    }

    private fun applyAiSuggestion(suggestion: com.youshu.app.data.ai.AiItemSuggestion) {
        val current = _state.value
        val matchedCategory = suggestion.categoryName?.let { name ->
            categories.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }
        val matchedLocation = suggestion.locationName?.let { name ->
            locations.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }

        _state.value = current.copy(
            name = current.name.ifBlank { suggestion.name.orEmpty() },
            categoryId = current.categoryId ?: matchedCategory?.id,
            locationId = current.locationId ?: matchedLocation?.id,
            quantity = if (current.quantity == 1) suggestion.quantity ?: current.quantity else current.quantity,
            unit = if (current.unit.isBlank() || current.unit == "件") suggestion.unit ?: current.unit else current.unit,
            price = current.price.ifBlank { suggestion.price?.toString().orEmpty() },
            expireTime = current.expireTime ?: suggestion.expireDate.toEpochMillisOrNull(),
            note = current.note.ifBlank { suggestion.note.orEmpty() },
            isAiRecognizing = false,
            aiStatusMessage = buildAiResultMessage(suggestion.confidence)
        )
    }

    private fun buildAiResultMessage(confidence: Double?): String {
        val confidenceText = confidence?.let { "，置信度 ${(it * 100).toInt()}%" }.orEmpty()
        return "AI 已填入识别建议$confidenceText，请确认后保存"
    }

    private fun String?.toEpochMillisOrNull(): Long? {
        val value = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            LocalDate.parse(value)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }
}
