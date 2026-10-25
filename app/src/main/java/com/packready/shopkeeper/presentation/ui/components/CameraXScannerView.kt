package com.packready.shopkeeper.presentation.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.packready.shopkeeper.data.hardware.BarcodeAnalyzer
import com.packready.shopkeeper.presentation.theme.Amber500
import com.packready.shopkeeper.presentation.theme.Emerald500
import com.packready.shopkeeper.presentation.theme.Roast900
import com.packready.shopkeeper.presentation.theme.Slate900
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun CameraXScannerView(
    onBarcodeDetected: ((String) -> Unit)? = null,
    customAnalyzer: ImageAnalysis.Analyzer? = null,
    onPhotoCaptured: ((Bitmap) -> Unit)? = null,
    showShutterButton: Boolean = false,
    shutterButtonLabel: String = "Capture",
    isFrontCamera: Boolean = false,
    isCameraActive: Boolean = true,
    onToggleLens: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var flashScreen by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableFloatStateOf(1.0f) }

    // Internal lens state when not externally managed
    var internalFrontCamera by remember { mutableStateOf(false) }
    val activeIsFront = if (onToggleLens != null) isFrontCamera else internalFrontCamera

    var lensRotation by remember { mutableFloatStateOf(0f) }
    val animatedLensRotation by animateFloatAsState(
        targetValue = lensRotation,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scanner_lens_rotation"
    )

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraExecutor.shutdown()
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    LaunchedEffect(flashScreen) {
        if (flashScreen) {
            delay(120)
            flashScreen = false
        }
    }

    // Dynamic camera binding effect responding to activeIsFront, customAnalyzer, showShutterButton, isCameraActive
    LaunchedEffect(activeIsFront, customAnalyzer, showShutterButton, isCameraActive) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                if (!isCameraActive) {
                    cameraProvider.unbindAll()
                    camera = null
                    return@addListener
                }

                val cameraSelector = if (activeIsFront) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val targetSelector = if (cameraProvider.hasCamera(cameraSelector)) {
                    cameraSelector
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val analyzerToUse = customAnalyzer ?: BarcodeAnalyzer(
                    debounceWindowMs = 2000L,
                    onBarcodeDetected = onBarcodeDetected ?: {}
                )

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, analyzerToUse)
                    }

                val needsCapture = showShutterButton || onPhotoCaptured != null
                val captureUseCase = if (needsCapture) {
                    ImageCapture.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build().also { imageCapture = it }
                } else null

                cameraProvider.unbindAll()
                camera = if (captureUseCase != null) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        targetSelector,
                        preview,
                        imageAnalysis,
                        captureUseCase
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        targetSelector,
                        preview,
                        imageAnalysis
                    )
                }

                // Re-apply torch state if rear camera
                if (!activeIsFront && isTorchOn) {
                    camera?.cameraControl?.enableTorch(true)
                }
            } catch (e: Exception) {
                android.util.Log.w("CameraXScannerView", "Camera bind fallback", e)
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
                        onPhotoCaptured?.invoke(finalBitmap)
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

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView }
        )

        // Shutter screen flash effect
        if (flashScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.80f))
            )
        }

        // Optional Shutter Button Overlay at Bottom Center
        if (showShutterButton && onPhotoCaptured != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(enabled = !isCapturing) { capturePhoto() },
                shape = RoundedCornerShape(24.dp),
                color = Roast900.copy(alpha = 0.85f),
                border = BorderStroke(1.5.dp, Emerald500),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Emerald500),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Shutter",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = shutterButtonLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        // Overlay controls for Lens Switch & Torch at Bottom End
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera Flip Button (Front / Rear)
            Surface(
                shape = CircleShape,
                color = Roast900.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        lensRotation += 180f
                        if (onToggleLens != null) {
                            onToggleLens()
                        } else {
                            internalFrontCamera = !internalFrontCamera
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Switch Camera Lens",
                        tint = Color.White,
                        modifier = Modifier
                            .size(19.dp)
                            .graphicsLayer(rotationZ = animatedLensRotation)
                    )
                }
            }

            // Torch Toggle
            Surface(
                shape = CircleShape,
                color = Roast900.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, if (isTorchOn) Amber500 else Color.White.copy(alpha = 0.25f)),
                shadowElevation = 6.dp
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
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}
