package com.example.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.DocumentType
import com.example.models.IdPhoto
import com.example.utils.ImageProcessor
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface AppScreen {
    object Home : AppScreen
    object DocSelection : AppScreen
    object CameraCapture : AppScreen
    object EditPhoto : AppScreen
    object ExportPhoto : AppScreen
}

class IDPhotoViewModel : ViewModel() {
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Home)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedDocType = MutableStateFlow<DocumentType>(DocumentType.ALL.first())
    val selectedDocType: StateFlow<DocumentType> = _selectedDocType.asStateFlow()

    // Camera settings
    private val _isFrontCamera = MutableStateFlow(true) // Selfie mode as convenient default
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0) // 0 = no timer, 3 = 3s, 10 = 10s
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    // Captured image state
    private val _originalCapturedBitmap = MutableStateFlow<Bitmap?>(null)
    val originalCapturedBitmap: StateFlow<Bitmap?> = _originalCapturedBitmap.asStateFlow()

    // Edit state
    private val _editScale = MutableStateFlow(1.0f)
    val editScale: StateFlow<Float> = _editScale.asStateFlow()

    private val _editOffsetX = MutableStateFlow(0f)
    val editOffsetX: StateFlow<Float> = _editOffsetX.asStateFlow()

    private val _editOffsetY = MutableStateFlow(0f)
    val editOffsetY: StateFlow<Float> = _editOffsetY.asStateFlow()

    private val _editRotation = MutableStateFlow(0f)
    val editRotation: StateFlow<Float> = _editRotation.asStateFlow()

    // Final result
    private val _croppedBitmap = MutableStateFlow<Bitmap?>(null)
    val croppedBitmap: StateFlow<Bitmap?> = _croppedBitmap.asStateFlow()

    private val _sheetBitmap = MutableStateFlow<Bitmap?>(null)
    val sheetBitmap: StateFlow<Bitmap?> = _sheetBitmap.asStateFlow()

    // History of saved ID photos
    private val _historyList = MutableStateFlow<List<IdPhoto>>(emptyList())
    val historyList: StateFlow<List<IdPhoto>> = _historyList.asStateFlow()

    // UI Toast and loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Init Moshi for history persistence
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val historyListAdapterType = Types.newParameterizedType(List::class.java, IdPhoto::class.java)
    private val jsonAdapter = moshi.adapter<List<IdPhoto>>(historyListAdapterType)

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.Home) {
            // Reset state
            _originalCapturedBitmap.value = null
            _croppedBitmap.value = null
            _sheetBitmap.value = null
            _editScale.value = 1.0f
            _editOffsetX.value = 0f
            _editOffsetY.value = 0f
            _editRotation.value = 0f
        }
    }

    fun selectDocumentType(docType: DocumentType) {
        _selectedDocType.value = docType
    }

    fun toggleCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun setTimer(seconds: Int) {
        _timerSeconds.value = seconds
    }

    fun setEditScale(scale: Float) {
        _editScale.value = scale
    }

    fun setEditOffset(x: Float, y: Float) {
        _editOffsetX.value = x
        _editOffsetY.value = y
    }

    fun rotateImage() {
        // Rotate 90 degrees clockwise
        _editRotation.value = (_editRotation.value + 90f) % 360f
    }

    fun setCapturedBitmap(bitmap: Bitmap) {
        _originalCapturedBitmap.value = bitmap
        // Reset crop settings
        _editScale.value = 1.0f
        _editOffsetX.value = 0f
        _editOffsetY.value = 0f
        _editRotation.value = 0f
        navigateTo(AppScreen.EditPhoto)
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showStatus(message: String) {
        _statusMessage.value = message
    }

    // Load History from SharedPreferences
    fun loadHistory(context: Context) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("id_photo_maker_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("history_list", null)
            if (json != null) {
                try {
                    val list = jsonAdapter.fromJson(json) ?: emptyList()
                    _historyList.value = list.sortedByDescending { it.timestamp }
                } catch (e: Exception) {
                    Log.e("IDPhotoViewModel", "Failed to parse history", e)
                }
            }
        }
    }

    // Save History
    private fun saveHistoryToPrefs(context: Context, list: List<IdPhoto>) {
        val prefs = context.getSharedPreferences("id_photo_maker_prefs", Context.MODE_PRIVATE)
        val json = jsonAdapter.toJson(list)
        prefs.edit().putString("history_list", json).apply()
        _historyList.value = list.sortedByDescending { it.timestamp }
    }

    /**
     * Deletes a photo record from history and from disk storage.
     */
    fun deletePhoto(context: Context, idPhoto: IdPhoto) {
        viewModelScope.launch {
            try {
                // Delete cropped photo
                val cropFile = File(idPhoto.filePath)
                if (cropFile.exists()) cropFile.delete()

                // Delete sheet photo
                idPhoto.sheetFilePath?.let { path ->
                    val sheetFile = File(path)
                    if (sheetFile.exists()) sheetFile.delete()
                }

                // Delete pdf if exist
                val pdfFile = File(context.filesDir, "id_photos/print_${idPhoto.id}.pdf")
                if (pdfFile.exists()) pdfFile.delete()

                val newList = _historyList.value.filter { it.id != idPhoto.id }
                saveHistoryToPrefs(context, newList)
                _statusMessage.value = "Photo d'identité supprimée"
            } catch (e: Exception) {
                Log.e("IDPhotoViewModel", "Error deleting photo", e)
            }
        }
    }

    /**
     * Processes the current original image with the user's edits, crops it,
     * generates the photo sheet, and opens the Export screen.
     */
    fun confirmEditsAndGenerate(
        context: Context,
        scale: Float,
        offsetXPercent: Float,
        offsetYPercent: Float,
        rotationDegrees: Float,
        viewportWidth: Float,
        viewportHeight: Float
    ) {
        val original = _originalCapturedBitmap.value ?: return
        val docType = _selectedDocType.value

        _isLoading.value = true
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. Create cropped high-res photo
                val cropped = ImageProcessor.createCroppedIdPhoto(
                    original, docType, scale, offsetXPercent, offsetYPercent, rotationDegrees, viewportWidth, viewportHeight
                )

                // 2. Create the 6-photo printing sheet
                val sheet = ImageProcessor.createPhotoSheet(cropped, docType)

                withContext(Dispatchers.Main) {
                    _croppedBitmap.value = cropped
                    _sheetBitmap.value = sheet
                    _isLoading.value = false
                    navigateTo(AppScreen.ExportPhoto)
                }
            } catch (e: Exception) {
                Log.e("IDPhotoViewModel", "Error processing image", e)
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _statusMessage.value = "Erreur lors du traitement de l'image"
                }
            }
        }
    }

    /**
     * Saves the final processed photo and sheet to internal storage and/or gallery,
     * registers it in history.
     */
    fun savePhotoToAppAndGallery(context: Context, onComplete: () -> Unit) {
        val cropped = _croppedBitmap.value ?: return
        val sheet = _sheetBitmap.value ?: return
        val docType = _selectedDocType.value

        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                val id = "photo_$timestamp"

                // 1. Save cropped photo to internal storage (for history display)
                val cropFile = ImageProcessor.saveBitmapToStorage(context, cropped, "cropped_$id")

                // 2. Save sheet to internal storage (for history details)
                val sheetFile = ImageProcessor.saveBitmapToStorage(context, sheet, "sheet_$id")

                // 3. Save PDF to internal storage (ready to print)
                val pdfFile = File(context.filesDir, "id_photos/print_${id}.pdf")
                ImageProcessor.exportToPdf(context, sheet, pdfFile)

                // 4. Save sheet to Public Gallery so the user can easily print/share it from Google Photos
                ImageProcessor.saveBitmapToGallery(context, sheet, "Planche_Photo_Identite_${docType.name}")

                // 4b. Also save the single cropped ID photo to the public Gallery!
                ImageProcessor.saveBitmapToGallery(context, cropped, "Photo_Identite_Unique_${docType.name}")

                // 5. Add to SharedPreferences history
                val newPhoto = IdPhoto(
                    id = id,
                    filePath = cropFile.absolutePath,
                    sheetFilePath = sheetFile.absolutePath,
                    documentId = docType.id,
                    documentName = docType.name,
                    sizeLabel = docType.sizeLabel,
                    timestamp = timestamp
                )

                val updatedList = _historyList.value + newPhoto
                saveHistoryToPrefs(context, updatedList)

                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _statusMessage.value = "Photos sauvegardées ! Retrouvez la photo unique ET la planche dans votre Galerie."
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("IDPhotoViewModel", "Error saving photo", e)
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _statusMessage.value = "Erreur lors de la sauvegarde"
                }
            }
        }
    }
}
