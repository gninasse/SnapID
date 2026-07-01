package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.DocSelectionScreen
import com.example.ui.screens.EditScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodels.AppScreen
import com.example.viewmodels.IDPhotoViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: IDPhotoViewModel = viewModel()
        val currentScreen by viewModel.currentScreen.collectAsState()
        val statusMessage by viewModel.statusMessage.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // Handle native toasts/notifications from state
        LaunchedEffect(statusMessage) {
          statusMessage?.let { msg ->
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
          }
        }

        Scaffold(
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
          when (currentScreen) {
            is AppScreen.Home -> {
              HomeScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
              )
            }
            is AppScreen.DocSelection -> {
              DocSelectionScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
              )
            }
            is AppScreen.CameraCapture -> {
              CameraScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize() // Full bleed stream for the camera
              )
            }
            is AppScreen.EditPhoto -> {
              EditScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
              )
            }
            is AppScreen.ExportPhoto -> {
              ExportScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
              )
            }
          }
        }
      }
    }
  }
}

// Kept for compatibility with GreetingScreenshotTest.kt
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}
