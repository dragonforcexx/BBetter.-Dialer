package com.bbetterdialer.telecom

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.bbetterdialer.data.PhoneRepository

/**
 * Kimenő hívás indítása.
 *
 * Ha van CALL_PHONE jogosultság, azonnal tárcsázunk (`ACTION_CALL`),
 * egyébként a rendszer tárcsázóját nyitjuk meg előtöltött számmal (`ACTION_DIAL`).
 * Így az app akkor is használható, ha a felhasználó megtagadta a hívási jogot.
 */
object CallController {

    fun place(context: Context, number: String): Boolean {
        val clean = number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (clean.isBlank()) return false

        val uri = Uri.fromParts("tel", clean, null)
        val intent = if (PhoneRepository.hasCallPhone(context)) {
            Intent(Intent.ACTION_CALL, uri)
        } else {
            Intent(Intent.ACTION_DIAL, uri)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
