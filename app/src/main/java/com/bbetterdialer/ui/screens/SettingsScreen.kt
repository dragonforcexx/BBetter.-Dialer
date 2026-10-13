package com.bbetterdialer.ui.screens

import com.bbetterdialer.ui.T

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.R
import com.bbetterdialer.data.AppLanguage
import com.bbetterdialer.data.AppPreferences
import com.bbetterdialer.data.Contact
import com.bbetterdialer.data.ContactOverrides
import com.bbetterdialer.data.LauncherIcon
import com.bbetterdialer.data.LauncherName
import com.bbetterdialer.data.ThemeMode
import com.bbetterdialer.ui.design.ContactAvatar
import com.bbetterdialer.ui.design.FlatChip
import com.bbetterdialer.ui.design.FlatIconButton
import com.bbetterdialer.ui.design.FlatSearchField
import com.bbetterdialer.ui.design.PillBadge
import com.bbetterdialer.ui.design.SectionLabel
import com.bbetterdialer.ui.initialsOf
import com.bbetterdialer.ui.rememberContactPhoto
import com.bbetterdialer.ui.theme.AppBg
import com.bbetterdialer.ui.theme.EndRed
import com.bbetterdialer.ui.theme.OnAccent
import com.bbetterdialer.ui.theme.Surface1
import com.bbetterdialer.ui.theme.Surface2
import com.bbetterdialer.ui.theme.Surface3
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextSecondary
import com.bbetterdialer.ui.theme.TextTertiary
import com.bbetterdialer.ui.theme.VividGreen

/** A beállításokon belüli oldalak. */
enum class SettingsPage { ROOT, NAMES, PHOTOS, COLORS, RINGTONES, THEME, LANGUAGE, CUSTOMIZATION }

/**
 * Teljes képernyős beállítások. Saját belső navigációval (gyökér → aloldalak),
 * a rendszer vissza gombját is kezelve.
 */
@Composable
fun SettingsScreen(
    contacts: List<Contact>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableStateOf(SettingsPage.ROOT) }

    BackHandler(enabled = true) {
        if (page == SettingsPage.ROOT) onBack() else page = SettingsPage.ROOT
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
    ) {
        when (page) {
            SettingsPage.ROOT -> SettingsRoot(onBack = onBack, onOpen = { page = it })
            SettingsPage.NAMES -> NameSettingsScreen(contacts = contacts, onBack = { page = SettingsPage.ROOT })
            SettingsPage.PHOTOS -> PhotoSettingsScreen(contacts = contacts, onBack = { page = SettingsPage.ROOT })
            SettingsPage.COLORS -> ColorSettingsScreen(contacts = contacts, onBack = { page = SettingsPage.ROOT })
            SettingsPage.RINGTONES -> RingtoneSettingsScreen(contacts = contacts, onBack = { page = SettingsPage.ROOT })
            SettingsPage.THEME -> ThemeSettingsScreen(onBack = { page = SettingsPage.ROOT })
            SettingsPage.LANGUAGE -> LanguageSettingsScreen(onBack = { page = SettingsPage.ROOT })
            SettingsPage.CUSTOMIZATION -> CustomizationScreen(onBack = { page = SettingsPage.ROOT })
        }
    }
}

// --------------------------------------------------------------------------- gyökér

