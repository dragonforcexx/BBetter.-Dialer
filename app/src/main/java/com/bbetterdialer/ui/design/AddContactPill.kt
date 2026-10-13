package com.bbetterdialer.ui.design

import com.bbetterdialer.ui.T

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.R
import com.bbetterdialer.data.ContactOverrides
import com.bbetterdialer.ui.theme.Surface2
import com.bbetterdialer.ui.theme.Surface3
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextSecondary
import com.bbetterdialer.ui.theme.TextTertiary
import com.bbetterdialer.ui.theme.VividGreen
import kotlinx.coroutines.delay

/**
 * "＋ Kontakt hozzáadása" – animált pirula a fejléc jobb szélén.
 *
 * Amikor a tárcsázóba beírt szám nincs a névjegyek között:
 *   1. egy **világos szürke kör** jelenik meg halványulva (nem csúszik, nem ugrik),
 *   2. egy rövid késleltetés után a kör **rugós animációval kapszulává szélesedik**,
 *      és megjelenik benne a felirat.
 */
@Composable
fun AddContactPill(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "addContactAlpha"
    )

    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(520)
            expanded = true
        } else {
            expanded = false
        }
    }

    val width by animateDpAsState(
        targetValue = if (expanded) 196.dp else 38.dp,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "addContactWidth"
    )

    if (alpha <= 0.01f) return

    Row(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .height(38.dp)
            .width(width)
            .clip(CircleShape)
            .background(Surface3)
            .clickable(enabled = expanded, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = T.addContact,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = T.addContact,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.width(152.dp)
        )
    }
}

/** Színválasztó rács – a profilszínekhez és az új névjegyhez is. */
@Composable
fun ColorPaletteGrid(
    selected: Color?,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        ContactOverrides.palette.chunked(4).forEach { rowColors ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selected != null && selected.value == color.value) 3.dp else 0.dp,
                                color = if (selected != null && selected.value == color.value) TextPrimary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onSelect(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected != null && selected.value == color.value) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lebegő ablak új névjegy létrehozásához – ugyanolyan stílusban, mint a
 * mappakezelő ablak: név megadása és profilszín választása.
 */
@Composable
fun AddContactDialog(
    number: String,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Color?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf<Color?>(null) }

    FlatDialog(onDismiss = onDismiss) {
        Text(
            text = T.newContact,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = number,
            color = TextTertiary,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(16.dp))

        FlatSearchField(
            value = name,
            onValueChange = { name = it },
            placeholder = T.name,
            showIcon = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(18.dp))

        SectionLabel(T.pickNameAndColor)
        Spacer(Modifier.height(6.dp))

        ColorPaletteGrid(selected = color, onSelect = { color = it })

        Spacer(Modifier.height(16.dp))

        Row {
            FlatChip(label = T.cancel, selected = false, onClick = onDismiss)
            Spacer(Modifier.width(10.dp))
            FlatChip(
                label = T.save,
                selected = true,
                accent = VividGreen,
                onClick = { if (name.isNotBlank()) onSave(name, color) }
            )
        }
    }
}
