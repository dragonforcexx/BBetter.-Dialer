package com.bbetterdialer.ui.screens

import com.bbetterdialer.ui.T

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.R
import com.bbetterdialer.data.Contact
import com.bbetterdialer.data.ContactOverrides
import com.bbetterdialer.data.RingtoneSettings
import com.bbetterdialer.telecom.RingtonePlayer
import com.bbetterdialer.ui.design.FlatChip
import com.bbetterdialer.ui.design.FlatDialog
import com.bbetterdialer.ui.design.FlatIconButton
import com.bbetterdialer.ui.design.FlatSearchField
import com.bbetterdialer.ui.design.SectionLabel
import com.bbetterdialer.ui.matchesQuery
import com.bbetterdialer.ui.theme.Surface1
import com.bbetterdialer.ui.theme.Surface2
import com.bbetterdialer.ui.theme.Surface3
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextSecondary
import com.bbetterdialer.ui.theme.TextTertiary
import com.bbetterdialer.ui.theme.VividGreen

/** Melyik csengőhangot szerkesztjük éppen. */
private sealed interface RingtoneTarget {
    data object General : RingtoneTarget
    data class ForContact(val contactId: Long, val name: String) : RingtoneTarget
}

/**
 * Csengőhangok beállítása:
 *  • általános csengőhang minden híváshoz,
 *  • kontakt-szintű csengőhangok,
 *  • választás a beépített hangok közül, vagy a telefonon lévő fájlokból
 *    (rendszer fájlkezelő / dokumentumválasztó), előhallgatással.
 */
@Composable
fun RingtoneSettingsScreen(
    contacts: List<Contact>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember { RingtonePlayer(context) }
    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    var target by remember { mutableStateOf<RingtoneTarget?>(null) }
    var playingUri by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    player.onFinished = { playingUri = null }

    val defaultChoice = RingtoneSettings.default
    val perContact = RingtoneSettings.perContact
    val systemDefaultLabel = remember(context) { RingtoneSettings.systemDefaultLabel(context) }
    val defaultLabel = defaultChoice?.label ?: systemDefaultLabel

    val filtered = remember(query, contacts, perContact) {
        contacts.filter { it.matchesQuery(query) }
    }

    val togglePreview: (Uri) -> Unit = { uri ->
        val key = uri.toString()
        if (playingUri == key) {
            player.stop()
            playingUri = null
        } else {
            player.play(uri, loop = false)
            playingUri = key
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        SettingsTopBar(title = T.ringtones, onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "general_label") {
                SectionLabel(T.general)
            }
            item(key = "general_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Surface1)
                        .clickable { target = RingtoneTarget.General }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = T.ringtone,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = defaultLabel,
                            color = if (defaultChoice == null) TextTertiary else VividGreen,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PreviewButton(
                        uri = RingtoneSettings.resolveUri(context, null),
                        playingUri = playingUri,
                        onToggle = togglePreview
                    )
                }
            }

            item(key = "contacts_label") {
                Spacer(Modifier.height(10.dp))
                SectionLabel(T.contactRingtone)
            }
            item(key = "search") {
                FlatSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = T.searchContacts,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filtered.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = T.noContactsShown,
                        color = TextTertiary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            items(items = filtered, key = { "c_${it.id}" }) { contact ->
                ContactRingtoneRow(
                    contact = contact,
                    ownChoice = perContact[contact.contactId],
                    fallbackLabel = defaultLabel,
                    playingUri = playingUri,
                    onTogglePreview = togglePreview,
                    onOpen = {
                        target = RingtoneTarget.ForContact(
                            contact.contactId,
                            ContactOverrides.nameFor(contact)
                        )
                    }
                )
            }
        }
    }

    target?.let { current ->
        val contactId = when (current) {
            RingtoneTarget.General -> null
            is RingtoneTarget.ForContact -> current.contactId
        }
        val title = when (current) {
            RingtoneTarget.General -> "Általános csengőhang"
            is RingtoneTarget.ForContact -> current.name
        }

        RingtonePickerDialog(
            title = title,
            selectedUri = RingtoneSettings.choiceFor(contactId)?.uri,
            player = player,
            onDismiss = {
                player.stop()
                playingUri = null
                target = null
            },
            onSelect = { choice ->
                player.stop()
                playingUri = null
                if (contactId == null) {
                    RingtoneSettings.setDefault(choice)
                } else {
                    RingtoneSettings.setForContact(contactId, choice)
                }
                target = null
            }
        )
    }
}

// --------------------------------------------------------------------------- sorok

