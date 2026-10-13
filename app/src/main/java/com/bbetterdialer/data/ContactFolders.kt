package com.bbetterdialer.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject

/**
 * ===========================================================================
 *  KONTAKTMAPPÁK (csoportok)
 * ===========================================================================
 *  A mappák az app saját, helyi beállításai – SharedPreferences-ben tárolva.
 *  Így nem kell írási jog a rendszer névjegyadatbázisához (WRITE_CONTACTS),
 *  nem függünk a szinkronizációs fiókoktól, és minden készüléken működik.
 *
 *  A hozzárendelés kulcsa a névjegy `contactId`-ja, ezért egy több számmal
 *  rendelkező névjegy minden számnál ugyanabba a mappába esik.
 *
 *  Compose `mutableStateOf`, ezért a felület automatikusan frissül, amint
 *  egy kontaktot mappába teszünk vagy új mappát hozunk létre.
 * ===========================================================================
 */
object ContactFolders {

    private const val PREFS = "bbetter_contact_folders"
    private const val KEY_FOLDERS = "folders"
    private const val KEY_MAP = "assignments"

    private val defaultFolders = listOf("Család", "Munka")

    private var appContext: Context? = null
    private var loaded = false

    private val foldersState = mutableStateOf(defaultFolders)
    private val assignmentsState = mutableStateOf<Map<Long, String>>(emptyMap())

    /** A mappák nevei, létrehozás sorrendjében. */
    val folders: List<String> get() = foldersState.value

    /** contactId -> mappanév */
    val assignments: Map<Long, String> get() = assignmentsState.value

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        val ctx = context.applicationContext
        appContext = ctx

        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        prefs.getString(KEY_FOLDERS, null)?.let { raw ->
            runCatching {
                val array = JSONArray(raw)
                List(array.length()) { array.getString(it) }
            }.getOrNull()?.let { foldersState.value = it }
        }

        prefs.getString(KEY_MAP, null)?.let { raw ->
            runCatching {
                val obj = JSONObject(raw)
                buildMap {
                    obj.keys().forEach { key ->
                        key.toLongOrNull()?.let { put(it, obj.getString(key)) }
                    }
                }
            }.getOrNull()?.let { assignmentsState.value = it }
        }
    }

    // ------------------------------------------------------------------ mappák

    /** Új mappa létrehozása. Visszaadja a tisztított nevet, vagy null-t, ha érvénytelen. */
    fun createFolder(rawName: String): String? {
        val name = rawName.trim().take(24)
        if (name.isEmpty()) return null
        if (foldersState.value.any { it.equals(name, ignoreCase = true) }) return null

        foldersState.value = foldersState.value + name
        persist()
        return name
    }

    /** Mappa törlése; a hozzá tartozó kontaktok mappáratlanok lesznek. */
    fun deleteFolder(name: String) {
        foldersState.value = foldersState.value.filterNot { it == name }
        assignmentsState.value = assignmentsState.value.filterValues { it != name }
        persist()
    }

    // ------------------------------------------------------------------ hozzárendelés

    /** Kontakt mappába helyezése, vagy `null` esetén kivétele a mappából. */
    fun assign(contactId: Long, folder: String?) {
        val current = assignmentsState.value
        assignmentsState.value = if (folder.isNullOrBlank()) {
            current - contactId
        } else {
            current + (contactId to folder)
        }
        persist()
    }

    /** Egy kontakt mappája, vagy null. */
    fun folderOf(contactId: Long?): String? =
        contactId?.let { assignmentsState.value[it] }

    /** Hány kontakt van az adott mappában. */
    fun countIn(folder: String): Int =
        assignmentsState.value.count { it.value == folder }

    /** Mappa átnevezése – a hozzárendelések is átkövetik. */
    fun renameFolder(oldName: String, newName: String) {
        val clean = newName.trim().take(24)
        if (clean.isEmpty()) return
        if (foldersState.value.any { it.equals(clean, ignoreCase = true) && it != oldName }) return
        foldersState.value = foldersState.value.map { if (it == oldName) clean else it }
        assignmentsState.value = assignmentsState.value.mapValues { (_, v) -> if (v == oldName) clean else v }
        persist()
    }

    // ------------------------------------------------------------------ tárolás

    private fun persist() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val foldersJson = JSONArray().apply { foldersState.value.forEach { put(it) } }
        val mapJson = JSONObject().apply {
            assignmentsState.value.forEach { (id, folder) -> put(id.toString(), folder) }
        }
        prefs.edit()
            .putString(KEY_FOLDERS, foldersJson.toString())
            .putString(KEY_MAP, mapJson.toString())
            .apply()
    }
}
