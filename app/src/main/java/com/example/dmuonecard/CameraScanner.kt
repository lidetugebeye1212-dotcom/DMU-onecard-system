package com.example.dmuonecard

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScanner(
    enabled: Boolean,
    onBarcodeDetected: (String) -> Unit,
    torchOn: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val latestEnabled by rememberUpdatedState(enabled)
    val latestOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var cameraError by remember { mutableStateOf<String?>(null) }

    // Keeps the bound camera so we can control the phone flashlight
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        cameraError = if (!granted) {
            "Camera permission denied. Allow it in Android Settings."
        } else {
            null
        }
    }

    val cameraExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    val scanner = remember {
        BarcodeScanning.getClient()
    }

    val scanLock = remember { AtomicBoolean(false) }

    // Unlock the scanner whenever 'enabled' becomes true
    LaunchedEffect(enabled) {
        if (enabled) {
            scanLock.set(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            cameraExecutor.shutdown()
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(hasPermission, lifecycleOwner) {
        if (hasPermission) {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                try {
                    val provider = providerFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!latestEnabled || scanLock.get()) {
                            imageProxy.close()
                        } else {
                            val mediaImage = imageProxy.image
                            if (mediaImage == null) {
                                imageProxy.close()
                            } else {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                scanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        val value = barcodes
                                            .firstNotNullOfOrNull { it.rawValue }
                                            ?.trim()

                                        if (!value.isNullOrBlank() && scanLock.compareAndSet(false, true)) {
                                            ContextCompat.getMainExecutor(context).execute {
                                                latestOnBarcodeDetected(value)
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            }
                        }
                    }

                    provider.unbindAll()
                    boundCamera = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )

                    cameraError = null
                } catch (e: Exception) {
                    cameraError = "Cannot start camera: ${e.message}"
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    // Phone flashlight on/off — scanning continues while the light is on
    LaunchedEffect(boundCamera, torchOn) {
        boundCamera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                cam.cameraControl.enableTorch(torchOn)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasPermission) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera permission is required.")
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    ) {
                        Text("ALLOW CAMERA")
                    }
                }
            }
        } else {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        }

        cameraError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}