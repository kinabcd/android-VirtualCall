package tw.lospot.kin.call

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tw.lospot.kin.call.connection.CallList
import tw.lospot.kin.call.phoneaccount.PhoneAccountManager
import tw.lospot.kin.call.toolkitinfo.InfoActivity
import tw.lospot.kin.call.viewholder.NewCallDetail

class InCallActivity : Activity(),
        CallList.Listener,
        PhoneAccountManager.Listener {

    companion object {
        private const val PREFERENCE_LAST_NUMBER = "last_number"
        private const val CODE_REQUEST_PERMISSIONS = 1000
        private const val CODE_REQUEST_MANAGE_OVERLAY_PERMISSION = 1001
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

        findViewById<ImageView>(R.id.addIncomingCall).apply {
            setOnClickListener { mDefaultPhoneAccount?.addIncomingCall(context, phoneNumber.text.toString()) }
            setOnLongClickListener { showNewIncomingCallDialog(); true }
        }
        findViewById<ImageView>(R.id.addOutgoingCall).apply {
            setOnClickListener { mDefaultPhoneAccount?.addOutgoingCall(context, phoneNumber.text.toString()) }
            setOnLongClickListener { showNewOutgoingCallDialog(); true }
        }
        findViewById<ImageView>(R.id.addPhoneAccount).apply {
            setOnClickListener { showNewAccountDialog() }
        }
        findViewById<ImageView>(R.id.app_information).apply {
            setOnClickListener { showApplicationInformation() }
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !checkReadPhoneNumbersPermission()) {
            requestPermission.add(Manifest.permission.READ_PHONE_NUMBERS)
        }
        if (requestPermission.size > 0) {
            requestPermissions(requestPermission.toTypedArray(), CODE_REQUEST_PERMISSIONS)
        } else {
            maybeRequestDrawOverlays()
        }
    }

    override fun onStart() {
        super.onStart()
        updateView()
        phoneNumber.text = Editable.Factory.getInstance()
                .newEditable(mPreferences.getString(PREFERENCE_LAST_NUMBER, "0987654321"))
        CallList.addListener(this)
        PhoneAccountManager.addListener(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CODE_REQUEST_PERMISSIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !checkReadPhoneNumbersPermission()) {
                    finish()
                } else if (!checkCallPhonePermission() || !checkReadPhoneStatePermission() || !checkCameraPermission()) {
                    finish()
                } else {
                    maybeRequestDrawOverlays()
                    updateView()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CODE_REQUEST_MANAGE_OVERLAY_PERMISSION -> {
                if (!Settings.canDrawOverlays(this)) {
                    finish()
                } else {
                    updateView()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mPreferences.edit().putString(PREFERENCE_LAST_NUMBER, phoneNumber.text.toString()).apply()
        CallList.removeListener(this)
        PhoneAccountManager.removeListener(this)
    }

    private fun maybeRequestDrawOverlays() {
        // Overlay permission is used by BubbleOverlays. On Q devices, we show bubble with notification.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && !Settings.canDrawOverlays(this)) {
            Log.w(this, "checkDrawOverlays failed")
            startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                    CODE_REQUEST_MANAGE_OVERLAY_PERMISSION
            )
        }
    }

    private fun updateView() {
        mAdapter.calls = CallList.getAllCalls().toList()
        mAdapter.accounts = PhoneAccountManager.getAll(this)
    }

    private fun showNewAccountDialog() {
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

    private fun showNewIncomingCallDialog() {
        val dialogView = layoutInflater.inflate(R.layout.new_incoming_call_detail_dialog, null)
        val holder = NewCallDetail(dialogView)
        holder.phoneAccounts = PhoneAccountManager.getAll(this)
        AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    holder.phoneAccount?.addIncomingCall(this, phoneNumber.text.toString(), holder.parameters)
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
                .show()
    }

    private fun showNewOutgoingCallDialog() {
        val dialogView = layoutInflater.inflate(R.layout.new_outgoing_call_detail_dialog, null)
        val holder = NewCallDetail(dialogView)
        holder.phoneAccounts = PhoneAccountManager.getAll(this)
        AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    holder.phoneAccount?.addOutgoingCall(this, phoneNumber.text.toString(), holder.parameters)
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
                .show()
    }

    private fun showApplicationInformation() {
        startActivity(Intent(applicationContext, InfoActivity::class.java))
    }

    private fun checkCallPhonePermission(): Boolean =
            checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    private fun checkReadPhoneStatePermission(): Boolean =
            checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    private fun checkCameraPermission(): Boolean =
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkReadPhoneNumbersPermission(): Boolean =
        checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED

    override fun onCallListChanged() {
        mAdapter.calls = CallList.getAllCalls().toList()
    }

    override fun onPhoneAccountListChanged() {
        mAdapter.accounts = PhoneAccountManager.getAll(this)
    }

}
