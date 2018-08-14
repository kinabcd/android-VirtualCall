package tw.lospot.kin.call.phoneaccount

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import java.util.concurrent.CopyOnWriteArraySet

object PhoneAccountManager {
    private const val ACCOUNT_PREFERENCES_NAME = "ACCOUNT"
    private const val ACCOUNT_ID_SET = "IDS"
    private val sListeners = CopyOnWriteArraySet<Listener>()

    fun addListener(listener: Listener) {
        sListeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        sListeners.remove(listener)
    }

    fun getAll(context: Context): List<PhoneAccountHelper> {
        return getAllIds(context).map { PhoneAccountHelper(context, it) }
    }

    fun add(context: Context, id: String) {
        getSharedPreferences(context).edit()
                .putStringSet(ACCOUNT_ID_SET, (getAllIds(context) + id).toSet())
                .apply()
        sListeners.forEach(Listener::onPhoneAccountListChanged)
    }

    fun remove(context: Context, id: String) {
        getSharedPreferences(context).edit()
                .putStringSet(ACCOUNT_ID_SET, (getAllIds(context).filter { it != id }).toSet())
                .apply()
        sListeners.forEach(Listener::onPhoneAccountListChanged)

    }

    fun getAllIds(context: Context): List<String> {
        return (getSharedPreferences(context).getStringSet(ACCOUNT_ID_SET, null)
                ?: setOf("default@lospot.tw"))
                .toList()
    }

    private fun getSharedPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences(ACCOUNT_PREFERENCES_NAME, MODE_PRIVATE)

    interface Listener {
        fun onPhoneAccountListChanged()
    }
}