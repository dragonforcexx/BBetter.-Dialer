package com.bbetterdialer.ui.screens

import com.bbetterdialer.ui.T

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.R
import com.bbetterdialer.data.AvatarOverrides
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
import kotlinx.coroutines.launch

/**
 * Névjegyek átnevezése.
 *
 * A mentés mindig eltárolódik az appban (hogy azonnal látszódjon), és
 * `WRITE_CONTACTS` jog birtokában a rendszer-névjegy is frissül – erről a
 * képernyő visszajelzést ad.
 */
@Composable
fun NameSettingsScreen(
    contacts: List<Contact>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Contact?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    val names = ContactOverrides.names
    val filtered = remember(query, contacts, names) {
        contacts.filter { it.matchesQuery(query) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        SettingsTopBar(title = T.name, onBack = onBack)

        Text(
            text = T.renameSub,
            color = TextTertiary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)
        )

        if (status != null) {
            Text(
                text = status.orEmpty(),
                color = VividGreen,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
            )
        }

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
                    val renamed = ContactOverrides.hasName(contact.contactId)

                    SettingsContactRow(
                        contact = contact,
                        subtitle = if (renamed) "${T.originalLabel}: ${contact.name}" else formatDial(contact.number),
                        badge = if (renamed) T.renamedBadge else null,
                        badgeHighlighted = renamed,
                        onClick = { editing = contact }
                    ) {
                        if (renamed) {
                            FlatIconButton(
                                iconRes = R.drawable.ic_delete,
                                contentDescription = T.reset,
                                onClick = {
                                    ContactOverrides.clearName(contact.contactId)
                                    status = T.nameReset
                                },
                                size = 38.dp,
                                iconSize = 16.dp,
                                background = Surface2,
                                tint = TextSecondary
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        FlatIconButton(
                            iconRes = R.drawable.ic_edit,
                            contentDescription = T.renameTitle,
                            onClick = { editing = contact },
                            size = 40.dp,
                            iconSize = 18.dp,
                            active = true,
                            activeBackground = VividGreen,
                            activeTint = OnAccent
                        )
                    }
                }
            }
        }
    }

    editing?.let { contact ->
        RenameDialog(
            contact = contact,
            onDismiss = { editing = null },
            onSave = { newName ->
                editing = null
                scope.launch {
                    status = when (val result = ContactOverrides.rename(context, contact.contactId, newName)) {
                        is ContactOverrides.RenameResult.Success ->
                            if (result.systemUpdated) T.nameSavedSystem
                            else T.nameSavedLocal
                        is ContactOverrides.RenameResult.Failure -> result.message
                    }
                }
            }
        )
    }
}

@Composable
private fun RenameDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(ContactOverrides.nameFor(contact)) }

    FlatDialog(onDismiss = onDismiss) {
        Text(
            text = T.renameTitle,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${T.originalLabel}: ${contact.name} · ${formatDial(contact.number)}",
            color = TextTertiary,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(16.dp))

        FlatSearchField(
            value = text,
            onValueChange = { text = it },
            placeholder = T.newName,
            showIcon = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Row {
            FlatChip(
                label = T.cancel,
                selected = false,
                onClick = onDismiss
            )
            Spacer(Modifier.width(10.dp))
            FlatChip(
                label = T.save,
                selected = true,
                accent = VividGreen,
                onClick = { if (text.isNotBlank()) onSave(text) }
            )
        }
    }
}
