package at.co.are.hardwarekeymapper

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Intent
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.Surface
import android.view.accessibility.AccessibilityEvent
import androidx.preference.PreferenceManager

class HardwareKeyMapperService : AccessibilityService() {
    companion object {
        private const val TAG = "HardwareKeyMapperService"
        lateinit var availableActionValues: Array<String>
    }
    init {
        availableActionValues = SettingsActivity.appContext.resources.getStringArray(R.array.action_values)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}
    override fun onInterrupt() {}

    public override fun onKeyEvent(event: KeyEvent): Boolean {
// Quick Leave when canceled
        if (event.isCanceled) return false
// Quick Leave when not the proper action
        val action = event.action
        if (
            action != KeyEvent.ACTION_UP
            && action != KeyEvent.ACTION_DOWN
        ) return false
        Log.d(TAG,"KeyAction = "+action.toString())
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
            else -> return false
        }
// Quick Leave when not the supported screen orientation
        val orientationRes =
            when ((getSystemService(DISPLAY_SERVICE) as DisplayManager).getDisplay(Display.DEFAULT_DISPLAY).rotation) {
                Surface.ROTATION_0 -> {
                    Log.d(TAG,getString(R.string.log_service_portrait_bottom))
                    R.string.orientation_portrait_bottom
                }
                Surface.ROTATION_90 -> {
                    Log.d(TAG,getString(R.string.log_service_landscape_right))
                    R.string.orientation_landscape_right
                }
                Surface.ROTATION_180 -> {
                    Log.d(TAG,getString(R.string.log_service_portrait_top))
                    R.string.orientation_portrait_top
                }
                Surface.ROTATION_270 -> {
                    Log.d(TAG,getString(R.string.log_service_landscape_left))
                    R.string.orientation_landscape_left
                }
                else -> return false
            }
// Check the global settings first
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
// Check if key mapping is active
        val keyName = getString(keyRes)
        if (keyRes == R.string.key_key_menu || keyRes == R.string.key_key_camera) {
            if (!sharedPreferences.getBoolean(keyName, false)) return false
        } else {
            if (!sharedPreferences.getBoolean(keyName, true)) return false
        }
// Check if orientation mapping is active
        val orientationName = getString(orientationRes)
        if (orientationRes == R.string.orientation_portrait_top) {
            if (!sharedPreferences.getBoolean(orientationName, false)) return false
        } else {
            if (!sharedPreferences.getBoolean(orientationName, true)) return false
        }

        val orientationKey = orientationName + "_" + keyName
// Check and run Overlay
        val overlayApp = sharedPreferences.getString(
            orientationKey + "_" + getString(R.string.key_overlay_app),
            ""
        )
        val overlayIntentDown = sharedPreferences.getString(
            orientationKey + "_" + getString(R.string.key_overlay_intent_down),
            ""
        )
        val overlayIntentUp = sharedPreferences.getString(
            orientationKey + "_" + getString(R.string.key_overlay_intent_up),
            ""
        )
        if (executeOverlay(overlayApp, overlayIntentDown, overlayIntentUp, action, event.isLongPress)) return true
// Check and run Actions
        val actionShortPress = sharedPreferences.getString(
            orientationKey + "_" + getString(R.string.key_action_short_press),
            availableActionValues[0]
        )
        val actionLongPress = sharedPreferences.getString(
            orientationKey + "_" + getString(R.string.key_action_long_press),
            availableActionValues[0]
        )
        if (executeAction(actionShortPress, actionLongPress, action, event.isLongPress)) return true
// Default if nothing was executed
        return false
    }
    private fun executeOverlay(overlayApp: String?, overlayIntentDown: String?, overlayIntentUp: String?, action: Int, isLongPress: Boolean): Boolean {
        // The first in the list of RunningTasks is always the foreground task.
        for (runningAppProcess in (getSystemService(ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses) {
            if (runningAppProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (runningAppProcess.pkgList.equals(overlayApp)) {
                    val actionIntent = when (action) {
                        KeyEvent.ACTION_UP -> {
                            if (overlayIntentUp != null && overlayIntentUp.isNotEmpty()) {
                                Intent(overlayIntentUp)
                            } else null
                        }
                        KeyEvent.ACTION_DOWN -> {
                            if (overlayIntentDown != null && overlayIntentDown.isNotEmpty()) {
                                if (overlayIntentUp == null || overlayIntentUp.isEmpty() || !isLongPress) {
                                    Intent(overlayIntentDown)
                                } else null
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
            }
        }
        return false
    }

    private fun executeAction(actionShortPress: String?, actionLongPress: String?, action: Int, isLongPress: Boolean): Boolean {
        fun performSingleAction(action: Int): Boolean {
            performGlobalAction(action)
            return true
        }
        fun performEventAction(actionEvent: String?): Boolean {
            return when(actionEvent) {
                availableActionValues[0] -> false
                availableActionValues[1] -> performSingleAction(GLOBAL_ACTION_BACK)
                availableActionValues[2] -> performSingleAction(GLOBAL_ACTION_HOME)
                availableActionValues[3] -> performSingleAction(GLOBAL_ACTION_RECENTS)
                availableActionValues[4] -> performSingleAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                availableActionValues[5] -> performSingleAction(GLOBAL_ACTION_LOCK_SCREEN)
                availableActionValues[6] -> performSingleAction(GLOBAL_ACTION_POWER_DIALOG)
                availableActionValues[7] -> performSingleAction(GLOBAL_ACTION_NOTIFICATIONS)
                availableActionValues[8] -> performSingleAction(GLOBAL_ACTION_QUICK_SETTINGS)
                availableActionValues[9] -> performSingleAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
        //       availableActionValues[10] -> return performLocalAction(GLOBAL_ACTION_KEYCODE_HEADSETHOOK)
                else -> false
            }
        }
        if (action == KeyEvent.ACTION_UP && !isLongPress) {
            return performEventAction(actionShortPress)
        } else if (action == KeyEvent.ACTION_DOWN && isLongPress) {
            return performEventAction(actionLongPress)
        }
        return false
    }
}