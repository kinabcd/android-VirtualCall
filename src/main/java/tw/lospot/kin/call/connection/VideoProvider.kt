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

/**
 * Video emulator
 * Created by Kin_Lo on 2017/8/9.
 */

class VideoProvider(private val context: Context) : android.telecom.Connection.VideoProvider() {
    var connection: ConnectionProxy? = null
    var previewSurface: Surface? = null
    var displaySurface: Surface? = null
    var camera: Camera? = null
    private var mediaPlayer = MediaPlayer.create(context, R.raw.jino)
    override fun onSetPreviewSurface(surface: Surface?) {
        Log.v(this, "onSetPreviewSurface $surface")
        if (previewSurface != surface) {
            if (previewSurface != null) {
                camera?.removePreviewSurface(previewSurface!!)
            }
            previewSurface?.release()
            if (surface != null) {
                camera?.addPreviewSurface(surface)
            }
        }
        previewSurface = surface
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
        val oldCameraId = camera?.cameraId
        Log.v(this, "onSetCamera $oldCameraId -> $cameraId")
        if (oldCameraId == cameraId) {
            return
        }
        if (previewSurface != null) {
            camera?.removePreviewSurface(previewSurface!!)
        }
        camera?.release()
        camera = null
        if (cameraId != null) {
            camera = Camera(context, cameraId)
            if (previewSurface != null) {
                camera?.addPreviewSurface(previewSurface!!)
            }
        }
    }

    override fun onSetDeviceOrientation(orientation: Int) {
        Log.i(this, "onSetDeviceOrientation $orientation")
    }

    override fun onSetDisplaySurface(surface: Surface?) {
        Log.v(this, "onSetDisplaySurface $surface")
        if (displaySurface != surface) {
            if (displaySurface == null) {
                changePeerDimensions(mediaPlayer.videoWidth, mediaPlayer.videoHeight)
            }
            displaySurface = surface
            if (surface != null) {
                mediaPlayer.setSurface(surface)
                mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                mediaPlayer.isLooping = true
                mediaPlayer.start()
            } else {
                mediaPlayer.setSurface(null)
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
            }
        }
    }

    override fun onSendSessionModifyRequest(fromProfile: VideoProfile?, toProfile: VideoProfile?) {
        Log.v(this, "onSendSessionModifyRequest $fromProfile->$toProfile")
        if (toProfile == null) {
            return
        }
        val delayTime = when (connection!!.videoState) {
            VideoProfile.STATE_AUDIO_ONLY -> 3000L
            else -> 100L
        }

        Handler().postDelayed({
            receiveSessionModifyResponse(SESSION_MODIFY_REQUEST_SUCCESS, fromProfile, toProfile)
            connection?.videoState = toProfile.videoState
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
        connection?.videoState = responseProfile.videoState
    }
}