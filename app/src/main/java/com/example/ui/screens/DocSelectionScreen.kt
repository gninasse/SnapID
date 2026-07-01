package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.DocumentType
import com.example.ui.theme.*
import com.example.viewmodels.AppScreen
import com.example.viewmodels.IDPhotoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocSelectionScreen(
    viewModel: IDPhotoViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDocType by viewModel.selectedDocType.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Type de Document", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            Text(
                text = "Choisissez le format requis :",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Selection list
            DocumentType.ALL.forEach { docType ->
                val isSelected = docType.id == selectedDocType.id
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { viewModel.selectDocumentType(docType) }
                        .testTag("doc_type_${docType.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) ElectricBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = docType.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Badge(
                                    containerColor = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = docType.sizeLabel,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = docType.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 18.sp
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(ElectricBlue, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sélectionné",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Rules / Specifications for the selected format
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FactCheck,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Exigences du format :",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    selectedDocType.officialSpecs.forEach { spec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                fontSize = 16.sp,
                                color = ElectricBlue,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = spec,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Button
            Button(
                onClick = { viewModel.navigateTo(AppScreen.CameraCapture) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_doc_type_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue600),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "Accéder à l'appareil photo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
