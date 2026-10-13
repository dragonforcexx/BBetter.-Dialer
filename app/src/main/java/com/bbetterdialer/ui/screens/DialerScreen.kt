package com.bbetterdialer.ui.screens

import com.bbetterdialer.ui.T

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.R
import com.bbetterdialer.data.Contact
import com.bbetterdialer.data.ContactFolders
import com.bbetterdialer.data.ContactOverrides
import com.bbetterdialer.data.CountryCodes
import com.bbetterdialer.ui.digitsOnly
import com.bbetterdialer.ui.design.AddContactPill
import com.bbetterdialer.ui.design.CallButton
import com.bbetterdialer.ui.design.FlatIconButton
import com.bbetterdialer.ui.design.FolderBadge
import com.bbetterdialer.ui.formatDial
import com.bbetterdialer.ui.theme.Surface2
import com.bbetterdialer.ui.theme.Surface3
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextSecondary
import com.bbetterdialer.ui.theme.TextTertiary

/**
 * Tárcsázó fül – lapos, AMOLED billentyűzet.
 *
 *  • A billentyűzet lejjebb került, hogy hüvelykujjal kényelmesen elérhető legyen.
 *  • A `0` hosszú nyomásra `+` jelet ír be (nemzetközi hívószámokhoz).
 *  • A kijelző fölött balra az **ország neve** látszik, a beírt előhívó
 *    alapján felismerve; jobbra a találat neve és mappája.
 *  • A hívásgomb üres kijelzőnél sötétzöld, szám beírásakor élénk zöldre vált,
 *    megnő és rugós animációval ugrik át.
 */
@Composable
fun DialerScreen(
    number: String,
    matched: Contact?,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onCall: () -> Unit,
    /** Ha igaz, a "Névjegy hozzáadása" pirula itt, az országsorban jelenik meg. */
    addContactEnabled: Boolean = false,
    onAddContact: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // A pirula csak ismeretlen, legalább 3 jegyű számnál jelenik meg
    val showAddContact = addContactEnabled && number.digitsOnly().length >= 3 && matched == null
    val homeCountry = remember(context) { CountryCodes.homeCountry(context) }
    val country = remember(number, homeCountry) { CountryCodes.detect(number, homeCountry) }
    val countryLabel = country?.let { c ->
        val flag = CountryCodes.flagOf(c.iso)
        if (flag.isBlank()) c.name.uppercase() else "$flag  ${c.name.uppercase()}"
    }.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.35f))

        // Ország (balra) + a beírt számhoz talált névjegy (jobbra)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = countryLabel,
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (matched != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ContactOverrides.nameFor(matched),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val folder = ContactFolders.folderOf(matched.contactId)
                    if (folder != null) {
                        Spacer(Modifier.width(8.dp))
                        FolderBadge(folder = folder)
                    }
                }
            } else {
                // Új névjegy a beírt, ismeretlen számhoz
                AddContactPill(visible = showAddContact, onClick = onAddContact)
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = if (number.isBlank()) T.dial else formatDial(number),
            fontSize = if (number.length > 11) 26.sp else 34.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 1.5.sp,
            color = if (number.isBlank()) TextTertiary else TextPrimary,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Nagyobb lég a kijelző és a billentyűzet között -> a gombok lejjebb vannak.
        Spacer(Modifier.weight(1.45f))

        DialPad(onDigit = onDigit)

        Spacer(Modifier.weight(0.45f))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Szimmetria kedvéért üres hely a hívásgomb bal oldalán.
                Spacer(Modifier.size(50.dp))

                CallButton(
                    onClick = onCall,
                    active = number.isNotBlank()
                )

                FlatIconButton(
                    iconRes = R.drawable.ic_backspace,
                    contentDescription = T.deleteLastDigit,
                    onClick = onBackspace,
                    size = 50.dp,
                    iconSize = 20.dp,
                    background = Surface2,
                    tint = TextSecondary,
                    enabled = number.isNotEmpty()
                )
            }
        }

        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun DialPad(onDigit: (String) -> Unit) {
    val rows = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Column(
        modifier = Modifier
            .widthIn(max = 268.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (digit, letters) ->
                    DialKey(
                        digit = digit,
                        letters = letters,
                        // A 0 hosszú nyomásra + jelet ír be – nemzetközi számokhoz.
                        onLongPress = if (digit == "0") ({ onDigit("+") }) else null,
                        onClick = { onDigit(digit) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Egyetlen lapos tárcsázógomb – nyomáskor csak a háttérszín vált. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKey(
    digit: String,
    letters: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (pressed) Surface3 else Surface2)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onLongClick = onLongPress,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal,
                color = TextPrimary
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    fontSize = 8.sp,
                    letterSpacing = 1.2.sp,
                    color = TextTertiary
                )
            }
        }
    }
}
