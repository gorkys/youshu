package com.youshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.ai.AiCallResult
import com.youshu.app.data.ai.AiRepository
import com.youshu.app.data.local.entity.AiModelConfig
import com.youshu.app.data.local.dao.ItemDao
import com.youshu.app.data.repository.AiModelConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    itemDao: ItemDao,
    private val aiModelConfigRepository: AiModelConfigRepository,
    private val aiRepository: AiRepository
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

    val aiModels: StateFlow<List<AiModelConfig>> = aiModelConfigRepository.getAllConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiModelMessage = MutableStateFlow<String?>(null)
    val aiModelMessage: StateFlow<String?> = _aiModelMessage.asStateFlow()

    fun saveAiModel(
        alias: String,
        provider: String,
        endpoint: String,
        modelName: String,
        apiKey: String,
        supportsVision: Boolean,
        isDefault: Boolean
    ) {
        viewModelScope.launch {
            val id = aiModelConfigRepository.upsert(
                AiModelConfig(
                    alias = alias.trim(),
                    provider = provider.trim(),
                    endpoint = endpoint.trim().trimEnd('/'),
                    modelName = modelName.trim(),
                    apiKey = apiKey.trim(),
                    supportsVision = supportsVision,
                    isDefault = false
                )
            )
            if (isDefault) {
                aiModelConfigRepository.setDefault(id)
            }
            _aiModelMessage.value = "AI 模型配置已保存"
        }
    }

    fun deleteAiModel(config: AiModelConfig) {
        viewModelScope.launch {
            aiModelConfigRepository.delete(config)
            _aiModelMessage.value = "AI 模型配置已删除"
        }
    }

    fun setDefaultAiModel(config: AiModelConfig) {
        viewModelScope.launch {
            aiModelConfigRepository.setDefault(config.id)
            _aiModelMessage.value = "已设为默认模型"
        }
    }

    fun testAiModel(config: AiModelConfig) {
        viewModelScope.launch {
            _aiModelMessage.value = "正在测试 AI 模型连接..."
            _aiModelMessage.value = when (val result = aiRepository.testConnection(config)) {
                is AiCallResult.Success -> "AI 模型连接正常"
                is AiCallResult.Error -> result.message
            }
        }
    }

    fun consumeAiModelMessage() {
        _aiModelMessage.value = null
    }
}
