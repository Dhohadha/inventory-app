package com.packready.shopkeeper.presentation.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.packready.shopkeeper.presentation.theme.Amber500
import com.packready.shopkeeper.presentation.theme.Caramel400
import com.packready.shopkeeper.presentation.theme.Caramel500
import com.packready.shopkeeper.presentation.theme.Emerald500
import com.packready.shopkeeper.presentation.theme.Roast900
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

enum class CameraCaptureMode {
    SINGLE_PHOTO, // For Bill Slip OCR, Invoice, or Payment Proof
    MULTI_PHOTO   // For Product Packaging Enrollment (Front, Back, Details)
}

@Composable
fun InAppCameraCaptureDialog(
    mode: CameraCaptureMode = CameraCaptureMode.SINGLE_PHOTO,
    title: String = if (mode == CameraCaptureMode.MULTI_PHOTO) "Capture Packaging Photos" else "Document & Bill Camera",
    instructionText: String? = null,
    onPhotosCaptured: (List<Bitmap>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val capturedBitmaps = remember { mutableStateListOf<Bitmap>() }
    var singleReviewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isFrontLens by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var flashScreen by remember { mutableStateOf(false) }

    // Lens flip rotation animation
    var flipRotation by remember { mutableFloatStateOf(0f) }
    val animatedFlipRotation by animateFloatAsState(
        targetValue = flipRotation,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
        label = "inapp_lens_rotation"
    )

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraExecutor.shutdown()
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
        }
    }

    // Shutter screen flash effect
    LaunchedEffect(flashScreen) {
        if (flashScreen) {
            delay(120)
            flashScreen = false
        }
    }

    // Reactive camera re-binding when lens or permission changes
    LaunchedEffect(isFrontLens, hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val desiredSelector = if (isFrontLens) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val targetSelector = if (cameraProvider.hasCamera(desiredSelector)) {
                    desiredSelector
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val captureUseCase = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = captureUseCase

                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    targetSelector,
                    preview,
                    captureUseCase
                )

                // Re-apply torch state if rear camera
                if (!isFrontLens && isTorchOn) {
                    camera?.cameraControl?.enableTorch(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun capturePhoto() {
        val capture = imageCapture ?: return
        if (isCapturing) return
        isCapturing = true
        flashScreen = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    try {
                        val rawBitmap = imageProxy.toBitmap()
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val finalBitmap = if (rotationDegrees != 0) {
                            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                        } else {
                            rawBitmap
                        }

                        if (mode == CameraCaptureMode.SINGLE_PHOTO) {
                            singleReviewBitmap = finalBitmap
                        } else {
                            capturedBitmaps.add(finalBitmap)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        imageProxy.close()
                        isCapturing = false
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    isCapturing = false
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (!hasCameraPermission) {
                // Permission Request Fallback View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PackReady needs access to your camera to snap photos of products and customer bill slips.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Camera Permission", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }
            } else if (mode == CameraCaptureMode.SINGLE_PHOTO && singleReviewBitmap != null) {
                // Review screen for SINGLE_PHOTO mode
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = singleReviewBitmap!!.asImageBitmap(),
                        contentDescription = "Captured Photo Review",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Top Bar
                    Surface(
                        color = Roast900.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Review Captured Photo",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }

                    // Bottom Action Controls
                    Surface(
                        color = Roast900.copy(alpha = 0.90f),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { singleReviewBitmap = null },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retake", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retake Photo", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    singleReviewBitmap?.let { bmp ->
                                        onPhotosCaptured(listOf(bmp))
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Use Photo", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Use Photo", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Live Camera View
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { previewView }
                )

                // Flash Shutter Effect Overlay
                if (flashScreen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }

                // Center Viewfinder Framing Box for Catalog Packaging
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .border(
                                BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)),
                                RoundedCornerShape(22.dp)
                            )
                    ) {
                        // Subtle center guide hint
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Roast900.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = if (mode == CameraCaptureMode.MULTI_PHOTO) "Fit item inside packaging frame" else "Align document inside frame",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }

                // Studio Top Floating Frosted Bar
                Surface(
                    color = Roast900.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Header Main Row: Title & Action Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Emerald500.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = Emerald500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isFrontLens) "Front Camera Active" else "Studio Rear Camera",
                                        color = Caramel400,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Torch Toggle
                                Surface(
                                    shape = CircleShape,
                                    color = if (isTorchOn) Amber500.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, if (isTorchOn) Amber500 else Color.White.copy(alpha = 0.2f))
                                ) {
                                    IconButton(
                                        onClick = {
                                            isTorchOn = !isTorchOn
                                            camera?.cameraControl?.enableTorch(isTorchOn)
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                            contentDescription = "Torch",
                                            tint = if (isTorchOn) Amber500 else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Lens Switch (Back <-> Front) with smooth rotation
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            flipRotation += 180f
                                            isFrontLens = !isFrontLens
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FlipCameraAndroid,
                                            contentDescription = "Switch Camera Lens",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .graphicsLayer(rotationZ = animatedFlipRotation)
                                        )
                                    }
                                }

                                // Close Button
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Guidance Step Pills for MULTI_PHOTO Mode
                        if (mode == CameraCaptureMode.MULTI_PHOTO) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val stepCount = capturedBitmaps.size
                                val steps = listOf(
                                    "1. Front Face" to (stepCount == 0),
                                    "2. Back & MRP" to (stepCount == 1),
                                    "3. Angle / Detail" to (stepCount >= 2)
                                )

                                steps.forEachIndexed { idx, (label, isCurrent) ->
                                    val isCompleted = stepCount > idx
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = when {
                                            isCurrent -> Emerald500.copy(alpha = 0.28f)
                                            isCompleted -> Emerald500.copy(alpha = 0.15f)
                                            else -> Color.White.copy(alpha = 0.08f)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            when {
                                                isCurrent -> Emerald500
                                                isCompleted -> Emerald500.copy(alpha = 0.5f)
                                                else -> Color.White.copy(alpha = 0.12f)
                                            }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isCompleted) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Emerald500,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                            }
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isCurrent || isCompleted) Color.White else Color.LightGray,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Studio Bottom Floating Controls Container
                Surface(
                    color = Roast900.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // MULTI_PHOTO Filmstrip Reel of Snapped Photos
                        if (mode == CameraCaptureMode.MULTI_PHOTO && capturedBitmaps.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(capturedBitmaps) { index, bitmap ->
                                    val tag = when (index) {
                                        0 -> "FRONT"
                                        1 -> "BACK"
                                        else -> "DETAIL ${index + 1}"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.5.dp, Emerald500, RoundedCornerShape(12.dp))
                                    ) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = tag,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Tag Label
                                        Surface(
                                            color = Roast900.copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter)
                                        ) {
                                            Text(
                                                text = tag,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }

                                        // Delete Button
                                        IconButton(
                                            onClick = { capturedBitmaps.removeAt(index) },
                                            modifier = Modifier
                                                .size(20.dp)
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Shutter & Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Photo Counter Badge Pill
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (mode == CameraCaptureMode.MULTI_PHOTO) {
                                    Surface(
                                        color = if (capturedBitmaps.isNotEmpty()) Emerald500.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, if (capturedBitmaps.isNotEmpty()) Emerald500.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = null,
                                                tint = if (capturedBitmaps.isNotEmpty()) Emerald500 else Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "${capturedBitmaps.size} Snapped",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Center: Pro Shutter Button with Dual Ring
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Emerald500.copy(alpha = 0.45f),
                                                Caramel400.copy(alpha = 0.20f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .clickable(enabled = !isCapturing) { capturePhoto() }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(2.5.dp, Emerald500, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Click Photo",
                                        tint = Roast900,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // Right: Done / Confirm Button with Animated Visibility
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (mode == CameraCaptureMode.MULTI_PHOTO && capturedBitmaps.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            onPhotosCaptured(capturedBitmaps.toList())
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                        shape = RoundedCornerShape(14.dp),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Done (${capturedBitmaps.size})",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
