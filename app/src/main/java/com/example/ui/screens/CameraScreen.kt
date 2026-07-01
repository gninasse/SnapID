package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.models.DocumentType
import com.example.ui.components.IdPhotoOverlay
import com.example.ui.theme.*
import com.example.viewmodels.AppScreen
import com.example.viewmodels.IDPhotoViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: IDPhotoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        CameraContent(viewModel = viewModel, modifier = modifier)
    } else {
        CameraPermissionDeniedView(
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
            onBackClick = { viewModel.navigateTo(AppScreen.DocSelection) },
            onDemoClick = {
                // Instantly load demo bitmap
                loadDemoPhoto(context, viewModel)
            }
        )
    }
}

@Composable
fun CameraContent(
    viewModel: IDPhotoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val selectedDocType by viewModel.selectedDocType.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var countdownRemaining by remember { mutableStateOf(-1) }
    val coroutineScope = rememberCoroutineScope()

    // Instruction dynamic tips
    val instructions = remember {
        listOf(
            "Alignez le visage dans l'ovale en pointillé",
            "Tenez l'appareil à hauteur de yeux",
            "Fond clair et uni obligatoire",
            "Expression neutre (pas de sourire)"
        )
    }
    var currentTipIndex by remember { mutableStateOf(0) }
    
    // Cycle tips every 4 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            currentTipIndex = (currentTipIndex + 1) % instructions.size
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX view or Simulated Camera fallback
        var isCameraXBound by remember { mutableStateOf(false) }

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderInstance = cameraProvider
                    
                    val preview = CameraPreview.Builder().build()
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(
                            if (isFrontCamera) CameraSelector.LENS_FACING_FRONT 
                            else CameraSelector.LENS_FACING_BACK
                        )
                        .build()

                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        isCameraXBound = true
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Camera binding failed", e)
                        isCameraXBound = false
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                // Update bind on camera flip (front/back)
                cameraProviderInstance?.let { provider ->
                    val preview = CameraPreview.Builder().build()
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(
                            if (isFrontCamera) CameraSelector.LENS_FACING_FRONT 
                            else CameraSelector.LENS_FACING_BACK
                        )
                        .build()

                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        isCameraXBound = true
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Update camera binding failed", e)
                        isCameraXBound = false
                    }
                }
            }
        )

        // Overlay with guidelines
        IdPhotoOverlay(documentType = selectedDocType)

        // Header controls (Back, Flip, Flash, Demo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.DocSelection) },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }

            // Demo Shortcut
            Button(
                onClick = { loadDemoPhoto(context, viewModel) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Démo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Flip Camera
            IconButton(
                onClick = { viewModel.toggleCamera() },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Retourner la caméra",
                    tint = Color.White
                )
            }
        }

        // Instructions banner at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp)
                .padding(horizontal = 24.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                .border(1.dp, ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentTipIndex,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "tip_animation"
            ) { index ->
                Text(
                    text = instructions[index],
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bottom control dashboard (Timer controls and circular shutter button)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timer selection row
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val timers = listOf(0 to "Sans", 3 to "3s", 10 to "10s")
                timers.forEach { (sec, label) ->
                    val isSelected = timerSeconds == sec
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) ElectricBlue else Color.Transparent)
                            .clickable { viewModel.setTimer(sec) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shutter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Circular Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .background(Color.White, CircleShape)
                        .clip(CircleShape)
                        .clickable(enabled = !isLoading && countdownRemaining == -1) {
                            if (timerSeconds > 0) {
                                coroutineScope.launch {
                                    countdownRemaining = timerSeconds
                                    while (countdownRemaining > 0) {
                                        delay(1000)
                                        countdownRemaining--
                                    }
                                    captureAndProcess(context, imageCapture, executor, viewModel)
                                    countdownRemaining = -1
                                }
                            } else {
                                captureAndProcess(context, imageCapture, executor, viewModel)
                            }
                        }
                        .testTag("shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = ElectricBlue, modifier = Modifier.size(30.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ElectricBlue, CircleShape)
                        )
                    }
                }
            }
        }

        // Timer Countdown Overlay
        AnimatedVisibility(
            visible = countdownRemaining > 0,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                    .border(3.dp, TimerYellow, CircleShape), // Yellow timer circle
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdownRemaining.toString(),
                    color = TimerYellow,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

private fun captureAndProcess(
    context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    viewModel: IDPhotoViewModel
) {
    val photoFile = File(context.cacheDir, "captured_temp_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    viewModel.showStatus("Prise de vue...")

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                if (bitmap != null) {
                    // Correct potential camera front mirroring or orientation issues
                    val matrix = Matrix()
                    if (viewModel.isFrontCamera.value) {
                        // Mirror horizontally for natural selfie view
                        matrix.postScale(-1f, 1f)
                    }
                    val correctedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )
                    
                    viewModel.setCapturedBitmap(correctedBitmap)
                } else {
                    viewModel.showStatus("Erreur : Impossible de lire la photo")
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed", exception)
                // Fallback to loading demo photo so the user can test on emulators
                loadDemoPhoto(context, viewModel)
            }
        }
    )
}

