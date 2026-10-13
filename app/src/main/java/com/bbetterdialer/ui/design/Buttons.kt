package com.bbetterdialer.ui.design

import com.bbetterdialer.ui.T

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bbetterdialer.R
import com.bbetterdialer.ui.theme.EndRed
import com.bbetterdialer.ui.theme.IdleGreen
import com.bbetterdialer.ui.theme.IdleGreenTint
import com.bbetterdialer.ui.theme.OnAccent
import com.bbetterdialer.ui.theme.VividGreen

/**
 * Nagy, lapos akciógomb (hívás / bontás / fogadás).
 * Nincs árnyék, nincs gradiens – egyetlen tömör szín, nyomáskor halványul.
 */
@Composable
fun CircleActionButton(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    iconSize: Dp = 30.dp,
    background: Color,
    tint: Color = OnAccent,
    rotateIcon: Float = 0f,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val backgroundColor = when {
        !enabled -> background.copy(alpha = 0.35f)
        pressed -> background.copy(alpha = 0.70f)
        else -> background
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
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer { rotationZ = rotateIcon }
        )
    }
}

/**
 * Hívásindító gomb.
 *
 * Amíg nincs beütött szám, **sötétzöld** és kicsit kisebb; amint a felhasználó
 * beír egy számjegyet, élénk zöldre vált, megnő, és mindezt rugós (spring)
 * animációval teszi – hasonlóan az alsó menü csúszó kapszulájához.
 */
@Composable
fun CallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    idleSize: Dp = 66.dp,
    activeSize: Dp = 76.dp,
) {
    val background by animateColorAsState(
        targetValue = if (active) VividGreen else IdleGreen,
        animationSpec = tween(durationMillis = 240),
        label = "callButtonColor"
    )
    val tint by animateColorAsState(
        targetValue = if (active) OnAccent else IdleGreenTint,
        animationSpec = tween(durationMillis = 240),
        label = "callButtonTint"
    )
    val diameter by animateDpAsState(
        targetValue = if (active) activeSize else idleSize,
        animationSpec = spring<Dp>(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "callButtonSize"
    )

    CircleActionButton(
        iconRes = R.drawable.ic_call,
        contentDescription = T.startCall,
        onClick = onClick,
        modifier = modifier,
        size = diameter,
        iconSize = diameter * 0.42f,
        background = background,
        tint = tint
    )
}

/** Piros hívásbontó gomb (a kagyló 135°-kal elforgatva). */
@Composable
fun EndCallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    CircleActionButton(
        iconRes = R.drawable.ic_call,
        contentDescription = "Hívás befejezése",
        onClick = onClick,
        modifier = modifier,
        size = size,
        iconSize = size * 0.42f,
        background = EndRed,
        rotateIcon = 135f
    )
}

/** Zöld hívásfogadó gomb. */
@Composable
fun AnswerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    CircleActionButton(
        iconRes = R.drawable.ic_call,
        contentDescription = "Hívás fogadása",
        onClick = onClick,
        modifier = modifier,
        size = size,
        iconSize = size * 0.42f,
        background = VividGreen
    )
}

/**
 * Sima, **kör nélküli** zöld telefon ikon – listaelemek hívásgombja.
 * Nincs háttér és nincs keret, csak a zöld kagyló; nyomáskor halványul.
 */
@Composable
fun PlainCallIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color = VividGreen,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_call),
            contentDescription = "Hívás",
            tint = if (pressed) tint.copy(alpha = 0.55f) else tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
