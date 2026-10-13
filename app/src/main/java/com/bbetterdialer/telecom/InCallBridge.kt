package com.bbetterdialer.telecom

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bbetterdialer.data.InCallState

/**
 * A Compose UI és a Telecom [android.telecom.InCallService] közötti híd.
 *
 * A szolgáltatás ide publikálja a hívás állapotát, a felület pedig innen
 * olvassa. Mivel Compose `mutableStateOf`, a recomposition automatikus.
 */
object InCallBridge {

    private var _state by mutableStateOf<InCallState?>(null)

    /** Az éppen futó hívás állapota, vagy null, ha nincs hívás. */
    val state: InCallState? get() = _state

    /** A Telecom által regisztrált hívást vezérlő műveletek. */
    private var _actions: CallActions? = null

    val actions: CallActions? get() = _actions

    fun publish(newState: InCallState?) {
        _state = newState
        if (newState == null) _actions = null
    }

    fun attachActions(callActions: CallActions) {
        _actions = callActions
    }
}

/** Hívásvezérlés absztrakció – így a UI nem függ közvetlenül a Telecom API-tól. */
interface CallActions {
    fun end()
    fun answer()
    fun setMuted(muted: Boolean)
    fun setSpeaker(on: Boolean)
}
