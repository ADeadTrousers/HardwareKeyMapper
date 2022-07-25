package at.co.are.hardwarekeymapper

import android.content.Context
import android.content.SharedPreferences

class AtomLXLDeviceSettings(sharedPreferences: SharedPreferences,
                            context: Context
) : DeviceSettings(sharedPreferences, context) {

    override fun getDefaultActive(res: Int): Boolean {
        return when(res) {
            R.string.orientation_portrait_top -> false
            R.string.key_key_camera -> false
            R.string.key_key_menu -> false
            else -> true
        }
    }
    override fun getOrientationKeyActionDefault(orientationRes: Int, keyRes: Int, actionRes: Int):String {
        return when(actionRes) {
            R.string.key_action_short_press -> availableActionValues[0]
            R.string.key_action_long_press -> availableActionValues[0]
            R.string.key_overlay_app ->
                when (keyRes) {
                    R.string.key_key_search -> "com.agold.intercom"
                    else -> ""
                }
            R.string.key_overlay_intent_down ->
                when (keyRes) {
                    R.string.key_key_search -> "android.intent.action.PTT.down"
                    else -> ""
                }
            R.string.key_overlay_intent_up ->
                when (keyRes) {
                    R.string.key_key_search -> "android.intent.action.PTT.up"
                    else -> ""
                }
            else -> ""
        }
    }
}