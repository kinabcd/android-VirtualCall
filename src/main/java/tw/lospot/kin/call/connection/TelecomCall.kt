package tw.lospot.kin.call.connection

import android.telecom.DisconnectCause

/**
 * Created by kin on 2018/2/28.
 */
object TelecomCall {
    interface Listener {
        fun onStateChanged(state: Int)
        fun onPlayDtmfTone(c: Char)
    }

    interface Common {
        var listener: Listener?
        fun getState(): Int
        fun unhold()
        fun hold()
        fun disconnect(disconnectCause: DisconnectCause)
        fun isExternal(): Boolean
        fun pullExternalCall()
        fun pushInternalCall()
    }
}