@Composable
private fun SettingsRoot(
    onBack: () -> Unit,
    onOpen: (SettingsPage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsTopBar(title = T.settings, onBack = onBack)

        Spacer(Modifier.height(10.dp))

        SectionLabel(T.sectionAppearance, Modifier.padding(start = 22.dp, bottom = 8.dp))
        SettingsRow(
            iconRes = R.drawable.ic_palette,
            title = T.theme,
            subtitle = currentThemeLabel(),
            onClick = { onOpen(SettingsPage.THEME) }
        )
        Spacer(Modifier.height(8.dp))
        SettingsRow(
            iconRes = R.drawable.ic_settings,
            title = T.language,
            subtitle = currentLanguageLabel(),
            onClick = { onOpen(SettingsPage.LANGUAGE) }
        )
        Spacer(Modifier.height(8.dp))
        SettingsRow(
            iconRes = R.drawable.ic_edit,
            title = T.customization,
            subtitle = AppPreferences.appName + " · " + AppPreferences.launcherName.label,
            onClick = { onOpen(SettingsPage.CUSTOMIZATION) }
        )

        Spacer(Modifier.height(22.dp))

        SectionLabel(T.sectionContacts, Modifier.padding(start = 22.dp, bottom = 8.dp))
        SettingsRow(R.drawable.ic_edit, T.name, T.nameSub) { onOpen(SettingsPage.NAMES) }
        Spacer(Modifier.height(8.dp))
        SettingsRow(R.drawable.ic_image, T.profilePhotos, T.profilePhotosSub) { onOpen(SettingsPage.PHOTOS) }
        Spacer(Modifier.height(8.dp))
        SettingsRow(R.drawable.ic_palette, T.profileColors, T.profileColorsSub) { onOpen(SettingsPage.COLORS) }

        Spacer(Modifier.height(22.dp))

        SectionLabel(T.sectionSounds, Modifier.padding(start = 22.dp, bottom = 8.dp))
        SettingsRow(R.drawable.ic_music_note, T.ringtones, T.ringtonesSub) { onOpen(SettingsPage.RINGTONES) }

        Spacer(Modifier.height(28.dp))
    }
}

private fun currentThemeLabel(): String = when (AppPreferences.themeMode) {
    ThemeMode.SYSTEM -> T.themeSystem
    ThemeMode.DARK -> T.themeDark
    ThemeMode.LIGHT -> T.themeLight
}

private fun currentLanguageLabel(): String = when (AppPreferences.language) {
    AppLanguage.HU -> "Magyar"
    AppLanguage.EN -> "English"
}

// --------------------------------------------------------------------------- téma

@Composable
private fun ThemeSettingsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        SettingsTopBar(title = T.theme, onBack = onBack)

        Spacer(Modifier.height(8.dp))

        SelectableRow(
            label = T.themeSystem,
            selected = AppPreferences.themeMode == ThemeMode.SYSTEM,
            onClick = { AppPreferences.applyThemeMode(ThemeMode.SYSTEM) }
        )
        Spacer(Modifier.height(8.dp))
        SelectableRow(
            label = T.themeDark,
            selected = AppPreferences.themeMode == ThemeMode.DARK,
            onClick = { AppPreferences.applyThemeMode(ThemeMode.DARK) }
        )
        Spacer(Modifier.height(8.dp))
        SelectableRow(
            label = T.themeLight,
            selected = AppPreferences.themeMode == ThemeMode.LIGHT,
            onClick = { AppPreferences.applyThemeMode(ThemeMode.LIGHT) }
        )
    }
}

// --------------------------------------------------------------------------- nyelv

@Composable
private fun LanguageSettingsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        SettingsTopBar(title = T.language, onBack = onBack)

        Spacer(Modifier.height(8.dp))

        SelectableRow(
            label = "Magyar",
            selected = AppPreferences.language == AppLanguage.HU,
            onClick = { AppPreferences.applyLanguage(AppLanguage.HU) }
        )
        Spacer(Modifier.height(8.dp))
        SelectableRow(
            label = "English",
            selected = AppPreferences.language == AppLanguage.EN,
            onClick = { AppPreferences.applyLanguage(AppLanguage.EN) }
        )
    }
}

// --------------------------------------------------------------------------- testreszabás

