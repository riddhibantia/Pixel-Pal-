@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)

package com.pixelpal.app.presentation.screens.agent

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.camera.core.ExperimentalGetImage
import com.pixelpal.app.data.remote.GenericHttpAgentConnector
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.LoadingState
import com.pixelpal.app.presentation.theme.Spacing

/**
 * QR agent pairing: point at a QR code holding the agent endpoint URL
 * (e.g. shown on the laptop dashboard). The decoded URL returns via the
 * previous entry's savedStateHandle ("scanned_url") and the screen pops.
 * Only http(s)/ws(s) URLs that pass [GenericHttpAgentConnector.isAllowedEndpoint]
 * are accepted — anything else is ignored so random QRs can't hijack pairing.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScanScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) navController.popBackStack()
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Scan agent QR", onBack = { navController.popBackStack() })
        if (!hasPermission) {
            LoadingState()
            return@Column
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            var consumed by remember { mutableStateOf(false) }
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val scanner = BarcodeScanning.getClient()
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener(
                        {
                            val provider = providerFuture.get()
                            val preview = androidx.camera.core.Preview.Builder().build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                if (consumed) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                val mediaImage = imageProxy.image
                                if (mediaImage == null) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                val input = InputImage.fromMediaImage(
                                    mediaImage, imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(input)
                                    .addOnSuccessListener { barcodes ->
                                        val url = barcodes.firstNotNullOfOrNull { it.rawValue }
                                            ?.trim()
                                            ?.takeIf { isPairableUrl(it) }
                                        if (url != null && !consumed) {
                                            consumed = true
                                            navController.previousBackStackEntry
                                                ?.savedStateHandle
                                                ?.set(SCANNED_URL_KEY, url)
                                            navController.popBackStack()
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            }
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        },
                        ContextCompat.getMainExecutor(ctx)
                    )
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = "Point at the QR code on your agent dashboard",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Spacing.lg)
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
        }
        // Square alignment guide, centered: keep the QR inside the corners.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ScanFrame(modifier = Modifier.size(260.dp))
        }
    }
}

/** Corner-bracket square guide for lining up the QR code. */
@Composable
private fun ScanFrame(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val arm = size.minDimension * 0.22f
        val stroke = 8f
        val corners = listOf(
            // top-left
            Triple(Offset(0f, arm), Offset(0f, 0f), Offset(arm, 0f)),
            // top-right
            Triple(Offset(w - arm, 0f), Offset(w, 0f), Offset(w, arm)),
            // bottom-right
            Triple(Offset(w, h - arm), Offset(w, h), Offset(w - arm, h)),
            // bottom-left
            Triple(Offset(arm, h), Offset(0f, h), Offset(0f, h - arm))
        )
        corners.forEach { (a, corner, b) ->
            drawLine(Color.White.copy(alpha = 0.35f), a, corner, strokeWidth = stroke + 4f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.35f), corner, b, strokeWidth = stroke + 4f, cap = StrokeCap.Round)
            drawLine(accent, a, corner, strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(accent, corner, b, strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

private fun isPairableUrl(url: String): Boolean {
    val lower = url.lowercase()
    val looksLikeUrl = lower.startsWith("http://") || lower.startsWith("https://") ||
        lower.startsWith("ws://") || lower.startsWith("wss://")
    return looksLikeUrl && GenericHttpAgentConnector.isAllowedEndpoint(url)
}

const val SCANNED_URL_KEY = "scanned_url"
