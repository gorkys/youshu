package com.youshu.app.ui.screen.camera

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onDisposed: () -> Unit = {},
    onPhotoTaken: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "需要相机权限才能拍照录入", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let(onPhotoTaken)
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
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { flashEnabled = !flashEnabled }) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "闪光灯",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.size(48.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "相册",
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .clickable {
                            takePhoto(context, imageCapture) { uri ->
                                onPhotoTaken(uri)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "多拍",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "连续录入更高效",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 10.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "需要相机权限",
                    color = Color.White,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "请在系统设置中开启权限后再继续使用。",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 13.sp
                )
            }
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
