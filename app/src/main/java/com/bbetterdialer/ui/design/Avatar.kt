package com.bbetterdialer.ui.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.ui.theme.Surface3
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.VividGreen

/*
 * ===========================================================================
 *  KONTAKT AVATAR – mindig sima, éles kör
 * ===========================================================================
 *  • **Éles kör** – nincs elmosás, nincs beleolvadás a háttérbe, és nincs
 *    semmilyen animált gyűrű a peremen.
 *  • Fotó nélkül a kezdőbetűk látszanak a választott színen – a betűk
 *    mindig fehérek és változatlanok.
 *  • Ha van fotó ÉS a felhasználó színt választott, a szín **gyűrűként**
 *    jelenik meg a kör peremén.
 * ===========================================================================
 */

@Composable
fun ContactAvatar(
    image: ImageBitmap?,
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    accent: Color = VividGreen,
    /**
     * A felhasználó által választott profilszín.
     * Fotó nélkül ez lesz a kör háttere; fotóval viszont **gyűrűként** jelenik
     * meg a kör peremén – így a színválasztás akkor is látszik, ha a
     * névjegynek már van profilképe.
     */
    colorOverride: Color? = null,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // --- a kör maga -------------------------------------------------
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (image == null) (colorOverride ?: accent) else Surface3)
                .then(
                    if (image != null && colorOverride != null) {
                        Modifier.border(3.dp, colorOverride, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    // Medium = bicubic (API 31+), így a kicsinyítés élesebb
                    filterQuality = FilterQuality.Medium,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = initials,
                    color = TextPrimary,
                    fontSize = (size.value / 2.6f).sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

    }
}
