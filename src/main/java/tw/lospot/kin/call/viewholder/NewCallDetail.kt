package tw.lospot.kin.call.viewholder

import android.telecom.VideoProfile
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import tw.lospot.kin.call.R
import tw.lospot.kin.call.phoneaccount.CallParameters
import tw.lospot.kin.call.phoneaccount.PhoneAccountHelper

class NewCallDetail(view: View) {
    var phoneAccounts: List<PhoneAccountHelper> = emptyList()
        set(value) {
            field = value
            accountAdapter.clear()
            accountAdapter.addAll(value.map { it.phoneAccountHandle.id })
        }

    val phoneAccount: PhoneAccountHelper? get() = phoneAccounts.getOrNull(accountSpinner.selectedItemPosition)
    val parameters
        get() = CallParameters(
                videoState = when (videoSpinner.selectedItemPosition) {
                    0 -> VideoProfile.STATE_AUDIO_ONLY
                    1 -> VideoProfile.STATE_TX_ENABLED
                    2 -> VideoProfile.STATE_RX_ENABLED
                    3 -> VideoProfile.STATE_BIDIRECTIONAL
                    else -> VideoProfile.STATE_AUDIO_ONLY
                },
                answerDelay = answerDelayET?.text.toString().toLongOrNull() ?: 0,
                rejectDelay = rejectDelayET?.text.toString().toLongOrNull() ?: 0,
                disconnectDelay = disconnectDelayET.text.toString().toLongOrNull() ?: 0
        )


    private val accountAdapter = ArrayAdapter<String>(view.context, android.R.layout.simple_spinner_dropdown_item)
    private val accountSpinner = view.findViewById<Spinner>(R.id.phoneAccount).apply {
        adapter = accountAdapter
    }
    private val videoSpinner = view.findViewById<Spinner>(R.id.videoState).apply {
        adapter = ArrayAdapter.createFromResource(context, R.array.video_state_array, android.R.layout.simple_spinner_dropdown_item)
    }
    private val answerDelayET by lazy { view.findViewById<EditText?>(R.id.answerDelay) }
    private val rejectDelayET by lazy { view.findViewById<EditText?>(R.id.rejectDelay) }
    private val disconnectDelayET by lazy { view.findViewById<EditText>(R.id.disconnectDelay) }


}