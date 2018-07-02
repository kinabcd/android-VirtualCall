package tw.lospot.kin.call.viewholder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import tw.lospot.kin.call.R
import tw.lospot.kin.call.phoneaccount.PhoneAccountHelper
import tw.lospot.kin.call.phoneaccount.PhoneAccountManager

class AccountViewHolder(view: View) : BaseViewHolder(view),
        View.OnClickListener,
        View.OnLongClickListener {
    companion object {
        private const val TELECOM_PACKAGE_NAME = "com.android.server.telecom"
        private const val ENABLE_ACCOUNT_PREFERENCE = "com.android.server.telecom.settings.EnableAccountPreferenceActivity"
    }

    private val register = view.findViewById<ImageButton>(R.id.registerPhoneAccount)
    private val id = view.findViewById<TextView>(R.id.accountId)
    private val delete = view.findViewById<ImageButton>(R.id.delete)
    var phoneAccountHelper: PhoneAccountHelper? = null
        set(value) {
            field = value
            updateView()
        }

    init {
        register.setOnClickListener(this)
        register.setOnLongClickListener(this)
        delete.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        val context = view.context
        when (view.id) {
            R.id.registerPhoneAccount -> phoneAccountHelper?.run {
                register()
                updateView()
                if (isRegistered && !isSelfManaged) {
                    val intent = Intent()
                    intent.setClassName(TELECOM_PACKAGE_NAME, ENABLE_ACCOUNT_PREFERENCE)
                    if (context.packageManager.queryIntentActivities(intent, 0).size > 0) {
                        context.startActivity(intent)
                    } else {
                        context.startActivity(Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS))
                    }
                }
            }
            R.id.delete -> phoneAccountHelper?.run {
                unregister()
                PhoneAccountManager.remove(view.context, phoneAccountHandle.id)
            }
        }
    }

    override fun onLongClick(view: View?): Boolean {
        phoneAccountHelper?.run {
            if (isRegistered) {
                unregister()
                updateView()
            }
        }
        return true
    }

    private fun updateView() {
        var resId = android.R.drawable.presence_invisible
        if (register.context.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            phoneAccountHelper?.run {
                resId = when {
                    !isRegistered -> android.R.drawable.presence_invisible
                    !isEnabled -> android.R.drawable.presence_busy
                    else -> android.R.drawable.presence_online
                }
            }
        }
        register.setImageResource(resId)
        id.text = phoneAccountHelper?.phoneAccountHandle?.id
    }
}