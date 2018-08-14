package tw.lospot.kin.call

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.telecom.VideoProfile
import android.text.Editable
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import tw.lospot.kin.call.connection.CallList
import tw.lospot.kin.call.phoneaccount.PhoneAccountManager

class InCallActivity : Activity(),
        CallList.Listener,
        PhoneAccountManager.Listener,
        View.OnClickListener {

    companion object {
        private const val PREFERENCE_LAST_NUMBER = "last_number"
    }

    private val mPreferences by lazy { getSharedPreferences("Connection", MODE_PRIVATE) }
    private val mDefaultPhoneAccount get() = mAdapter.accounts.firstOrNull { it.isEnabled }

    private val phoneNumber by lazy { findViewById<EditText>(R.id.phoneNumber) }

    private val mAdapter by lazy { InCallAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_call)
        val callList = findViewById<RecyclerView>(R.id.callList)
        callList.adapter = mAdapter
        callList.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.addIncomingCall).setOnClickListener(this)
        findViewById<ImageView>(R.id.addIncomingVideoCall).setOnClickListener(this)
        findViewById<ImageView>(R.id.addOutgoingCall).setOnClickListener(this)
        findViewById<ImageView>(R.id.addOutgoingVideoCall).setOnClickListener(this)
        findViewById<ImageView>(R.id.addPhoneAccount).setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        updateView()
        phoneNumber.text = Editable.Factory.getInstance()
                .newEditable(mPreferences.getString(PREFERENCE_LAST_NUMBER, "0987654321"))
        CallList.addListener(this)
        PhoneAccountManager.addListener(this)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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
        PhoneAccountManager.removeListener(this)
    }

    private fun maybeRequestDrawOverlays() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(this, "checkDrawOverlays failed")
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            finish()
        }
    }

    private fun updateView() {
        mAdapter.calls = CallList.getAllCalls().toList()
        mAdapter.accounts = PhoneAccountManager.getAll(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.addIncomingCall -> run {
                mDefaultPhoneAccount?.addIncomingCall(this, phoneNumber.text.toString())
            }
            R.id.addIncomingVideoCall -> run {
                mDefaultPhoneAccount?.addIncomingCall(this, phoneNumber.text.toString(), VideoProfile.STATE_BIDIRECTIONAL)
            }
            R.id.addOutgoingCall -> run {
                mDefaultPhoneAccount?.addOutgoingCall(this, phoneNumber.text.toString())
            }
            R.id.addOutgoingVideoCall -> run {
                mDefaultPhoneAccount?.addOutgoingCall(this, phoneNumber.text.toString(), VideoProfile.STATE_BIDIRECTIONAL)
            }
            R.id.addPhoneAccount -> run {
                val editText = EditText(this).apply {
                    id = R.id.dialog_edit_id
                    hint = "PhoneAccount ID"
                }
                AlertDialog.Builder(this)
                        .setView(editText)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val newId = editText.text.toString()
                            if (newId.isNotBlank() && !PhoneAccountManager.getAllIds(this).contains(newId)) {
                                PhoneAccountManager.add(this, newId)
                            }
                        }
                        .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
                        .show()
            }
        }
    }

    private fun checkCallPhonePermission(): Boolean =
            checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    private fun checkReadPhoneStatePermission(): Boolean =
            checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    private fun checkCameraPermission(): Boolean =
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onCallListChanged() {
        mAdapter.calls = CallList.getAllCalls().toList()
    }

    override fun onPhoneAccountListChanged() {
        mAdapter.accounts = PhoneAccountManager.getAll(this)
    }

}
