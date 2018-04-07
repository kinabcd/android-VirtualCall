package tw.lospot.kin.call.connection

import android.telecom.Conferenceable
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

    init {
        telecomCall.listener = this
    }

    override fun onStateChanged(state: Int) {
        notifyStateChange()
    }

    override fun onPlayDtmfTone(c: Char) {
        if (c == '1') {
            toggleRxVideo()
        } else if (c == '2') {
            push()
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

    fun getName(): String {
        if (telecomCall is Conference) {
            return "Conference Call"
        } else if (telecomCall is Connection) {
            return telecomCall.address.schemeSpecificPart
        }
        return ""
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
        get() = telecomCall.getState()

    var conferenceable: List<Call> = emptyList()
        set(calls) {
            field = when {
                isConference -> calls.filter { !it.isConference && it != this }
                else -> calls.filter { it != this }
            }

            if (telecomCall is Conference) {
                telecomCall.conferenceableConnections =
                        conferenceable.map { it.telecomCall as Connection }
            } else if (telecomCall is Connection) {
                telecomCall.conferenceables =
                        conferenceable.map { it.telecomCall as Conferenceable }
            }
        }

    val hasParent: Boolean
        get() = telecomCall is Connection && telecomCall.conference != null

    val children: List<Call>
        get() {
            val children = ArrayList<Call>()
            if (telecomCall is Conference) {
                telecomCall.connections
                        .map { it -> CallList.getCall(it as Connection) }
                        .forEach { if (it != null) children.add(it) }
            }
            return children
        }

    val isConference: Boolean
        get() = telecomCall is Conference

    val isExternal: Boolean
        get() = telecomCall.isExternal()

    fun maybeDestroy() {
        if (telecomCall is Conference) {
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
        if (telecomCall is Connection) {
            telecomCall.onAnswer(telecomCall.vState)
        }
    }

    fun requestVideo(videoState: Int = VideoProfile.STATE_BIDIRECTIONAL) {
        if (telecomCall is Connection && telecomCall.vState != videoState) {
            val videoProfile = VideoProfile(videoState)
            Log.v(this, "requestVideo $videoProfile")
            val isUpgrade = (telecomCall.vState.and(VideoProfile.STATE_RX_ENABLED) == 0 && videoState.and(VideoProfile.STATE_RX_ENABLED) != 0)
                    || (telecomCall.vState.and(VideoProfile.STATE_TX_ENABLED) == 0 && videoState.and(VideoProfile.STATE_TX_ENABLED) != 0)
            if (isUpgrade) {
                telecomCall.videoProvider.receiveSessionModifyRequest(videoProfile)
            } else {
                telecomCall.vState = videoState
            }
        }
    }

    fun toggleRxVideo() {
        if (telecomCall is Connection) {
            val nowVideoState = telecomCall.vState
            val newVideoState = nowVideoState xor VideoProfile.STATE_RX_ENABLED
            requestVideo(newVideoState)
        }
    }
}