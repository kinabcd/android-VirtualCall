package tw.lospot.kin.call.connection

import android.telecom.Connection
import android.telecom.DisconnectCause
import android.telecom.VideoProfile
import tw.lospot.kin.call.Log
import java.util.concurrent.CopyOnWriteArraySet

class Call(val telecomCall: TelecomCall.Common) : TelecomCall.Listener {
    interface Listener {
        fun onCallStateChanged(call: Call, newState: Int)
    }

    companion object {
        var callCount = 0
    }

    private val listeners = CopyOnWriteArraySet<Listener>()
    var id = callCount++
    val phoneAccountHandle get() = telecomCall.phoneAccountHandle

    init {
        telecomCall.listener = this
    }

    override fun onStateChanged(state: Int) {
        notifyStateChange()
    }

    override fun onPlayDtmfTone(c: Char) {
        when (c) {
            '1' -> toggleRxVideo()
            '2' -> push()
            '3' -> requestRtt()
        }
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    private fun notifyStateChange() {
        listeners.forEach {
            it.onCallStateChanged(this, state)
        }
    }

    val name
        get() = when (telecomCall) {
            is ConferenceProxy -> "Conference Call"
            is ConnectionProxy -> telecomCall.telecomConnection.address?.schemeSpecificPart
                    ?: ""
            else -> ""
        }

    fun hold() {
        telecomCall.hold()
    }

    fun unhold() {
        telecomCall.unhold()
    }

    fun disconnect(disconnectCause: DisconnectCause = DisconnectCause(DisconnectCause.REMOTE)) {
        telecomCall.disconnect(disconnectCause)
    }

    val state: Int
        get() = telecomCall.state

    var conferenceables: List<Call> = emptyList()
        set(calls) {
            field = when {
                isConference -> calls.filter { !it.isConference && it != this }
                else -> calls.filter { it != this }
            }

            if (telecomCall is ConferenceProxy) {
                telecomCall.telecomConference.conferenceableConnections =
                        conferenceables.map { it.telecomCall.conferenceable as Connection }
            } else if (telecomCall is ConnectionProxy) {
                telecomCall.telecomConnection.conferenceables =
                        conferenceables.map { it.telecomCall.conferenceable }
            }
        }

    val hasParent: Boolean
        get() = telecomCall is ConnectionProxy && telecomCall.telecomConnection.conference != null

    val children: List<Call>
        get() {
            if (telecomCall !is ConferenceProxy) {
                return emptyList()
            }
            val children = ArrayList<Call>()
            telecomCall.telecomConference.connections
                    .map { CallList.getCall(it) }
                    .forEach { if (it != null) children.add(it) }
            return children
        }

    val isConference: Boolean
        get() = telecomCall is ConferenceProxy

    val isExternal: Boolean
        get() = telecomCall.isExternal()

    fun maybeDestroy() {
        if (telecomCall is ConferenceProxy) {
            telecomCall.maybeDestroy()
        }
    }

    fun pull() {
        if (isExternal) {
            telecomCall.pullExternalCall()
        }
    }

    fun push() {
        if (!isExternal) {
            telecomCall.pushInternalCall()
        }
    }

    fun answer() {
        telecomCall.answer(telecomCall.videoState)
    }

    fun requestVideo(videoState: Int = VideoProfile.STATE_BIDIRECTIONAL) {
        if (telecomCall is ConnectionProxy && telecomCall.videoState != videoState) {
            val videoProfile = VideoProfile(videoState)
            Log.v(this, "requestVideo $videoProfile")
            val isUpgrade = (telecomCall.videoState.and(VideoProfile.STATE_RX_ENABLED) == 0 && videoState.and(VideoProfile.STATE_RX_ENABLED) != 0)
                    || (telecomCall.videoState.and(VideoProfile.STATE_TX_ENABLED) == 0 && videoState.and(VideoProfile.STATE_TX_ENABLED) != 0)
            if (isUpgrade) {
                telecomCall.videoProvider.receiveSessionModifyRequest(videoProfile)
            } else {
                telecomCall.videoState = videoState
            }
        }
    }

    fun toggleRxVideo() {
        if (telecomCall is ConnectionProxy) {
            val nowVideoState = telecomCall.videoState
            val newVideoState = nowVideoState xor VideoProfile.STATE_RX_ENABLED
            requestVideo(newVideoState)
        }
    }

    fun requestRtt() {
        telecomCall.requestRtt()
    }
}