package com.bbetterdialer.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import com.bbetterdialer.ui.theme.Surface1
import com.bbetterdialer.ui.theme.Surface3
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextSecondary
import com.bbetterdialer.ui.theme.TextTertiary

/**
 * Bal oldali fiók (drawer).
 *
 * Csak a képernyő kb. [widthFraction] részét fedi le, a külső (jobb) élei
 * lekerekítettek. A mozgás ugyanaz a rugós (spring) animáció, mint az alsó
 * fülek csúszó kapszulájáé – az `Animatable`-lel kézzel vezérelve, hogy a
 * bezárulás animációja is lefuthasson, mielőtt a kompozícióból eltűnik.
 */
@Composable
fun SideDrawer(
    open: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.62f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val progress = remember { Animatable(0f) }
    var rendered by remember { mutableStateOf(open) }

    LaunchedEffect(open) {
        if (open) {
            rendered = true
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            rendered = false
        }
    }

    if (!rendered) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val panelWidth = maxWidth * widthFraction
        val slideOffset = panelWidth * (progress.value - 1f)

        // Sötétítő réteg – koppintásra zárul
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f * progress.value))
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onClose
                )
        )

        // Maga a fiók
        Column(
            modifier = Modifier
                .offset(x = slideOffset)
                .width(panelWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                .background(Surface1)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = 10.dp),
            content = content
        )
    }
}

/** A fiók fejléce. */
@Composable
fun DrawerHeader(
    title: String,
    subtitle: String,
) {
    Column(Modifier.padding(horizontal = 22.dp, vertical = 14.dp)) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = TextTertiary,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
    }
}

/** Egy fiókelem: ikon + címke, koppintásra művelet. */
@Composable
fun DrawerItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) Surface3 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (active) TextPrimary else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            color = if (active) TextPrimary else TextSecondary,
            fontSize = 15.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
        )
    }
}

/** Elválasztó vonal a fiókban / beállításokban. */
@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(com.bbetterdialer.ui.theme.Divider)
    )
}
