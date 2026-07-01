package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.example.models.DocumentType

@Composable
fun IdPhotoOverlay(
    documentType: DocumentType,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(alpha = 0.99f) // Forces hardware layer for correct BlendMode.Clear blending
    ) {
        val width = size.width
        val height = size.height

        // Calculate the bounding box for the document photo based on the aspect ratio.
        // It should occupy about 80% of the screen width or 65% of the screen height, whichever is smaller.
        val marginPercent = 0.12f
        val docWidth: Float
        val docHeight: Float

        val targetRatio = documentType.aspectRatio // width / height
        val currentRatio = width / height

        if (currentRatio > targetRatio) {
            // Screen is wider than target ratio: limit by height
            docHeight = height * (1f - 2 * marginPercent)
            docWidth = docHeight * targetRatio
        } else {
            // Screen is taller than target ratio: limit by width
            docWidth = width * (1f - 2 * marginPercent)
            docHeight = docWidth / targetRatio
        }

        val left = (width - docWidth) / 2
        val top = (height - docHeight) / 2
        val right = left + docWidth
        val bottom = top + docHeight

        // 1. Draw the dark semi-transparent overlay
        drawRect(
            color = Color.Black.copy(alpha = 0.65f),
            topLeft = Offset.Zero,
            size = Size(width, height)
        )

        // 2. Clear out the document bounding box
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(docWidth, docHeight),
            cornerRadius = CornerRadius(16f, 16f),
            blendMode = BlendMode.Clear
        )

        // 3. Draw bounding box border (white and institutional blue)
        drawRoundRect(
            color = Color(0xFF0055A5), // Institutional Blue
            topLeft = Offset(left - 4f, top - 4f),
            size = Size(docWidth + 8f, docHeight + 8f),
            cornerRadius = CornerRadius(20f, 20f),
            style = Stroke(width = 4f)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(docWidth, docHeight),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 2f)
        )

        // 4. Calculate Face Oval dimensions inside the bounding box
        // Face should be centered, taking up ~70-80% of the height for standard passports
        val faceWidth = docWidth * 0.62f
        val faceHeight = docHeight * 0.68f
        val faceLeft = left + (docWidth - faceWidth) / 2
        val faceTop = top + (docHeight - faceHeight) / 2.5f // slightly higher than center

        // Draw dotted face oval guide
        drawOval(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(faceLeft, faceTop),
            size = Size(faceWidth, faceHeight),
            style = Stroke(
                width = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
            )
        )

        // 5. Draw Eye Guidelines
        // Eye level should be at about 40% from the top of the face oval
        val eyeY = faceTop + faceHeight * 0.4f
        drawLine(
            color = Color(0xFF38BDF8), // Light blue / Cyan
            start = Offset(left + docWidth * 0.15f, eyeY),
            end = Offset(right - docWidth * 0.15f, eyeY),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        // Small indicator labels or crosses for eyes
        val leftEyeX = faceLeft + faceWidth * 0.33f
        val rightEyeX = faceLeft + faceWidth * 0.67f
        
        val crossSize = 12f
        listOf(leftEyeX, rightEyeX).forEach { eyeX ->
            drawLine(
                color = Color(0xFF38BDF8),
                start = Offset(eyeX - crossSize, eyeY),
                end = Offset(eyeX + crossSize, eyeY),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFF38BDF8),
                start = Offset(eyeX, eyeY - crossSize),
                end = Offset(eyeX, eyeY + crossSize),
                strokeWidth = 3f
            )
        }

        // 6. Draw vertical centerline for nose/alignment
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(width / 2, top),
            end = Offset(width / 2, bottom),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
        
        // Chin guide line
        val chinY = faceTop + faceHeight
        drawLine(
            color = Color.White.copy(alpha = 0.7f),
            start = Offset(faceLeft + faceWidth * 0.25f, chinY),
            end = Offset(faceLeft + faceWidth * 0.75f, chinY),
            strokeWidth = 4f
        )
    }
}
