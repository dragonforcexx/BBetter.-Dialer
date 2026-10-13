package com.bbetterdialer.data

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Egy névjegy a tárcsázóhoz.
 *
 * @param id        a Phone adatsor azonosítója (lista kulcshoz)
 * @param contactId a valódi névjegy-azonosító – a mappák ehhez vannak rendelve,
 *                  így egy több számmal rendelkező névjegy minden számnál ugyanabba
 *                  a mappába tartozik
 */
data class Contact(
    val id: Long,
    val contactId: Long,
    val name: String,
    val number: String,
    val photoUri: String?,
)

/** Egy bejegyzés a hívásnaplóban. */
data class CallEntry(
    val id: Long,
    val name: String?,
    val number: String,
    val type: Int,
    val dateMillis: Long,
    val durationSec: Long,
    val photoUri: String?,
)

/** Hívó fél azonosítása telefonszám alapján (hívásképernyőhöz). */
data class CallerIdentity(
    val name: String?,
    val photoUri: String?,
    val contactId: Long?,
)

/** Hívás állapota – a Telecom szolgáltatás és a UI közös nyelve. */
enum class CallStatus {
    DIALING,
    RINGING,
    ACTIVE,
    HOLDING,
    ENDED
}

/** A hívásképernyő teljes állapota. */
data class InCallState(
    val number: String,
    val name: String?,
    val photoUri: String?,
    val photo: ImageBitmap?,
    val status: CallStatus,
    val startedAtMillis: Long,
    /** A hívó fél névjegy-azonosítója (a helyi profilkép-felülíráshoz és a mappához). */
    val contactId: Long? = null,
    /** A hívó fél mappája (pl. "Család") – a hívásképernyő címkéje. */
    val folder: String? = null,
    val muted: Boolean = false,
    val speaker: Boolean = false,
)
