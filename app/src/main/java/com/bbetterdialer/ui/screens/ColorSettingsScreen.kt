package com.bbetterdialer.ui.screens

import com.bbetterdialer.ui.T

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.bbetterdialer.R
import com.bbetterdialer.data.Contact
import com.bbetterdialer.data.ContactOverrides
import com.bbetterdialer.ui.design.FlatChip
import com.bbetterdialer.ui.design.FlatDialog
import com.bbetterdialer.ui.design.FlatIconButton
import com.bbetterdialer.ui.design.FlatSearchField
import com.bbetterdialer.ui.design.SectionLabel
import com.bbetterdialer.ui.formatDial
import com.bbetterdialer.ui.matchesQuery
import com.bbetterdialer.ui.theme.OnAccent
import com.bbetterdialer.ui.theme.Surface2
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextSecondary
import com.bbetterdialer.ui.theme.TextTertiary
import com.bbetterdialer.ui.theme.VividGreen

/**
 * Profilszín választó fotó nélküli névjegyekhez.
 *
 * A kör háttere lesz a választott szín, a **kezdőbetűk változatlanok**
 * (fehérek) maradnak. A választék szándékosan kék nélküli.
 */
@Composable
fun ColorSettingsScreen(
    contacts: List<Contact>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Contact?>(null) }

    val colors = ContactOverrides.colors
    val filtered = remember(query, contacts, colors) {
        contacts.filter { it.matchesQuery(query) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        SettingsTopBar(title = T.profileColors, onBack = onBack)

        Text(
            text = T.profileColorsHint,
            color = TextTertiary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)
        )

        FlatSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = T.searchContacts,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        SectionLabel(
            T.sectionContacts,
            Modifier.padding(start = 22.dp, top = 6.dp, bottom = 6.dp)
        )

        if (filtered.isEmpty()) {
            Text(
                text = T.noContactsShown,
                color = TextTertiary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = filtered, key = { it.id }) { contact ->
                    val hasColor = ContactOverrides.hasColor(contact.contactId)
                    val chosen = ContactOverrides.colorFor(contact.contactId)

                    SettingsContactRow(
                        contact = contact,
                        subtitle = formatDial(contact.number),
                        badge = if (hasColor) T.coloredBadge else null,
                        badgeHighlighted = hasColor,
                        onClick = { editing = contact }
                    ) {
                        if (hasColor) {
                            FlatIconButton(
                                iconRes = R.drawable.ic_delete,
                                contentDescription = T.deleteColor,
                                onClick = { ContactOverrides.setColor(contact.contactId, null) },
                                size = 38.dp,
                                iconSize = 16.dp,
                                background = Surface2,
                                tint = TextSecondary
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        FlatIconButton(
                            iconRes = R.drawable.ic_palette,
                            contentDescription = T.pickColor,
                            onClick = { editing = contact },
                            size = 40.dp,
                            iconSize = 18.dp,
                            active = hasColor,
                            activeBackground = chosen ?: VividGreen,
                            activeTint = OnAccent,
                            background = Surface2,
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
    }

    editing?.let { contact ->
        ColorPickerDialog(
            current = ContactOverrides.colorFor(contact.contactId),
            onDismiss = { editing = null },
            onPick = { color ->
                ContactOverrides.setColor(contact.contactId, color)
                editing = null
            }
        )
    }
}

@Composable
private fun ColorPickerDialog(
    current: Color?,
    onDismiss: () -> Unit,
    onPick: (Color?) -> Unit,
) {
    FlatDialog(onDismiss = onDismiss) {
        Text(
            text = T.profileColor,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = T.profileColorHint,
            color = TextTertiary,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(18.dp))

        val palette = ContactOverrides.palette
        palette.chunked(4).forEach { rowColors ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowColors.forEach { color ->
                    ColorSwatch(
                        color = color,
                        selected = current != null && current.value == color.value,
                        onClick = { onPick(color) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row {
            FlatChip(
                label = T.defaultColor,
                selected = current == null,
                onClick = { onPick(null) }
            )
            Spacer(Modifier.width(10.dp))
            FlatChip(
                label = T.close,
                selected = false,
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = if (selected) TextPrimary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
