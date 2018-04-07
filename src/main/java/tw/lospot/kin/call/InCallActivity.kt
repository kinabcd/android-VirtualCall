package tw.lospot.kin.call

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.text.Editable
import android.view.View
import kotlinx.android.synthetic.main.activity_in_call.*
import tw.lospot.kin.call.connection.CallList
import tw.lospot.kin.call.connection.PhoneAccountHelper

class InCallActivity : Activity(),
        View.OnClickListener,
        View.OnLongClickListener,
        CallList.Listener {

    companion object {
        private const val TELECOM_PACKAGE_NAME = "com.android.server.telecom"
        private const val ENABLE_ACCOUNT_PREFERENCE = "com.android.server.telecom.settings.EnableAccountPreferenceActivity"
        private const val PREFERENCE_LAST_NUMBER = "last_number"
    }

    private val mPreferences by lazy { getSharedPreferences("Connection", MODE_PRIVATE) }
    private val mDefaultPhoneAccount by lazy { PhoneAccountHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_call)
        registerPhoneAccount.setOnClickListener(this)
        registerPhoneAccount.setOnLongClickListener(this)
        addIncomingCall.setOnClickListener(this)
        addOutgoingCall.setOnClickListener(this)
        addIncomingVideoCall.setOnClickListener(this)
        addOutgoingVideoCall.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        updateView()
        phoneNumber.text = Editable.Factory.getInstance()
                .newEditable(mPreferences.getString(PREFERENCE_LAST_NUMBER, "0987654321"))
        CallList.addListener(this)
    }

    override fun onResume() {
        super.onResume()
        val requestPermission = ArrayList<String>(2)
        if (!checkCallPhonePermission()) {
            requestPermission.add(Manifest.permission.CALL_PHONE)
        }
        if (!checkReadPhoneStatePermission()) {
            requestPermission.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (!checkCameraPermission()) {
            requestPermission.add(Manifest.permission.CAMERA)
        }
        if (requestPermission.size > 0) {
            requestPermissions(requestPermission.toTypedArray(), 0)
        } else {
            maybeRequestDrawOverlays()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!checkCallPhonePermission() || !checkReadPhoneStatePermission() || !checkCameraPermission()) {
            finish()
        }
        maybeRequestDrawOverlays()
        updateView()
    }

    override fun onStop() {
        super.onStop()
        mPreferences.edit().putString(PREFERENCE_LAST_NUMBER, phoneNumber.text.toString()).apply()
        CallList.removeListener(this)
    }

    private fun maybeRequestDrawOverlays() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(this, "checkDrawOverlays failed")
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            finish()
        }
    }

    private fun updateView() {
        var resId = android.R.drawable.presence_invisible
        if (checkReadPhoneStatePermission()) {
            if (mDefaultPhoneAccount.isRegistered) {
                resId = if (mDefaultPhoneAccount.isEnabled) {
                    android.R.drawable.presence_online
                } else {
                    android.R.drawable.presence_busy
                }
            }
        }
        registerPhoneAccount.setImageResource(resId)

        callList.removeAllViews()
        CallList.getAllCalls()
                .filter { it.isConference || !it.hasParent }
                .forEach {
                    val item = InCallListItem(it, layoutInflater, callList)
                    callList.addView(item.view)
                }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.registerPhoneAccount -> run {
                mDefaultPhoneAccount.register()
                updateView()
                if (mDefaultPhoneAccount.isRegistered) {
                    val intent = Intent()
                    intent.setClassName(TELECOM_PACKAGE_NAME, ENABLE_ACCOUNT_PREFERENCE)
                    if (packageManager.queryIntentActivities(intent, 0).size > 0) {
                        startActivity(intent)
                    } else {
                        startActivity(Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS))
                    }
                }
            }
            R.id.addIncomingCall -> run {
                mDefaultPhoneAccount.addIncomingCall(this, phoneNumber.text.toString())
            }
            R.id.addIncomingVideoCall -> run {
                mDefaultPhoneAccount.addIncomingCall(this, phoneNumber.text.toString(), VideoProfile.STATE_BIDIRECTIONAL)
            }
            R.id.addOutgoingCall -> run {
                mDefaultPhoneAccount.addOutgoingCall(this, phoneNumber.text.toString())
            }
            R.id.addOutgoingVideoCall -> run {
                mDefaultPhoneAccount.addOutgoingCall(this, phoneNumber.text.toString(), VideoProfile.STATE_BIDIRECTIONAL)
            }
        }
    }

    override fun onLongClick(v: View?): Boolean {
        when (v?.id) {
            R.id.registerPhoneAccount -> run {
                if (mDefaultPhoneAccount.isRegistered) {
                    mDefaultPhoneAccount.unregister()
                    updateView()
                }
            }
            else -> run {
                return false
            }
        }
        return true
    }

    private fun checkCallPhonePermission(): Boolean =
            checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    private fun checkReadPhoneStatePermission(): Boolean =
            checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    private fun checkCameraPermission(): Boolean =
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED


    override fun onCallListChanged() {
        updateView()
    }
}
