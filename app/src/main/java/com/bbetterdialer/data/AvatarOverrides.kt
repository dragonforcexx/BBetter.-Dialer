package com.bbetterdialer.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.util.LruCache
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * ===========================================================================
 *  PROFILKÉP CSERE – kijelöléssel (zoom + mozgatás)
 * ===========================================================================
 *  A felhasználó a kiválasztott képet egy körben igazíthatja: csippentéssel
 *  nagyíthat, húzással mozgathatja, így pontosan beállíthatja, melyik rész
 *  kerüljön a profilba. A kijelölés eredménye:
 *
 *   1. **Helyi felülírás** – 1024 px-es, kiváló minőségű JPEG az app saját
 *      tárhelyén (`filesDir/avatars/<contactId>.jpg`), ezt mutatja az app.
 *   2. **Rendszer-névjegy** – ha van WRITE_CONTACTS jog, a kép a
 *      névjegyadatbázisba is bekerül (a 180 KB-os provider-limit alá
 *      tömörítve), így a rendszer Névjegyek appban is látszik.
 *
 *  A kijelölés matematikája normalizált (0..1) egységnégyzetben történik,
 *  ezért felbontásfüggetlen.
 * ===========================================================================
 */
object AvatarOverrides {

    /** A körbe illesztés beállításai – mindhárom érték felbontásfüggetlen. */
    data class AvatarCrop(
        val scale: Float = 1f,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f
    ) {
        val isIdentity: Boolean
            get() = scale == 1f && offsetX == 0f && offsetY == 0f
    }

    sealed interface Result {
        data class Success(val systemUpdated: Boolean) : Result
        data class Failure(val message: String) : Result
    }

    private const val PREFS = "bbetter_avatars"
    private const val KEY_MAP = "map"
    private const val DIR_NAME = "avatars"
    private const val MAX_SYSTEM_BYTES = 180_000

    /** Az appban tárolt profilkép mérete – a v2 640 px-e helyett. */
    private const val LOCAL_SIZE = 1024

    /** A rendszer-névjegyhez küldött kép mérete. */
    private const val SYSTEM_SIZE = 720

    /** Ennél nagyobb képet már dekódoláskor lekicsinyítünk. */
    private const val EDIT_MAX_SIZE = 2048

    private var appContext: Context? = null
    private var loaded = false

    private val mapState = mutableStateOf<Map<Long, String>>(emptyMap())
    private val versionState = mutableStateOf(0)

    /** Nő, ha bármelyik kép megváltozott – a Compose újratölti a képeket. */
    val version: Int get() = versionState.value

