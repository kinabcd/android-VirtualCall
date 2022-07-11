package tw.lospot.kin.call.units

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import androidx.annotation.MainThread
import android.view.Surface
import tw.lospot.kin.call.Log
import java.lang.Exception


class Camera(private val context: Context, val cameraId: String) {

    private val cameraManager: CameraManager = context.getSystemService(CameraManager::class.java)
    private var cameraDevice: CameraDevice? = null

    private val previewSurfaces: MutableList<Surface> = ArrayList()
    private var previewSession: CameraCaptureSession? = null
    private var previewRepeatingId: Int? = -1

    private var isCameraOpening: Boolean = false
    private var isCameraClosing: Boolean = false
    private var isSessionCreating: Boolean = false

    private fun checkPermission(): Boolean =
            context.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    @MainThread
    fun release() {
        Log.d(this, "($cameraId) release")
        destroyPreviewSession()
        closeCamera()
    }

    @MainThread
    fun addPreviewSurface(surface: Surface) {
        Log.d(this, "($cameraId) addPreviewSurface $surface")
        previewSurfaces.add(surface)
        updatePreview()
    }

    @MainThread
    fun removePreviewSurface(surface: Surface) {
        Log.d(this, "($cameraId) removePreviewSurface $surface")
        previewSurfaces.remove(surface)
        updatePreview()
    }

    private fun updatePreview() {
        destroyPreviewSession()
        if (previewSurfaces.size > 0) {
            if (cameraDevice == null) {
                openCamera()
            } else {
                createPreviewSession()
            }
        }
    }

    private fun openCamera() {
        if (!checkPermission()) {
            Log.w(this, "($cameraId) openCamera: Permission denied")
            return
        }
        if (!isCameraOpening && cameraDevice == null) {
            Log.d(this, "($cameraId) openCamera")
            isCameraOpening = true
            cameraManager.openCamera(cameraId, DeviceStateCallback(), null)
        }
    }

    private fun closeCamera() {
        if (isCameraClosing) {
            return
        }
        Log.d(this, "($cameraId) closeCamera")
        isCameraClosing = true
        isCameraOpening = false
        cameraDevice?.close()
        cameraDevice = null
    }

    inner class DeviceStateCallback : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            if (isCameraOpening) {
                Log.d(this@Camera, "($cameraId) onOpened")
                isCameraOpening = false
                cameraDevice = camera
                if (previewSurfaces.size > 0) {
                    createPreviewSession()
                }
            } else {
                Log.d(this@Camera, "($cameraId) onOpened - Was asked to close at opening, close camera")
                camera.close()
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(this@Camera, "($cameraId) onDisconnected")
            camera.close()
            isCameraClosing = false
            isCameraOpening = false
            cameraDevice = null
            previewSession = null
            previewRepeatingId = -1
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d(this@Camera, "($cameraId) onError $error")
            camera.close()
            if (isCameraOpening) {
                isCameraOpening = false
                // try again
                Handler().postDelayed({ openCamera() }, 100)
            }
        }
    }

    private fun createPreviewSession() {
        if (!isSessionCreating && previewSurfaces.size > 0 && previewSession == null) {
            Log.d(this, "($cameraId) createPreviewSession")
            isSessionCreating = true
            try {
                cameraDevice?.createCaptureSession(ArrayList(previewSurfaces), PreviewSessionStateCallback(), null)
            } catch (e: CameraAccessException) {
                cameraDevice = null
            }
        }
    }

    private fun destroyPreviewSession() {
        if (previewSession == null) {
            return
        }
        Log.d(this, "($cameraId) destroyPreviewSession")
        if (previewRepeatingId != -1) {
            previewSession?.stopRepeating()
            previewRepeatingId = -1
        }
        previewSession?.close()
        previewSession = null
    }

    private inner class PreviewSessionStateCallback : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) {
            Log.d(this@Camera, "($cameraId) onConfigured")
            isSessionCreating = false
            previewSession = session
            startPreviewRepeating()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.d(this@Camera, "($cameraId) onConfigureFailed")
            session.close()
            isSessionCreating = false
            previewSession = null
            previewRepeatingId = -1
        }
    }

    private fun startPreviewRepeating() {
        Log.d(this, "($cameraId) startPreviewRepeating")
        val newSurfaces = ArrayList(previewSurfaces)
        val previewBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        newSurfaces.forEach { previewBuilder?.addTarget(it) }
        if (previewRepeatingId != -1) {
            previewSession?.stopRepeating()
        }
        if (previewBuilder != null) {
            try {
                previewRepeatingId = previewSession?.setRepeatingRequest(previewBuilder.build(), null, null)
            } catch (e: Exception) {
                Log.w(this, "($cameraId) startPreviewRepeating: failed. surfaces=$newSurfaces ${e.message}")
            }
        }
    }
}