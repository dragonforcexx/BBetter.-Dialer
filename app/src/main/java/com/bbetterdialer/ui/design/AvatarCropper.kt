package com.bbetterdialer.ui.design

import com.bbetterdialer.ui.T

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.bbetterdialer.data.AvatarOverrides
import com.bbetterdialer.ui.theme.Surface2
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextTertiary
import com.bbetterdialer.ui.theme.VividGreen

private val PreviewSize = 260.dp

/**
 * Képbeállító a profil kiválasztásához.
 *
 * A kiválasztott kép egy körben jelenik meg. **Húzással** mozgatható,
 * **csippentéssel** (két ujjal) nagyítható – így pontosan beállítható, hogy a
 * kép melyik része kerüljön a profilba. A körön kívüli rész halvány, a
 * megerősítés után a rendszer ugyanezt a kivágást alkalmazza a mentett képre.
 */
@Composable
fun AvatarCropperDialog(
    image: ImageBitmap,
    onCancel: () -> Unit,
    onConfirm: (AvatarOverrides.AvatarCrop) -> Unit,
) {
    var previewPx by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // A mozgatás korlátozása: a kivágott kör soha ne lógjon ki a képből.
    fun clamp(candidate: Offset, newScale: Float): Offset {
        val preview = previewPx
        if (preview <= 0f || image.width == 0 || image.height == 0) return Offset.Zero
        val cover = maxOf(preview / image.width, preview / image.height)
        val effective = cover * newScale
        val maxX = ((image.width * effective - preview) / 2f).coerceAtLeast(0f)
        val maxY = ((image.height * effective - preview) / 2f).coerceAtLeast(0f)
        return Offset(
            candidate.x.coerceIn(-maxX, maxX),
            candidate.y.coerceIn(-maxY, maxY)
        )
    }

    FlatDialog(onDismiss = onCancel) {
        Text(
            text = T.cropTitle,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = T.cropHint,
            color = TextTertiary,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(PreviewSize)
                .onSizeChanged { previewPx = it.width.toFloat() }
                .clip(CircleShape)
                .background(Surface2)
                .border(1.dp, TextTertiary, CircleShape)
                .pointerInput(image) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 6f)
                        scale = newScale
                        offset = clamp(offset + pan, newScale)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            FlatChip(
                label = T.cancel,
                selected = false,
                onClick = onCancel
            )
            Spacer(Modifier.width(10.dp))
            FlatChip(
                label = T.apply,
                selected = true,
                accent = VividGreen,
                onClick = {
                    val safePreview = previewPx.coerceAtLeast(1f)
                    onConfirm(
                        AvatarOverrides.AvatarCrop(
                            scale = scale,
                            offsetX = offset.x / safePreview,
                            offsetY = offset.y / safePreview
                        )
                    )
                }
            )
        }
    }
}
