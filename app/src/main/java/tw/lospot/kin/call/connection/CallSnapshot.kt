package tw.lospot.kin.call.connection

import android.telecom.Conferenceable
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import androidx.compose.runtime.Stable


data class CallSnapshot(
    @Stable val rawCall: TelecomCall,
    val accountHandle: PhoneAccountHandle,
    val id: Int,
    val state: Int,
    val name: String,
    val isExternal: Boolean,
    val isConference: Boolean,
    val hasParent: Boolean,
    val children: List<CallSnapshot>,
) {
    constructor(telecomCall: TelecomCall, calls: Map<Conferenceable, TelecomCall>) : this(
        rawCall = telecomCall,
        accountHandle = telecomCall.phoneAccountHandle,
        id = telecomCall.id,
        state = telecomCall.state,
        name = telecomCall.name,
        isExternal = telecomCall.isExternal,
        isConference = telecomCall.isConference,
        hasParent = telecomCall.hasParent,
        children = telecomCall.children.mapNotNull { connection ->
            calls[connection]?.let { CallSnapshot(it, calls) }
        },
    )

    fun answer() = rawCall.answer()
    fun disconnect(disconnectCause: DisconnectCause = DisconnectCause(DisconnectCause.UNKNOWN)) =
        rawCall.disconnect(disconnectCause)

    fun pull() = rawCall.pullExternalCall()
    fun push() = rawCall.pushInternalCall()
    fun toggleRxVideo() = rawCall.toggleRxVideo()
    fun requestVideo(videoState: Int) = rawCall.requestVideo(videoState)
}