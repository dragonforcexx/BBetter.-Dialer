package com.bbetterdialer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bbetterdialer.data.AppPreferences
import com.bbetterdialer.data.AvatarOverrides
import com.bbetterdialer.data.ContactFolders
import com.bbetterdialer.data.ContactOverrides
import com.bbetterdialer.data.RingtoneSettings
import com.bbetterdialer.ui.DialerApp

/**
 * Az egyetlen Activity. A teljes felület Jetpack Compose.
 *
 * Kezeli a külső indításokat:
 *   • `tel:+3630...` linkek / ACTION_DIAL  -> a szám a kijelzőre kerül
 *   • a Telecom szolgáltatás által kért hívásképernyő megjelenítése
 */
class MainActivity : ComponentActivity() {

    private var incomingNumber by mutableStateOf("")
    private var showInCall by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A helyi tárolók betöltése még az első kompozíció előtt, hogy a
        // Kontaktok fül és a beállítások rögtön a mentett adatokkal induljanak.
        ContactFolders.init(this)
        RingtoneSettings.init(this)
        AvatarOverrides.init(this)
        ContactOverrides.init(this)
        AppPreferences.init(this)
        // A launcher ikon és név szinkronban tartása a mentett beállítással.
        AppPreferences.applyLauncherAlias(this)
        consume(intent)

        setContent {
            DialerApp(
                pendingNumber = incomingNumber,
                onPendingNumberConsumed = { incomingNumber = "" },
                showInCallRequest = showInCall,
                onShowInCallConsumed = { showInCall = false }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consume(intent)
    }

    private fun consume(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_DIAL, Intent.ACTION_VIEW -> {
                val data = intent.data
                val raw = data?.schemeSpecificPart ?: data?.toString().orEmpty()
                if (raw.isNotBlank()) incomingNumber = Uri.decode(raw)
            }
        }

        if (intent.getBooleanExtra(EXTRA_SHOW_IN_CALL, false)) {
            showInCall = true
        }
    }

    companion object {
        const val EXTRA_SHOW_IN_CALL = "show_incall"
    }
}
