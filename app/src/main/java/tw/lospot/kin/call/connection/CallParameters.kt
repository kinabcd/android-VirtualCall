package tw.lospot.kin.call.connection

import android.os.Bundle
import android.telecom.TelecomManager
import android.telecom.VideoProfile

data class CallParameters(
    val videoState: Int = VideoProfile.STATE_AUDIO_ONLY,
    val answerDelay: Long = 0,
    val rejectDelay: Long = 0,
    val disconnectDelay: Long = 0,
    val isWifi: Boolean = false,
    val isHdAudio: Boolean = false,
) {
    companion object {
        private const val EXTRA_DELAY_DISCONNECT = "delay_disconnect"
        private const val EXTRA_DELAY_REJECT = "delay_reject"
        private const val EXTRA_DELAY_ANSWER = "delay_answer"
        private const val EXTRA_HIGH_DEF_AUDIO = "hd_audio"
        private const val EXTRA_WIFI = "wifi"
    }
    constructor(bundle: Bundle) : this(
        videoState = bundle.getInt(TelecomManager.EXTRA_INCOMING_VIDEO_STATE, 0),
        answerDelay = bundle.getLong(EXTRA_DELAY_ANSWER, 0),
        rejectDelay = bundle.getLong(EXTRA_DELAY_REJECT, 0),
        disconnectDelay = bundle.getLong(EXTRA_DELAY_DISCONNECT, 0),
        isWifi = bundle.getBoolean(EXTRA_HIGH_DEF_AUDIO, false),
        isHdAudio = bundle.getBoolean(EXTRA_WIFI, false)
    )

    fun toBundle(): Bundle = Bundle().apply {
        putInt(TelecomManager.EXTRA_INCOMING_VIDEO_STATE, videoState)
        putLong(EXTRA_DELAY_ANSWER, answerDelay)
        putLong(EXTRA_DELAY_REJECT, rejectDelay)
        putLong(EXTRA_DELAY_DISCONNECT, disconnectDelay)
        putBoolean(EXTRA_HIGH_DEF_AUDIO, isHdAudio)
        putBoolean(EXTRA_WIFI, isWifi)
    }
}