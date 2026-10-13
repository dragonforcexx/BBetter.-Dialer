package com.bbetterdialer.ui

import com.bbetterdialer.ui.T

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.bbetterdialer.data.AvatarOverrides
import com.bbetterdialer.data.Contact
import com.bbetterdialer.data.PhoneRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Csak a számjegyek (megtartja a + jelet is, ha az elején van). */
fun String.digitsOnly(): String = filter { it.isDigit() }

/**
 * Emberi szemnek kellemes számformázás.
 * Magyar számokat a hazai séma szerint (06 30 123 4567 / +36 30 123 4567),
 * minden mást 3-asával csoportosítva jelenít meg.
 */
fun formatDial(raw: String): String {
    if (raw.isBlank()) return ""
    if (raw.startsWith("*") || raw.startsWith("#")) return raw

    val hasPlus = raw.startsWith("+")
    val d = raw.filter { it.isDigit() }
    if (d.isEmpty()) return raw
    if (d.length <= 4) return if (hasPlus) "+$d" else d

    return when {
        d.startsWith("06") && d.length in 9..11 -> "06 " + huTail(d.substring(2))
        hasPlus && d.startsWith("36") && d.length in 10..12 -> "+36 " + huTail(d.substring(2))
        hasPlus -> "+" + groupedFromRight(d)
        else -> groupedFromRight(d)
    }
}

private fun huTail(rest: String): String = when {
    rest.length <= 2 -> rest
    rest.length <= 5 -> rest.substring(0, 2) + " " + rest.substring(2)
    else -> rest.substring(0, 2) + " " + rest.substring(2, 5) + " " + rest.substring(5)
}

private fun groupedFromRight(d: String): String {
    val sb = StringBuilder()
    var i = d.length
    while (i > 0) {
        val start = (i - 3).coerceAtLeast(0)
        sb.insert(0, d.substring(start, i))
        if (start > 0) sb.insert(0, ' ')
        i = start
    }
    return sb.toString()
}

/** 65 -> "1:05", 3725 -> "1:02:05" */
fun formatDuration(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
    } else {
        String.format(Locale.US, "%d:%02d", m, sec)
    }
}

/** Hívásnapló bejegyzés ideje közérthetően, a választott nyelven. */
fun relativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 0 -> T.timeNow
        diff < 60_000 -> T.timeNow
        diff < 3_600_000 -> T.minutesAgo(diff / 60_000)
        diff < 86_400_000 -> T.hoursAgo(diff / 3_600_000)
        diff < 172_800_000 -> T.timeYesterday
        diff < 604_800_000 -> T.daysAgo(diff / 86_400_000)
        else -> SimpleDateFormat("MM. dd.", Locale.getDefault()).format(Date(millis))
    }
}

/** Névből (vagy számból) kezdőbetűk az avatarhoz. */
fun initialsOf(label: String?): String {
    val text = label?.trim().orEmpty()
    if (text.isEmpty()) return "?"
    if (text.first().isDigit() || text.startsWith("+")) {
        return text.filter { it.isDigit() }.takeLast(2).ifEmpty { "#" }
    }
    val parts = text.split(' ', '.', '-').filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

/**
 * Profilkép betöltése, Compose állapotként.
 *
 * Sorrend:
 *   1. a felhasználó által beállított **helyi felülírás** ([AvatarOverrides]),
 *   2. a rendszer-névjegy képe ([PhoneRepository], LRU cache-sel).
 *
 * Az `avatarVersion` a kulcsban biztosítja, hogy egy új kép beállítása után
 * azonnal újratöltődjön a megjelenített fotó.
 */
@Composable
fun rememberContactPhoto(
    uri: String?,
    contactId: Long? = null,
    sizePx: Int = 256,
): ImageBitmap? {
    val context = LocalContext.current
    val avatarVersion = AvatarOverrides.version
    val state by produceState<ImageBitmap?>(
        initialValue = null,
        uri,
        contactId,
        sizePx,
        avatarVersion
    ) {
        value = AvatarOverrides.load(context, contactId, sizePx)
            ?: PhoneRepository.loadPhoto(context, uri, sizePx, contactId)
    }
    return state
}

// --------------------------------------------------------------------------- keresés

/**
 * Kis- és nagybetűtől, valamint ékezetektől független normalizálás.
 * Így az „Orsós" keresésre az „orsos" is talál, a „Béla"-ra pedig a „bela".
 */
fun normalizeForSearch(text: String): String {
    if (text.isEmpty()) return ""
    val lower = text.lowercase(Locale.getDefault())
    val builder = StringBuilder(lower.length)
    for (ch in lower) {
        builder.append(
            when (ch) {
                'á', 'à', 'â', 'ä', 'ã', 'å' -> 'a'
                'é', 'è', 'ê', 'ë' -> 'e'
                'í', 'ì', 'î', 'ï' -> 'i'
                'ó', 'ò', 'ô', 'ö', 'õ', 'ő' -> 'o'
                'ú', 'ù', 'û', 'ü', 'ű' -> 'u'
                'ç' -> 'c'
                'ñ' -> 'n'
                'š' -> 's'
                'ž' -> 'z'
                'ý', 'ÿ' -> 'y'
                'ß' -> 's'
                else -> ch
            }
        )
    }
    return builder.toString()
}

/** Szám összehasonlításhoz: csak a számjegyek és a vezető + marad meg. */
fun normalizeNumberForSearch(number: String): String =
    number.filter { it.isDigit() || it == '+' }

/**
 * Valós idejű keresés: minden beírt szónak szerepelnie kell a névben
 * (ékezet- és kis/nagybetű-függetlenül), vagy a számban (formázástól
 * függetlenül). Részleges egyezés is elég.
 *
 * @param displayName az esetlegesen felülírt (átnevezett) megjelenítendő név;
 *                    ha null, a névjegy eredeti neve számít.
 */
fun Contact.matchesQuery(query: String, displayName: String? = null): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true

    val name = normalizeForSearch(displayName ?: this.name)
    val number = normalizeNumberForSearch(this.number)

    val tokens = normalizeForSearch(trimmed).split(' ').filter { it.isNotBlank() }
    if (tokens.isEmpty()) return true

    return tokens.all { token ->
        name.contains(token) || (token.any { it.isDigit() } && number.contains(token))
    }
}
