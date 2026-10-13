package com.bbetterdialer.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.ui.theme.AppBg
import com.bbetterdialer.ui.theme.Surface1
import com.bbetterdialer.ui.theme.Surface2
import com.bbetterdialer.ui.theme.Surface3
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextSecondary
import com.bbetterdialer.ui.theme.TextTertiary

/*
 * ===========================================================================
 *  AMOLED MINIMAL alapkomponensek
 * ===========================================================================
 *  Szigorú szabályok:
 *    • minden háttér tiszta fekete (#000000), a felületek sötétszürkék,
 *    • NINCS árnyék (Modifier.shadow), NINCS gradiens, NINCS blur,
 *    • nyomási visszajelzés kizárólag a háttérszín változása (nincs skálázás),
 *    • a "kiválasztott" állapot fehér felület + fekete tartalom.
 * ===========================================================================
 */

/** A teljes app háttere: sötét témában tiszta AMOLED fekete, világosban világos szürke. */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg),
        content = content
    )
}

/** Lekerekített, lapos felület. */
fun Modifier.flatSurface(
    shape: Shape = RoundedCornerShape(20.dp),
    color: Color = Surface1
): Modifier = this
    .clip(shape)
    .background(color)

/** Lapos kártya – egyetlen tömör szín, keret és árnyék nélkül. */
@Composable
fun FlatCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    color: Color = Surface1,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.flatSurface(shape, color).padding(padding),
        content = content
    )
}

/** Kis, nagybetűs szakaszcím. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.4.sp
    )
}

/**
 * Lapos, kör alakú ikongomb. Nyomáskor csak a háttérszín vált.
 */
@Composable
fun FlatIconButton(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    iconSize: Dp = 22.dp,
    enabled: Boolean = true,
    active: Boolean = false,
    activeBackground: Color = TextPrimary,
    activeTint: Color = AppBg,
    background: Color = Surface2,
    tint: Color = TextPrimary,
    rotateIcon: Float = 0f,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val backgroundColor = when {
        !enabled -> Surface1
        active -> activeBackground
        pressed -> Surface3
        else -> background
    }
    val iconColor = when {
        !enabled -> TextTertiary
        active -> activeTint
        else -> tint
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer { rotationZ = rotateIcon }
        )
    }
}

/**
 * Lapos chip. Kiválasztva fehér háttér + fekete szöveg, egyébként sötétszürke.
 */
@Composable
fun FlatChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val background = when {
        selected -> accent ?: TextPrimary
        pressed -> Surface3
        else -> Surface2
    }
    val foreground = when {
        selected -> AppBg
        accent != null -> accent
        else -> TextSecondary
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 15.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = foreground,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
