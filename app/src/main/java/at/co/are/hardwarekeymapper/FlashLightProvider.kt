package at.co.are.hardwarekeymapper

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.util.Log

class FlashLightProvider(private val context: Context?) {
    private var isFlashLightOn: Boolean = false
    private var hasFlashLight: Boolean = false
    private lateinit var camManager: CameraManager

    init {
        hasFlashLight = context?.packageManager?.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)!!
        if (hasFlashLight) {
            val torchCallback = object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    isFlashLightOn = enabled
                }
            }
            try {
                camManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                camManager.registerTorchCallback(torchCallback, null)
            } catch (e: CameraAccessException) {
                Log.e(TAG, e.toString())
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun hasFlashLight(): Boolean {
        return hasFlashLight
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun isFlashLightOn(): Boolean {
        return isFlashLightOn
    }


    @Suppress("MemberVisibilityCanBePrivate")
    fun turnFlashlightOn() {
        if (!hasFlashLight()) return
        try {
            camManager = context!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = camManager.cameraIdList[0] // Usually front camera is at 0 position.
            camManager.setTorchMode(cameraId, true)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun turnFlashlightOff() {
        if (!hasFlashLight()) return
        try {
            camManager = context!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = camManager.cameraIdList[0] // Usually front camera is at 0 position.
            camManager.setTorchMode(cameraId, false)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun toggleFlashLight() {
        if (!hasFlashLight()) return
        if (isFlashLightOn()) turnFlashlightOff()
        else turnFlashlightOn()
    }

    companion object {
        private val TAG: String = FlashLightProvider::class.java.simpleName
    }
}