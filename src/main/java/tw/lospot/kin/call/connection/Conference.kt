package tw.lospot.kin.call.connection

import android.content.Context
import android.os.Build
import android.telecom.Connection
import android.telecom.Connection.*
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.VideoProfile
import tw.lospot.kin.call.Log

/**
 * Conference emulator
 * Created by Kin_Lo on 2017/8/9.
 */
class Conference(val context: Context, phoneAccountHandle: PhoneAccountHandle) :
        android.telecom.Conference(phoneAccountHandle),
        TelecomCall.Common {
    init {
        connectionCapabilities = connectionCapabilities
                .or(Connection.CAPABILITY_SUPPORT_HOLD)
                .or(Connection.CAPABILITY_HOLD)
                .or(Connection.CAPABILITY_MUTE)
                .or(Connection.CAPABILITY_MANAGE_CONFERENCE)
    }

    override var listener: TelecomCall.Listener? = null

    private fun onStateChanged(state: Int) {
        listener?.onStateChanged(state)
    }

    override fun getVideoProvider(): Connection.VideoProvider = VideoProvider(context)

    override fun getVideoState(): Int = VideoProfile.STATE_AUDIO_ONLY

    override fun onConnectionAdded(connection: Connection?) {
        super.onConnectionAdded(connection)
        if (connection === null) {
            return
        }
        connection.connectionCapabilities = connection.connectionCapabilities
                .or(CAPABILITY_DISCONNECT_FROM_CONFERENCE)
                .or(CAPABILITY_SEPARATE_FROM_CONFERENCE)
        connection.setActive()
        onStateChanged(state)
    }

    override fun onMerge(connection: Connection?) {
        super.onMerge(connection)
        if (connection === null) {
            return
        }
        addConnection(connection)
        onUnhold()
    }

    override fun onSeparate(connection: Connection?) {
        if (connection === null) {
            return
        }
        connection.connectionCapabilities = connection.connectionCapabilities
                .and(CAPABILITY_DISCONNECT_FROM_CONFERENCE.inv())
                .and(CAPABILITY_SEPARATE_FROM_CONFERENCE.inv())
        removeConnection(connection)
        connection.setActive()
        connection.onStateChanged(connection.state)
        maybeDestroy()
    }

    override fun onHold() {
        hold()
    }

    override fun onUnhold() {
        unhold()
    }

    override fun onDisconnect() {
        disconnect(DisconnectCause(DisconnectCause.LOCAL))
    }

    override fun onPlayDtmfTone(c: Char) {
        super.onPlayDtmfTone(c)
        listener?.onPlayDtmfTone(c)
    }

    fun maybeDestroy() {
        Log.d(this, "connections.size " + connections.size)
        connections
                .filter { it.state == STATE_DISCONNECTED }
                .forEach { removeConnection(it) }
        if (connections.size < 2) {
            connections.forEach {
                removeConnection(it)
                if (state == STATE_ACTIVE)
                    it.setActive()
                onDisconnect()
            }
        }
    }

    override fun isExternal(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 &&
                    connectionProperties.and(android.telecom.Connection.PROPERTY_IS_EXTERNAL_CALL) > 0

    override fun pullExternalCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            connectionCapabilities = connectionCapabilities
                    .and(android.telecom.Connection.CAPABILITY_CAN_PULL_CALL.inv())
            connectionProperties = connectionProperties
                    .and(android.telecom.Connection.PROPERTY_IS_EXTERNAL_CALL.inv())
            onStateChanged(state)
        }
    }

    override fun pushInternalCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            connectionCapabilities = connectionCapabilities
                    .or(android.telecom.Connection.CAPABILITY_CAN_PULL_CALL)
            connectionProperties = connectionProperties
                    .or(android.telecom.Connection.PROPERTY_IS_EXTERNAL_CALL)
            onStateChanged(state)
        }
    }

    override fun hold() {
        setOnHold()
        onStateChanged(state)
    }

    override fun unhold() {
        setActive()
        onStateChanged(state)
    }

    override fun disconnect(disconnectCause: DisconnectCause) {
        connections.forEach { it.onDisconnect() }
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        onStateChanged(state)
        destroy()
    }
}