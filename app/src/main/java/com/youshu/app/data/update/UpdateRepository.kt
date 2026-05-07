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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLatestRelease(): AppReleaseInfo? {
        return runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/gorkys/youshu/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                val release = json.decodeFromString<GitHubReleaseResponse>(body)
                AppReleaseInfo(
                    currentVersion = BuildConfig.VERSION_NAME,
                    latestVersion = release.tagName.removePrefix("v"),
                    releasePageUrl = release.htmlUrl,
                    apkUrl = release.assets.firstOrNull {
                        it.name.endsWith(".apk") && it.name.contains("release")
                    }?.downloadUrl,
                    hasUpdate = isVersionNewer(
                        current = BuildConfig.VERSION_NAME,
                        latest = release.tagName.removePrefix("v")
                    )
                )
            }
        }.getOrNull()
    }

    fun downloadApk(apkUrl: String, versionName: String): Long {
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("有数 ${versionName} 更新包")
            .setDescription("正在下载最新版本")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "updates/youshu-${versionName}.apk")
            .setMimeType("application/vnd.android.package-archive")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }

    fun installDownloadedApk(versionName: String): Boolean {
        val apkFile = File(context.getExternalFilesDir(null), "updates/youshu-${versionName}.apk")
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

data class AppReleaseInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releasePageUrl: String,
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
