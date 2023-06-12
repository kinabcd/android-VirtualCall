package tw.lospot.kin.call.phoneaccount

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import tw.lospot.kin.call.connection.CallParameters

class Dialer(private val context: Context, private val telecomManager: TelecomManager) {
    fun addIncomingCall(
        phoneAccount: PhoneAccount,
        phoneNumber: String,
        parameters: CallParameters = CallParameters()
    ) {
        val uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, phoneNumber, null)
        addIncomingCall(phoneAccount, uri, parameters)
    }

    fun addIncomingCall(
        phoneAccount: PhoneAccount, uri: Uri, parameters: CallParameters = CallParameters()
    ) {
        if (!checkCallPhonePermission() || !phoneAccount.isEnabled) {
            return
        }
        val extras = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri)
            putAll(parameters.toBundle())
        }
        telecomManager.addNewIncomingCall(phoneAccount.accountHandle, extras)
    }

    fun addOutgoingCall(
        phoneAccount: PhoneAccount,
        phoneNumber: String,
        parameters: CallParameters = CallParameters()
    ) {
        val uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, phoneNumber, null)
        addOutgoingCall(phoneAccount, uri, parameters)
    }

    fun addOutgoingCall(
        phoneAccount: PhoneAccount, uri: Uri, parameters: CallParameters = CallParameters()
    ) {
        if (!checkCallPhonePermission() || !phoneAccount.isEnabled) {
            return
        }
        val extras = Bundle()
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccount.accountHandle)
        extras.putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, parameters.videoState)
        extras.putBundle(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, parameters.toBundle())

        telecomManager.placeCall(uri, extras)
    }

    private fun checkCallPhonePermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
}