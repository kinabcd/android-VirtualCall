package tw.lospot.kin.call.connection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.DisconnectCause
import android.telecom.VideoProfile
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tw.lospot.kin.call.Log
import tw.lospot.kin.call.phoneaccount.PhoneAccountManager

class InCallReceiver : BroadcastReceiver() {
    companion object {
        private const val ACTION_CALL = "tw.lospot.Call.OutgoingCall"
        private const val ACTION_INCOMING_CALL = "tw.lospot.Call.IncomingCall"
        const val ACTION_DISCONNECT = "tw.lospot.Call.Disconnect"
        private const val ACTION_UPGRADE = "tw.lospot.Call.Upgrade"
        const val ACTION_ANSWER = "tw.lospot.Call.Answer"

        const val EXTRA_CALL_ID = "callId"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CALL -> {
                Log.v(this, "ACTION_CALL")
                intent.data?.let { data ->
                    val videoState = intent.getIntExtra("video", VideoProfile.STATE_AUDIO_ONLY)
                    val id = intent.getStringExtra("id") ?: PhoneAccountManager.DEFAULT_ACCOUNT
                    val phoneAccountManager = PhoneAccountManager(context)
                    phoneAccountManager.phoneAccountFor(id)?.let {
                        val dialer = phoneAccountManager.createDialer()
                        dialer.addOutgoingCall(it, data, CallParameters(videoState = videoState))

                    }
                }
            }

            ACTION_INCOMING_CALL -> {
                Log.v(this, "ACTION_INCOMING_CALL")
                intent.data?.let { data ->
                    val videoState = intent.getIntExtra("video", VideoProfile.STATE_AUDIO_ONLY)
                    val id = intent.getStringExtra("id") ?: PhoneAccountManager.DEFAULT_ACCOUNT
                    val phoneAccountManager = PhoneAccountManager(context)
                    phoneAccountManager.phoneAccountFor(id)?.let {
                        val dialer = phoneAccountManager.createDialer()
                        dialer.addIncomingCall(it, data, CallParameters(videoState = videoState))
                    }
                }
            }

            ACTION_DISCONNECT -> {
                val callId = intent.getIntExtra(EXTRA_CALL_ID, -1)
                Log.v(this, "(Call_$callId) ACTION_DISCONNECT")
                val disconnectCause = when (intent.getStringExtra("cause")) {
                    "UNKNOWN" -> DisconnectCause(DisconnectCause.UNKNOWN)
                    "ERROR" -> DisconnectCause(DisconnectCause.ERROR, "ERROR", "ERROR", "ERROR")
                    "LOCAL" -> DisconnectCause(DisconnectCause.LOCAL)
                    "REMOTE" -> DisconnectCause(DisconnectCause.REMOTE)
                    "CANCELED" -> DisconnectCause(DisconnectCause.CANCELED)
                    "MISSED" -> DisconnectCause(DisconnectCause.MISSED)
                    "REJECTED" -> DisconnectCause(DisconnectCause.REJECTED)
                    "BUSY" -> DisconnectCause(DisconnectCause.BUSY)
                    "RESTRICTED" -> DisconnectCause(
                        DisconnectCause.RESTRICTED,
                        "RESTRICTED",
                        "RESTRICTED",
                        "RESTRICTED"
                    )

                    "OTHER" -> DisconnectCause(DisconnectCause.OTHER)
                    "CONNECTION_MANAGER_NOT_SUPPORTED" -> DisconnectCause(DisconnectCause.CONNECTION_MANAGER_NOT_SUPPORTED)
                    "ANSWERED_ELSEWHERE" -> DisconnectCause(DisconnectCause.ANSWERED_ELSEWHERE)
                    else -> DisconnectCause(DisconnectCause.REMOTE)
                }

                MainScope().launch {
                    CallList.calls.first().firstOrNull { callId == -1 || it.id == callId }
                        ?.disconnect(disconnectCause)
                }
            }

            ACTION_UPGRADE -> {
                val callId = intent.getIntExtra(EXTRA_CALL_ID, -1)
                val videoState = intent.getIntExtra("video", VideoProfile.STATE_BIDIRECTIONAL)
                Log.v(this, "(Call_$callId) ACTION_UPGRADE")
                MainScope().launch {
                    CallList.calls.first().firstOrNull { callId == -1 || it.id == callId }
                        ?.requestVideo(videoState)
                }
            }

            ACTION_ANSWER -> {
                val callId = intent.getIntExtra(EXTRA_CALL_ID, -1)
                Log.v(this, "(Call_$callId) ACTION_ANSWER")
                MainScope().launch {
                    CallList.calls.first().firstOrNull { callId == -1 || it.id == callId }
                        ?.answer()
                }
            }
        }
    }
}
