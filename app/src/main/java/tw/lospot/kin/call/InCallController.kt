package tw.lospot.kin.call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Connection
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import tw.lospot.kin.call.connection.CallList
import tw.lospot.kin.call.phoneaccount.CallParameters
import tw.lospot.kin.call.phoneaccount.PhoneAccountHelper
import tw.lospot.kin.call.phoneaccount.PhoneAccountManager

class InCallController(private val context: Context) {
    private val requiredPermission = arrayListOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Manifest.permission.READ_PHONE_NUMBERS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val accounts = mutableStateListOf<PhoneAccount>()

    private val callListListener = object : CallList.Listener {
        override fun onCallListChanged() {
            val currentCalls = CallList.getAllCalls().toList()
            accounts.forEach { account ->
                val calls =
                    currentCalls.filter { it.phoneAccountHandle.id == account.id && !it.hasParent }
                val newIds = calls.map { it.id }.toSet()
                val oldIds = account.calls.map { it.id }.toSet()
                val idsRemoved = oldIds - newIds
                val idsAdded = newIds - oldIds
                val callsAdded = calls.filter { it.id in idsAdded }
                account.calls.apply {
                    removeIf { it.id in idsRemoved }
                    addAll(callsAdded.map { Call(it) })
                    forEach { it.updateData() }
                }
            }
        }
    }
    private val accountsListener = object : PhoneAccountManager.Listener {
        override fun onPhoneAccountListChanged() {
            val currentAccounts = PhoneAccountManager.getAll(context)
            val newIds = currentAccounts.map { it.phoneAccountHandle.id }.toSet()
            val oldIds = accounts.map { it.id }.toSet()
            val idsRemoved = oldIds - newIds
            val idsAdded = newIds - oldIds
            val accountAdded = currentAccounts.filter { it.phoneAccountHandle.id in idsAdded }
            accounts.apply {
                removeIf { it.id in idsRemoved }
                addAll(accountAdded.map { PhoneAccount(helper = it) })
                forEach { it.updateData() }
            }
            callListListener.onCallListChanged()
        }
    }

    fun start(requestMultiplePermissions: ActivityResultLauncher<Array<String>>) {
        val missedPermission = getMissedPermission()
        if (missedPermission.isNotEmpty()) {
            requestMultiplePermissions.launch(missedPermission)
        }
        CallList.addListener(callListListener)
        callListListener.onCallListChanged()
        PhoneAccountManager.addListener(accountsListener)
        accountsListener.onPhoneAccountListChanged()
    }

    fun stop() {
        CallList.removeListener(callListListener)
        PhoneAccountManager.removeListener(accountsListener)
    }

    fun refresh() {
        accountsListener.onPhoneAccountListChanged()
    }

    private fun getMissedPermission(): Array<String> = requiredPermission.filter {
        context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
    }.toTypedArray()

    data class PhoneAccount(private val helper: PhoneAccountHelper) {
        val id: String = helper.phoneAccountHandle.id
        var isRegistered by mutableStateOf(false)
        var isSelfManaged by mutableStateOf(false)
        var isEnabled by mutableStateOf(false)
        val calls = mutableStateListOf<Call>()
        fun register() {
            helper.register()
            updateData()
        }

        fun unregister() {
            helper.unregister()
            updateData()
        }

        fun addOutgoingCall(context: Context, number: String, parameters: CallParameters) =
            helper.addOutgoingCall(context, number, parameters)

        fun addIncomingCall(context: Context, number: String, parameters: CallParameters) =
            helper.addIncomingCall(context, number, parameters)

        fun updateData() {
            isSelfManaged = helper.isSelfManaged
            isRegistered = helper.isRegistered
            isEnabled = helper.isEnabled
        }
    }

    data class Call(private val rawCall: tw.lospot.kin.call.connection.Call) {
        val id: Int = rawCall.id
        var state: Int by mutableStateOf(Connection.STATE_NEW)
        var name: String by mutableStateOf("")
        var isExternal: Boolean by mutableStateOf(false)
        val children = mutableStateListOf<Call>()
        fun answer() = rawCall.answer()
        fun disconnect() = rawCall.disconnect()
        fun pull() = rawCall.pull()
        fun push() = rawCall.push()
        fun toggleRxVideo() = rawCall.toggleRxVideo()
        fun updateData() {
            state = rawCall.state
            name = rawCall.name
            isExternal = rawCall.isExternal
            val currentChildren = rawCall.children
            val newIds = rawCall.children.map { it.id }.toSet()
            val oldIds = children.map { it.id }.toSet()
            val idsRemoved = oldIds - newIds
            val idsAdded = newIds - oldIds
            val childAdded = currentChildren.filter { it.id in idsAdded }
            children.apply {
                removeIf { it.id in idsRemoved }
                addAll(childAdded.map { Call(it) })
                forEach { it.updateData() }
            }
        }
    }
}