/**
 * Fallback helper to load a mock portrait photo when physical camera isn't accessible
 */
private fun loadDemoPhoto(context: Context, viewModel: IDPhotoViewModel) {
    viewModel.showStatus("Chargement de la photo de démo...")
    
    // Draw an elegant simulated portrait face onto a bitmap
    val width = 1200
    val height = 1600
    val demoBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(demoBitmap)
    
    // Solid uniform light grey/blue background (compliant with EU standards)
    canvas.drawColor(android.graphics.Color.parseColor("#E0E6ED"))
    
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
    }
    
    // Draw body/shoulders
    paint.color = android.graphics.Color.parseColor("#3B82F6") // Blue suit/tshirt
    val bodyPath = android.graphics.Path().apply {
        moveTo(width * 0.15f, height.toFloat())
        cubicTo(
            width * 0.2f, height * 0.75f,
            width * 0.8f, height * 0.75f,
            width * 0.85f, height.toFloat()
        )
        close()
    }
    canvas.drawPath(bodyPath, paint)
    
    // Draw neck
    paint.color = android.graphics.Color.parseColor("#FDBA74") // Neck/Skin tone
    canvas.drawRect(width * 0.44f, height * 0.62f, width * 0.56f, height * 0.75f, paint)
    
    // Draw face oval
    paint.color = android.graphics.Color.parseColor("#FDBA74") // Skin tone
    canvas.drawOval(
        width * 0.32f, height * 0.32f,
        width * 0.68f, height * 0.66f,
        paint
    )
    
    // Hair
    paint.color = android.graphics.Color.parseColor("#1E293B") // Dark hair
    canvas.drawOval(
        width * 0.31f, height * 0.28f,
        width * 0.69f, height * 0.38f,
        paint
    )
    
    // Draw perfect symmetrical eyes
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(width * 0.43f, height * 0.46f, 22f, paint)
    canvas.drawCircle(width * 0.57f, height * 0.46f, 22f, paint)
    
    paint.color = android.graphics.Color.parseColor("#1E293B") // Pupils
    canvas.drawCircle(width * 0.43f, height * 0.46f, 10f, paint)
    canvas.drawCircle(width * 0.57f, height * 0.46f, 10f, paint)
    
    // Nose
    paint.color = android.graphics.Color.parseColor("#F97316")
    canvas.drawRoundRect(
        width * 0.485f, height * 0.46f,
        width * 0.515f, height * 0.54f,
        10f, 10f,
        paint
    )
    
    // Neutral mouth (closed, straight, compliant with standards)
    paint.color = android.graphics.Color.parseColor("#E11D48")
    canvas.drawRoundRect(
        width * 0.45f, height * 0.58f,
        width * 0.55f, height * 0.595f,
        4f, 4f,
        paint
    )

    viewModel.setCapturedBitmap(demoBitmap)
}

@Composable
fun CameraPermissionDeniedView(
    onRequestPermission: () -> Unit,
    onBackClick: () -> Unit,
    onDemoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Accès caméra requis",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "L'application a besoin d'accéder à votre caméra pour vous guider en temps réel et capturer votre photo.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue600)
            ) {
                Text("Autoriser l'accès", color = Color.White)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Demo button as fallback
            OutlinedButton(
                onClick = onDemoClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simuler une photo de démo")
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retourner à la sélection", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}
