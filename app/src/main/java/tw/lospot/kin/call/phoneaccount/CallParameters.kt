package tw.lospot.kin.call.phoneaccount

import android.telecom.VideoProfile

data class CallParameters(
        val videoState: Int = VideoProfile.STATE_AUDIO_ONLY,
        val answerDelay: Long = 0,
        val rejectDelay: Long = 0,
        val disconnectDelay: Long = 0,
        val isWifi: Boolean = false,
        val isHdAudio: Boolean = false
)