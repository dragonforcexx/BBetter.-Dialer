package com.bbetterdialer.data

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import org.json.JSONObject

/**
 * ===========================================================================
 *  CSENGŐHANGOK
 * ===========================================================================
 *  Két szint:
 *    • általános csengőhang – minden híváshoz (a rendszer alapértelmezettje,
 *      egy beépített hang, vagy a fájlkezelőből kiválasztott saját fájl),
 *    • kontakt-szintű csengőhang – egy adott névjegyhez rendelt hang,
 *      amely felülírja az általánost.
 *
 *  Miért az app játssza le? A manifestben szerepel a
 *  `android.telecom.IN_CALL_SERVICE_RINGING` metaadat, ami azt jelenti, hogy
 *  a tárcsázó **átveszi** a csengőhang lejátszását a rendszertől
 *  (`TelecomManager.METADATA_IN_CALL_SERVICE_RINGING`). Ezért a
 *  [com.bbetterdialer.telecom.RingtonePlayer] felel a hangért – ez teszi
 *  lehetővé a kontakt-szintű csengőhangokat is.
 * ===========================================================================
 */
object RingtoneSettings {

    /** Egy kiválasztott csengőhang. A `null` URI a rendszer alapértelmezettjét jelenti. */
    data class Choice(
        val uri: String?,
        val label: String
    )

    /** Egy választható, rendszerben elérhető csengőhang. */
    data class RingtoneItem(
        val uri: Uri,
        val title: String
    )

    private const val PREFS = "bbetter_ringtones"
    private const val KEY_DEFAULT_URI = "default_uri"
    private const val KEY_DEFAULT_LABEL = "default_label"
    private const val KEY_CONTACT_URIS = "contact_uris"
    private const val KEY_CONTACT_LABELS = "contact_labels"

    private var appContext: Context? = null
    private var loaded = false

    private val defaultState = mutableStateOf<Choice?>(null)
    private val contactState = mutableStateOf<Map<Long, Choice>>(emptyMap())

    /** Az általános csengőhang, vagy null, ha a rendszer alapértelmezettje. */
    val default: Choice? get() = defaultState.value

    /** Kontakt-szintű felülírások. */
    val perContact: Map<Long, Choice> get() = contactState.value

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        val ctx = context.applicationContext
        appContext = ctx
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val defaultUri = prefs.getString(KEY_DEFAULT_URI, null)
        if (!defaultUri.isNullOrBlank()) {
            defaultState.value = Choice(
                uri = defaultUri,
                label = prefs.getString(KEY_DEFAULT_LABEL, null) ?: "Egyéni csengőhang"
            )
        }

        val uris = readMap(prefs.getString(KEY_CONTACT_URIS, null))
        val labels = readMap(prefs.getString(KEY_CONTACT_LABELS, null))
        if (uris.isNotEmpty()) {
            contactState.value = uris.mapValues { (id, uri) ->
                Choice(uri = uri, label = labels[id] ?: "Egyéni csengőhang")
            }
        }
    }

    // ------------------------------------------------------------------ írás

    fun setDefault(choice: Choice?) {
        defaultState.value = choice
        val prefs = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
        prefs.edit().apply {
            if (choice?.uri == null) {
                remove(KEY_DEFAULT_URI)
                remove(KEY_DEFAULT_LABEL)
            } else {
                putString(KEY_DEFAULT_URI, choice.uri)
                putString(KEY_DEFAULT_LABEL, choice.label)
            }
        }.apply()
    }

    fun setForContact(contactId: Long, choice: Choice?) {
        val current = contactState.value
        contactState.value = if (choice?.uri == null) current - contactId else current + (contactId to choice)
        persistContacts()
    }

    fun clearAllContactChoices() {
        contactState.value = emptyMap()
        persistContacts()
    }

    // ------------------------------------------------------------------ olvasás

    /** A kontaktra érvényes beállítás: saját csengőhang, különben az általános. */
    fun choiceFor(contactId: Long?): Choice? =
        contactId?.let { contactState.value[it] } ?: defaultState.value

    /** A ténylegesen lejátszandó URI (soha nem null – a rendszer alapértelmezettjére esik vissza). */
    fun resolveUri(context: Context, contactId: Long?): Uri {
        val custom = choiceFor(contactId)?.uri?.takeIf { it.isNotBlank() }
        if (custom != null) {
            return Uri.parse(custom)
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: Settings.System.DEFAULT_RINGTONE_URI
    }

    /** Megjeleníthető név az általános csengőhanghoz. */
    fun defaultLabel(context: Context): String =
        defaultState.value?.label ?: systemDefaultLabel(context)

    fun systemDefaultLabel(context: Context): String = runCatching {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Rendszer alapértelmezett"

    /** A készüléken elérhető csengőhangok listája a választóhoz. */
    fun systemRingtones(context: Context): List<RingtoneItem> = runCatching {
        val manager = RingtoneManager(context)
        manager.setType(RingtoneManager.TYPE_RINGTONE)
        buildList {
            manager.cursor?.use { cursor ->
                while (cursor.moveToNext()) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue
                    val uri = manager.getRingtoneUri(cursor.position) ?: continue
                    add(RingtoneItem(uri = uri, title = title))
                }
            }
        }
    }.getOrDefault(emptyList())

    // ------------------------------------------------------------------ belső

    private fun persistContacts() {
        val prefs = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
        val uris = JSONObject()
        val labels = JSONObject()
        contactState.value.forEach { (id, choice) ->
            choice.uri?.let {
                uris.put(id.toString(), it)
                labels.put(id.toString(), choice.label)
            }
        }
        prefs.edit()
            .putString(KEY_CONTACT_URIS, uris.toString())
            .putString(KEY_CONTACT_LABELS, labels.toString())
            .apply()
    }

    private fun readMap(raw: String?): Map<Long, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { key ->
                    key.toLongOrNull()?.let { put(it, obj.getString(key)) }
                }
            }
        }.getOrDefault(emptyMap())
    }
}
