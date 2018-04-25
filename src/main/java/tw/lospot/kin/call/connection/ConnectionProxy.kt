package tw.lospot.kin.call.connection


import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.telecom.*
import android.telecom.Connection.*
import tw.lospot.kin.call.Log

/**
 * Connection emulator
 * Created by Kin_Lo on 2017/8/9.
 */

class ConnectionProxy(context: Context, address: Uri, override val phoneAccountHandle: PhoneAccountHandle) :
        TelecomCall.Common {
    val videoProvider = VideoProvider(context)
    val telecomConnection = object : Connection() {
        init {
            setAddress(address, TelecomManager.PRESENTATION_ALLOWED)
            connectionCapabilities = connectionCapabilities
                    .or(CAPABILITY_SUPPORT_HOLD)
                    .or(CAPABILITY_HOLD)
                    .or(CAPABILITY_MUTE)
                    .or(CAPABILITY_RESPOND_VIA_TEXT)
                    .or(CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO)
        }

        override fun onStateChanged(state: Int) {
            notifyStateChanged()
        }

        override fun onDisconnect() {
            disconnect(DisconnectCause(DisconnectCause.LOCAL))
        }

        override fun onAbort() {
            disconnect(DisconnectCause(DisconnectCause.UNKNOWN))
        }

        override fun onHold() {
            hold()
        }

        override fun onUnhold() {
            unhold()
        }

        override fun onAnswer() {
            onAnswer(VideoProfile.STATE_AUDIO_ONLY)
        }

        override fun onAnswer(vs: Int) {
            answer(vs)
        }

        override fun onReject() {
            onReject(null)
        }

        override fun onReject(replyMessage: String?) {
            disconnect(DisconnectCause(DisconnectCause.REJECTED))
        }

        override fun onCallEvent(event: String, extras: Bundle) {
            super.onCallEvent(event, extras)
            Log.v(this, "onCallEvent $event")
        }

        override fun onPullExternalCall() {
            pullExternalCall()
        }

        override fun onExtrasChanged(extras: Bundle) {
            Log.v(this, "onExtrasChanged $extras")
        }

        override fun sendConnectionEvent(event: String, extras: Bundle) {
            super.sendConnectionEvent(event, extras)
            Log.v(this, "sendConnectionEvent $event $extras")
        }

        override fun onPlayDtmfTone(c: Char) {
            listener?.onPlayDtmfTone(c)
        }

        override fun onSeparate() {
        }
    }

    init {
        videoProvider.connection = this
        telecomConnection.videoProvider = videoProvider
    }

    override var listener: TelecomCall.Listener? = null

    override val conferenceable: Conferenceable get() = telecomConnection
    override val state: Int get() = telecomConnection.state
    override var videoState: Int
        get() = videoProvider.videoState
        set(value) {
            telecomConnection.setVideoState(value)
            videoProvider.videoState = value
        }

    private fun notifyStateChanged() {
        telecomConnection.connectionCapabilities = when (state) {
            Connection.STATE_ACTIVE -> telecomConnection.connectionCapabilities
                    .or(CAPABILITY_CAN_UPGRADE_TO_VIDEO)
                    .or(CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL)
                    .or(CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL)
                    .and(CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO.inv())
            else -> telecomConnection.connectionCapabilities
                    .and(CAPABILITY_CAN_UPGRADE_TO_VIDEO.inv())
                    .and(CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL.inv())
                    .and(CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL.inv())
                    .or(CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO)
        }
        listener?.onStateChanged(state)
    }

    override fun answer(videoState: Int) {
        this.videoState = videoState
        telecomConnection.setActive()
    }

    override fun hold() {
        telecomConnection.setOnHold()
    }

    override fun unhold() {
        telecomConnection.setActive()
    }

    override fun disconnect(disconnectCause: DisconnectCause) {
        videoState = VideoProfile.STATE_AUDIO_ONLY
        videoProvider.onSetPreviewSurface(null)
        videoProvider.onSetDisplaySurface(null)
        telecomConnection.setDisconnected(disconnectCause)
        notifyStateChanged()
        telecomConnection.destroy()
    }

    override fun isExternal(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 &&
                    telecomConnection.connectionProperties.and(Connection.PROPERTY_IS_EXTERNAL_CALL) > 0

    override fun pullExternalCall() {
        if (Build.VERSION.SDK_INT < 25) {
            return
        }
        telecomConnection.setPulling()
        Handler().postDelayed({
            telecomConnection.connectionCapabilities = telecomConnection.connectionCapabilities
                    .and(Connection.CAPABILITY_CAN_PULL_CALL.inv())
            telecomConnection.connectionProperties = telecomConnection.connectionProperties
                    .and(Connection.PROPERTY_IS_EXTERNAL_CALL.inv())
            telecomConnection.setActive()
        }, 1000)
    }

    override fun pushInternalCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            telecomConnection.connectionCapabilities = telecomConnection.connectionCapabilities
                    .or(Connection.CAPABILITY_CAN_PULL_CALL)
            telecomConnection.connectionProperties = telecomConnection.connectionProperties
                    .or(Connection.PROPERTY_IS_EXTERNAL_CALL)
            notifyStateChanged()
        }
    }
}
