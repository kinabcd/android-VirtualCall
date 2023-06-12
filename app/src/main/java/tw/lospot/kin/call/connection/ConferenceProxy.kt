package tw.lospot.kin.call.connection

import android.content.Context
import android.os.Build
import android.telecom.Conference
import android.telecom.Conferenceable
import android.telecom.Connection
import android.telecom.Connection.CAPABILITY_CAN_PULL_CALL
import android.telecom.Connection.CAPABILITY_DISCONNECT_FROM_CONFERENCE
import android.telecom.Connection.CAPABILITY_HOLD
import android.telecom.Connection.CAPABILITY_MANAGE_CONFERENCE
import android.telecom.Connection.CAPABILITY_MUTE
import android.telecom.Connection.CAPABILITY_SEPARATE_FROM_CONFERENCE
import android.telecom.Connection.CAPABILITY_SUPPORT_HOLD
import android.telecom.Connection.PROPERTY_IS_EXTERNAL_CALL
import android.telecom.Connection.STATE_ACTIVE
import android.telecom.Connection.STATE_DISCONNECTED
import android.telecom.Connection.STATE_RINGING
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.VideoProfile
import androidx.annotation.RequiresApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import tw.lospot.kin.call.Log

/**
 * Conference emulator
 * Created by Kin_Lo on 2017/8/9.
 */
