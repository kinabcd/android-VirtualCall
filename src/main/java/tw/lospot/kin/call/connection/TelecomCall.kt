package tw.lospot.kin.call.connection

import android.telecom.Conferenceable
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle

/**
 * Created by kin on 2018/2/28.
 */
object TelecomCall {
    const val EXTRA_DELAY_DISCONNECT = "tw.lospot.kin.call.delay_disconnect"
    const val EXTRA_DELAY_REJECT = "tw.lospot.kin.call.delay_reject"
    const val EXTRA_DELAY_ANSWER = "tw.lospot.kin.call.delay_answer"
    const val EXTRA_HIGH_DEF_AUDIO = "tw.lospot.kin.call.hd_audio"
    const val EXTRA_WIFI = "tw.lospot.kin.call.wifi"

    const val PROPERTY_HIGH_DEF_AUDIO = 1 shl 2
    const val PROPERTY_WIFI = 1 shl 3

    interface Listener {
        fun onStateChanged(state: Int)
        fun onPlayDtmfTone(c: Char)
    }

    interface Common {
        var listener: Listener?
        val state: Int
        val conferenceable: Conferenceable
        val videoState: Int
        val phoneAccountHandle: PhoneAccountHandle
        var isWifiCall: Boolean
        var isHdAudio: Boolean
        fun answer(videoState: Int)
        fun unhold()
        fun hold()
        fun disconnect(disconnectCause: DisconnectCause)
        fun isExternal(): Boolean
        fun pullExternalCall()
        fun pushInternalCall()
        fun requestRtt()
    }
}