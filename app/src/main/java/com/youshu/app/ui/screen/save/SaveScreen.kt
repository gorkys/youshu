package com.youshu.app.ui.screen.save

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.youshu.app.util.DateUtil
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.youshu.app.ui.components.GradientButton
import com.youshu.app.ui.theme.OrangeEnd
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.SaveViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SaveScreen(
    imageUri: Uri,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SaveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val locations by viewModel.locations.collectAsState()

    var showAdvanced by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        viewModel.initFromPhoto(context, imageUri, null, null)
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onSaved()
            viewModel.reset()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred background image
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(20.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
        )

        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFE0E0E0))
                        )
                    }

                    // Image preview
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "预览",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Name field with AI tag
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "名称",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(OrangeStart.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "AI识别",
                                fontSize = 10.sp,
                                color = OrangeStart
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = state.name,
                        onValueChange = { viewModel.updateName(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFFF6F7FB),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F1F1F)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category chips
                    Text(
                        text = "分类",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = state.categoryId == category.id,
                                onClick = {
                                    viewModel.updateCategory(
                                        if (state.categoryId == category.id) null else category.id
                                    )
                                },
                                label = { Text(category.name, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OrangeStart.copy(alpha = 0.15f),
                                    selectedLabelColor = OrangeStart
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location
                    Text(
                        text = "位置",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        locations.filter { it.parentId != null }.forEach { location ->
                            FilterChip(
                                selected = state.locationId == location.id,
                                onClick = {
                                    viewModel.updateLocation(
                                        if (state.locationId == location.id) null else location.id
                                    )
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(location.name, fontSize = 13.sp)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OrangeStart.copy(alpha = 0.15f),
                                    selectedLabelColor = OrangeStart
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Advanced section toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvanced = !showAdvanced }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "更多信息",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Icon(
                            imageVector = if (showAdvanced) Icons.Default.ExpandLess
                            else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }

                    // Advanced fields
                    AnimatedVisibility(
                        visible = showAdvanced,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            // Quantity
                            Text(
                                text = "数量",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF6F7FB))
                                        .clickable {
                                            viewModel.updateQuantity(state.quantity - 1)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "${state.quantity} ${state.unit}",
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF6F7FB))
                                        .clickable {
                                            viewModel.updateQuantity(state.quantity + 1)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Expiry date
                            Text(
                                text = "有效期",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val datePickerState = rememberDatePickerState(
                                initialSelectedDateMillis = state.expireTime
                            )
                            var showDatePicker by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF6F7FB), RoundedCornerShape(12.dp))
                                    .clickable { showDatePicker = true }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (state.expireTime != null)
                                            DateUtil.formatDate(state.expireTime!!)
                                        else "点击选择日期",
                                        fontSize = 15.sp,
                                        color = if (state.expireTime != null) Color(0xFF1F1F1F) else Color(0xFFBFBFBF)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = TextSecondary
                                    )
                                }
                            }

                            if (showDatePicker) {
                                DatePickerDialog(
                                    onDismissRequest = { showDatePicker = false },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            datePickerState.selectedDateMillis?.let {
                                                viewModel.updateExpireTime(it)
                                            }
                                            showDatePicker = false
                                        }) {
                                            Text("确定")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDatePicker = false }) {
                                            Text("取消")
                                        }
                                    }
                                ) {
                                    DatePicker(state = datePickerState)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Price
                            Text(
                                text = "价格",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            BasicTextField(
                                value = state.price,
                                onValueChange = { viewModel.updatePrice(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color(0xFFF6F7FB),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(14.dp),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 15.sp,
                                    color = Color(0xFF1F1F1F)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Note
                            Text(
                                text = "备注",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            BasicTextField(
                                value = state.note,
                                onValueChange = { viewModel.updateNote(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .background(
                                        Color(0xFFF6F7FB),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(14.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 15.sp,
                                    color = Color(0xFF1F1F1F)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Save button
                    GradientButton(
                        text = if (state.isSaving) "保存中…" else "保存",
                        onClick = { viewModel.save() },
                        enabled = state.name.isNotBlank() && !state.isSaving
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
