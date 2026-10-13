package com.bbetterdialer.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.compose.ui.graphics.ImageBitmap
import com.bbetterdialer.MainActivity
import com.bbetterdialer.data.CallStatus
import com.bbetterdialer.data.ContactFolders
import com.bbetterdialer.data.ContactOverrides
import com.bbetterdialer.data.InCallState
import com.bbetterdialer.data.PhoneRepository
import com.bbetterdialer.data.RingtoneSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ===========================================================================
 *  TELECOM INTEGRÁCIÓ – InCallService
 * ===========================================================================
 *
 *  A Telecom akkor köti be, ha ez az app az alapértelmezett tárcsázó
 *  (Settings > Default apps > Phone), illetve MANAGE_OWN_CALLS joggal saját
 *  hívásokhoz.
 *
 *  Feladatai:
 *    • figyeli a [Call] állapotváltozásait (tárcsázás, csengés, aktív, tartás),
 *    • feloldja a hívófél nevét, profilképét, mappáját és névjegy-azonosítóját,
 *    • **lejátssza a csengőhangot bejövő híváskor** – ezt a manifestben
 *      deklarált `IN_CALL_SERVICE_RINGING` miatt a rendszer nem teszi meg
 *      helyettünk, ezért volt néma a bejövő hívás,
 *    • a teljes állapotot átadja a Compose felületnek az [InCallBridge]-en át,
 *    • fogadja a felület vezérlő parancsait (néma, kihangosítás, bontás).
 * ===========================================================================
 */
class BBetterInCallService : InCallService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ringtonePlayer by lazy { RingtonePlayer(this) }

    private var activeCall: Call? = null

    private var number: String = ""
    private var displayName: String? = null
    private var photoUri: String? = null
    private var photo: ImageBitmap? = null
    private var folder: String? = null
    private var contactId: Long? = null
    private var startedAtMillis: Long = 0L
    private var status: CallStatus = CallStatus.DIALING
    private var muted = false
    private var speaker = false

    // ------------------------------------------------------------------ életciklus

    override fun onCreate() {
        super.onCreate()
        InCallBridge.attachActions(callActions)
    }

    override fun onDestroy() {
        ringtonePlayer.stop()
        scope.cancel()
        InCallBridge.publish(null)
        super.onDestroy()
    }

    // ------------------------------------------------------------------ hívás események

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        activeCall = call
        call.registerCallback(callCallback)

        number = call.details.handle?.schemeSpecificPart.orEmpty()
        displayName = call.details.callerDisplayName
        contactId = null
        folder = null
        startedAtMillis = 0L
        status = call.state.toCallStatus()

        InCallBridge.attachActions(callActions)
        publish()
        updateRinger()
        launchInCallUi()

        if (number.isNotBlank()) resolveCaller()
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        runCatching { call.unregisterCallback(callCallback) }
        if (activeCall !== call) return

        activeCall = null
        status = CallStatus.ENDED
        ringtonePlayer.stop()
        publish()

        scope.launch {
            delay(1200)
            InCallBridge.publish(null)
            runCatching {
                startActivity(
                    Intent(this@BBetterInCallService, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            }
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        speaker = audioState.route == CallAudioState.ROUTE_SPEAKER
        publish()
    }

    private val callCallback = object : Call.Callback() {

        override fun onStateChanged(call: Call, state: Int) {
            if (state == Call.STATE_ACTIVE && startedAtMillis == 0L) {
                startedAtMillis = System.currentTimeMillis()
            }
            status = state.toCallStatus()
            updateRinger()
            publish()
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            val handle = details.handle?.schemeSpecificPart
            if (!handle.isNullOrBlank() && handle != number) {
                number = handle
                resolveCaller()
            }
            details.callerDisplayName?.let { displayName = it }
            publish()
        }
    }

    // ------------------------------------------------------------------ csengőhang

    /**
     * Csengőhang indítása/leállítása az aktuális állapot alapján.
     *
     * A kontakt-szintű csengőhang miatt kétszer is lefuthat egy híváson belül:
     * először azonnal (ekkor még nincs feloldva a névjegy, ezért az általános
     * csengőhang szól), majd a névjegy feloldása után a saját hangjával.
     * A [RingtonePlayer] a már szóló URI-t nem indítja újra.
     */
    private fun updateRinger() {
        if (status != CallStatus.RINGING) {
            ringtonePlayer.stop()
            return
        }
        if (!ringtonePlayer.isAudible()) {
            ringtonePlayer.stop()
            return
        }
        runCatching {
            ringtonePlayer.play(RingtoneSettings.resolveUri(this, contactId))
        }
    }

    // ------------------------------------------------------------------ segédek

    /** Név + profilkép + mappa + névjegy-azonosító feloldása. */
    private fun resolveCaller() {
        scope.launch {
            val identity = PhoneRepository.resolveCaller(this@BBetterInCallService, number)
            if (!identity.name.isNullOrBlank()) displayName = identity.name
            photoUri = identity.photoUri
            photo = PhoneRepository.loadPhoto(this@BBetterInCallService, identity.photoUri)
            contactId = identity.contactId
            folder = ContactFolders.folderOf(identity.contactId)
            // Ha a felhasználó átnevezte a névjegyet az appban, az érvényesül.
            displayName = ContactOverrides.nameFor(identity.contactId, displayName)

            // Ha ehhez a névjegyhez saját csengőhang tartozik, az szólal meg.
            updateRinger()
            publish()
        }
    }

    private fun publish() {
        InCallBridge.publish(
            InCallState(
                number = number,
                name = displayName,
                photoUri = photoUri,
                photo = photo,
                status = status,
                startedAtMillis = startedAtMillis,
                contactId = contactId,
                folder = folder,
                muted = muted,
                speaker = speaker
            )
        )
    }

    private fun launchInCallUi() {
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(MainActivity.EXTRA_SHOW_IN_CALL, true)
            )
        }
    }

    // ------------------------------------------------------------------ UI -> Telecom parancsok

    private val callActions = object : CallActions {

        override fun end() {
            ringtonePlayer.stop()
            val call = activeCall ?: return
            if (call.state != Call.STATE_DISCONNECTED) call.disconnect()
        }

        override fun answer() {
            ringtonePlayer.stop()
            val call = activeCall ?: return
            // minSdk 26 -> a videóállapotot átvevő változat mindig elérhető.
            call.answer(VideoProfile.STATE_AUDIO_ONLY)
        }

        override fun setMuted(muted: Boolean) {
            this@BBetterInCallService.muted = muted
            this@BBetterInCallService.setMuted(muted)
            publish()
        }

        override fun setSpeaker(on: Boolean) {
            speaker = on
            this@BBetterInCallService.setAudioRoute(
                if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
            )
            publish()
        }

    }
}

private fun Int.toCallStatus(): CallStatus = when (this) {
    Call.STATE_RINGING -> CallStatus.RINGING
    Call.STATE_ACTIVE -> CallStatus.ACTIVE
    Call.STATE_HOLDING -> CallStatus.HOLDING
    Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> CallStatus.ENDED
    Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_SELECT_PHONE_ACCOUNT -> CallStatus.DIALING
    else -> CallStatus.DIALING
}
