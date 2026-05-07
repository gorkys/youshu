package com.youshu.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youshu.app.data.backup.AppBackupManager
import com.youshu.app.data.update.AppReleaseInfo
import com.youshu.app.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isBusy: Boolean = false,
    val latestRelease: AppReleaseInfo? = null,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupManager: AppBackupManager,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun exportBackup(targetUri: Uri) {
        viewModelScope.launch {
            runBusyAction(
                successMessage = "备份导出完成"
            ) {
                backupManager.exportBackup(targetUri)
            }
        }
    }

    fun importBackup(sourceUri: Uri) {
        viewModelScope.launch {
            runBusyAction(
                successMessage = "备份导入完成，建议重启应用后检查数据"
            ) {
                backupManager.importBackup(sourceUri)
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, message = null)
            val release = updateRepository.fetchLatestRelease()
            _state.value = _state.value.copy(
                isBusy = false,
                latestRelease = release,
                message = when {
                    release == null -> "检查更新失败，请稍后再试"
                    release.hasUpdate -> "发现新版本 ${release.latestVersion}"
                    else -> "当前已经是最新版本"
                }
            )
        }
    }

    fun downloadLatestApk(): Boolean {
        val release = _state.value.latestRelease ?: return false
        val apkUrl = release.apkUrl ?: return false
        updateRepository.downloadApk(apkUrl, release.latestVersion)
        _state.value = _state.value.copy(message = "已开始下载更新包")
        return true
    }

    fun installDownloadedApk(): Boolean {
        val release = _state.value.latestRelease ?: return false
        val installed = updateRepository.installDownloadedApk(release.latestVersion)
        if (!installed) {
            _state.value = _state.value.copy(message = "未找到已下载的安装包，请先下载")
        }
        return installed
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private suspend fun runBusyAction(
        successMessage: String,
        action: suspend () -> Unit
    ) {
        _state.value = _state.value.copy(isBusy = true, message = null)
        runCatching { action() }
            .onSuccess {
                _state.value = _state.value.copy(isBusy = false, message = successMessage)
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isBusy = false,
                    message = error.message ?: "操作失败，请稍后再试"
                )
            }
    }
}
