package com.bbetterdialer.data

import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * ===========================================================================
 *  NÉV- ÉS SZÍNFELÜLÍRÁSOK
 * ===========================================================================
 *  • **Név**: átnevezés a rendszer-névjegyben (WRITE_CONTACTS), plusz helyi
 *    felülírás, hogy jog hiányában is működjön és azonnal látszódjon.
 *  • **Szín**: fotó nélküli névjegyekhez választható háttérszín. A kezdőbetűk
 *    változatlanok maradnak, csak a kör színe lesz a választott szín.
 *
 *  Mindkettő Compose-state, ezért a felület azonnal frissül.
 * ===========================================================================
 */
object ContactOverrides {

    /** A választható profilszínek – szándékosan kék nélkül. */
    val palette: List<Color> = listOf(
        Color(0xFFE53935), // piros
        Color(0xFFF4511E), // narancs
        Color(0xFFFB8C00), // borostyán
        Color(0xFFFDD835), // citrom
        Color(0xFF7CB342), // zöld
        Color(0xFF00E676), // menta
        Color(0xFF00897B), // teal
        Color(0xFF8E24AA), // lila
        Color(0xFFD81B60), // málna
        Color(0xFFEC407A), // rózsaszín
        Color(0xFF6D4C41), // barna
        Color(0xFF546E7A), // pala
    )

    private const val PREFS = "bbetter_contact_overrides"
    private const val KEY_NAMES = "names"
    private const val KEY_COLORS = "colors"

    private var appContext: Context? = null
    private var loaded = false

    private val namesState = mutableStateOf<Map<Long, String>>(emptyMap())
    private val colorsState = mutableStateOf<Map<Long, Int>>(emptyMap())

    val names: Map<Long, String> get() = namesState.value
    val colors: Map<Long, Int> get() = colorsState.value

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        val ctx = context.applicationContext
        appContext = ctx
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        readLongMap(prefs.getString(KEY_NAMES, null))?.let { raw ->
            namesState.value = raw
        }
        readLongMap(prefs.getString(KEY_COLORS, null))?.let { raw ->
            colorsState.value = raw.mapNotNull { (id, value) ->
                value.toIntOrNull()?.let { id to it }
            }.toMap()
        }
    }

    // ------------------------------------------------------------------ olvasás

    /** A névjegy megjelenítendő neve (felülírás, különben az eredeti). */
    fun nameFor(contact: Contact): String =
        namesState.value[contact.contactId] ?: contact.name

    fun nameFor(contactId: Long?, fallback: String?): String? =
        contactId?.let { namesState.value[it] } ?: fallback

    fun hasName(contactId: Long?): Boolean = contactId != null && namesState.value.containsKey(contactId)

    /** A névjegyhez választott szín, vagy null (ekkor az alap akcentszín jár). */
    fun colorFor(contactId: Long?): Color? =
        contactId?.let { colorsState.value[it] }?.let { Color(it) }

    fun hasColor(contactId: Long?): Boolean = contactId != null && colorsState.value.containsKey(contactId)

    // ------------------------------------------------------------------ írás

    fun setColor(contactId: Long, color: Color?) {
        colorsState.value = if (color == null) {
            colorsState.value - contactId
        } else {
            colorsState.value + (contactId to color.toArgb())
        }
        persistColors()
    }

    fun clearName(contactId: Long) {
        namesState.value = namesState.value - contactId
        persistNames()
    }

    /**
     * Átnevezés: mindig elmentjük helyben (hogy azonnal látszódjon), és
     * megpróbáljuk a rendszer-névjegyet is frissíteni.
     */
    suspend fun rename(context: Context, contactId: Long, newName: String): RenameResult {
        val clean = newName.trim().take(80)
        if (clean.isEmpty()) return RenameResult.Failure("A név nem lehet üres")

        namesState.value = namesState.value + (contactId to clean)
        persistNames()

        val systemUpdated = withContext(Dispatchers.IO) {
            writeNameToSystem(context, contactId, clean)
        }
        return RenameResult.Success(systemUpdated)
    }

    sealed interface RenameResult {
        data class Success(val systemUpdated: Boolean) : RenameResult
        data class Failure(val message: String) : RenameResult
    }

    // ------------------------------------------------------------------ belső

    private fun writeNameToSystem(context: Context, contactId: Long, name: String): Boolean =
        runCatching {
            val resolver = context.contentResolver
            val rawContactId = PhoneRepository.rawContactId(resolver, contactId) ?: return false

            val values = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            }

            val where = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
            val args = arrayOf(
                rawContactId.toString(),
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
            )

            val updated = resolver.update(ContactsContract.Data.CONTENT_URI, values, where, args)
            if (updated > 0) true
            else resolver.insert(ContactsContract.Data.CONTENT_URI, values) != null
        }.getOrDefault(false)

    private fun persistNames() {
        val ctx = appContext ?: return
        val payload = JSONObject()
        namesState.value.forEach { (id, value) -> payload.put(id.toString(), value) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAMES, payload.toString())
            .apply()
    }

    private fun persistColors() {
        val ctx = appContext ?: return
        val payload = JSONObject()
        colorsState.value.forEach { (id, value) -> payload.put(id.toString(), value.toString()) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COLORS, payload.toString())
            .apply()
    }

    private fun readLongMap(raw: String?): Map<Long, String>? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { k ->
                    k.toLongOrNull()?.let { put(it, obj.getString(k)) }
                }
            }
        }.getOrNull()
    }
}
