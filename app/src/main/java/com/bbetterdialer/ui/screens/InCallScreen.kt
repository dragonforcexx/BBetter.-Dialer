package com.bbetterdialer.ui.screens

import com.bbetterdialer.ui.T

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.R
import com.bbetterdialer.data.CallStatus
import com.bbetterdialer.data.ContactOverrides
import com.bbetterdialer.data.InCallState
import com.bbetterdialer.telecom.CallActions
import com.bbetterdialer.ui.design.AnswerButton
import com.bbetterdialer.ui.design.ContactAvatar
import com.bbetterdialer.ui.design.EndCallButton
import com.bbetterdialer.ui.design.FlatIconButton
import com.bbetterdialer.ui.design.FolderBadge
import com.bbetterdialer.ui.formatDial
import com.bbetterdialer.ui.formatDuration
import com.bbetterdialer.ui.initialsOf
import com.bbetterdialer.ui.rememberContactPhoto
import com.bbetterdialer.ui.theme.AppBg
import com.bbetterdialer.ui.theme.EndRed
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextSecondary
import com.bbetterdialer.ui.theme.TextTertiary
import com.bbetterdialer.ui.theme.VividGreen
import kotlinx.coroutines.delay

/**
 * ===========================================================================
 *  HÍVÁSKÉPERNYŐ – flat / AMOLED
 * ===========================================================================
 *  A profilkép **sima, éles kör** ([ContactAvatar]) – nem olvad bele a
 *  háttérbe, és nincs semmilyen animált gyűrű a peremén.
 *
 *  A hívó fél **mappacímkéje** (pl. CSALÁD) közvetlenül a profilkép felett
 *  jelenik meg, fehér kiemeléssel – így azonos nevű ismerősök vagy céges
 *  hívások azonnal megkülönböztethetők.
 * ===========================================================================
 */
@Composable
fun InCallScreen(
    state: InCallState,
    actions: CallActions?,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolved = rememberContactPhoto(state.photoUri, state.contactId, sizePx = 720)
    val photo = state.photo ?: resolved
    val ringing = state.status == CallStatus.RINGING
    val displayName = ContactOverrides.nameFor(state.contactId, state.name)
    val accent = ContactOverrides.colorFor(state.contactId)
        ?: if (ringing) VividGreen else VividGreen

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.status) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }
    val elapsed = if (state.status == CallStatus.ACTIVE && state.startedAtMillis > 0L) {
        ((now - state.startedAtMillis) / 1000L).coerceAtLeast(0L)
    } else 0L

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusLabel(state.status),
                    color = if (ringing) VividGreen else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.weight(1f))
                FlatIconButton(
                    iconRes = R.drawable.ic_close,
                    contentDescription = T.hideCallScreen,
                    onClick = onMinimize,
                    size = 38.dp,
                    iconSize = 15.dp,
                    background = com.bbetterdialer.ui.theme.Surface2,
                    tint = TextSecondary
                )
            }

            Spacer(Modifier.weight(0.5f))

            // ---- Mappacímke a profilkép felett ----------------------------
            if (state.folder != null) {
                FolderBadge(folder = state.folder, highlighted = true)
                Spacer(Modifier.height(12.dp))
            }

            // ---- Profilkép: sima, éles kör
            ContactAvatar(
                image = photo,
                initials = initialsOf(displayName ?: state.number),
                size = 200.dp,
                accent = accent,
                contentDescription = displayName
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = displayName?.takeIf { it.isNotBlank() } ?: formatDial(state.number),
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            if (!displayName.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatDial(state.number),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = if (elapsed > 0) formatDuration(elapsed) else statusHint(state.status),
                color = TextTertiary,
                fontSize = 15.sp,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.weight(1f))

            if (ringing) {
                // Bejövő hívás: piros elutasítás + zöld fogadás
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BigActionLabel(label = T.decline, color = EndRed) {
                        EndCallButton(onClick = { actions?.end() }, size = 70.dp)
                    }
                    BigActionLabel(label = T.answer, color = VividGreen) {
                        AnswerButton(onClick = { actions?.answer() }, size = 70.dp)
                    }
                }
                Spacer(Modifier.height(28.dp))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallControl(
                        iconRes = if (state.muted) R.drawable.ic_mic_off else R.drawable.ic_mic,
                        label = T.mute,
                        active = state.muted,
                        onClick = { actions?.setMuted(!state.muted) }
                    )
                    CallControl(
                        iconRes = R.drawable.ic_volume_up,
                        label = T.speaker,
                        active = state.speaker,
                        onClick = { actions?.setSpeaker(!state.speaker) }
                    )
                }

                Spacer(Modifier.height(26.dp))

                EndCallButton(onClick = { actions?.end() }, size = 70.dp)

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun CallControl(
    iconRes: Int,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FlatIconButton(
            iconRes = iconRes,
            contentDescription = label,
            onClick = onClick,
            size = 56.dp,
            iconSize = 22.dp,
            active = active,
            activeBackground = TextPrimary,
            activeTint = AppBg
        )
        Spacer(Modifier.height(8.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun BigActionLabel(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    button: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        button()
        Spacer(Modifier.height(10.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun statusLabel(status: CallStatus): String = when (status) {
    CallStatus.DIALING -> T.statusDialing
    CallStatus.RINGING -> T.statusIncoming
    CallStatus.ACTIVE -> T.statusActive
    CallStatus.HOLDING -> T.statusHolding
    CallStatus.ENDED -> T.statusEnded
}

private fun statusHint(status: CallStatus): String = when (status) {
    CallStatus.DIALING -> T.hintConnecting
    CallStatus.RINGING -> T.hintRinging
    CallStatus.ACTIVE -> "00:00"
    CallStatus.HOLDING -> T.hintHolding
    CallStatus.ENDED -> T.hintEnded
}
