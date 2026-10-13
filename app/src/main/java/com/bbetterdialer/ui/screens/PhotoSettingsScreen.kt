package com.bbetterdialer.ui.screens

import com.bbetterdialer.ui.T

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.R
import com.bbetterdialer.data.AvatarOverrides
import com.bbetterdialer.data.Contact
import com.bbetterdialer.ui.design.AvatarCropperDialog
import com.bbetterdialer.ui.design.FlatIconButton
import com.bbetterdialer.ui.design.FlatSearchField
import com.bbetterdialer.ui.design.SectionLabel
import com.bbetterdialer.ui.formatDial
import com.bbetterdialer.ui.matchesQuery
import com.bbetterdialer.ui.theme.OnAccent
import com.bbetterdialer.ui.theme.Surface2
import com.bbetterdialer.ui.theme.TextSecondary
import com.bbetterdialer.ui.theme.TextTertiary
import com.bbetterdialer.ui.theme.VividGreen
import kotlinx.coroutines.launch

/**
 * Profilkép-kezelés.
 *
 * A kiválasztott kép egy **kijelölőben** jelenik meg, ahol húzással és
 * csippentéssel állítható be, melyik rész kerüljön a körbe. A mentett kép
 * 1024 px-es, kiváló minőségű JPEG, így a névjegyek profilképe nem lesz
 * pixeles – sem a listában, sem a hívásképernyőn.
 */
@Composable
fun PhotoSettingsScreen(
    contacts: List<Contact>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var pendingContactId by remember { mutableStateOf<Long?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var cropping by remember { mutableStateOf<ImageBitmap?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val avatarVersion = AvatarOverrides.version
    val filtered = remember(query, contacts, avatarVersion) {
        contacts.filter { it.matchesQuery(query) }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null || pendingContactId == null) {
            pendingContactId = null
            return@rememberLauncherForActivityResult
        }
        pendingUri = uri
        busy = true
        scope.launch {
            val bitmap = AvatarOverrides.loadForEditing(context, uri)
            busy = false
            if (bitmap == null) {
                status = T.imageUnreadable
                pendingContactId = null
                pendingUri = null
            } else {
                cropping = bitmap
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        SettingsTopBar(title = T.profilePhotos, onBack = onBack)

        Text(
            text = T.profilePhotosHint,
            color = TextTertiary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)
        )

        if (status != null || busy) {
            Text(
                text = if (busy) T.working else status.orEmpty(),
                color = if (busy) TextTertiary else VividGreen,
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
                    val hasOverride = AvatarOverrides.hasOverride(contact.contactId)

                    SettingsContactRow(
                        contact = contact,
                        subtitle = formatDial(contact.number),
                        badge = if (hasOverride) T.customPhotoBadge else null,
                        badgeHighlighted = hasOverride,
                        onClick = {
                            status = null
                            pendingContactId = contact.contactId
                            picker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    ) {
                        if (hasOverride) {
                            FlatIconButton(
                                iconRes = R.drawable.ic_delete,
                                contentDescription = T.resetPhoto,
                                onClick = {
                                    AvatarOverrides.clear(contact.contactId)
                                    status = T.photoReset
                                },
                                size = 38.dp,
                                iconSize = 16.dp,
                                background = Surface2,
                                tint = TextSecondary
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        FlatIconButton(
                            iconRes = R.drawable.ic_image,
                            contentDescription = T.changePhoto,
                            onClick = {
                                status = null
                                pendingContactId = contact.contactId
                                picker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
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

    // ---- Kijelölő: húzás + csippentés, majd mentés --------------------------
    val cropImage = cropping
    val cropContactId = pendingContactId
    if (cropImage != null && cropContactId != null) {
        AvatarCropperDialog(
            image = cropImage,
            onCancel = {
                cropping = null
                pendingContactId = null
                pendingUri = null
            },
            onConfirm = { crop ->
                cropping = null
                val uri = pendingUri
                pendingContactId = null
                pendingUri = null
                if (uri == null) return@AvatarCropperDialog

                busy = true
                scope.launch {
                    status = when (val result = AvatarOverrides.apply(context, cropContactId, uri, crop)) {
                        is AvatarOverrides.Result.Success ->
                            if (result.systemUpdated) T.photoSavedSystem
                            else T.photoSavedLocal
                        is AvatarOverrides.Result.Failure -> result.message
                    }
                    busy = false
                }
            }
        )
    }
}
