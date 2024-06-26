package at.co.are.hardwarekeymapper

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.KeyEvent
import android.view.Surface
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import androidx.preference.PreferenceManager

class HardwareKeyMapperService : AccessibilityService() {
    private var longPressHandler: Handler? = null
    private var foregroundApp: String? = ""
    private var globalLongPressed = false
    private var flashLight: FlashLightProvider? = null

    companion object {
        const val LOCAL_ACTION_MIN = 1000
        const val LOCAL_ACTION_TOGGLE_FLASH_LIGHT = 1000
        const val LOCAL_ACTION_MAX = 1000
    }

    @SuppressLint("SwitchIntDef")
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
// Only works as a system APP
            AccessibilityEvent.TYPE_WINDOWS_CHANGED ->
                if (event.windowChanges.and(AccessibilityEvent.WINDOWS_CHANGE_ACTIVE) != 0)
                    if (windows[event.windowId].isActive)
                        foregroundApp = event.packageName as String?
                    else if (foregroundApp.equals(event.packageName as String?))
                        foregroundApp = ""
// Should work as a normal APP (last state changed should be fired on foreground app)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ->
                foregroundApp = event.packageName as String?
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        longPressHandler = null
        globalLongPressed = false
        flashLight = null
        return false
    }

    override fun onServiceConnected() {
        longPressHandler = Handler(Looper.myLooper()!!)
        globalLongPressed = false
        flashLight = FlashLightProvider(applicationContext)
    }

    public override fun onKeyEvent(event: KeyEvent): Boolean {
// Quick Leave when canceled
        if (event.isCanceled) return super.onKeyEvent(event)
        val action = event.action
// Quick Leave when not the proper action
        when (action) {
// Clear global buffer
            KeyEvent.ACTION_UP -> {
                longPressHandler?.removeCallbacksAndMessages(null)
                if (globalLongPressed) {
                    globalLongPressed = false
                    return true
                }
            }
            KeyEvent.ACTION_DOWN -> {
                if (event.isLongPress) return true
            }
            else -> return super.onKeyEvent(event)
        }
// Quick Leave when not the supported hardware key
        val keyRes = when (event.keyCode) {
            KeyEvent.KEYCODE_HOME -> R.string.key_key_home
            KeyEvent.KEYCODE_BACK -> R.string.key_key_back
            KeyEvent.KEYCODE_MENU -> R.string.key_key_menu
            KeyEvent.KEYCODE_SEARCH -> R.string.key_key_search
            KeyEvent.KEYCODE_APP_SWITCH -> R.string.key_key_app_switch
            KeyEvent.KEYCODE_CAMERA -> R.string.key_key_camera
            KeyEvent.KEYCODE_VOLUME_DOWN -> R.string.key_key_volume_down
            KeyEvent.KEYCODE_VOLUME_UP -> R.string.key_key_volume_up
            KeyEvent.KEYCODE_UNKNOWN -> R.string.key_key_unknown
            else -> return super.onKeyEvent(event)
        }
// Quick Leave when not the supported screen orientation
        val orientationRes =
            when ((getSystemService(DISPLAY_SERVICE) as DisplayManager).getDisplay(Display.DEFAULT_DISPLAY).rotation) {
                Surface.ROTATION_0 -> {
                    R.string.orientation_portrait_bottom
                }
                Surface.ROTATION_90 -> {
                    R.string.orientation_landscape_right
                }
                Surface.ROTATION_180 -> {
                    R.string.orientation_portrait_top
                }
                Surface.ROTATION_270 -> {
                    R.string.orientation_landscape_left
                }
                else -> return super.onKeyEvent(event)
            }
// Check the global settings first
        val deviceSettings = DeviceSettings.getCurrentDeviceSettings(PreferenceManager.getDefaultSharedPreferences(this), this)
// Check if key mapping is active
        if (keyRes == R.string.key_key_unknown) {
            if (deviceSettings.getKeyScanCode(keyRes) != event.scanCode) return super.onKeyEvent(event)
        } else {
            if (!deviceSettings.isKeyActive(keyRes)) return super.onKeyEvent(event)
        }
// Check if orientation mapping is active
        if (!deviceSettings.isOrientationActive(orientationRes)) return super.onKeyEvent(event)

// Check and run Overlay
        val overlayApp = deviceSettings.getOrientationKeyActionValue(orientationRes, keyRes, R.string.key_overlay_app)
        val overlayIntentDown = deviceSettings.getOrientationKeyActionValue(orientationRes, keyRes, R.string.key_overlay_intent_down)
        val overlayIntentUp = deviceSettings.getOrientationKeyActionValue(orientationRes, keyRes, R.string.key_overlay_intent_up)

        if (executeOverlay(overlayApp, overlayIntentDown, overlayIntentUp, action)) return true
// Check and run Actions
        val actionShortPress = deviceSettings.getOrientationKeyActionGlobal(orientationRes, keyRes, R.string.key_action_short_press)
        val actionLongPress = deviceSettings.getOrientationKeyActionGlobal(orientationRes, keyRes, R.string.key_action_long_press)

        if (executeAction(actionShortPress, actionLongPress, action)) return true
// Default if nothing was executed
        return super.onKeyEvent(event)
    }

    private fun executeOverlay(overlayApp: String?, overlayIntentDown: String?, overlayIntentUp: String?, action: Int): Boolean {
        // The first in the list of RunningTasks is always the foreground task.
        if (overlayApp.isNullOrEmpty()) return false
        if (overlayIntentDown.isNullOrEmpty() && overlayIntentUp.isNullOrEmpty()) return false
        if (foregroundApp == overlayApp) {
            val actionIntent = when (action) {
                KeyEvent.ACTION_UP -> {
                    if (!overlayIntentUp.isNullOrEmpty()) {
                        Intent(overlayIntentUp)
                    } else null
                }
                KeyEvent.ACTION_DOWN -> {
                    if (!overlayIntentDown.isNullOrEmpty()) {
                        Intent(overlayIntentDown)
                    } else null
                }
                else -> null
            }
            if (actionIntent != null) {
                actionIntent.`package` = overlayApp
                //actionIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                try {
                    sendBroadcast(actionIntent)
                    //Toast.makeText(applicationContext, "Successfully broadcast Intent", Toast.LENGTH_SHORT).show()
                } catch (error: ActivityNotFoundException) {
                    //Toast.makeText(applicationContext, "Error broadcasting Intent", Toast.LENGTH_SHORT).show()
                }
            }
            return true
        }
        return false
    }
    private fun executeAction(actionShortPress: Int, actionLongPress: Int, action: Int): Boolean {
        if (action == KeyEvent.ACTION_UP) {
            if (actionShortPress > 0) {
                performLocalAction(actionShortPress)
                return true
            }
        } else if (action == KeyEvent.ACTION_DOWN) {
// Make sure the key isn't pressed continuously
            if (actionLongPress > 0) {
                longPressHandler?.postDelayed({
                    if (actionLongPress == GLOBAL_ACTION_LOCK_SCREEN) {
                        longPressHandler?.removeCallbacksAndMessages(null)
                    } else {
                        globalLongPressed = true
                    }
                    performLocalAction(actionLongPress)
                }, ViewConfiguration.getLongPressTimeout().toLong())
                return true
            }
        }
        return false
    }

    private fun performLocalAction(action: Int) {
        if (action in LOCAL_ACTION_MIN..LOCAL_ACTION_MAX ) {
            when (action) {
                LOCAL_ACTION_TOGGLE_FLASH_LIGHT -> flashLight?.toggleFlashLight()
            }
        } else {
            performGlobalAction(action)
        }
    }
}
