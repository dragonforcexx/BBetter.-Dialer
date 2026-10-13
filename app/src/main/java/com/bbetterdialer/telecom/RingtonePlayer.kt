package com.bbetterdialer.telecom

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

/**
 * ===========================================================================
 *  CSENGŐHANG LEJÁTSZÓ
 * ===========================================================================
 *  A manifestben szerepel az `android.telecom.IN_CALL_SERVICE_RINGING` metaadat,
 *  ami azt jelenti, hogy ez a tárcsázó **átveszi** a csengőhang lejátszását a
 *  rendszertől (`TelecomManager.METADATA_IN_CALL_SERVICE_RINGING`). Emiatt a
 *  Telecom NEM játszik csengőhangot – azt nekünk kell megtennünk, különben
 *  néma marad a bejövő hívás. Ez a hiba forrása és a javítás helye is.
 *
 *  A lejátszás a `USAGE_NOTIFICATION_RINGTONE` hangsávon történik, így a
 *  hangerő a rendszer csengőhang-csúszkáját követi, és a néma/rezgő módot is
 *  tiszteletben tartjuk.
 * ===========================================================================
 */
class RingtonePlayer(private val context: Context) {

    private var player: MediaPlayer? = null
    private var fallbackRingtone: Ringtone? = null
    private var currentUri: Uri? = null

    /** Előhallgatásnál (nem ismétlő lejátszás) jelzi, hogy a hang véget ért. */
    var onFinished: (() -> Unit)? = null

    val isPlaying: Boolean
        get() = player != null || fallbackRingtone != null

    /** Nem szólunk, ha a készülék néma vagy rezgő módban van. */
    fun isAudible(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return true
        return audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    /**
     * @param loop igaz hívásnál ismétli a hangot, előhallgatásnál egyszer szól.
     */
    fun play(uri: Uri, loop: Boolean = true) {
        if (currentUri == uri && isPlaying) return
        stop()
        currentUri = uri

        val started = runCatching {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.isLooping = loop
            mediaPlayer.setOnCompletionListener {
                if (!loop) {
                    stop()
                    onFinished?.invoke()
                }
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                stop()
                true
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
            player = mediaPlayer
            true
        }.getOrDefault(false)

        if (!started) playWithRingtone(uri, loop)
    }

    /** Tartalék, ha a MediaPlayer nem tudta megnyitni az URI-t. */
    private fun playWithRingtone(uri: Uri, loop: Boolean) {
        runCatching {
            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
            if (loop && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = true
            }
            ringtone.play()
            fallbackRingtone = ringtone
        }
    }

    fun stop() {
        player?.let { mediaPlayer ->
            runCatching { if (mediaPlayer.isPlaying) mediaPlayer.stop() }
            runCatching { mediaPlayer.setOnCompletionListener(null) }
            runCatching { mediaPlayer.release() }
        }
        player = null

        fallbackRingtone?.let { ringtone ->
            runCatching { if (ringtone.isPlaying) ringtone.stop() }
        }
        fallbackRingtone = null
        currentUri = null
    }
}
