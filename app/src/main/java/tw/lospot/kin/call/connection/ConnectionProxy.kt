package tw.lospot.kin.call.connection


import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.CallAudioState
import android.telecom.Conferenceable
import android.telecom.Connection
import android.telecom.Connection.CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO
import android.telecom.Connection.CAPABILITY_CAN_PULL_CALL
import android.telecom.Connection.CAPABILITY_CAN_UPGRADE_TO_VIDEO
import android.telecom.Connection.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL
import android.telecom.Connection.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL
import android.telecom.Connection.PROPERTY_IS_EXTERNAL_CALL
import android.telecom.Connection.RttTextStream
import android.telecom.Connection.STATE_ACTIVE
import android.telecom.Connection.STATE_RINGING
import android.telecom.ConnectionRequest
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.telecom.VideoProfile.STATE_RX_ENABLED
import android.telecom.VideoProfile.STATE_TX_ENABLED
import androidx.annotation.RequiresApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import tw.lospot.kin.call.Log
import java.util.LinkedList
import java.util.Queue
import java.util.regex.Pattern
import kotlin.properties.Delegates

/**
 * Connection emulator
 * Created by Kin_Lo on 2017/8/9.
 */

class ConnectionProxy(context: Context, request: ConnectionRequest) :
    TelecomCall {
    companion object {
        const val TAG = "ConnectionProxy"
    }

    private val scope = MainScope()
    override val id: Int = TelecomCall.callCount++
    val telecomConnection = object : Connection() {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                connectionProperties = connectionProperties
                    .or(TelecomCall.PROPERTY_WIFI)
                    .or(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && request.isRequestingRtt) PROPERTY_IS_RTT else 0)
            }
        }

        override fun onStateChanged(state: Int) = notifyStateChanged()
        override fun onDisconnect() {
            scope.launch {
                delay(callParameters.disconnectDelay)
                disconnect(DisconnectCause(DisconnectCause.LOCAL))
            }
        }

        override fun onAbort() = disconnect(DisconnectCause(DisconnectCause.UNKNOWN))
        override fun onHold() = hold()
        override fun onUnhold() = unhold()
        override fun onAnswer() = onAnswer(VideoProfile.STATE_AUDIO_ONLY)
        override fun onAnswer(vs: Int) {
            scope.launch {
                delay(callParameters.answerDelay)
                answer(vs)
            }
        }

        override fun onReject() = onReject(null)
        override fun onReject(replyMessage: String?) {
            scope.launch {
                delay(callParameters.rejectDelay)
                disconnect(DisconnectCause(DisconnectCause.REJECTED))
            }
        }

        override fun onCallEvent(event: String, extras: Bundle?) {
            Log.v(TAG, "onCallEvent $event $extras")
        }

        override fun onPullExternalCall() = pullExternalCall()
        override fun onExtrasChanged(extras: Bundle) {
            Log.v(TAG, "onExtrasChanged $extras")
        }

        override fun sendConnectionEvent(event: String, extras: Bundle) {
            Log.v(TAG, "sendConnectionEvent $event $extras")
        }

        override fun onPlayDtmfTone(c: Char) {
            Log.v(TAG, "onPlayDtmfTone $c")
            scope.launch { onPlayDtmfTone.emit(c) }
        }

        override fun onStopDtmfTone() {
            Log.v(TAG, "onStopDtmfTone")
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
    override val onStateChanged: MutableSharedFlow<Unit> = MutableSharedFlow()
    override val onPlayDtmfTone: MutableSharedFlow<Char> = MutableSharedFlow()

    private var postDial: Queue<String> =
        request.address.encodedSchemeSpecificPart.split(Pattern.compile(";|(%3B)")).let {
            LinkedList(if (it.size > 1) it.subList(1, it.size) else emptyList())
        }
    override val phoneAccountHandle: PhoneAccountHandle = request.accountHandle
    private var rttTextStream: RttTextStream? by Delegates.observable(null) { _, old, new ->
        if (old != new) rttRobot = if (new != null) RttRobot(new) else null
    }
    private var rttRobot: RttRobot? by Delegates.observable(null) { _, old, new ->
        old?.stop()
        new?.start()
    }
    private val videoProvider = VideoProvider(context, this)
    override val conferenceable: Conferenceable get() = telecomConnection
    override var conferenceables: List<Conferenceable>
        get() = telecomConnection.conferenceables
        set(value) {
            telecomConnection.conferenceables = value
        }
    override val name: String get() = telecomConnection.address?.schemeSpecificPart ?: "Empty"
    override val state: Int get() = telecomConnection.state
    override var videoState: Int = 0
        set(value) {
            field = value
            telecomConnection.videoState = value
        }
    override var isWifiCall: Boolean
        get() = hasProperty(TelecomCall.PROPERTY_WIFI)
        set(value) = setProperty(TelecomCall.PROPERTY_WIFI, value)

    override var isHdAudio: Boolean
        get() = hasProperty(TelecomCall.PROPERTY_HIGH_DEF_AUDIO)
        set(value) = setProperty(TelecomCall.PROPERTY_HIGH_DEF_AUDIO, value)

    override val isConference: Boolean = false
    override var isExternal: Boolean
        @RequiresApi(Build.VERSION_CODES.N_MR1)
        get() = hasProperty(PROPERTY_IS_EXTERNAL_CALL)
        @RequiresApi(Build.VERSION_CODES.N_MR1)
        set(value) = setProperty(PROPERTY_IS_EXTERNAL_CALL, value)
    override val hasParent: Boolean get() = telecomConnection.conference != null
    override val children: List<Connection> = emptyList()
    private val callParameters = CallParameters(request.extras)

    init {
        Log.v(TAG, "request=$request")
        telecomConnection.videoProvider = videoProvider
        videoState = request.videoState
        if (Build.VERSION.SDK_INT >= 28 && request.isRequestingRtt) {
            rttTextStream = request.rttTextStream
        }

        if (Build.VERSION.SDK_INT >= 25) {
            isHdAudio = callParameters.isHdAudio
            isWifiCall = callParameters.isWifi
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
        scope.launch { onStateChanged.emit(Unit) }
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
        rttTextStream = null
        videoProvider.onSetPreviewSurface(null)
        videoProvider.onSetDisplaySurface(null)
        telecomConnection.setDisconnected(
            when {
                disconnectCause.code != DisconnectCause.UNKNOWN -> disconnectCause
                else -> when (state) {
                    STATE_RINGING -> DisconnectCause(DisconnectCause.MISSED)
                    else -> DisconnectCause(DisconnectCause.REMOTE)
                }
            }
        )
        notifyStateChanged()
        telecomConnection.destroy()
    }

    override fun pullExternalCall() {
        if (Build.VERSION.SDK_INT < 25) return
        telecomConnection.setPulling()
        scope.launch {
            delay(1000)
            telecomConnection.connectionCapabilities = telecomConnection.connectionCapabilities
                .and(CAPABILITY_CAN_PULL_CALL.inv())
            isExternal = false
            telecomConnection.setActive()
        }
    }

    override fun pushInternalCall() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        telecomConnection.connectionCapabilities = telecomConnection.connectionCapabilities
            .or(CAPABILITY_CAN_PULL_CALL)
        isExternal = true
        notifyStateChanged()
    }

    override fun requestRtt() {
        if (Build.VERSION.SDK_INT < 28) return
        telecomConnection.sendRemoteRttRequest()

    }

    override fun requestVideo(state: Int) {
        if (videoState != state) {
            val videoProfile = VideoProfile(state)
            Log.v(this, "requestVideo $videoProfile")
            val isRxUpgrade = (videoState.and(STATE_RX_ENABLED) < state.and(STATE_RX_ENABLED))
            val isTxUpgrade = (videoState.and(STATE_TX_ENABLED) < state.and(STATE_TX_ENABLED))
            if (isRxUpgrade || isTxUpgrade) {
                videoProvider.receiveSessionModifyRequest(videoProfile)
            } else {
                videoState = state
            }
        }
    }

    private fun hasProperty(property: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return false
        return telecomConnection.connectionProperties and property != 0
    }

    private fun setProperty(property: Int, on: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        telecomConnection.connectionProperties = if (on) {
            telecomConnection.connectionProperties.or(property)
        } else {
            telecomConnection.connectionProperties.and(property.inv())
        }
    }
}
