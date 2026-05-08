package com.youshu.app.ui.screen.camera

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.youshu.app.ui.theme.OrangeEnd
import com.youshu.app.ui.theme.OrangeStart
import java.io.File
import java.util.concurrent.TimeUnit

private enum class CameraTab(val label: String) {
    GALLERY("图库"),
    CAMERA("拍摄")
}

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onDisposed: () -> Unit = {},
    onSkipPhoto: () -> Unit = {},
    onPhotoTaken: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }
    var focusMarker by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var focusMarkerStamp by remember { mutableLongStateOf(0L) }
    var selectedTab by remember { mutableStateOf(CameraTab.CAMERA) }
    val capturedUris = remember { mutableStateListOf<Uri>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "需要相机权限才能拍照录入", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            capturedUris.clear()
            capturedUris.addAll(uris)
            selectedTab = CameraTab.GALLERY
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(flashEnabled, imageCapture) {
        imageCapture?.flashMode = if (flashEnabled) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
    }

    LaunchedEffect(focusMarkerStamp) {
        if (focusMarkerStamp == 0L) return@LaunchedEffect
        kotlinx.coroutines.delay(900)
        focusMarker = null
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            onDisposed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (selectedTab == CameraTab.CAMERA && hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { createdPreviewView ->
                        previewView = createdPreviewView
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val provider = cameraProviderFuture.get()
                            cameraProvider = provider
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = createdPreviewView.surfaceProvider
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            try {
                                provider.unbindAll()
                                camera = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (_: Exception) {
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(camera, previewView) {
                        detectTapGestures { offset ->
                            val view = previewView ?: return@detectTapGestures
                            val cameraInstance = camera ?: return@detectTapGestures
                            val meteringPoint = view.meteringPointFactory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(
                                meteringPoint,
                                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                            )
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                            cameraInstance.cameraControl.startFocusAndMetering(action)
                            focusMarker = offset.x to offset.y
                            focusMarkerStamp = System.currentTimeMillis()
                        }
                    }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF181818))
            ) {
                if (capturedUris.isEmpty()) {
                    EmptyGalleryState(
                        modifier = Modifier.align(Alignment.Center),
                        onPickGallery = { galleryLauncher.launch("image/*") }
                    )
                } else {
                    AsyncImage(
                        model = capturedUris.last(),
                        contentDescription = "图库预览",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.14f))
                    )
                }
            }
        }

        focusMarker?.takeIf { selectedTab == CameraTab.CAMERA }?.let { (x, y) ->
            Box(
                modifier = Modifier
                    .padding(
                        start = with(density) { x.toDp() - 36.dp },
                        top = with(density) { y.toDp() - 36.dp }
                    )
                    .size(72.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(onClick = onBack)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }

            Text(
                text = if (capturedUris.isEmpty()) "拍封面图" else "继续补拍细节图",
                color = Color.White,
                fontSize = 18.sp
            )

            if (selectedTab == CameraTab.CAMERA) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable { flashEnabled = !flashEnabled }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "闪光灯",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if (selectedTab == CameraTab.CAMERA) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(2.dp, Color.White.copy(alpha = 0.78f), CircleShape)
                            .clickable {
                                takePhoto(context, imageCapture) { uri ->
                                    capturedUris.add(uri)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(OrangeStart, OrangeEnd)
                                    )
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (capturedUris.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 2.dp)
                    ) {
                        itemsIndexed(capturedUris, key = { index, uri -> "$index-$uri" }) { index, uri ->
                            Box(modifier = Modifier.size(72.dp)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "缩略图 ${index + 1}",
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.42f))
                                        .clickable { capturedUris.removeAt(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "删除缩略图",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFFFC44A))
                            .clickable {
                                onPhotoTaken(capturedUris.toList())
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "下一步",
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CameraTab.values().forEach { tab ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            selectedTab = tab
                            if (tab == CameraTab.GALLERY && capturedUris.isEmpty()) {
                                galleryLauncher.launch("image/*")
                            }
                        }
                    ) {
                        Text(
                            text = tab.label,
                            color = if (selectedTab == tab) Color.White else Color.White.copy(alpha = 0.58f),
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 3.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (selectedTab == tab) Color(0xFFFFC44A) else Color.Transparent
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGalleryState(
    modifier: Modifier = Modifier,
    onPickGallery: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "还没有选择图片",
            color = Color.White,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .clickable(onClick = onPickGallery)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = "打开图库",
                color = Color.White,
                fontSize = 13.sp
            )
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onResult: (Uri) -> Unit
) {
    val capture = imageCapture ?: return
    val imagesDir = File(context.filesDir, "images")
    if (!imagesDir.exists()) imagesDir.mkdirs()

    val file = File(imagesDir, "capture_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    capture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onResult(Uri.fromFile(file))
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "拍照失败：${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
