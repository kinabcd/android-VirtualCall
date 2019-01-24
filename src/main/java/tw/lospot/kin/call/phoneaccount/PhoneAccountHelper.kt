package tw.lospot.kin.call.phoneaccount

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import tw.lospot.kin.call.R
import tw.lospot.kin.call.connection.ConnectionService
import tw.lospot.kin.call.connection.TelecomCall

/**
 * Utils of PhoneAccount
 * Created by Kin_Lo on 2017/8/23.
 */

class PhoneAccountHelper(context: Context, private val address: String = "default@lospot.tw") {
    private val context = context.applicationContext
    private val telecomManager by lazy { context.getSystemService(TelecomManager::class.java) }

    val phoneAccount: PhoneAccount?
        get() = telecomManager.getPhoneAccount(phoneAccountHandle)

    val phoneAccountHandle: PhoneAccountHandle
        get() {
            val componentName = ComponentName(context, ConnectionService::class.java)
            return PhoneAccountHandle(componentName, address)
        }

    val isRegistered get() = phoneAccount != null
    val isEnabled get() = isRegistered && phoneAccount!!.isEnabled
    val isSelfManaged get() = isRegistered && phoneAccount!!.hasCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)

    fun register() {
        val newPhoneAccount = PhoneAccount.builder(phoneAccountHandle, "LoSpot Telecom")
                .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER
                        .or(PhoneAccount.CAPABILITY_CONNECTION_MANAGER)
                        .or(PhoneAccount.CAPABILITY_VIDEO_CALLING)
                        .or(PhoneAccount.CAPABILITY_RTT)
                )
                .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
                .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                .addSupportedUriScheme(PhoneAccount.SCHEME_VOICEMAIL)
                .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                .setAddress(Uri.parse(address))
                .setShortDescription(address)
                .build()

        telecomManager.registerPhoneAccount(newPhoneAccount)
    }

    fun unregister() {
        if (!checkCallPhonePermission(context) || !isRegistered) {
            return
        }
        telecomManager.unregisterPhoneAccount(phoneAccountHandle)
    }

    fun addIncomingCall(context: Context, phoneNumber: String, parameters: CallParameters = CallParameters()) {
        val uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, phoneNumber, null)
        addIncomingCall(context, uri, parameters)
    }

    fun addIncomingCall(context: Context, uri: Uri, parameters: CallParameters = CallParameters()) {
        if (!checkCallPhonePermission(context) || !isEnabled) {
            return
        }
        val extras = Bundle()
        extras.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri)
        extras.putInt(TelecomManager.EXTRA_INCOMING_VIDEO_STATE, parameters.videoState)
        extras.putLong(TelecomCall.EXTRA_DELAY_ANSWER, parameters.answerDelay)
        extras.putLong(TelecomCall.EXTRA_DELAY_REJECT, parameters.rejectDelay)
        extras.putLong(TelecomCall.EXTRA_DELAY_DISCONNECT, parameters.disconnectDelay)

        val telecomManager = context.getSystemService(TelecomManager::class.java)
        telecomManager!!.addNewIncomingCall(phoneAccountHandle, extras)
    }

    fun addOutgoingCall(context: Context, phoneNumber: String, parameters: CallParameters = CallParameters()) {
        val uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, phoneNumber, null)
        addOutgoingCall(context, uri, parameters)
    }

    fun addOutgoingCall(context: Context, uri: Uri, parameters: CallParameters = CallParameters()) {
        if (!checkCallPhonePermission(context) || !isEnabled) {
            return
        }
        val extras = Bundle()
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
        extras.putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, parameters.videoState)
        extras.putBundle(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, Bundle().apply {
            putLong(TelecomCall.EXTRA_DELAY_DISCONNECT, parameters.disconnectDelay)
        })

        val telecomManager = context.getSystemService(TelecomManager::class.java)
        telecomManager!!.placeCall(uri, extras)
    }


    private fun checkCallPhonePermission(context: Context): Boolean =
            context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
}
