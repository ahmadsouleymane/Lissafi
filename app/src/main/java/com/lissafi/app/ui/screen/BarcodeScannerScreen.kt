package com.lissafi.app.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.LissafiWhite
import com.lissafi.app.ui.theme.Neutral400
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var torchOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var showManualEntry by remember { mutableStateOf(false) }
    var cameraPermissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (!granted) {
            Toast.makeText(context, "La caméra est nécessaire pour scanner les codes-barres.", Toast.LENGTH_LONG).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // ── PERMISSION CAMÉRA NON ACCORDÉE ──
        if (!cameraPermissionGranted) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    tint = LissafiWhite,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "L'app a besoin d'accéder à la caméra pour scanner les codes-barres.",
                    color = LissafiWhite,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = LissafiGreen)
                ) {
                    Text("Autoriser la caméra")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fermer",
                        tint = LissafiWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        } else {
            // ── APERÇU CAMÉRA ──
            AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val executor = Executors.newSingleThreadExecutor()

            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        BarcodeScanning.getClient().process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let { value ->
                                        executor.shutdown()
                                        imageProxy.close()
                                        onBarcodeScanned(value)
                                        return@addOnSuccessListener
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    provider.unbindAll()
                    val bound = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    camera = bound
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(context))
        }

        // ── CADRE DE SCAN (coins) ──
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            ScanFrame()
        }

        // ── BARRE DU HAUT ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Fermer",
                    tint = LissafiWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "Scanner un code-barres",
                color = LissafiWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Icon(
                imageVector = Icons.Filled.QrCodeScanner,
                contentDescription = null,
                tint = LissafiGreen,
                modifier = Modifier.size(26.dp)
            )
        }

        // ── BARRE DU BAS : torche + saisie manuelle ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Place le code-barres dans le cadre",
                color = LissafiWhite,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Torche
                ScannerActionButton(
                    icon = if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    label = if (torchOn) "Lampe" else "Lampe",
                    selected = torchOn,
                    onClick = {
                        torchOn = !torchOn
                        camera?.cameraControl?.enableTorch(torchOn)
                    },
                    modifier = Modifier.weight(1f)
                )
                // Saisie manuelle
                ScannerActionButton(
                    icon = Icons.Filled.Edit,
                    label = "Taper le code",
                    selected = false,
                    onClick = { showManualEntry = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } // fin du else (permission accordée)
}

    // ── SAISIE MANUELLE ──
    if (showManualEntry) {
        ManualBarcodeDialog(
            onDismiss = { showManualEntry = false },
            onBarcode = {
                showManualEntry = false
                onBarcodeScanned(it)
            }
        )
    }
}

// ============================================================
// CADRE DE SCAN — coins verts lumineux
// ============================================================
@Composable
private fun ScanFrame() {
    val corner = 34.dp
    val thickness = 4.dp
    val length = 46.dp
    val color = LissafiGreen

    Box(modifier = Modifier.size(250.dp)) {
        // Coins
        Box(
            modifier = Modifier
                .size(length)
                .align(Alignment.TopStart)
                .border(thickness, color, RoundedCornerShape(topStart = 22.dp))
        )
        Box(
            modifier = Modifier
                .size(length)
                .align(Alignment.TopEnd)
                .border(thickness, color, RoundedCornerShape(topEnd = 22.dp))
        )
        Box(
            modifier = Modifier
                .size(length)
                .align(Alignment.BottomStart)
                .border(thickness, color, RoundedCornerShape(bottomStart = 22.dp))
        )
        Box(
            modifier = Modifier
                .size(length)
                .align(Alignment.BottomEnd)
                .border(thickness, color, RoundedCornerShape(bottomEnd = 22.dp))
        )
    }
}

// ============================================================
// BOUTON D'ACTION DU SCANNER
// ============================================================
@Composable
private fun ScannerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) LissafiGreen.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) LissafiGreen else LissafiWhite,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = if (selected) LissafiGreen else LissafiWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

// ============================================================
// DIALOGUE SAISIE MANUELLE DU CODE
// ============================================================
@Composable
private fun ManualBarcodeDialog(
    onDismiss: () -> Unit,
    onBarcode: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                tint = LissafiGreen,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Column {
                Text(text = "Taper le code-barres", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tu trouveras le code sous le produit.",
                    fontSize = 12.sp,
                    color = Neutral400
                )
            }
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Ex : 6112692011002") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onBarcode(text.trim()) },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LissafiGreen)
            ) {
                Text("AJOUTER AU PANIER", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = Neutral400) }
        }
    )
}
