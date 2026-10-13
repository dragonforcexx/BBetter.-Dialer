package com.bbetterdialer.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Új névjegy létrehozása a rendszer névjegyadatbázisában.
 *
 * A tárcsázóba beírt, ismeretlen számhoz innen jön létre névjegy – a
 * felhasználó megadja a nevet és a profilszínt, mi pedig létrehozzuk a
 * raw contactot, a nevet és a telefonszámot.
 *
 * `WRITE_CONTACTS` jogosultság kell hozzá.
 */
object ContactCreator {

    sealed interface Result {
        data class Success(val contactId: Long) : Result
        data class Failure(val message: String) : Result
    }

    suspend fun create(
        context: Context,
        name: String,
        number: String
    ): Result = withContext(Dispatchers.IO) {
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty()) return@withContext Result.Failure("empty-name")

        val contactId = runCatching {
            val resolver = context.contentResolver

            val rawUri = resolver.insert(
                ContactsContract.RawContacts.CONTENT_URI,
                ContentValues().apply {
                    put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?)
                    put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?)
                }
            ) ?: return@runCatching null

            val rawContactId = ContentUris.parseId(rawUri)

            resolver.insert(
                ContactsContract.Data.CONTENT_URI,
                ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, cleanName)
                    put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, cleanName)
                }
            )

            resolver.insert(
                ContactsContract.Data.CONTENT_URI,
                ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    put(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                    put(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                }
            )

            resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.CONTACT_ID),
                "${ContactsContract.RawContacts._ID} = ?",
                arrayOf(rawContactId.toString()),
                null
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
        }.getOrNull()

        if (contactId == null) Result.Failure("failed") else Result.Success(contactId)
    }
}