@Composable
private fun ContactRingtoneRow(
    contact: Contact,
    ownChoice: RingtoneSettings.Choice?,
    fallbackLabel: String,
    playingUri: String?,
    onTogglePreview: (Uri) -> Unit,
    onOpen: () -> Unit,
) {
    val own = ownChoice?.label
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = ContactOverrides.nameFor(contact),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = own ?: T.generalFallback(fallbackLabel),
                color = if (own != null) VividGreen else TextTertiary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (own != null) {
            FlatIconButton(
                iconRes = R.drawable.ic_delete,
                contentDescription = T.removeOwnRingtone,
                onClick = { RingtoneSettings.setForContact(contact.contactId, null) },
                size = 36.dp,
                iconSize = 15.dp,
                background = Surface2,
                tint = TextSecondary
            )
            Spacer(Modifier.width(6.dp))
        }
        FlatIconButton(
            iconRes = R.drawable.ic_music_note,
            contentDescription = T.pickRingtone,
            onClick = onOpen,
            size = 40.dp,
            iconSize = 18.dp,
            active = own != null,
            activeBackground = VividGreen,
            activeTint = com.bbetterdialer.ui.theme.OnAccent,
            background = Surface2,
            tint = TextSecondary
        )
    }
}

@Composable
private fun PreviewButton(
    uri: Uri,
    playingUri: String?,
    onToggle: (Uri) -> Unit,
) {
    val playing = playingUri == uri.toString()
    FlatIconButton(
        iconRes = if (playing) R.drawable.ic_stop else R.drawable.ic_play,
        contentDescription = if (playing) T.stopPlayback else T.preview,
        onClick = { onToggle(uri) },
        size = 40.dp,
        iconSize = 16.dp,
        background = Surface2,
        tint = if (playing) VividGreen else TextSecondary
    )
}

// --------------------------------------------------------------------------- választó

@Composable
private fun RingtonePickerDialog(
    title: String,
    selectedUri: String?,
    player: RingtonePlayer,
    onDismiss: () -> Unit,
    onSelect: (RingtoneSettings.Choice?) -> Unit,
) {
    val context = LocalContext.current
    val systemRingtones = remember { RingtoneSettings.systemRingtones(context) }
    var playingUri by remember { mutableStateOf<String?>(null) }
    player.onFinished = { playingUri = null }

    // Fájlkezelő integráció: a telefonon lévő zenék/fájlok kiválasztása.
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        onSelect(RingtoneSettings.Choice(uri.toString(), displayNameOf(context, uri)))
    }

    val togglePreview: (Uri) -> Unit = { uri ->
        val key = uri.toString()
        if (playingUri == key) {
            player.stop()
            playingUri = null
        } else {
            player.play(uri, loop = false)
            playingUri = key
        }
    }

    FlatDialog(onDismiss = { player.stop(); onDismiss() }) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = T.ringtonePickerHint,
            color = TextTertiary,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(14.dp))

        PickerOption(
            label = T.systemDefault,
            selected = selectedUri == null,
            playing = false,
            onClick = { onSelect(null) },
            onPreview = null
        )

        Spacer(Modifier.height(6.dp))

        PickerOption(
            label = T.pickFile,
            selected = false,
            playing = false,
            onClick = { filePicker.launch(arrayOf("audio/*")) },
            onPreview = null
        )

        Spacer(Modifier.height(14.dp))
        SectionLabel(T.builtInSounds)
        Spacer(Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
            items(items = systemRingtones, key = { it.uri.toString() }) { item ->
                PickerOption(
                    label = item.title,
                    selected = selectedUri == item.uri.toString(),
                    playing = playingUri == item.uri.toString(),
                    onClick = {
                        onSelect(RingtoneSettings.Choice(item.uri.toString(), item.title))
                    },
                    onPreview = { togglePreview(item.uri) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        FlatChip(
            label = T.close,
            selected = false,
            onClick = { player.stop(); onDismiss() }
        )
    }
}

@Composable
private fun PickerOption(
    label: String,
    selected: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onPreview: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Surface3 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (onPreview != null) {
            Icon(
                painter = painterResource(if (playing) R.drawable.ic_stop else R.drawable.ic_play),
                contentDescription = if (playing) T.stopPlayback else T.preview,
                tint = if (playing) VividGreen else TextTertiary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onPreview)
            )
            Spacer(Modifier.width(14.dp))
        }

        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = VividGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun displayNameOf(context: Context, uri: Uri): String = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
}.getOrNull()?.takeIf { it.isNotBlank() } ?: "Kiválasztott fájl"
