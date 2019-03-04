package tw.lospot.kin.call.connection

import android.content.Context
import android.os.Build
import android.telecom.*
import android.telecom.Connection.*
import androidx.annotation.RequiresApi
import tw.lospot.kin.call.Log

/**
 * Conference emulator
 * Created by Kin_Lo on 2017/8/9.
 */
class ConferenceProxy(val context: Context, override val phoneAccountHandle: PhoneAccountHandle) :
        TelecomCall.Common {

    val telecomConference = object : Conference(phoneAccountHandle) {
        init {
            connectionCapabilities = connectionCapabilities
                    .or(Connection.CAPABILITY_SUPPORT_HOLD)
                    .or(Connection.CAPABILITY_HOLD)
                    .or(Connection.CAPABILITY_MUTE)
                    .or(Connection.CAPABILITY_MANAGE_CONFERENCE)
            setActive()
        }

        override fun getVideoProvider(): Connection.VideoProvider = VideoProvider(context)
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
            if (connection === null) {
                return
            }
            connection.connectionCapabilities = connection.connectionCapabilities
                    .or(CAPABILITY_DISCONNECT_FROM_CONFERENCE)
                    .or(CAPABILITY_SEPARATE_FROM_CONFERENCE)
            connection.setActive()
            unhold()
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
    }

    override var listener: TelecomCall.Listener? = null
    override val conferenceable: Conferenceable get() = telecomConference
    override val state: Int get() = telecomConference.state
    override val videoState: Int = VideoProfile.STATE_AUDIO_ONLY
    private val childrenCount get() = telecomConference.connections.size

    private fun notifyStateChanged(state: Int) {
        listener?.onStateChanged(state)
    }

    fun maybeDestroy() {
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

    override fun answer(videoState: Int) {
    }

    override fun isExternal(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 &&
                    telecomConference.connectionProperties.and(android.telecom.Connection.PROPERTY_IS_EXTERNAL_CALL) > 0

    override fun pullExternalCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            telecomConference.connectionCapabilities = telecomConference.connectionCapabilities
                    .and(android.telecom.Connection.CAPABILITY_CAN_PULL_CALL.inv())
            telecomConference.connectionProperties = telecomConference.connectionProperties
                    .and(android.telecom.Connection.PROPERTY_IS_EXTERNAL_CALL.inv())
            notifyStateChanged(state)
        }
    }

    override fun pushInternalCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            telecomConference.connectionCapabilities = telecomConference.connectionCapabilities
                    .or(android.telecom.Connection.CAPABILITY_CAN_PULL_CALL)
            telecomConference.connectionProperties = telecomConference.connectionProperties
                    .or(android.telecom.Connection.PROPERTY_IS_EXTERNAL_CALL)
            notifyStateChanged(state)
        }
    }

    override fun hold() {
        telecomConference.setOnHold()
        notifyStateChanged(state)
    }

    override fun unhold() {
        telecomConference.setActive()
        notifyStateChanged(state)
    }

    override fun disconnect(disconnectCause: DisconnectCause) {
        disconnectAllChildren()
        telecomConference.setDisconnected(disconnectCause)
        notifyStateChanged(state)
        telecomConference.destroy()
    }

    override fun requestRtt() {
    }

    private fun removeDisconnectedChildren() {
        telecomConference.connections
                .filter { it.state == STATE_DISCONNECTED }
                .forEach { telecomConference.removeConnection(it) }
    }

    private fun disconnectAllChildren() {
        telecomConference.connections.forEach {
            it.onDisconnect()
        }
    }
    override var isWifiCall: Boolean
        @RequiresApi(25)
        get() = hasProperty(TelecomCall.PROPERTY_WIFI)
        @RequiresApi(25)
        set(value) {
            setProperty(TelecomCall.PROPERTY_WIFI, value)
        }
    override var isHdAudio: Boolean
        @RequiresApi(25)
        get() = hasProperty(TelecomCall.PROPERTY_HIGH_DEF_AUDIO)
        @RequiresApi(25)
        set(value) {
            setProperty(TelecomCall.PROPERTY_HIGH_DEF_AUDIO, value)
        }

    @RequiresApi(25)
    fun hasProperty(property: Int) = telecomConference.connectionProperties and property != 0

    @RequiresApi(25)
    fun setProperty(property: Int, on: Boolean) {
        telecomConference.connectionProperties = if (on) {
            telecomConference.connectionProperties.or(property)
        } else {
            telecomConference.connectionProperties.and(property.inv())
        }
    }
}