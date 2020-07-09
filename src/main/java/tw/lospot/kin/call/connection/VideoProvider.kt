package tw.lospot.kin.call.connection

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.SystemClock
import android.telecom.VideoProfile
import android.view.Surface
import tw.lospot.kin.call.Log
import tw.lospot.kin.call.R
import tw.lospot.kin.call.units.Camera
import kotlin.properties.Delegates

/**
 * Video emulator
 * Created by Kin_Lo on 2017/8/9.
 */

class VideoProvider(private val context: Context, private val connection: ConnectionProxy) : android.telecom.Connection.VideoProvider() {
    private var previewSurface: Surface? = null
        set(new) {
            val old = field
            if (old != new) {
                field = new
                Log.v(this, "previewSurface $old -> $new")
                old?.let {
                    camera?.removePreviewSurface(it)
                }
                new?.let {
                    camera?.addPreviewSurface(it)
                }
            }
        }
    private var displaySurface: Surface? = null
        set(new) {
            val old = field
            if (old != new) {
                field = new
                Log.v(this, "displaySurface $old -> $new")

                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.stop()
                        it.setSurface(null)
                        it.release()
                    }
                }
                if (new != null) {
                    mediaPlayer = MediaPlayer.create(context, R.raw.chino).also {
                        changePeerDimensions(it.videoWidth, it.videoHeight)
                        it.setSurface(new)
                        it.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                        it.isLooping = true
                        it.start()
                    }
                }
            }
        }
    private var cameraId by Delegates.observable<String?>(null) { _, old, new ->
        if (old != new) {
            Log.v(this, "cameraId $old -> $new")
            if (new != null) {
                camera = Camera(context, new)
            } else {
                camera = null
            }
        }
    }
    private var camera by Delegates.observable<Camera?>(null) { _, old, new ->
        if (old != new) {
            previewSurface?.let { surface ->
                old?.removePreviewSurface(surface)
                old?.release()
                new?.addPreviewSurface(surface)
            }
        }
    }
    private var mediaPlayer: MediaPlayer? = null
    override fun onSetPreviewSurface(surface: Surface?) {
        previewSurface = surface
    }

    override fun onSetDisplaySurface(surface: Surface?) {
        displaySurface = surface
    }

    override fun onRequestCameraCapabilities() {
        Log.v(this, "onRequestCameraCapabilities")
        if (camera != null) {
            changeCameraCapabilities(VideoProfile.CameraCapabilities(640, 480))
        } else {
            changeCameraCapabilities(null)
        }
    }

    override fun onSetCamera(cameraId: String?) {
        this.cameraId = cameraId
    }

    override fun onSetDeviceOrientation(orientation: Int) {
        Log.i(this, "onSetDeviceOrientation $orientation")
    }

    override fun onSendSessionModifyRequest(fromProfile: VideoProfile?, toProfile: VideoProfile?) {
        Log.v(this, "onSendSessionModifyRequest $fromProfile->$toProfile")
        if (toProfile == null) {
            return
        }
        val delayTime = when (connection.videoState) {
            VideoProfile.STATE_AUDIO_ONLY -> 3000L
            else -> 100L
        }

        Handler().postDelayed({
            receiveSessionModifyResponse(SESSION_MODIFY_REQUEST_SUCCESS, fromProfile, toProfile)
            connection.videoState = toProfile.videoState
        }, delayTime)
    }

    override fun onSetPauseImage(p0: Uri?) {
    }

    override fun onRequestConnectionDataUsage() {
        setCallDataUsage(SystemClock.elapsedRealtime())
    }

    override fun onSetZoom(p0: Float) {
    }

    override fun onSendSessionModifyResponse(responseProfile: VideoProfile?) {
        Log.v(this, "onSendSessionModifyResponse $responseProfile")
        if (responseProfile == null) {
            return
        }
        connection.videoState = responseProfile.videoState
    }
}