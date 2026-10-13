package com.bbetterdialer.ui.screens

import com.bbetterdialer.ui.T

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.R
import com.bbetterdialer.data.Contact
import com.bbetterdialer.data.ContactFolders
import com.bbetterdialer.data.ContactOverrides
import com.bbetterdialer.ui.design.ContactAvatar
import com.bbetterdialer.ui.design.FlatCard
import com.bbetterdialer.ui.design.FlatChip
import com.bbetterdialer.ui.design.FlatDialog
import com.bbetterdialer.ui.design.FlatIconButton
import com.bbetterdialer.ui.design.FlatSearchField
import com.bbetterdialer.ui.design.FolderBadge
import com.bbetterdialer.ui.design.PlainCallIcon
import com.bbetterdialer.ui.design.SectionLabel
import com.bbetterdialer.ui.formatDial
import com.bbetterdialer.ui.initialsOf
import com.bbetterdialer.ui.matchesQuery
import com.bbetterdialer.ui.rememberContactPhoto
import com.bbetterdialer.ui.theme.OnAccent
import com.bbetterdialer.ui.theme.Surface2
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextSecondary
import com.bbetterdialer.ui.theme.TextTertiary
import com.bbetterdialer.ui.theme.VividGreen

/**
 * Kontaktok fül – keresősáv, mappaszűrő chipek és névjegylista.
 * A mappák az app saját, helyi csoportjai ([ContactFolders]).
 */
@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    hasPermission: Boolean,
    onCall: (Contact) -> Unit,
    onPick: (Contact) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!hasPermission) {
        HintCard(title = T.noPermissionContacts, body = T.noPermissionContactsBody)
        return
    }

    var query by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var dialogContact by remember { mutableStateOf<Contact?>(null) }
    var manageFolders by remember { mutableStateOf(false) }

    val folders = ContactFolders.folders
    val assignments = ContactFolders.assignments

    val filtered = remember(query, contacts, selectedFolder, assignments) {
        contacts.filter { contact ->
            val inFolder = selectedFolder == null || assignments[contact.contactId] == selectedFolder
            inFolder && contact.matchesQuery(query, ContactOverrides.nameFor(contact))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        FlatSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = T.searchNameOrNumber,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "__all__") {
                FlatChip(
                    label = T.all,
                    selected = selectedFolder == null,
                    onClick = { selectedFolder = null }
                )
            }
            items(items = folders, key = { "f_$it" }) { folder ->
                FlatChip(
                    label = folder,
                    selected = selectedFolder == folder,
                    onClick = { selectedFolder = folder }
                )
            }
            item(key = "__new__") {
                FlatChip(
                    label = T.newFolder,
                    selected = false,
                    onClick = { manageFolders = true },
                    accent = VividGreen
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Box(Modifier.weight(1f)) {
            if (filtered.isEmpty()) {
                HintCard(
                    title = if (selectedFolder != null) T.emptyFolder else T.noResults,
                    body = when {
                        contacts.isEmpty() -> T.noContactsDevice
                        selectedFolder != null -> T.emptyFolderBody
                        else -> T.tryOtherName
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = filtered, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            folder = ContactFolders.folderOf(contact.contactId),
                            onOpen = { dialogContact = contact },
                            onCall = { onCall(contact) }
                        )
                    }
                }
            }
        }
    }

    dialogContact?.let { contact ->
        ContactDialog(
            contact = contact,
            onDismiss = { dialogContact = null },
            onCall = {
                dialogContact = null
                onCall(contact)
            },
            onPick = {
                dialogContact = null
                onPick(contact)
            }
        )
    }

    if (manageFolders) {
        ManageFoldersDialog(onDismiss = { manageFolders = false })
    }
}

// --------------------------------------------------------------------------- sor

@Composable
private fun ContactRow(
    contact: Contact,
    folder: String?,
    onOpen: () -> Unit,
    onCall: () -> Unit,
) {
    val displayName = ContactOverrides.nameFor(contact)
    val colorOverride = ContactOverrides.colorFor(contact.contactId)
    val photo = rememberContactPhoto(contact.photoUri, contact.contactId, sizePx = 192)

    FlatCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContactAvatar(
                image = photo,
                initials = initialsOf(displayName),
                size = 46.dp,
                colorOverride = colorOverride
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDial(contact.number),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (folder != null) {
                        Spacer(Modifier.width(8.dp))
                        FolderBadge(folder = folder)
                    }
                }
            }

            PlainCallIcon(onClick = onCall, size = 44.dp, iconSize = 22.dp)
        }
    }
}

// --------------------------------------------------------------------------- dialogok

@Composable
private fun ContactDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onPick: () -> Unit,
) {
    val photo = rememberContactPhoto(contact.photoUri, contact.contactId, sizePx = 320)
    val folders = ContactFolders.folders
    val current = ContactFolders.folderOf(contact.contactId)
    val displayName = ContactOverrides.nameFor(contact)
    val colorOverride = ContactOverrides.colorFor(contact.contactId)

    FlatDialog(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContactAvatar(
                image = photo,
                initials = initialsOf(displayName),
                size = 54.dp,
                colorOverride = colorOverride
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = displayName,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDial(contact.number),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Row {
            FlatChip(label = T.call, selected = true, onClick = onCall, accent = VividGreen)
            Spacer(Modifier.width(8.dp))
            FlatChip(label = T.toKeypad, selected = false, onClick = onPick)
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel(T.folder)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlatChip(
                label = T.none,
                selected = current == null,
                onClick = { ContactFolders.assign(contact.contactId, null) }
            )
            folders.forEach { folder ->
                FlatChip(
                    label = folder,
                    selected = current == folder,
                    onClick = { ContactFolders.assign(contact.contactId, folder) }
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface2)
                .clickable(onClick = onDismiss)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(T.close, color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ManageFoldersDialog(onDismiss: () -> Unit) {
    val folders = ContactFolders.folders
    val assignments = ContactFolders.assignments
    var newName by remember { mutableStateOf("") }

    FlatDialog(onDismiss = onDismiss) {
        Text(
            text = T.folders,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(text = T.foldersHint, color = TextTertiary, fontSize = 12.sp)

        Spacer(Modifier.height(16.dp))

        folders.forEach { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(folder, color = TextPrimary, fontSize = 15.sp)
                    Text(
                        text = T.contactCount(assignments.count { it.value == folder }),
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
                FlatIconButton(
                    iconRes = R.drawable.ic_close,
                    contentDescription = T.delete,
                    onClick = { ContactFolders.deleteFolder(folder) },
                    size = 34.dp,
                    iconSize = 14.dp,
                    tint = TextSecondary
                )
            }
        }

        if (folders.isEmpty()) {
            Text(text = T.noFoldersYet, color = TextTertiary, fontSize = 13.sp)
        }

        Spacer(Modifier.height(16.dp))

        FlatSearchField(
            value = newName,
            onValueChange = { newName = it },
            placeholder = T.newFolderName,
            showIcon = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row {
            FlatChip(
                label = T.create,
                selected = true,
                accent = VividGreen,
                onClick = { if (ContactFolders.createFolder(newName) != null) newName = "" }
            )
            Spacer(Modifier.width(8.dp))
            FlatChip(label = T.done, selected = false, onClick = onDismiss)
        }
    }
}
