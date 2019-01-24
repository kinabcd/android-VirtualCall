package tw.lospot.kin.call.connection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.DisconnectCause
import android.telecom.VideoProfile
import tw.lospot.kin.call.Log
import tw.lospot.kin.call.phoneaccount.CallParameters
import tw.lospot.kin.call.phoneaccount.PhoneAccountHelper

class InCallReceiver : BroadcastReceiver() {
    companion object {
        private const val ACTION_CALL = "tw.lospot.Call.OutgoingCall"
        private const val ACTION_INCOMING_CALL = "tw.lospot.Call.IncomingCall"
        private const val ACTION_DISCONNECT = "tw.lospot.Call.Disconnect"
        private const val ACTION_UPGRADE = "tw.lospot.Call.Upgrade"
        private const val ACTION_ANSWER = "tw.lospot.Call.Answer"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.v(this, "onReceive " + intent.action)
        when (intent.action) {
            ACTION_CALL -> {
                intent.data?.let { data ->
                    val videoState = intent.getIntExtra("video", VideoProfile.STATE_AUDIO_ONLY)
                    PhoneAccountHelper(context).addOutgoingCall(context, data, CallParameters(videoState = videoState))
                }
            }
            ACTION_INCOMING_CALL -> {
                intent.data?.let { data ->
                    val videoState = intent.getIntExtra("video", VideoProfile.STATE_AUDIO_ONLY)
                    PhoneAccountHelper(context).addIncomingCall(context, data, CallParameters(videoState = videoState))
                }
            }
            ACTION_DISCONNECT -> {
                val call = if (intent.hasExtra("callId")) {
                    val callId = intent.getIntExtra("callId", -1)
                    CallList.getAllCalls().firstOrNull { it.id == callId }
                } else {
                    CallList.getAllCalls().firstOrNull()
                }
                val disconnectCause = when (intent.getStringExtra("cause")) {
                    "UNKNOWN" -> DisconnectCause(DisconnectCause.UNKNOWN)
                    "ERROR" -> DisconnectCause(DisconnectCause.ERROR, "ERROR", "ERROR", "ERROR")
                    "LOCAL" -> DisconnectCause(DisconnectCause.LOCAL)
                    "REMOTE" -> DisconnectCause(DisconnectCause.REMOTE)
                    "CANCELED" -> DisconnectCause(DisconnectCause.CANCELED)
                    "MISSED" -> DisconnectCause(DisconnectCause.MISSED)
                    "REJECTED" -> DisconnectCause(DisconnectCause.REJECTED)
                    "BUSY" -> DisconnectCause(DisconnectCause.BUSY)
                    "RESTRICTED" -> DisconnectCause(DisconnectCause.RESTRICTED, "RESTRICTED", "RESTRICTED", "RESTRICTED")
                    "OTHER" -> DisconnectCause(DisconnectCause.OTHER)
                    "CONNECTION_MANAGER_NOT_SUPPORTED" -> DisconnectCause(DisconnectCause.CONNECTION_MANAGER_NOT_SUPPORTED)
                    "ANSWERED_ELSEWHERE" -> DisconnectCause(DisconnectCause.ANSWERED_ELSEWHERE)
                    else -> DisconnectCause(DisconnectCause.REMOTE)
                }

                call?.disconnect(disconnectCause)
            }
            ACTION_UPGRADE -> {
                val call = if (intent.hasExtra("callId")) {
                    val callId = intent.getIntExtra("callId", -1)
                    CallList.getAllCalls().firstOrNull { it.id == callId }
                } else {
                    CallList.getAllCalls().firstOrNull()
                }
                if (call != null) {
                    val videoState = intent.getIntExtra("video", VideoProfile.STATE_BIDIRECTIONAL)
                    call.requestVideo(videoState)
                }
            }
            ACTION_ANSWER -> {
                val call = if (intent.hasExtra("callId")) {
                    val callId = intent.getIntExtra("callId", -1)
                    CallList.getAllCalls().firstOrNull { it.id == callId }
                } else {
                    CallList.getAllCalls().firstOrNull()
                }
                call?.answer()
            }
        }
    }
}
