package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.models.DocumentType
import com.example.ui.theme.*
import com.example.viewmodels.AppScreen
import com.example.viewmodels.IDPhotoViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: IDPhotoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val docType by viewModel.selectedDocType.collectAsState()
    val croppedBitmap by viewModel.croppedBitmap.collectAsState()
    val sheetBitmap by viewModel.sheetBitmap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val scrollState = rememberScrollState()
    var isSavedSuccessfully by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Export & Planche Photo", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.EditPhoto) }) {
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
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Header Info Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Photo cadrée avec succès !",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Le format ${docType.sizeLabel} a été généré en haute résolution.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Two Previews Side-by-Side (Single Photo Preview and full Planche)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Single Cropped Photo Display
                Column(
                    modifier = Modifier.weight(0.4f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Photo d'Identité",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .aspectRatio(docType.aspectRatio)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (croppedBitmap != null) {
                            Image(
                                bitmap = croppedBitmap!!.asImageBitmap(),
                                contentDescription = "Photo d'identité recadrée",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // 6-Photo Planche Preview
                Column(
                    modifier = Modifier.weight(0.6f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Planche d'Impression (10x15cm)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.5f) // Landscape 10x15 planche is 3:2 ratio
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (sheetBitmap != null) {
                            Image(
                                bitmap = sheetBitmap!!.asImageBitmap(),
                                contentDescription = "Planche de photos d'identité",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Options: Save, Share image, Share A4 PDF
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Options de sauvegarde et d'impression",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // 1. Save to Gallery
                    Button(
                        onClick = {
                            viewModel.savePhotoToAppAndGallery(context) {
                                isSavedSuccessfully = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_to_gallery_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSavedSuccessfully) SuccessGreen else ElectricBlue600,
                            contentColor = Color.White
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(
                                imageVector = if (isSavedSuccessfully) Icons.Default.CheckCircle else Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSavedSuccessfully) "Sauvegardé avec succès !" else "Tout enregistrer dans la Galerie",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Enregistre la photo d'identité individuelle ET la planche de 6 photos dans votre galerie de photos.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    )

                    // Divider
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), 
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Row for Sharing JPG and PDF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 2. Share Image Planche
                        OutlinedButton(
                            onClick = {
                                shareLocalSheetImage(context, viewModel)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f))
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Partager Planche", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 3. Share Print PDF (A4)
                        OutlinedButton(
                            onClick = {
                                shareLocalPrintPdf(context, viewModel)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f))
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Imprimer / PDF A4", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Done / End Flow
            Button(
                onClick = { viewModel.navigateTo(AppScreen.Home) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("done_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Retour à l'accueil",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Shares the temporary sheet JPG file from internal cache/files directory.
 */
private fun shareLocalSheetImage(context: Context, viewModel: IDPhotoViewModel) {
    val sheet = viewModel.sheetBitmap.value ?: return
    try {
        val cacheFile = File(context.cacheDir, "temp_id_photo_sheet.jpg")
        cacheFile.outputStream().use { out ->
            sheet.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            cacheFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager la planche photo"))
    } catch (e: Exception) {
        e.printStackTrace()
        viewModel.showStatus("Impossible de partager l'image : ${e.localizedMessage}")
    }
}

/**
 * Shares the generated A4 PDF from internal cache/files directory.
 */
private fun shareLocalPrintPdf(context: Context, viewModel: IDPhotoViewModel) {
    val sheet = viewModel.sheetBitmap.value ?: return
    try {
        val cacheFile = File(context.cacheDir, "id_photo_planche_A4.pdf")
        // Render current sheet to PDF
        com.example.utils.ImageProcessor.exportToPdf(context, sheet, cacheFile)

        val uri = FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            cacheFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager ou Imprimer le PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
        viewModel.showStatus("Impossible d'exporter en PDF : ${e.localizedMessage}")
    }
}
