package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.models.DocumentType
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageProcessor {

    /**
     * Rotates a bitmap by the given degrees.
     */
    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Generates a high-resolution ID photo (e.g., 700x900 pixels) by rendering the original image
     * onto a target bitmap using the scale and offsets adjusted by the user.
     *
     * @param originalBitmap The high-res original captured image.
     * @param docType The selected DocumentType (defining aspect ratio).
     * @param scale The user's zoom factor (1.0f to 2.5f).
     * @param offsetXPercent The horizontal offset as a fraction (-0.5f to 0.5f) of the template width.
     * @param offsetYPercent The vertical offset as a fraction (-0.5f to 0.5f) of the template height.
     * @param rotationDegrees The user's custom rotation (0, 90, 180, 270).
     * @param viewportWidth The actual width of the viewport/container box on the screen.
     * @param viewportHeight The actual height of the viewport/container box on the screen.
     */
    fun createCroppedIdPhoto(
        originalBitmap: Bitmap,
        docType: DocumentType,
        scale: Float,
        offsetXPercent: Float,
        offsetYPercent: Float,
        rotationDegrees: Float,
        viewportWidth: Float,
        viewportHeight: Float
    ): Bitmap {
        // Define high-res output resolution
        // Standard EU passport: 35mm x 45mm. Let's output at 300 DPI:
        // 35mm = 1.378 inches -> 413 pixels
        // 45mm = 1.772 inches -> 531 pixels
        // Let's use a crisp double-density target (approx 600 DPI) for premium quality:
        val targetWidth = if (docType.aspectRatio == 1.0f) 800 else 700
        val targetHeight = (targetWidth / docType.aspectRatio).toInt()

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        // Fill background with white
        canvas.drawColor(Color.WHITE)

        // First apply user rotation to the original bitmap
        val rotatedSource = rotateBitmap(originalBitmap, rotationDegrees)
        val I_w = rotatedSource.width.toFloat()
        val I_h = rotatedSource.height.toFloat()

        // Fallback if viewport sizes are not loaded
        val V_w = if (viewportWidth <= 0f) I_w else viewportWidth
        val V_h = if (viewportHeight <= 0f) I_h else viewportHeight

        // Calculate crop box dimensions on screen (matching IdPhotoOverlay exactly)
        val marginPercent = 0.12f
        val targetRatio = docType.aspectRatio
        val currentRatio = V_w / V_h

        val C_w: Float
        val C_h: Float
        if (currentRatio > targetRatio) {
            // Screen is wider than target ratio: limit by height
            C_h = V_h * (1f - 2 * marginPercent)
            C_w = C_h * targetRatio
        } else {
            // Screen is taller than target ratio: limit by width
            C_w = V_w * (1f - 2 * marginPercent)
            C_h = C_w / targetRatio
        }

        // Calculate fitScale of rotatedSource inside viewport (ContentScale.Fit)
        val fitScale = Math.min(V_w / I_w, V_h / I_h)

        // Actual translations applied in pixels relative to viewport
        val translationX = offsetXPercent * V_w
        val translationY = offsetYPercent * V_h

        // Map crop box corners to the rotated original image coordinates
        // Using coordinate space mapping:
        val left_img = I_w / 2f + (-C_w / 2f - translationX) / (fitScale * scale)
        val top_img = I_h / 2f + (-C_h / 2f - translationY) / (fitScale * scale)
        val right_img = I_w / 2f + (C_w / 2f - translationX) / (fitScale * scale)
        val bottom_img = I_h / 2f + (C_h / 2f - translationY) / (fitScale * scale)

        // Robust map of source rect to destination rect
        val srcRect = RectF(left_img, top_img, right_img, bottom_img)
        val dstRect = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())

        val matrix = Matrix()
        matrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.FILL)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        canvas.drawBitmap(rotatedSource, matrix, paint)
        
        // Clean up temporary rotated source if it's a new instance
        if (rotatedSource != originalBitmap) {
            rotatedSource.recycle()
        }

        return output
    }

    /**
     * Arranges 6 copies of the ID photo onto a single standard photo sheet (planche) of size 10x15 cm (4x6 inches).
     * This format is standard for instant photobooths and home photo printers.
     * Sheet size at 300 DPI is 1200 x 1800 pixels.
     */
    fun createPhotoSheet(
        photo: Bitmap,
        docType: DocumentType
    ): Bitmap {
        // 4x6 inches @ 300 DPI is 1200 x 1800 (portrait) or 1800 x 1200 (landscape).
        // Let's create a landscape sheet of 1800 x 1200 pixels.
        val sheetWidth = 1800
        val sheetHeight = 1200
        
        val sheet = Bitmap.createBitmap(sheetWidth, sheetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        
        // White background
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Calculate individual photo dimensions on the 10x15cm (1800x1200) sheet.
        // For standard 35x45mm:
        // 35mm on 100mm (10cm) is 35% of width.
        // 45mm on 150mm (15cm) is 30% of height.
        // At 300 DPI:
        // 35mm = 413 pixels.
        // 45mm = 531 pixels.
        val pWidth: Int
        val pHeight: Int
        
        if (docType.aspectRatio == 1.0f) {
            // US Visa: 2x2 inches on 4x6 inches sheet.
            // 2 inches = 600 pixels. We can fit 2 x 3 photos or just 2 x 2.
            // Let's scale it to fit nicely.
            pWidth = 500
            pHeight = 500
        } else {
            // EU Passport: 3.5cm x 4.5cm.
            // At 300 DPI, 3.5cm is ~413px, 4.5cm is ~531px.
            pWidth = 413
            pHeight = 531
        }

        // Scale the input photo to this dimension
        val scaledPhoto = Bitmap.createScaledBitmap(photo, pWidth, pHeight, true)

        // Arrange 6 photos in a 2 rows x 3 columns layout
        // Landscape sheet (1800 x 1200)
        // Horizontal spacing: (1800 - 3 * pWidth) / 4
        // Vertical spacing: (1200 - 2 * pHeight) / 3
        val hGap = (sheetWidth - 3 * pWidth) / 4
        val vGap = (sheetHeight - 2 * pHeight) / 3.5f

        for (row in 0 until 2) {
            for (col in 0 until 3) {
                val x = hGap + col * (pWidth + hGap)
                val y = vGap.toInt() + row * (pHeight + vGap.toInt())

                // Draw photo
                canvas.drawBitmap(scaledPhoto, x.toFloat(), y.toFloat(), paint)

                // Draw thin light-gray cutting border around each photo
                canvas.drawRect(
                    x.toFloat(),
                    y.toFloat(),
                    (x + pWidth).toFloat(),
                    (y + pHeight).toFloat(),
                    borderPaint
                )
            }
        }

        // Add a professional footer instruction
        canvas.drawText(
            "Planche Photo d'Identité Conforme — Format ${docType.sizeLabel} — Imprimer sur papier photo 10x15 cm (4\"x6\") sans marge",
            (sheetWidth / 2).toFloat(),
            (sheetHeight - 40).toFloat(),
            textPaint
        )

        scaledPhoto.recycle()
        return sheet
    }

    /**
     * Generates a standard A4 size PDF document with the sheet of 6 photos printed at the exact center.
     * A4 dimensions are 595 x 842 points (at 72 points per inch).
     * 10cm x 15cm sheet is 4 inches x 6 inches, which translates to 288 x 432 PDF points.
     */
    fun exportToPdf(context: Context, sheetBitmap: Bitmap, outputFile: File) {
        val pdfDocument = PdfDocument()

        // Page info for A4 (595 x 842)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Draw title
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            isAntiAlias = true
        }

        canvas.drawText("PLANCHE PHOTO D'IDENTITÉ", 50f, 60f, titlePaint)
        canvas.drawText("Généré avec ID Photo Maker — Prêt pour impression sur papier A4 standard", 50f, 78f, subPaint)

        // Draw cutting guides instructions
        val instructionPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9f
            isAntiAlias = true
        }
        canvas.drawText("⚠️ IMPORTANT : Pour préserver la conformité administrative,", 50f, 720f, instructionPaint)
        canvas.drawText("imprimez ce fichier PDF à 'Taille Réelle' (échelle 100%) sans ajuster la page aux marges.", 50f, 735f, instructionPaint)

        // 10x15cm print sheet is exactly 4x6 inches.
        // 4 inches * 72 points/inch = 288 points width
        // 6 inches * 72 points/inch = 432 points height
        // Let's place it landscape at the center of the page.
        // Landscape 15x10cm sheet is 432 points wide and 288 points high.
        val sheetPdfW = 432
        val sheetPdfH = 288

        val left = (595 - sheetPdfW) / 2
        val top = (842 - sheetPdfH) / 2 - 20 // slightly offset upwards for balance

        val destRect = Rect(left, top, left + sheetPdfW, top + sheetPdfH)
        
        val paint = Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }

        // Draw the 10x15cm planche bitmap onto the A4 PDF page
        canvas.drawBitmap(sheetBitmap, null, destRect, paint)

        // Draw a neat boundary box around the whole sheet to guide cutting
        val boundaryPaint = Paint().apply {
            color = Color.GRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(destRect, boundaryPaint)

        pdfDocument.finishPage(page)

        // Save PDF file
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }

        pdfDocument.close()
    }

    /**
     * Saves a bitmap to the application's internal files directory.
     */
    fun saveBitmapToStorage(context: Context, bitmap: Bitmap, prefix: String): File {
        val directory = File(context.filesDir, "id_photos")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return file
    }

    /**
     * Saves a bitmap to the device's public photo gallery.
     * Uses the modern MediaStore API to ensure compatibility across Android versions
     * and automatic indexation in the system Gallery.
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Uri? {
        val filename = "${title.replace(" ", "_")}_${System.currentTimeMillis()}.jpg"
        var outStream: OutputStream? = null
        var uri: Uri? = null

        try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ID_Photo_Maker")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                outStream = contentResolver.openOutputStream(uri)
                if (outStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            outStream?.close()
        }

        return uri
    }
}
