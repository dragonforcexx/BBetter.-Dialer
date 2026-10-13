package com.bbetterdialer.data

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Minden telefonos adat egy helyen: névjegyek, hívásnapló, hívófél-keresés
 * és profilkép-betöltés. Szándékosan `ContentResolver`-alapú, hogy ne
 * kerüljön a projektbe egyetlen plusz könyvtár sem.
 */
object PhoneRepository {

    /** Kis LRU cache a profilképképekhez, hogy a lista görgetése ne indítson újratöltést. */
    private val photoCache = LruCache<String, ImageBitmap>(64)

    // ---------------------------------------------------------------- jogosultságok

    fun hasContacts(context: Context): Boolean = granted(context, Manifest.permission.READ_CONTACTS)

    fun hasCallLog(context: Context): Boolean = granted(context, Manifest.permission.READ_CALL_LOG)

    fun hasCallPhone(context: Context): Boolean = granted(context, Manifest.permission.CALL_PHONE)

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    // ---------------------------------------------------------------- névjegyek

    suspend fun loadContacts(resolver: ContentResolver, limit: Int = 400): List<Contact> =
        withContext(Dispatchers.IO) {
            runCatching {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    // A nagy felbontású profilkép URI-ja (nem a thumbnail),
                    // hogy a hívásképernyőn ne legyen pixeles a fotó.
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                )
                val out = ArrayList<Contact>(128)
                val seen = HashSet<String>()

                resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " ASC"
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(projection[0])
                    val contactIdIdx = cursor.getColumnIndexOrThrow(projection[1])
                    val nameIdx = cursor.getColumnIndexOrThrow(projection[2])
                    val numberIdx = cursor.getColumnIndexOrThrow(projection[3])
                    val photoIdx = cursor.getColumnIndexOrThrow(projection[4])

                    while (cursor.moveToNext() && out.size < limit) {
                        val number = cursor.getString(numberIdx) ?: continue
                        val name = cursor.getString(nameIdx) ?: number
                        val key = runCatching { PhoneNumberUtils.normalizeNumber(number) }
                            .getOrNull()
                            ?.takeIf { it.isNotBlank() }
                            ?: number
                        if (!seen.add(key)) continue

                        out += Contact(
                            id = cursor.getLong(idIdx),
                            contactId = cursor.getLong(contactIdIdx),
                            name = name,
                            number = number,
                            photoUri = cursor.getString(photoIdx)
                        )
                    }
                }
                out
            }.getOrDefault(emptyList())
        }

    // ---------------------------------------------------------------- hívásnapló

    suspend fun loadRecentCalls(resolver: ContentResolver, limit: Int = 120): List<CallEntry> =
        withContext(Dispatchers.IO) {
            runCatching {
                val projection = arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.CACHED_PHOTO_URI
                )
                val out = ArrayList<CallEntry>(limit)

                resolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    CallLog.Calls.DATE + " DESC"
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(projection[0])
                    val nameIdx = cursor.getColumnIndexOrThrow(projection[1])
                    val numberIdx = cursor.getColumnIndexOrThrow(projection[2])
                    val typeIdx = cursor.getColumnIndexOrThrow(projection[3])
                    val dateIdx = cursor.getColumnIndexOrThrow(projection[4])
                    val durationIdx = cursor.getColumnIndexOrThrow(projection[5])
                    val photoIdx = cursor.getColumnIndexOrThrow(projection[6])

                    while (cursor.moveToNext() && out.size < limit) {
                        val number = cursor.getString(numberIdx) ?: continue
                        out += CallEntry(
                            id = cursor.getLong(idIdx),
                            name = cursor.getString(nameIdx),
                            number = number,
                            type = cursor.getInt(typeIdx),
                            dateMillis = cursor.getLong(dateIdx),
                            durationSec = cursor.getLong(durationIdx),
                            photoUri = cursor.getString(photoIdx)
                        )
                    }
                }
                out
            }.getOrDefault(emptyList())
        }

    // ---------------------------------------------------------------- hívófél keresés

    /**
     * Telefonszám -> név + profilkép URI + névjegy-azonosító a
     * [ContactsContract.PhoneLookup] táblából.
     * A hívásképernyő ezt használja, amikor a Telecom csak a számot adja át;
     * a `contactId` kell ahhoz, hogy a mappacímkét is meg tudjuk jeleníteni.
     */
    suspend fun resolveCaller(context: Context, number: String): CallerIdentity =
        withContext(Dispatchers.IO) {
            if (number.isBlank() || !hasContacts(context)) {
                return@withContext CallerIdentity(null, null, null)
            }
            runCatching {
                val uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(number)
                )
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        // A PhoneLookup._ID a névjegy azonosítója (Contacts._ID).
                        ContactsContract.PhoneLookup._ID,
                        ContactsContract.PhoneLookup.DISPLAY_NAME,
                        ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
                    ),
                    null, null, null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        CallerIdentity(
                            name = c.getString(1),
                            photoUri = c.getString(2),
                            contactId = c.getLong(0)
                        )
                    } else {
                        CallerIdentity(null, null, null)
                    }
                } ?: CallerIdentity(null, null, null)
            }.getOrDefault(CallerIdentity(null, null, null))
        }

    // ---------------------------------------------------------------- profilképek

    /**
     * Profilkép betöltése a lehető legjobb minőségben, LRU cache-sel.
     *
     *   1. Ha ismert a névjegy-azonosító, először a **nagy felbontású**
     *      névjegy-fotót kérjük le (`openContactPhotoInputStream(preferHighres = true)`),
     *      ami a teljes méretű display fotót adja vissza.
     *   2. Ha az nem elérhető, a photo URI-t dekódoljuk **teljes felbontásban**,
     *      lépcsozetes mintavételezéssel (`inSampleSize`), majd sima (bilineáris)
     *      kicsinyítéssel készítjük a négyzetet.
     *
     * A korábbi `loadThumbnail` hívás a Contacts provideren keresztül lényegesen
     * kisebb képet adhatott vissza – ez okozta a pixeles profilképet a
     * hívásképernyőn. A cache kulcsa a kért méretet is tartalmazza.
     */
    suspend fun loadPhoto(
        context: Context,
        uriString: String?,
        sizePx: Int = 320,
        contactId: Long? = null
    ): ImageBitmap? {
        if (uriString.isNullOrBlank() && contactId == null) return null
        val cacheKey = "${uriString ?: contactId}@$sizePx"
        photoCache.get(cacheKey)?.let { return it }

        return withContext(Dispatchers.IO) {
            runCatching {
                decodeContactPhoto(context, uriString, contactId, sizePx)
                    ?.asImageBitmap()
                    ?.also { photoCache.put(cacheKey, it) }
            }.getOrNull()
        }
    }

    private fun decodeContactPhoto(
        context: Context,
        uriString: String?,
        contactId: Long?,
        sizePx: Int
    ): Bitmap? {
        val resolver = context.contentResolver

        // 1) nagy felbontású névjegy-fotó
        if (contactId != null) {
            runCatching {
                val contactUri = ContentUris.withAppendedId(
                    ContactsContract.Contacts.CONTENT_URI,
                    contactId
                )
                ContactsContract.Contacts
                    .openContactPhotoInputStream(resolver, contactUri, true)
                    ?.use { stream -> decodeSquare(stream.readBytes(), sizePx) }
            }.getOrNull()?.let { return it }
        }

        // 2) a photo URI teljes felbontásban
        if (!uriString.isNullOrBlank()) {
            runCatching {
                resolver.openInputStream(Uri.parse(uriString))
                    ?.use { stream -> decodeSquare(stream.readBytes(), sizePx) }
            }.getOrNull()?.let { return it }
        }

        return null
    }

    /** Teljes felbontású dekódolás, majd középre igazított, simán kicsinyített négyzet. */
    private fun decodeSquare(bytes: ByteArray, sizePx: Int): Bitmap? {
        if (bytes.isEmpty()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null

        // Lépcsőzetes mintavételezés: a dekódolt kép 1x és 2x célméret között marad,
        // így a végső kicsinyítés sima és éles lesz.
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= sizePx) sample *= 2

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null

        val side = minOf(decoded.width, decoded.height)
        if (side <= 0) return null

        val square = if (decoded.width == decoded.height) {
            decoded
        } else {
            Bitmap.createBitmap(
                decoded,
                (decoded.width - side) / 2,
                (decoded.height - side) / 2,
                side,
                side
            )
        }

        // Soha nem nagyítunk fel: ha a forrás kisebb a kértnél, marad az eredeti méret.
        val target = minOf(sizePx, side)
        return if (side == target) square
        else Bitmap.createScaledBitmap(square, target, target, true)
    }

    /**
     * A névjegyhez tartozó első raw contact azonosítója.
     * Erre kell a név és a profilkép rendszer-névjegybe írásához.
     */
    fun rawContactId(resolver: ContentResolver, contactId: Long): Long? = runCatching {
        resolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
    }.getOrNull()
}

