package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.DocumentType
import com.example.ui.components.IdPhotoOverlay
import com.example.ui.theme.*
import com.example.viewmodels.AppScreen
import com.example.viewmodels.IDPhotoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    viewModel: IDPhotoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val docType by viewModel.selectedDocType.collectAsState()
    val originalBitmap by viewModel.originalCapturedBitmap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Local copy of transform states for real-time fluid rendering
    var scale by remember { mutableStateOf(1.0f) }
    var offsetXPercent by remember { mutableStateOf(0f) }
    var offsetYPercent by remember { mutableStateOf(0f) }
    var rotationDegrees by remember { mutableStateOf(0f) }

    // Sync back to ViewModel before trigger
    LaunchedEffect(scale, offsetXPercent, offsetYPercent, rotationDegrees) {
        viewModel.setEditScale(scale)
        viewModel.setEditOffset(offsetXPercent, offsetYPercent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Ajustement de la Photo", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.CameraCapture) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Interactive Photo Adjustment Box (takes remaining space minus the slider controls)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Map drag pixels to approximate screen offset percentage (divided by screen sizes)
                            offsetXPercent = (offsetXPercent + dragAmount.x / 1000f).coerceIn(-0.6f, 0.6f)
                            offsetYPercent = (offsetYPercent + dragAmount.y / 1000f).coerceIn(-0.6f, 0.6f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (originalBitmap != null) {
                    Image(
                        bitmap = originalBitmap!!.asImageBitmap(),
                        contentDescription = "Photo d'origine",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                // Apply real-time visualization transformations
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetXPercent * size.width
                                translationY = offsetYPercent * size.height
                                rotationZ = rotationDegrees
                            }
                    )
                }

                // Guidelines overlaid on top of the image
                IdPhotoOverlay(documentType = docType)
                
                // Overlay text instruction
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Ajustez pour cadrer le visage dans l'ovale",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Slider control center
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                ) {
                    // Zoom Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Zoom",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.width(70.dp)
                        )
                        Slider(
                            value = scale,
                            onValueChange = { scale = it },
                            valueRange = 1.0f..2.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricBlue,
                                activeTrackColor = ElectricBlue,
                                inactiveTrackColor = ElectricBlue.copy(alpha = 0.24f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%.1fx", scale),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    // Horizontal offset Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "H-Pan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.width(70.dp)
                        )
                        Slider(
                            value = offsetXPercent,
                            onValueChange = { offsetXPercent = it },
                            valueRange = -0.5f..0.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricBlue,
                                activeTrackColor = ElectricBlue,
                                inactiveTrackColor = ElectricBlue.copy(alpha = 0.24f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%d%%", (offsetXPercent * 100).toInt()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    // Vertical offset Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "V-Pan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.width(70.dp)
                        )
                        Slider(
                            value = offsetYPercent,
                            onValueChange = { offsetYPercent = it },
                            valueRange = -0.5f..0.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricBlue,
                                activeTrackColor = ElectricBlue,
                                inactiveTrackColor = ElectricBlue.copy(alpha = 0.24f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%d%%", (offsetYPercent * 100).toInt()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons Row (Rotate 90, Recapture, Confirm)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rotation button
                        Button(
                            onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f },
                            modifier = Modifier.weight(0.4f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = "Pivoter")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("90°", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Recommencer (Red/Outline)
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(AppScreen.CameraCapture) },
                            modifier = Modifier.weight(0.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.Cached, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reprendre", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Valider (ElectricBlue600)
                        Button(
                            onClick = { 
                                viewModel.confirmEditsAndGenerate(context) 
                            },
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("save_task_button"), // Keep tests happy using structured snake_case tags
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue600),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Valider", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
