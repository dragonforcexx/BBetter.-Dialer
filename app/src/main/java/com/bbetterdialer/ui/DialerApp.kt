package com.bbetterdialer.ui

import com.bbetterdialer.ui.T

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.R
import com.bbetterdialer.data.AppPreferences
import com.bbetterdialer.data.CallEntry
import com.bbetterdialer.data.Contact
import com.bbetterdialer.data.ContactCreator
import com.bbetterdialer.data.ContactOverrides
import com.bbetterdialer.data.InCallState
import com.bbetterdialer.data.PhoneRepository
import com.bbetterdialer.telecom.CallController
import com.bbetterdialer.telecom.InCallBridge
import com.bbetterdialer.ui.design.AddContactDialog
import com.bbetterdialer.ui.design.AddContactPill
import com.bbetterdialer.ui.design.AppBackground
import com.bbetterdialer.ui.design.DrawerHeader
import com.bbetterdialer.ui.design.DrawerItem
import com.bbetterdialer.ui.design.FlatIconButton
import com.bbetterdialer.ui.design.SideDrawer
import com.bbetterdialer.ui.screens.ContactsScreen
import com.bbetterdialer.ui.screens.DialerScreen
import com.bbetterdialer.ui.screens.HistoryScreen
import com.bbetterdialer.ui.screens.InCallScreen
import com.bbetterdialer.ui.screens.SettingsScreen
import com.bbetterdialer.ui.theme.AppBg
import com.bbetterdialer.ui.theme.DialerTheme
import com.bbetterdialer.ui.theme.Surface1
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextSecondary
import com.bbetterdialer.ui.theme.TextTertiary
import com.bbetterdialer.ui.theme.VividGreen
import kotlinx.coroutines.launch

/**
 * Alsó fülek – pontosan három, ebben a sorrendben.
 * Az alapértelmezett fül a Kontaktok.
 */
enum class DialerTab(val labelOf: () -> String) {
    CONTACTS({ T.tabContacts }),
    KEYPAD({ T.tabKeypad }),
    HISTORY({ T.tabHistory })
}

/**
 * Az app gyökere: téma, jogosultságok, adatbetöltés, fülek, oldalsó fiók,
 * beállítások és a hívás overlay.
 */
