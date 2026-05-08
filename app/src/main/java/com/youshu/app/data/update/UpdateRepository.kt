package com.youshu.app.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.youshu.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLatestRelease(): UpdateCheckResult {
        return runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/gorkys/youshu/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "YouShu-Android/${BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return UpdateCheckResult.Error("检查更新失败：GitHub 返回 ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    return UpdateCheckResult.Error("检查更新失败：未获取到版本信息")
                }

                val release = json.decodeFromString<GitHubReleaseResponse>(body)
                val debugApk = release.assets.firstOrNull { asset ->
                    asset.name.equals("app-debug.apk", ignoreCase = true)
                }
                val fallbackApk = release.assets.firstOrNull { asset ->
                    asset.name.endsWith(".apk", ignoreCase = true)
                }

                UpdateCheckResult.Success(
                    AppReleaseInfo(
                        currentVersion = BuildConfig.VERSION_NAME,
                        latestVersion = release.tagName.removePrefix("v"),
                        releasePageUrl = release.htmlUrl,
                        apkName = debugApk?.name ?: fallbackApk?.name,
                        apkUrl = debugApk?.downloadUrl ?: fallbackApk?.downloadUrl,
                        hasUpdate = isVersionNewer(
                            current = BuildConfig.VERSION_NAME,
                            latest = release.tagName.removePrefix("v")
                        )
                    )
                )
            }
        }.getOrElse { throwable ->
            UpdateCheckResult.Error(buildErrorMessage(throwable))
        }
    }

    fun downloadApk(apkUrl: String, versionName: String, apkFileName: String): Long {
        val outputName = apkFileName.ifBlank { "youshu-$versionName.apk" }
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("有数 $versionName 更新包")
            .setDescription("正在下载 $outputName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "updates/$outputName")
            .setMimeType("application/vnd.android.package-archive")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }

    fun installDownloadedApk(apkFileName: String): Boolean {
        val normalizedName = apkFileName.ifBlank { return false }
        val apkFile = File(context.getExternalFilesDir(null), "updates/$normalizedName")
        if (!apkFile.exists()) return false
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            data = contentUri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(installIntent)
        return true
    }

    private fun buildErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is IOException -> "检查更新失败：网络连接异常"
            else -> "检查更新失败：${throwable.message ?: "未知错误"}"
        }
    }

    private fun isVersionNewer(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val maxSize = maxOf(currentParts.size, latestParts.size)
        for (index in 0 until maxSize) {
            val currentValue = currentParts.getOrElse(index) { 0 }
            val latestValue = latestParts.getOrElse(index) { 0 }
            if (latestValue > currentValue) return true
            if (latestValue < currentValue) return false
        }
        return false
    }
}

sealed interface UpdateCheckResult {
    data class Success(val release: AppReleaseInfo) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

data class AppReleaseInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releasePageUrl: String,
    val apkName: String?,
    val apkUrl: String?,
    val hasUpdate: Boolean
)

@Serializable
private data class GitHubReleaseResponse(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<GitHubReleaseAsset>
)

@Serializable
private data class GitHubReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String
)
