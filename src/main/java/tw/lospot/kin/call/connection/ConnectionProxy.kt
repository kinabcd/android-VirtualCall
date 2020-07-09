package tw.lospot.kin.call.connection


import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.*
import android.telecom.Connection.*
import androidx.annotation.RequiresApi
import tw.lospot.kin.call.Log
import java.util.*
import java.util.regex.Pattern

/**
 * Connection emulator
 * Created by Kin_Lo on 2017/8/9.
 */

class ConnectionProxy(context: Context, request: ConnectionRequest) :
        TelecomCall.Common {
    companion object {
        const val TAG = "ConnectionProxy"
    }

    val telecomConnection = object : Connection() {
        private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

        init {
            when (request.address?.schemeSpecificPart) {
                "hidden" -> setAddress(null, TelecomManager.PRESENTATION_RESTRICTED)
                "unknown" -> setAddress(null, TelecomManager.PRESENTATION_UNKNOWN)
                "payphone" -> setAddress(null, TelecomManager.PRESENTATION_PAYPHONE)
                else -> setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
            }
            connectionCapabilities = connectionCapabilities
                    .or(CAPABILITY_SUPPORT_HOLD)
                    .or(CAPABILITY_HOLD)
                    .or(CAPABILITY_MUTE)
                    .or(CAPABILITY_RESPOND_VIA_TEXT)
                    .or(CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO)
            connectionProperties = connectionProperties
                    .or(1 shl 3)
            if (request.isRequestingRtt) {
                connectionProperties = connectionProperties
                        .or(PROPERTY_IS_RTT)
            }
        }

        override fun onStateChanged(state: Int) {
            notifyStateChanged()
        }

        override fun onDisconnect() {
            mainHandler.postDelayed({
                disconnect(DisconnectCause(DisconnectCause.LOCAL))
            }, disconnectDelay)

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
            mainHandler.postDelayed({
                answer(vs)
            }, answerDelay)

        }

        override fun onReject() {
            onReject(null)
        }

        override fun onReject(replyMessage: String?) {
            mainHandler.postDelayed({
                disconnect(DisconnectCause(DisconnectCause.REJECTED))
            }, rejectDelay)
        }

        override fun onCallEvent(event: String, extras: Bundle) {
            super.onCallEvent(event, extras)
            Log.v(TAG, "onCallEvent $event")
        }

        override fun onPullExternalCall() {
            pullExternalCall()
        }

        override fun onExtrasChanged(extras: Bundle) {
            Log.v(TAG, "onExtrasChanged $extras")
        }

        override fun sendConnectionEvent(event: String, extras: Bundle) {
            Log.v(TAG, "sendConnectionEvent $event $extras")
        }

        override fun onPlayDtmfTone(c: Char) {
            Log.v(TAG, "onPlayDtmfTone $c")
            listener?.onPlayDtmfTone(c)
        }

        override fun onStopDtmfTone() {
            Log.v(TAG, "onStopDtmfTone")
            super.onStopDtmfTone()
        }

        override fun onSeparate() {
            Log.v(TAG, "onSeparate")
        }

        override fun onCallAudioStateChanged(state: CallAudioState?) {
            Log.v(TAG, "onCallAudioStateChanged $state")
        }

        override fun onShowIncomingCallUi() {
            Log.v(TAG, "onShowIncomingCallUi")
        }

        override fun onPostDialContinue(proceed: Boolean) {
            Log.v(TAG, "onPostDialContinue $proceed")
            if (proceed) {
                maybeSetPostDialWait()
            } else {
                clearPostDial()
            }
        }

        override fun onDeflect(address: Uri?) {
            Log.v(TAG, "onDeflect")
        }

        override fun onHandoverComplete() {
            Log.v(TAG, "onHandoverComplete")
        }

        override fun requestBluetoothAudio(bluetoothDevice: BluetoothDevice) {
            Log.v(TAG, "requestBluetoothAudio $bluetoothDevice")
        }

        @RequiresApi(28)
        override fun handleRttUpgradeResponse(rttTextStream: RttTextStream?) {
            Log.v(TAG, "handleRttUpgradeResponse $rttTextStream")
            rttTextStream?.let {
                this@ConnectionProxy.rttTextStream = it
                connectionProperties = connectionProperties.or(PROPERTY_IS_RTT)
            }
        }

        @RequiresApi(28)
        override fun onStartRtt(rttTextStream: RttTextStream) {
            Log.v(TAG, "onStartRtt $rttTextStream")
            this@ConnectionProxy.rttTextStream = rttTextStream
            connectionProperties = connectionProperties.or(PROPERTY_IS_RTT)
            sendRttInitiationSuccess()
        }

        @RequiresApi(28)
        override fun onStopRtt() {
            Log.v(TAG, "onStopRtt")
            this@ConnectionProxy.rttTextStream = null
            connectionProperties = connectionProperties.and(PROPERTY_IS_RTT.inv())
        }
    }

    private var postDial: Queue<String> =
            request.address.encodedSchemeSpecificPart.split(Pattern.compile(";|(%3B)")).let {
                LinkedList(if (it.size > 1) it.subList(1, it.size) else emptyList())
            }
    override val phoneAccountHandle: PhoneAccountHandle = request.accountHandle
    private var rttTextStream: RttTextStream? = null
        set(value) {
            if (field != value) {
                field = value
                rttRobot = if (value != null) {
                    RttRobot(value)
                } else {
                    null
                }
            }
        }
    private var rttRobot: RttRobot? = null
        set(value) {
            field?.stop()
            field = value
            field?.start()
        }
    val videoProvider = VideoProvider(context, this)
    override var listener: TelecomCall.Listener? = null

    override val conferenceable: Conferenceable get() = telecomConnection
    override val state: Int get() = telecomConnection.state
    override var videoState: Int = 0
        set(value) {
            field = value
            telecomConnection.videoState = value
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
    private val disconnectDelay: Long = request.extras.getLong(TelecomCall.EXTRA_DELAY_DISCONNECT, 0)
    private val rejectDelay: Long = request.extras.getLong(TelecomCall.EXTRA_DELAY_REJECT, 0)
    private val answerDelay: Long = request.extras.getLong(TelecomCall.EXTRA_DELAY_ANSWER, 0)

    init {
        Log.v(TAG, "request=$request")
        telecomConnection.videoProvider = videoProvider
        videoState = request.videoState
        if (Build.VERSION.SDK_INT >= 28 && request.isRequestingRtt) {
            rttTextStream = request.rttTextStream
            Log.v(TAG, "isRequestingRtt=${request.isRequestingRtt}, rttTextStream=${request.rttTextStream}")
        }

        if (Build.VERSION.SDK_INT >= 25) {
            isHdAudio = request.extras.getBoolean(TelecomCall.EXTRA_HIGH_DEF_AUDIO, false)
            isWifiCall = request.extras.getBoolean(TelecomCall.EXTRA_WIFI, false)
        }
    }

    private fun notifyStateChanged() {
        telecomConnection.connectionCapabilities = when (state) {
            STATE_ACTIVE -> telecomConnection.connectionCapabilities
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

    private fun maybeSetPostDialWait() {
        if (postDial.isNotEmpty()) {
            telecomConnection.setPostDialWait(postDial.poll())
        }
    }

    private fun clearPostDial() {
        postDial.clear()
    }

    override fun answer(videoState: Int) {
        this.videoState = videoState
        telecomConnection.setActive()
        maybeSetPostDialWait()
    }

    override fun hold() {
        telecomConnection.setOnHold()
    }

    override fun unhold() {
        telecomConnection.setActive()
    }

    override fun disconnect(disconnectCause: DisconnectCause) {
        videoState = VideoProfile.STATE_AUDIO_ONLY
        rttTextStream = null
        videoProvider.onSetPreviewSurface(null)
        videoProvider.onSetDisplaySurface(null)
        telecomConnection.setDisconnected(disconnectCause)
        notifyStateChanged()
        telecomConnection.destroy()
    }

    override fun isExternal(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 &&
                    telecomConnection.connectionProperties.and(PROPERTY_IS_EXTERNAL_CALL) > 0

    override fun pullExternalCall() {
        if (Build.VERSION.SDK_INT < 25) {
            return
        }
        telecomConnection.setPulling()
        Handler().postDelayed({
            telecomConnection.connectionCapabilities = telecomConnection.connectionCapabilities
                    .and(CAPABILITY_CAN_PULL_CALL.inv())
            telecomConnection.connectionProperties = telecomConnection.connectionProperties
                    .and(PROPERTY_IS_EXTERNAL_CALL.inv())
            telecomConnection.setActive()
        }, 1000)
    }

    override fun pushInternalCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            telecomConnection.connectionCapabilities = telecomConnection.connectionCapabilities
                    .or(CAPABILITY_CAN_PULL_CALL)
            telecomConnection.connectionProperties = telecomConnection.connectionProperties
                    .or(PROPERTY_IS_EXTERNAL_CALL)
            notifyStateChanged()
        }
    }

    override fun requestRtt() {
        if (Build.VERSION.SDK_INT >= 28) {
            telecomConnection.sendRemoteRttRequest()
        }
    }

    @RequiresApi(25)
    private fun hasProperty(property: Int) = telecomConnection.connectionProperties and property != 0

    @RequiresApi(25)
    private fun setProperty(property: Int, on: Boolean) {
        telecomConnection.connectionProperties = if (on) {
            telecomConnection.connectionProperties.or(property)
        } else {
            telecomConnection.connectionProperties.and(property.inv())
        }
    }
}
