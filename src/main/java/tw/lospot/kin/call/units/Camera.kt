package tw.lospot.kin.call.units

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.support.annotation.MainThread
import android.view.Surface
import tw.lospot.kin.call.Log


class Camera(private val mContext: Context, val cameraId: String) {

    private val mCameraManager: CameraManager = mContext.getSystemService(CameraManager::class.java)
    private var mCameraDevice: CameraDevice? = null

    private val mPreviewSurfaces: MutableList<Surface> = ArrayList()
    private var mPreviewSession: CameraCaptureSession? = null
    private var mPreviewRepeatingId: Int? = -1

    private var isCameraOpening: Boolean = false
    private var isCameraClosing: Boolean = false
    private var isSessionCreating: Boolean = false

    private fun checkPermission(): Boolean =
            mContext.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    @MainThread
    fun release() {
        Log.d(this, "($cameraId) release")
        destroyPreviewSession()
        closeCamera()
    }

    @MainThread
    fun addPreviewSurface(surface: Surface) {
        Log.d(this, "($cameraId) addPreviewSurface $surface")
        mPreviewSurfaces.add(surface)
        updatePreview()
    }

    @MainThread
    fun removePreviewSurface(surface: Surface) {
        Log.d(this, "($cameraId) removePreviewSurface $surface")
        mPreviewSurfaces.remove(surface)
        updatePreview()
    }

    private fun updatePreview() {
        destroyPreviewSession()
        if (mPreviewSurfaces.size > 0) {
            if (mCameraDevice == null) {
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
        if (!isCameraOpening && mCameraDevice == null) {
            Log.d(this, "($cameraId) openCamera")
            isCameraOpening = true
            mCameraManager.openCamera(cameraId, DeviceStateCallback(), null)
        }
    }

    private fun closeCamera() {
        if (isCameraClosing) {
            return
        }
        Log.d(this, "($cameraId) closeCamera")
        isCameraClosing = true
        isCameraOpening = false
        mCameraDevice?.close()
        mCameraDevice = null
    }

    inner class DeviceStateCallback : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            if (isCameraOpening) {
                Log.d(this@Camera, "($cameraId) onOpened")
                isCameraOpening = false
                mCameraDevice = camera
                if (mPreviewSurfaces.size > 0) {
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
            mCameraDevice = null
            mPreviewSession = null
            mPreviewRepeatingId = -1
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
        if (!isSessionCreating && mPreviewSurfaces.size > 0 && mPreviewSession == null) {
            Log.d(this, "($cameraId) createPreviewSession")
            isSessionCreating = true
            try {
                mCameraDevice?.createCaptureSession(ArrayList(mPreviewSurfaces), PreviewSessionStateCallback(), null)
            } catch (e: CameraAccessException) {
                mCameraDevice = null
            }
        }
    }

    private fun destroyPreviewSession() {
        if (mPreviewSession == null) {
            return
        }
        Log.d(this, "($cameraId) destroyPreviewSession")
        if (mPreviewRepeatingId != -1) {
            mPreviewSession?.stopRepeating()
            mPreviewRepeatingId = -1
        }
        mPreviewSession?.close()
        mPreviewSession = null
    }

    private inner class PreviewSessionStateCallback : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) {
            Log.d(this@Camera, "($cameraId) onConfigured")
            isSessionCreating = false
            mPreviewSession = session
            startPreviewRepeating()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.d(this@Camera, "($cameraId) onConfigureFailed")
            session.close()
            isSessionCreating = false
            mPreviewSession = null
            mPreviewRepeatingId = -1
        }
    }

    private fun startPreviewRepeating() {
        Log.d(this, "($cameraId) startPreviewRepeating")
        val newSurfaces = ArrayList(mPreviewSurfaces)
        val previewBuilder = mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        newSurfaces.forEach { previewBuilder?.addTarget(it) }
        if (mPreviewRepeatingId != -1) {
            mPreviewSession?.stopRepeating()
        }
        if (previewBuilder != null) {
            mPreviewRepeatingId = mPreviewSession?.setRepeatingRequest(previewBuilder.build(), null, null)
        }
    }
}