@Composable
fun DialerApp(
    pendingNumber: String = "",
    onPendingNumberConsumed: () -> Unit = {},
    showInCallRequest: Boolean = false,
    onShowInCallConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    DialerTheme {
        // Induláskor a Kontaktok fület mutatjuk.
        var tab by rememberSaveable { mutableStateOf(DialerTab.CONTACTS) }
        var dialed by rememberSaveable { mutableStateOf("") }
        var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
        var history by remember { mutableStateOf<List<CallEntry>>(emptyList()) }
        var reloadToken by remember { mutableStateOf(0) }
        var minimized by remember { mutableStateOf(false) }
        var drawerOpen by remember { mutableStateOf(false) }
        var settingsOpen by remember { mutableStateOf(false) }
        var addContactNumber by remember { mutableStateOf<String?>(null) }
        var statusMessage by remember { mutableStateOf<String?>(null) }

        val inCall = InCallBridge.state

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { reloadToken++ }

        LaunchedEffect(Unit) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.WRITE_CONTACTS
                )
            )
        }

        LaunchedEffect(reloadToken) {
            contacts = PhoneRepository.loadContacts(context.contentResolver)
            history = PhoneRepository.loadRecentCalls(context.contentResolver)
        }

        // Külső indítás: tel: link vagy ACTION_DIAL
        LaunchedEffect(pendingNumber) {
            if (pendingNumber.isNotBlank()) {
                dialed = pendingNumber
                tab = DialerTab.KEYPAD
                onPendingNumberConsumed()
            }
        }

        LaunchedEffect(showInCallRequest) {
            if (showInCallRequest) {
                minimized = false
                onShowInCallConsumed()
            }
        }

        LaunchedEffect(inCall != null) {
            if (inCall != null) {
                drawerOpen = false
                settingsOpen = false
                statusMessage = null
            } else {
                minimized = false
            }
        }

        // Névjegy-egyeztetés a beírt számhoz
        val matched = remember(dialed, contacts) {
            val digits = dialed.digitsOnly()
            if (digits.length < 3) null
            else contacts.firstOrNull { it.number.digitsOnly().endsWith(digits) }
        }

        // Ismeretlen szám -> felajánljuk az új névjegy létrehozását
        val digitsTyped = dialed.digitsOnly()
        val offerAddContact = tab == DialerTab.KEYPAD && digitsTyped.length >= 3 && matched == null

        val placeCall: (String) -> Unit = { value -> CallController.place(context, value) }

        Box(Modifier.fillMaxSize()) {
            AppBackground {
                if (inCall != null && !minimized) {
                    InCallScreen(
                        state = inCall,
                        actions = InCallBridge.actions,
                        onMinimize = { minimized = true }
                    )
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        if (minimized && inCall != null) {
                            ActiveCallBanner(state = inCall, onReturn = { minimized = false })
                        }

                        TopBar(
                            onMenu = { drawerOpen = true },
                            showAddContact = offerAddContact && AppPreferences.appName.isBlank(),
                            onAddContact = { addContactNumber = dialed }
                        )

                        statusMessage?.let { message ->
                            Text(
                                text = message,
                                color = VividGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                            )
                        }

                        // Lágy átmenet a három képernyő között: áttűnés + finom
                        // vízszintes eltolás, aminek az irányát a fülek sorrendje adja.
                        AnimatedContent<DialerTab>(
                            targetState = tab,
                            transitionSpec = { tabTransition(initialState, targetState) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            label = "tabContent"
                        ) { targetTab ->
                            when (targetTab) {
                                DialerTab.CONTACTS -> ContactsScreen(
                                    contacts = contacts,
                                    hasPermission = PhoneRepository.hasContacts(context),
                                    onCall = { contact -> placeCall(contact.number) },
                                    onPick = { contact ->
                                        dialed = contact.number
                                        tab = DialerTab.KEYPAD
                                    }
                                )

                                DialerTab.KEYPAD -> DialerScreen(
                                    number = dialed,
                                    matched = matched,
                                    onDigit = { digit -> dialed += digit },
                                    onBackspace = { dialed = dialed.dropLast(1) },
                                    onCall = { placeCall(dialed) },
                                    addContactEnabled = AppPreferences.appName.isNotBlank(),
                                    onAddContact = { addContactNumber = dialed }
                                )

                                DialerTab.HISTORY -> HistoryScreen(
                                    entries = history,
                                    contacts = contacts,
                                    hasPermission = PhoneRepository.hasCallLog(context),
                                    onCall = { number -> placeCall(number) },
                                    onPick = { number ->
                                        dialed = number
                                        tab = DialerTab.KEYPAD
                                    }
                                )
                            }
                        }

                        BottomTabs(selected = tab, onSelect = { tab = it })
                    }
                }

                // ---- Oldalsó fiók ------------------------------------------
                SideDrawer(open = drawerOpen, onClose = { drawerOpen = false }) {
                    DrawerHeader(
                        title = AppPreferences.appName.removeSuffix(".").ifBlank { "Dialer" },
                        subtitle = "AMOLED · MINIMAL"
                    )
                    Spacer(Modifier.height(6.dp))
                    DrawerItem(
                        iconRes = R.drawable.ic_settings,
                        label = T.settings,
                        onClick = {
                            drawerOpen = false
                            settingsOpen = true
                        }
                    )
                }

                // ---- Beállítások (teljes képernyő) -------------------------
                if (settingsOpen) {
                    SettingsScreen(
                        contacts = contacts,
                        onBack = { settingsOpen = false }
                    )
                }

                // ---- Új névjegy ablaka -------------------------------------
                addContactNumber?.let { number ->
                    AddContactDialog(
                        number = number,
                        onDismiss = { addContactNumber = null },
                        onSave = { name, color ->
                            addContactNumber = null
                            scope.launch {
                                when (val result = ContactCreator.create(context, name, number)) {
                                    is ContactCreator.Result.Success -> {
                                        if (color != null) {
                                            ContactOverrides.setColor(result.contactId, color)
                                        }
                                        statusMessage = T.contactSaved
                                        dialed = ""
                                        reloadToken++
                                        tab = DialerTab.CONTACTS
                                    }
                                    is ContactCreator.Result.Failure -> {
                                        statusMessage = T.contactSaveFailed
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * A fülváltás animációja.
 *
 * A kimenő képernyő elhalványul és finoman balra/Jobbra csúszik, az új
 * pedig áttűnéssel, enyhe méretváltással érkezik. Az irányt a fülek
 * sorrendje adja, ezért a váltás "lapozás" érzetű.
 */
private fun tabTransition(from: DialerTab, to: DialerTab): ContentTransform {
    val direction = if (to.ordinal >= from.ordinal) 1 else -1
    val enter = fadeIn(tween(durationMillis = 220)) +
        slideInHorizontally(tween(durationMillis = 320)) { width -> direction * width / 7 } +
        scaleIn(tween(durationMillis = 320), initialScale = 0.96f)
    val exit = fadeOut(tween(durationMillis = 160)) +
        slideOutHorizontally(tween(durationMillis = 320)) { width -> -direction * width / 10 } +
        scaleOut(tween(durationMillis = 320), targetScale = 0.98f)
    return enter.togetherWith(exit)
}

// --------------------------------------------------------------------------- felső sáv

/**
 * Fejléc: balra a hamburger, középen halványan az app neve, jobbra pedig –
 * ha a beírt szám ismeretlen – a "＋ Kontakt hozzáadása" pirula.
 */
@Composable
private fun TopBar(
    onMenu: () -> Unit,
    showAddContact: Boolean,
    onAddContact: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(46.dp),
        contentAlignment = Alignment.Center
    ) {
        FlatIconButton(
            iconRes = R.drawable.ic_menu,
            contentDescription = T.menu,
            onClick = onMenu,
            size = 42.dp,
            iconSize = 20.dp,
            background = Color.Transparent,
            tint = TextPrimary,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        // A középső felirat csak akkor látszik, ha van szöveg
        if (AppPreferences.appName.isNotBlank()) {
            Text(
                text = AppPreferences.appName,
                color = TextTertiary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                maxLines = 1,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        AddContactPill(
            visible = showAddContact,
            onClick = onAddContact,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun ActiveCallBanner(
    state: InCallState,
    onReturn: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .clickable(onClick = onReturn)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(VividGreen)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = ContactOverrides.nameFor(state.contactId, state.name)
                ?.takeIf { it.isNotBlank() } ?: formatDial(state.number),
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        Text(text = T.activeCallBanner, color = TextSecondary, fontSize = 12.sp)
    }
}

// --------------------------------------------------------------------------- alsó fülek

/**
 * Alsó navigáció csúszó kapszula-indikátorral.
 * A fehér kapszula rugós (spring) animációval csúszik a kiválasztott fül alá.
 */
@Composable
private fun BottomTabs(
    selected: DialerTab,
    onSelect: (DialerTab) -> Unit,
) {
    val tabs = DialerTab.entries

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Surface1)
            .padding(5.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            val tabWidth = maxWidth / tabs.size
            val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)

            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring<Dp>(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "tabIndicatorOffset"
            )

            Box(
                Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(23.dp))
                    .background(TextPrimary)
            )

            Row(Modifier.fillMaxSize()) {
                tabs.forEach { item ->
                    TabItem(
                        label = item.labelOf(),
                        active = item == selected,
                        onClick = { onSelect(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val labelColor by animateColorAsState(
        targetValue = if (active) AppBg else TextSecondary,
        animationSpec = tween(durationMillis = 220),
        label = "tabLabelColor"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(23.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
