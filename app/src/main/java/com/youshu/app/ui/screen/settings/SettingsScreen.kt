package com.youshu.app.ui.screen.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.youshu.app.ui.components.AppDecorativeBackground
import com.youshu.app.ui.components.AppDialog
import com.youshu.app.ui.components.AppSurfaceCard
import com.youshu.app.ui.components.GradientButton
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var showMessageDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let(viewModel::exportBackup)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(viewModel::importBackup)
    }

    LaunchedEffect(state.message) {
        showMessageDialog = state.message != null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppDecorativeBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "设置",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            AppSurfaceCard(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 12.dp
            ) {
                Text(
                    text = "数据备份",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "导出时会打包分类、位置、物品和已保存图片；导入会覆盖当前本地数据。",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.size(16.dp))
                SettingsActionRow(
                    icon = Icons.Default.FileDownload,
                    title = "导出数据",
                    subtitle = "生成 ZIP 备份包",
                    onClick = {
                        exportLauncher.launch("youshu-backup-${System.currentTimeMillis()}.zip")
                    }
                )
                Spacer(modifier = Modifier.size(10.dp))
                SettingsActionRow(
                    icon = Icons.Default.FileUpload,
                    title = "导入数据",
                    subtitle = "从 ZIP 备份恢复",
                    onClick = {
                        importLauncher.launch(arrayOf("application/zip", "*/*"))
                    }
                )
            }

            AppSurfaceCard(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 12.dp
            ) {
                Text(
                    text = "检查更新",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "根据 GitHub Release 的最新 tag 检查版本状态，并优先下载可直接安装的 debug APK。",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.size(16.dp))
                SettingsActionRow(
                    icon = Icons.Default.SystemUpdate,
                    title = "检查新版本",
                    subtitle = state.latestRelease?.let {
                        if (it.hasUpdate) "发现 ${it.latestVersion}" else "当前已经是最新版本"
                    } ?: "点击后拉取远程版本信息",
                    onClick = viewModel::checkForUpdates
                )

                state.latestRelease?.takeIf { it.hasUpdate }?.let { release ->
                    Spacer(modifier = Modifier.size(14.dp))
                    GradientButton(
                        text = "下载 ${release.latestVersion}",
                        onClick = { viewModel.downloadLatestApk() }
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    SettingsActionRow(
                        icon = Icons.Default.CloudDownload,
                        title = "安装已下载更新",
                        subtitle = release.apkName?.let { "当前目标：$it" } ?: "下载完成后可直接调起安装",
                        onClick = { viewModel.installDownloadedApk() }
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = release.releasePageUrl,
                        fontSize = 12.sp,
                        color = OrangeStart,
                        modifier = Modifier.clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, release.releasePageUrl.toUri())
                            )
                        }
                    )
                }
            }

            if (state.isBusy) {
                Text(
                    text = "正在处理，请稍候…",
                    fontSize = 12.sp,
                    color = TextHint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showMessageDialog && state.message != null) {
        AppDialog(
            title = "提示",
            onDismissRequest = {
                showMessageDialog = false
                viewModel.consumeMessage()
            },
            confirmText = "知道了",
            onConfirm = {
                showMessageDialog = false
                viewModel.consumeMessage()
            }
        ) {
            Text(
                text = state.message.orEmpty(),
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF8F6FC))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(OrangeStart.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OrangeStart
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextHint
            )
        }
    }
}