class ConferenceProxy(
    val context: Context,
    override val phoneAccountHandle: PhoneAccountHandle
) : TelecomCall {
    override val id: Int = TelecomCall.callCount++
    private val scope = MainScope()
    override val onStateChanged: MutableSharedFlow<Unit> = MutableSharedFlow()
    override val onPlayDtmfTone: MutableSharedFlow<Char> = MutableSharedFlow()

    val telecomConference = object : Conference(phoneAccountHandle) {
        init {
            connectionCapabilities = connectionCapabilities
                .or(CAPABILITY_SUPPORT_HOLD)
                .or(CAPABILITY_HOLD)
                .or(CAPABILITY_MUTE)
                .or(CAPABILITY_MANAGE_CONFERENCE)
            setActive()
        }

        override fun getVideoProvider(): Connection.VideoProvider? = null
        override fun getVideoState(): Int = VideoProfile.STATE_AUDIO_ONLY
        override fun onConnectionAdded(connection: Connection?) {
            if (connection === null) {
                return
            }
            connection.connectionCapabilities = connection.connectionCapabilities
                .or(CAPABILITY_DISCONNECT_FROM_CONFERENCE)
                .or(CAPABILITY_SEPARATE_FROM_CONFERENCE)
            connection.setActive()
        }

        override fun onMerge(connection: Connection?) {
            if (connection === null) return
            addConnection(connection)
            unhold()
        }

        override fun onSeparate(connection: Connection?) {
            if (connection === null) return
            connection.connectionCapabilities = connection.connectionCapabilities
                .and(CAPABILITY_DISCONNECT_FROM_CONFERENCE.inv())
                .and(CAPABILITY_SEPARATE_FROM_CONFERENCE.inv())
            removeConnection(connection)
            when (connections.size) {
                0 -> destroy()
                1 -> {
                    if (state == STATE_ACTIVE) connection.setActive() else connection.setOnHold()
                    val anotherConnection = connections[0]
                    anotherConnection.setOnHold()
                    anotherConnection.connectionCapabilities =
                        anotherConnection.connectionCapabilities
                            .and(CAPABILITY_DISCONNECT_FROM_CONFERENCE.inv())
                            .and(CAPABILITY_SEPARATE_FROM_CONFERENCE.inv())
                    removeConnection(anotherConnection)
                    anotherConnection.onStateChanged(anotherConnection.state)
                    setDisconnected(DisconnectCause(DisconnectCause.OTHER))
                    destroy()
                }

                else -> connection.setOnHold()
            }
            connection.onStateChanged(connection.state)
            notifyStateChanged()
        }

        override fun onHold() = hold()
        override fun onUnhold() = unhold()
        override fun onDisconnect() = disconnect(DisconnectCause(DisconnectCause.LOCAL))
        override fun onPlayDtmfTone(c: Char) {
            scope.launch { onPlayDtmfTone.emit(c) }
        }
    }
    override val conferenceable: Conferenceable get() = telecomConference
    override var conferenceables: List<Conferenceable>
        get() = telecomConference.conferenceableConnections
        set(value) {
            telecomConference.conferenceableConnections = value.mapNotNull { it as? Connection }
        }
    override val name: String = "Conference Call"
    override val state: Int get() = telecomConference.state
    override val videoState: Int = VideoProfile.STATE_AUDIO_ONLY
    override var isExternal: Boolean
        @RequiresApi(Build.VERSION_CODES.N_MR1)
        get() = hasProperty(PROPERTY_IS_EXTERNAL_CALL)
        @RequiresApi(Build.VERSION_CODES.N_MR1)
        set(value) = setProperty(PROPERTY_IS_EXTERNAL_CALL, value)
    override val hasParent: Boolean = false
    override val children: List<Connection> get() = telecomConference.connections
    override var isWifiCall: Boolean
        get() = hasProperty(TelecomCall.PROPERTY_WIFI)
        set(value) = setProperty(TelecomCall.PROPERTY_WIFI, value)
    override var isHdAudio: Boolean
        get() = hasProperty(TelecomCall.PROPERTY_HIGH_DEF_AUDIO)
        set(value) = setProperty(TelecomCall.PROPERTY_HIGH_DEF_AUDIO, value)

    override val isConference: Boolean = true
    private val childrenCount get() = children.size

    private fun notifyStateChanged() {
        scope.launch { onStateChanged.emit(Unit) }
    }

    override fun maybeUnboxConference() {
        Log.d(this, "connections.size $childrenCount")
        removeDisconnectedChildren()
        val firstConnection = telecomConference.connections[0]
        val oldState = state
        if (childrenCount == 1) {
            telecomConference.removeConnection(firstConnection)
        }
        if (childrenCount == 0) {
            disconnect(DisconnectCause(DisconnectCause.OTHER))
            if (oldState == STATE_ACTIVE) {
                firstConnection?.setActive()
            }
        }
    }

    override fun pullExternalCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            telecomConference.connectionCapabilities = telecomConference.connectionCapabilities
                .and(CAPABILITY_CAN_PULL_CALL.inv())
            isExternal = false
            notifyStateChanged()
        }
    }

    override fun pushInternalCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            telecomConference.connectionCapabilities = telecomConference.connectionCapabilities
                .or(CAPABILITY_CAN_PULL_CALL)
            isExternal = true
            notifyStateChanged()
        }
    }

    override fun hold() {
        telecomConference.setOnHold()
        notifyStateChanged()
    }

    override fun unhold() {
        telecomConference.setActive()
        notifyStateChanged()
    }

    override fun disconnect(disconnectCause: DisconnectCause) {
        telecomConference.connections.forEach { it.onDisconnect() }
        telecomConference.setDisconnected(
            when {
                disconnectCause.code != DisconnectCause.UNKNOWN -> disconnectCause
                else -> when (state) {
                    STATE_RINGING -> DisconnectCause(DisconnectCause.MISSED)
                    else -> DisconnectCause(DisconnectCause.REMOTE)
                }
            }
        )
        notifyStateChanged()
        telecomConference.destroy()
    }

    private fun removeDisconnectedChildren() {
        telecomConference.connections
            .filter { it.state == STATE_DISCONNECTED }
            .forEach { telecomConference.removeConnection(it) }
    }

    private fun hasProperty(property: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return false
        return telecomConference.connectionProperties and property != 0
    }

    private fun setProperty(property: Int, on: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        telecomConference.connectionProperties = if (on) {
            telecomConference.connectionProperties.or(property)
        } else {
            telecomConference.connectionProperties.and(property.inv())
        }
    }
}