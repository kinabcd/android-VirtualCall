package tw.lospot.kin.call.phoneaccount

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.telecom.PhoneAccount
import android.telecom.PhoneAccount.CAPABILITY_SELF_MANAGED
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.edit
import androidx.core.content.getSystemService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import tw.lospot.kin.call.R
import tw.lospot.kin.call.connection.ConnectionService

class PhoneAccountManager(private val context: Context) {
    companion object {
        private const val ACCOUNT_PREFERENCES_NAME = "ACCOUNT"
        private const val ACCOUNT_ID_SET = "IDS"
        const val DEFAULT_ACCOUNT = "default@lospot.tw"
    }

    private val telecomManager: TelecomManager = context.getSystemService()!!
    private val pref = context.getSharedPreferences(ACCOUNT_PREFERENCES_NAME, MODE_PRIVATE)
    val allIds = callbackFlow {
        val callback = OnSharedPreferenceChangeListener { _, key ->
            if (key == ACCOUNT_ID_SET) trySend(Unit)
        }
        pref.registerOnSharedPreferenceChangeListener(callback)
        awaitClose { pref.unregisterOnSharedPreferenceChangeListener(callback) }
    }.onStart { emit(Unit) }
        .map { getAllIds() }
    private val accountChanged = MutableSharedFlow<Unit>()
    private val anyChanged = merge(flowOf(Unit), accountChanged)
    val allAccounts = allIds.combine(anyChanged) { ids, _ -> ids.map { id -> snapshot(id) } }

    fun add(id: String) {
        pref.edit { putStringSet(ACCOUNT_ID_SET, (getAllIds() + id).toSet()) }
    }

    fun remove(id: String) {
        pref.edit { putStringSet(ACCOUNT_ID_SET, (getAllIds().filter { it != id }).toSet()) }
    }

    fun register(id: String): PhoneAccountSnapshot {
        val phoneAccountHandle = phoneAccountHandleFor(id)
        val newPhoneAccount = PhoneAccount.builder(phoneAccountHandle, "LoSpot Telecom")
            .setCapabilities(
                PhoneAccount.CAPABILITY_CALL_PROVIDER
                    .or(PhoneAccount.CAPABILITY_CONNECTION_MANAGER)
                    .or(PhoneAccount.CAPABILITY_VIDEO_CALLING)
                    .or(PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING)
                    .or(PhoneAccount.CAPABILITY_RTT)
            )
            .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_VOICEMAIL)
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
            .setAddress(Uri.parse(id))
            .setShortDescription(id)
            .build()

        telecomManager.registerPhoneAccount(newPhoneAccount)
        reload()
        return snapshot(id)
    }

    fun unregister(id: String) {
        val handle = phoneAccountHandleFor(id)
        if (phoneAccountFor(handle) != null) {
            telecomManager.unregisterPhoneAccount(handle)
            reload()
        }
    }

    fun reload() {
        MainScope().launch { accountChanged.emit(Unit) }
    }

    fun createDialer(): Dialer = Dialer(context, telecomManager)

    private fun getAllIds(): List<String> =
        (pref.getStringSet(ACCOUNT_ID_SET, null) ?: setOf(DEFAULT_ACCOUNT)).toList()

    fun phoneAccountHandleFor(id: String) =
        PhoneAccountHandle(ComponentName(context, ConnectionService::class.java), id)

    fun phoneAccountFor(id: String) = phoneAccountFor(phoneAccountHandleFor(id))
    private fun phoneAccountFor(handle: PhoneAccountHandle) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (context.checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS)) {
                PERMISSION_GRANTED -> telecomManager.getPhoneAccount(handle)
                else -> null
            }
        } else {
            when (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)) {
                PERMISSION_GRANTED -> telecomManager.getPhoneAccount(handle)
                else -> null
            }
        }

    private fun snapshot(id: String): PhoneAccountSnapshot = phoneAccountFor(id).let { account ->
        val state = account?.isEnabled?.let { if (it) 2 else 1 } ?: 0
        val isSelfManaged = account?.hasCapabilities(CAPABILITY_SELF_MANAGED) ?: false
        PhoneAccountSnapshot(id = id, state = state, isSelfManaged = isSelfManaged)
    }
}