@Composable
private fun CustomizationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(AppPreferences.appName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsTopBar(title = T.customization, onBack = onBack)

        Spacer(Modifier.height(8.dp))

        SectionLabel(T.appName, Modifier.padding(start = 22.dp, bottom = 8.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            FlatSearchField(
                value = name,
                onValueChange = { name = it },
                placeholder = T.appName,
                showIcon = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                FlatChip(
                    label = T.save,
                    selected = true,
                    accent = VividGreen,
                    onClick = { AppPreferences.applyAppName(name) }
                )
                Spacer(Modifier.width(8.dp))
                FlatChip(
                    label = T.appNameNone,
                    selected = AppPreferences.appName.isBlank(),
                    accent = VividGreen,
                    onClick = {
                        name = ""
                        AppPreferences.applyAppName("")
                    }
                )
                Spacer(Modifier.width(8.dp))
                FlatChip(
                    label = T.reset,
                    selected = false,
                    onClick = {
                        name = AppPreferences.DEFAULT_APP_NAME
                        AppPreferences.applyAppName(AppPreferences.DEFAULT_APP_NAME)
                    }
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(text = T.appNameHint, color = TextTertiary, fontSize = 11.sp)
        }

        Spacer(Modifier.height(24.dp))

        SectionLabel(T.launcherName, Modifier.padding(start = 22.dp, bottom = 8.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            LauncherName.entries.forEach { entry ->
                SelectableRow(
                    label = entry.label,
                    selected = AppPreferences.launcherName == entry,
                    onClick = { AppPreferences.setLauncherName(context, entry) }
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(text = T.launcherNameHint, color = TextTertiary, fontSize = 11.sp)
        }

        Spacer(Modifier.height(24.dp))

        SectionLabel(T.launcherIcon, Modifier.padding(start = 22.dp, bottom = 8.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            IconOptionRow(
                label = T.iconGreenOnBlack,
                selected = AppPreferences.launcherIcon == LauncherIcon.GREEN_PHONE_ON_BLACK,
                previewBackground = Color(0xFF000000),
                previewIcon = VividGreen,
                onClick = { AppPreferences.setLauncherIcon(context, LauncherIcon.GREEN_PHONE_ON_BLACK) }
            )
            Spacer(Modifier.height(8.dp))
            IconOptionRow(
                label = T.iconWhiteOnGreen,
                selected = AppPreferences.launcherIcon == LauncherIcon.WHITE_PHONE_ON_GREEN,
                previewBackground = VividGreen,
                previewIcon = Color(0xFFFFFFFF),
                onClick = { AppPreferences.setLauncherIcon(context, LauncherIcon.WHITE_PHONE_ON_GREEN) }
            )
            Spacer(Modifier.height(8.dp))
            Text(text = T.launcherApplied, color = TextTertiary, fontSize = 11.sp)
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun IconOptionRow(
    label: String,
    selected: Boolean,
    previewBackground: Color,
    previewIcon: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Surface3 else Surface1)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(previewBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_call),
                contentDescription = null,
                tint = previewIcon,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
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

@Composable
private fun SelectableRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Surface3 else Surface1)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
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

// --------------------------------------------------------------------------- közös elemek

@Composable
fun SettingsTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlatIconButton(
            iconRes = R.drawable.ic_arrow_back,
            contentDescription = T.back,
            onClick = onBack,
            size = 42.dp,
            iconSize = 18.dp,
            background = Color.Transparent,
            tint = TextPrimary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingsRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, color = TextTertiary, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Egy névjegy sora a beállítások aloldalain: avatar (a felülírásokkal),
 * név, másodlagos sor és opcionális jelvény + műveletek.
 */
@Composable
internal fun SettingsContactRow(
    contact: Contact,
    subtitle: String,
    badge: String? = null,
    badgeHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit,
) {
    val displayName = ContactOverrides.nameFor(contact)
    val colorOverride = ContactOverrides.colorFor(contact.contactId)
    val photo = rememberContactPhoto(contact.photoUri, contact.contactId, sizePx = 192)

    FlatContactCard(onClick = onClick) {
        ContactAvatar(
            image = photo,
            initials = initialsOf(displayName),
            size = 44.dp,
            colorOverride = colorOverride
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = displayName,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    PillBadge(text = badge.uppercase(), highlighted = badgeHighlighted)
                }
            }
        }
        trailing()
    }
}

@Composable
private fun FlatContactCard(
    onClick: (() -> Unit)?,
    content: @Composable RowScope.() -> Unit,
) {
    val base = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(Surface1)

    Row(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base)
            .padding(PaddingValues(horizontal = 14.dp, vertical = 10.dp)),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
