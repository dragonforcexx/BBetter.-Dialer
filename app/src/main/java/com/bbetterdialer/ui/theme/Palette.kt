package com.bbetterdialer.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/*
 * ===========================================================================
 *  SZÍNPALETTA – sötét (AMOLED) és világos téma
 * ===========================================================================
 *  A színek szándékosan **getterként** élnek, nem konstansként: így a meglévő
 *  hívási helyek (`TextPrimary`, `Surface1`, …) változtatás nélkül működnek,
 *  viszont a témaváltásra automatikusan újrarenderelődnek, mert a getter egy
 *  Compose-state-et (`ThemeState.dark`) olvas.
 *
 *  Szabály mindkét témában: nincs kék, nincs gradiens, nincs árnyék.
 * ===========================================================================
 */

object ThemeState {
    var dark: Boolean by mutableStateOf(true)
}

// ---- sötét: tiszta AMOLED fekete + semleges szürkék ------------------------
private val DarkBg = Color(0xFF000000)
private val DarkSurface1 = Color(0xFF0D0D0D)
private val DarkSurface2 = Color(0xFF1C1C1E)
private val DarkSurface3 = Color(0xFF2C2C2E)
private val DarkDivider = Color(0xFF2A2A2C)
private val DarkTextPrimary = Color(0xFFFFFFFF)
private val DarkTextSecondary = Color(0xFF9A9AA0)
private val DarkTextTertiary = Color(0xFF5E5E64)

// ---- világos: párás fehér / világos szürke --------------------------------
private val LightBg = Color(0xFFF5F6F8)
private val LightSurface1 = Color(0xFFFFFFFF)
private val LightSurface2 = Color(0xFFE7EAEF)
private val LightSurface3 = Color(0xFFD8DCE2)
private val LightDivider = Color(0xFFE2E5E9)
private val LightTextPrimary = Color(0xFF101216)
private val LightTextSecondary = Color(0xFF5A616B)
private val LightTextTertiary = Color(0xFF8B929C)

private val isDarkTheme: Boolean get() = ThemeState.dark

/** Az app háttere (sötét témában tiszta fekete). */
val AppBg: Color get() = if (isDarkTheme) DarkBg else LightBg

/** Kártyák, panelek. */
val Surface1: Color get() = if (isDarkTheme) DarkSurface1 else LightSurface1

/** Gombok, chipek, beviteli mezők. */
val Surface2: Color get() = if (isDarkTheme) DarkSurface2 else LightSurface2

/** Lenyomott / kiválasztott állapot. */
val Surface3: Color get() = if (isDarkTheme) DarkSurface3 else LightSurface3

/** Elválasztók, keretek. */
val Divider: Color get() = if (isDarkTheme) DarkDivider else LightDivider

val TextPrimary: Color get() = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
val TextSecondary: Color get() = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
val TextTertiary: Color get() = if (isDarkTheme) DarkTextTertiary else LightTextTertiary

// ---- márkaszínek: mindkét témában ugyanazok --------------------------------

/** Élénk, telített zöld a hívásindításhoz – semmiképp sem fakó. */
val VividGreen = Color(0xFF00E676)

/** A hívásgomb "üres kijelző" állapota. */
val IdleGreen: Color get() = if (isDarkTheme) Color(0xFF0C3B22) else Color(0xFFD3EFDF)

/** Az ikon színe az inaktív hívásgombon. */
val IdleGreenTint: Color get() = if (isDarkTheme) Color(0xFF2F7F55) else Color(0xFF4B9C72)

/** Telített piros a hívásbontáshoz és a nem fogadott hívásokhoz. */
val EndRed = Color(0xFFFF3B30)

/** Az akciógombokon lévő ikon színe (élénk zöld/piros felületen). */
val OnAccent = Color(0xFF000000)
