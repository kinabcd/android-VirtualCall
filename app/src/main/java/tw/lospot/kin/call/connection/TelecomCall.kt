package tw.lospot.kin.call.connection

import android.telecom.Conferenceable
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.Flow

/**
 * Created by kin on 2018/2/28.
 */
interface TelecomCall {
    companion object {
        const val PROPERTY_HIGH_DEF_AUDIO = 1 shl 2
        const val PROPERTY_WIFI = 1 shl 3
        var callCount = 0
    }

    val id: Int

    val onStateChanged: Flow<Unit>
    val onPlayDtmfTone: Flow<Char>
    val name: String
    val state: Int
    val conferenceable: Conferenceable
    var conferenceables: List<Conferenceable>
    val videoState: Int
    val phoneAccountHandle: PhoneAccountHandle
    var isWifiCall: Boolean
    var isHdAudio: Boolean
    val isConference: Boolean
    val isExternal: Boolean
    val hasParent: Boolean
    val children: List<Connection>
    fun answer(videoState: Int = this.videoState) = Unit
    fun unhold()
    fun hold()
    fun disconnect(disconnectCause: DisconnectCause = DisconnectCause(DisconnectCause.UNKNOWN))
    fun pullExternalCall()
    fun pushInternalCall()
    fun requestRtt() = Unit
    fun requestVideo(state: Int = VideoProfile.STATE_BIDIRECTIONAL) = Unit
    fun toggleRxVideo() = requestVideo(videoState xor VideoProfile.STATE_RX_ENABLED)
    fun maybeUnboxConference() = Unit
}