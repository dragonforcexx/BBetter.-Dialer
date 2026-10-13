package com.bbetterdialer.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.ui.theme.AppBg
import com.bbetterdialer.ui.theme.Surface2
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextSecondary

/**
 * Általános kis pirula-címke.
 *
 * @param highlighted igaz esetén fehér háttér + fekete szöveg (kiemelés).
 */
@Composable
fun PillBadge(
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    fontSize: Int = 10,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (highlighted) TextPrimary else Surface2)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = if (highlighted) AppBg else TextSecondary,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            maxLines = 1
        )
    }
}

/**
 * Mappacímke (pl. CSALÁD) – a hívásképernyőn fehér kiemeléssel.
 */
@Composable
fun FolderBadge(
    folder: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    PillBadge(
        text = folder.uppercase(),
        modifier = modifier,
        highlighted = highlighted
    )
}
