package tw.lospot.kin.call.connection


import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import tw.lospot.kin.call.Log

/**
 * Connection emulator
 * Created by Kin_Lo on 2017/8/9.
 */

class Connection(val context: Context, address: Uri, val phoneAccountHandle: PhoneAccountHandle) :
        android.telecom.Connection(),
        TelecomCall.Common {
    init {
        setAddress(address, TelecomManager.PRESENTATION_ALLOWED)
        connectionCapabilities = connectionCapabilities
                .or(CAPABILITY_SUPPORT_HOLD)
                .or(CAPABILITY_HOLD)
                .or(CAPABILITY_MUTE)
                .or(CAPABILITY_RESPOND_VIA_TEXT)
                .or(CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO)
        val vp = tw.lospot.kin.call.connection.VideoProvider(context)
        vp.connection = this
        videoProvider = vp
    }

    override var listener: TelecomCall.Listener? = null

    var vState: Int
        get() = (videoProvider as tw.lospot.kin.call.connection.VideoProvider).videoState
        set(value) {
            super.setVideoState(value)
            (videoProvider as tw.lospot.kin.call.connection.VideoProvider).videoState = value
        }

    override fun onStateChanged(state: Int) {
        if (state == android.telecom.Connection.STATE_ACTIVE) {
            connectionCapabilities = connectionCapabilities
                    .or(CAPABILITY_CAN_UPGRADE_TO_VIDEO)
                    .or(CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL)
                    .or(CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL)
                    .and(CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO.inv())
        } else {
            connectionCapabilities = connectionCapabilities
                    .and(CAPABILITY_CAN_UPGRADE_TO_VIDEO.inv())
                    .and(CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL.inv())
                    .and(CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL.inv())
                    .or(CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO)
        }
        listener?.onStateChanged(state)
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

    override fun onAnswer(videoState: Int) {
        vState = videoState
        setActive()
    }

    override fun onAnswer() {
        onAnswer(VideoProfile.STATE_AUDIO_ONLY)
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
        super.onPullExternalCall()
        pullExternalCall()
    }

    override fun onExtrasChanged(extras: Bundle) {
        super.onExtrasChanged(extras)
        Log.v(this, "onExtrasChanged $extras")
    }

    override fun sendConnectionEvent(event: String, extras: Bundle) {
        super.sendConnectionEvent(event, extras)
        Log.v(this, "sendConnectionEvent $event $extras")
    }

    override fun onPlayDtmfTone(c: Char) {
        super.onPlayDtmfTone(c)
        listener?.onPlayDtmfTone(c)
    }

    override fun hold() {
        setOnHold()
    }

    override fun unhold() {
        setActive()
    }

    override fun onSeparate() {
    }

    override fun disconnect(disconnectCause: DisconnectCause) {
        vState = VideoProfile.STATE_AUDIO_ONLY
        videoProvider.onSetPreviewSurface(null)
        videoProvider.onSetDisplaySurface(null)
        setDisconnected(disconnectCause)
        onStateChanged(state)
        destroy()
    }

    override fun isExternal(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 &&
                    connectionProperties.and(android.telecom.Connection.PROPERTY_IS_EXTERNAL_CALL) > 0

    override fun pullExternalCall() {
        if (Build.VERSION.SDK_INT < 25) {
            return
        }
        setPulling()
        Handler().postDelayed({
            connectionCapabilities = connectionCapabilities
                    .and(android.telecom.Connection.CAPABILITY_CAN_PULL_CALL.inv())
            connectionProperties = connectionProperties
                    .and(android.telecom.Connection.PROPERTY_IS_EXTERNAL_CALL.inv())
            setActive()
        }, 1000)
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
}