    private val cache = LruCache<String, ImageBitmap>(32)

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        val ctx = context.applicationContext
        appContext = ctx
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MAP, null)
        if (!raw.isNullOrBlank()) {
            runCatching {
                val obj = JSONObject(raw)
                buildMap {
                    obj.keys().forEach { key ->
                        key.toLongOrNull()?.let { put(it, obj.getString(key)) }
                    }
                }
            }.getOrNull()?.let { mapState.value = it }
        }
    }

    /** A kontakt helyi képfájlja, ha van. */
    fun localFile(contactId: Long?): File? {
        val path = contactId?.let { mapState.value[it] } ?: return null
        val file = File(path)
        return if (file.exists()) file else null
    }

    fun hasOverride(contactId: Long?): Boolean = localFile(contactId) != null

    /** Helyi felülírás betöltése (cache-sel), vagy null. */
    suspend fun load(context: Context, contactId: Long?, sizePx: Int): ImageBitmap? {
        val file = localFile(contactId) ?: return null
        val key = "local:$contactId@${versionState.value}@$sizePx"
        cache.get(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@runCatching null
                bitmap.scaleDown(sizePx).asImageBitmap().also { cache.put(key, it) }
            }.getOrNull()
        }
    }

    /**
     * A galériából kiválasztott kép betöltése a **kijelölő szerkesztőhöz**.
     * Legfeljebb [EDIT_MAX_SIZE] px, hogy a csippentés sima maradjon.
     */
    suspend fun loadForEditing(context: Context, source: Uri): ImageBitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(source)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }

                var sample = 1
                val longest = maxOf(bounds.outWidth, bounds.outHeight)
                while (longest / sample > EDIT_MAX_SIZE) sample *= 2

                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                context.contentResolver.openInputStream(source)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }?.asImageBitmap()
            }.getOrNull()
        }

    /**
     * Új profilkép beállítása a kiválasztott (és beállított) képből.
     */
    suspend fun apply(
        context: Context,
        contactId: Long,
        source: Uri,
        crop: AvatarCrop? = null
    ): Result {
        val original = decodeFull(context, source)
            ?: return Result.Failure("A kiválasztott kép nem olvasható")

        val square = original.cropToSquare(crop)

        val file = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
                val target = File(dir, "$contactId.jpg")
                val highQuality = square.scaleDown(LOCAL_SIZE)
                FileOutputStream(target).use { out ->
                    highQuality.compress(Bitmap.CompressFormat.JPEG, 94, out)
                }
                target
            }.getOrNull()
        } ?: return Result.Failure("Nem sikerült menteni a képet")

        mapState.value = mapState.value + (contactId to file.absolutePath)
        versionState.value += 1
        cache.evictAll()
        persist()

        val systemUpdated = withContext(Dispatchers.IO) {
            writeToSystemContact(
                context,
                contactId,
                square.scaleDown(SYSTEM_SIZE).toContactPhotoBytes()
            )
        }

        return Result.Success(systemUpdated)
    }

    /** Helyi felülírás törlése (a rendszer-névjegyhez nem nyúlunk). */
    fun clear(contactId: Long) {
        mapState.value[contactId]?.let { runCatching { File(it).delete() } }
        mapState.value = mapState.value - contactId
        versionState.value += 1
        cache.evictAll()
        persist()
    }

    // ------------------------------------------------------------------ belső

    private fun persist() {
        val prefs = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
        val obj = JSONObject()
        mapState.value.forEach { (id, path) -> obj.put(id.toString(), path) }
        prefs.edit().putString(KEY_MAP, obj.toString()).apply()
    }

    private fun decodeFull(context: Context, source: Uri): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / sample > EDIT_MAX_SIZE) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }.getOrNull()

    /**
     * A kijelölés alkalmazása: a felhasználó által beállított zoom/mozgatás
     * alapján kivágott négyzet. A matematika egységnégyzetben fut, ezért
     * ugyanaz az eredmény minden felbontáson.
     */
    private fun Bitmap.cropToSquare(crop: AvatarCrop?): Bitmap {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return this
        if (crop == null || crop.isIdentity) {
            val side = minOf(width, height)
            return Bitmap.createBitmap(this, (width - side) / 2, (height - side) / 2, side, side)
        }

        // k0: ekkora nagyítás kell ahhoz, hogy a kép lefedje az egységnégyzetet
        val k0 = maxOf(1f / w, 1f / h)
        val k = k0 * crop.scale

        val imageLeft = (1f - w * k) / 2f + crop.offsetX
        val imageTop = (1f - h * k) / 2f + crop.offsetY

        var side = (1f / k)
        side = side.coerceAtMost(minOf(w, h))
        val left = (-imageLeft / k).coerceIn(0f, (w - side).coerceAtLeast(0f))
        val top = (-imageTop / k).coerceIn(0f, (h - side).coerceAtLeast(0f))

        return runCatching {
            Bitmap.createBitmap(this, left.toInt(), top.toInt(), side.toInt(), side.toInt())
        }.getOrDefault(this)
    }

    private fun Bitmap.scaleDown(maxSide: Int): Bitmap {
        val current = maxOf(width, height)
        if (current <= maxSide || current <= 0) return this
        val ratio = maxSide.toFloat() / current
        return Bitmap.createScaledBitmap(
            this,
            (width * ratio).toInt().coerceAtLeast(1),
            (height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    /** A névjegyadatbázis limitje miatt 180 KB alá kell tömöríteni. */
    private fun Bitmap.toContactPhotoBytes(): ByteArray {
        var bitmap = this
        var quality = 90
        repeat(5) {
            val bytes = java.io.ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
            if (bytes.size <= MAX_SYSTEM_BYTES) return bytes
            quality -= 12
            bitmap = bitmap.scaleDown((bitmap.width * 0.75f).toInt())
        }
        return java.io.ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 55, out)
            out.toByteArray()
        }
    }

    private fun writeToSystemContact(context: Context, contactId: Long, bytes: ByteArray): Boolean =
        runCatching {
            val resolver = context.contentResolver
            val rawContactId = PhoneRepository.rawContactId(resolver, contactId) ?: return false

            val values = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                )
                put(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                put(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes)
            }

            val where = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
            val args = arrayOf(
                rawContactId.toString(),
                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
            )

            val updated = resolver.update(ContactsContract.Data.CONTENT_URI, values, where, args)
            if (updated > 0) true else resolver.insert(ContactsContract.Data.CONTENT_URI, values) != null
        }.getOrDefault(false)
}
