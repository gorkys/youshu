package com.youshu.app.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.youshu.app.ui.components.AppDecorativeBackground
import com.youshu.app.ui.components.AppDialog
import com.youshu.app.ui.components.AppSurfaceCard
import com.youshu.app.ui.theme.OrangeEnd
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.StatusExpired
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.ProfileViewModel
import com.youshu.app.util.DateUtil

private data class AiModelItem(
    val alias: String,
    val provider: String,
    val endpoint: String,
    val apiKey: String
)

@Composable
fun ProfileScreen(
    onOpenExpiry: () -> Unit,
    onOpenLibrary: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val totalCount by viewModel.totalCount.collectAsState()
    val expiringCount by viewModel.expiringCount.collectAsState()
    val totalValue by viewModel.totalValue.collectAsState()

    val models = remember {
        mutableStateListOf(
            AiModelItem("默认云端模型", "OpenAI Compatible", "https://api.example.com/v1", "sk-demo-default"),
            AiModelItem("本地模型", "Ollama", "http://127.0.0.1:11434", "")
        )
    }
    var showModelDialog by remember { mutableStateOf(false) }
    var showAddModelDialog by remember { mutableStateOf(false) }
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogMessage by remember { mutableStateOf<String?>(null) }
    var newAlias by remember { mutableStateOf("") }
    var newProvider by remember { mutableStateOf("") }
    var newEndpoint by remember { mutableStateOf("") }
    var newApiKey by remember { mutableStateOf("") }

    Box {
        AppDecorativeBackground()

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 88.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(OrangeStart, OrangeEnd)
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .align(Alignment.TopEnd)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.24f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "有",
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Column {
                            Text(
                                text = "有数用户",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "记录生活中的每一件物品",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            AppSurfaceCard(
                modifier = Modifier.padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 12.dp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactStat(label = "物品总数", value = totalCount.toString())
                    CompactStat(label = "即将过期", value = expiringCount.toString(), color = StatusExpired)
                    CompactStat(label = "物品价值", value = DateUtil.formatCurrency(totalValue))
                }
            }

            AppSurfaceCard(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 12.dp
            ) {
                MenuRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI 模型管理",
                    subtitle = "添加、查看和预置模型连接",
                    onClick = { showModelDialog = true }
                )
                DividerSpacer()
                MenuRow(
                    icon = Icons.Default.Notifications,
                    title = "到期提醒",
                    subtitle = "查看即将到期的物品与提醒状态",
                    onClick = onOpenExpiry
                )
                DividerSpacer()
                MenuRow(
                    icon = Icons.Default.History,
                    title = "回收站",
                    subtitle = "当前版本预留恢复入口",
                    onClick = {
                        infoDialogTitle = "回收站"
                        infoDialogMessage = "回收站入口已预留，后续会接入已删除和已丢弃物品的恢复能力。"
                    }
                )
                DividerSpacer()
                MenuRow(
                    icon = Icons.Default.Settings,
                    title = "设置",
                    subtitle = "通知、相机与展示偏好",
                    onClick = {
                        infoDialogTitle = "设置"
                        infoDialogMessage = "当前预置了通知、相机与 UI 展示位，后续可继续扩展为完整设置页。"
                    }
                )
                DividerSpacer()
                MenuRow(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "帮助与反馈",
                    subtitle = "常见问题与功能建议",
                    onClick = {
                        infoDialogTitle = "帮助与反馈"
                        infoDialogMessage = "你可以通过这个预置入口集中管理使用说明、FAQ 和反馈渠道。"
                    }
                )
                DividerSpacer()
                MenuRow(
                    icon = Icons.Default.Info,
                    title = "关于我们",
                    subtitle = "版本 1.0.0",
                    onClick = {
                        infoDialogTitle = "关于我们"
                        infoDialogMessage = "有数：拍一下、保存一下，就能把家里的物品管理起来。"
                    }
                )
            }

            Text(
                text = "心中有数，遇事不怵",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showModelDialog) {
        AppDialog(
            title = "AI 模型管理",
            subtitle = "预制入口已打通，可先维护模型别名、接口和 API Key。",
            onDismissRequest = { showModelDialog = false },
            confirmText = "新增模型",
            secondaryText = "进入库房",
            onSecondary = onOpenLibrary,
            onConfirm = {
                newAlias = ""
                newProvider = ""
                newEndpoint = ""
                newApiKey = ""
                showAddModelDialog = true
            }
        ) {
            if (models.isEmpty()) {
                Text(
                    text = "还没有添加模型。",
                    fontSize = 14.sp,
                    color = TextHint
                )
            } else {
                models.forEachIndexed { index, model ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.alias,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${model.provider} · ${model.endpoint}",
                                fontSize = 12.sp,
                                color = TextHint
                            )
                            Text(
                                text = if (model.apiKey.isBlank()) "未配置 API Key" else "API Key: ${model.apiKey.take(4)}••••${model.apiKey.takeLast(2)}",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(StatusExpired.copy(alpha = 0.12f))
                                .clickable { models.removeAt(index) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "移除",
                                color = StatusExpired,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddModelDialog) {
        AppDialog(
            title = "新增 AI 模型",
            subtitle = "先把必要连接信息录入，后续再接真实调用。",
            onDismissRequest = { showAddModelDialog = false },
            confirmText = "保存",
            confirmEnabled = newAlias.isNotBlank() && newProvider.isNotBlank() && newEndpoint.isNotBlank(),
            onConfirm = {
                models.add(
                    AiModelItem(
                        alias = newAlias.trim(),
                        provider = newProvider.trim(),
                        endpoint = newEndpoint.trim(),
                        apiKey = newApiKey.trim()
                    )
                )
                showAddModelDialog = false
            }
        ) {
            OutlinedTextField(
                value = newAlias,
                onValueChange = { newAlias = it },
                singleLine = true,
                label = { Text("模型别名") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newProvider,
                onValueChange = { newProvider = it },
                singleLine = true,
                label = { Text("模型来源") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newEndpoint,
                onValueChange = { newEndpoint = it },
                singleLine = true,
                label = { Text("接口地址") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newApiKey,
                onValueChange = { newApiKey = it },
                singleLine = true,
                label = { Text("API Key") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = TextHint
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (infoDialogTitle != null && infoDialogMessage != null) {
        AppDialog(
            title = infoDialogTitle!!,
            onDismissRequest = {
                infoDialogTitle = null
                infoDialogMessage = null
            },
            confirmText = "知道了",
            onConfirm = {
                infoDialogTitle = null
                infoDialogMessage = null
            }
        ) {
            Text(
                text = infoDialogMessage!!,
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun CompactStat(
    label: String,
    value: String,
    color: Color = OrangeStart
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(OrangeStart.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OrangeStart,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
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
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextHint
        )
    }
}

@Composable
private fun DividerSpacer() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .height(1.dp)
            .background(Color(0xFFF1EEF7))
    )
}
