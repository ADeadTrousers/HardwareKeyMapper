package at.co.are.hardwarekeymapper

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.Surface
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import androidx.preference.PreferenceManager
import java.util.*

class HardwareKeyMapperService : AccessibilityService() {
    companion object {
        private const val TAG = "HardwareKeyMapperService"
    }

    private var longPressHandler : Handler? = null
    private var globalLongPressed = false

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {
        longPressHandler = null
        globalLongPressed = false
    }

    override fun onServiceConnected() {
        longPressHandler = Handler(Looper.myLooper()!!)
        globalLongPressed = false
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
                    Log.d(TAG, getString(R.string.log_service_portrait_bottom))
                    R.string.orientation_portrait_bottom
                }
                Surface.ROTATION_90 -> {
                    Log.d(TAG, getString(R.string.log_service_landscape_right))
                    R.string.orientation_landscape_right
                }
                Surface.ROTATION_180 -> {
                    Log.d(TAG, getString(R.string.log_service_portrait_top))
                    R.string.orientation_portrait_top
                }
                Surface.ROTATION_270 -> {
                    Log.d(TAG, getString(R.string.log_service_landscape_left))
                    R.string.orientation_landscape_left
                }
                else -> return super.onKeyEvent(event)
            }
// Check the global settings first
        val deviceSettings = DeviceSettings.getCurrentDeviceSettings(PreferenceManager.getDefaultSharedPreferences(this), this)
// Check if key mapping is active
        if (!deviceSettings.isKeyActive(keyRes)) return super.onKeyEvent(event)
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
        if (overlayApp == null || overlayApp.isEmpty()) return false
        if ((overlayIntentDown == null || overlayIntentDown.isEmpty()) && (overlayIntentUp == null || overlayIntentUp.isEmpty())) return false
        if (getForegroundApp().equals(overlayApp)) {
            val actionIntent = when (action) {
                KeyEvent.ACTION_UP -> {
                    if (overlayIntentUp != null && overlayIntentUp.isNotEmpty()) {
                        Intent(overlayIntentUp)
                    } else null
                }
                KeyEvent.ACTION_DOWN -> {
                    if (overlayIntentDown != null && overlayIntentDown.isNotEmpty()) {
                        Intent(overlayIntentDown)
                    } else null
                }
                else -> null
            }
            if (actionIntent != null) {
                actionIntent.`package` = overlayApp
                startActivity(actionIntent)
            }
            return true
        }
        return false
    }

    private fun getForegroundApp(): String? {
        val time = System.currentTimeMillis()
        val appStatsList = (getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager).queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time)
        if (appStatsList != null && appStatsList.isNotEmpty()) {
            return Collections.max(appStatsList) { o1, o2 ->
                o1.lastTimeUsed.compareTo(o2.lastTimeUsed)
            }.packageName
        }
        return null
    }

    private fun executeAction(actionShortPress: Int, actionLongPress: Int, action: Int): Boolean {
        if (action == KeyEvent.ACTION_UP) {
            if (actionShortPress > 0) {
                performGlobalAction(actionShortPress)
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
                    performGlobalAction(actionLongPress)
                }, ViewConfiguration.getLongPressTimeout().toLong())
                return true
            }
        }
        return false
    }
}
