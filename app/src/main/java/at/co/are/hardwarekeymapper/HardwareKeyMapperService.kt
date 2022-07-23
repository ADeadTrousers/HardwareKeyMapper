package at.co.are.hardwarekeymapper

import android.accessibilityservice.AccessibilityService
import android.hardware.display.DisplayManager
import android.view.accessibility.AccessibilityEvent
import androidx.preference.PreferenceManager
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.Surface

class HardwareKeyMapperService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {}
    public override fun onKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        if ((action != KeyEvent.ACTION_UP && action != KeyEvent.ACTION_DOWN) || event.isCanceled) {
            return false
        }
        return false

//To-DO: Redo the AccessibilityService
        val keycode = event.keyCode
        //if (action == KeyEvent.ACTION_UP) {
        //    Toast.makeText(getApplicationContext(), "KeyCode = " + String.valueOf(keycode), Toast.LENGTH_LONG).show();
        //}
        if (action == KeyEvent.ACTION_DOWN && (keycode == KeyEvent.KEYCODE_BACK || keycode == KeyEvent.KEYCODE_APP_SWITCH)) {
            // Don't want to send ACTION_DOWN to the apps as they may interpret it as a button
            // press that's held down.
            return !event.isLongPress
        }
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val backButtonActionPrefKey: String
        val switchAppButtonActionPrefKey: String

        when ((getSystemService(DISPLAY_SERVICE) as DisplayManager).getDisplay(Display.DEFAULT_DISPLAY).rotation) {
            Surface.ROTATION_90 -> {
                // button on the right.
                backButtonActionPrefKey = "back_button_action_buttons_right"
                switchAppButtonActionPrefKey = "switch_app_button_action_buttons_right"
                Log.d(TAG, "Landscape 90 - buttons on the right")
            }
            Surface.ROTATION_180 -> {
                // buttons on the top.
                backButtonActionPrefKey = "back_button_action_buttons_left"
                switchAppButtonActionPrefKey = "switch_app_button_action_buttons_left"
                Log.d(TAG, "Portrait 180 - buttons on the top")
            }
            Surface.ROTATION_270 -> {
                // buttons on the left.
                backButtonActionPrefKey = "back_button_action_buttons_left"
                switchAppButtonActionPrefKey = "switch_app_button_action_buttons_left"
                Log.d(TAG, "Landscape 270 - buttons on the left")
            }
            else -> {
                backButtonActionPrefKey = "back_button_action_portrait"
                switchAppButtonActionPrefKey = "switch_app_button_action_portrait"
                Log.d(TAG, "Portrait 0 - buttons on the bottom")
            }
        }
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK -> return if (pref.getString(
                    backButtonActionPrefKey,
                    BACK
                ) == SWITCH_APP
            ) {
                performGlobalAction(GLOBAL_ACTION_RECENTS)
                true
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
                true
            }
            KeyEvent.KEYCODE_APP_SWITCH -> if (pref.getString(
                    switchAppButtonActionPrefKey,
                    SWITCH_APP
                ) == BACK
            ) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                return true
            } else {
                performGlobalAction(GLOBAL_ACTION_RECENTS)
            }
        }
        return false
    }

    override fun onInterrupt() {}

    companion object {
        private const val TAG = "NavButtonRemapService"
        private const val BACK = "0"
        private const val SWITCH_APP = "1"
    }
}