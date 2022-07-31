package at.co.are.hardwarekeymapper

import android.content.Context
import android.content.SharedPreferences

class AtomLDeviceSettings(sharedPreferences: SharedPreferences,
                          context: Context
) : DeviceSettings(sharedPreferences, context) {

    override fun getDefaultActive(res: Int): Boolean {
        return when(res) {
            R.string.orientation_portrait_top -> false
            R.string.key_key_camera -> false
            R.string.key_key_search -> false
            R.string.key_key_menu -> false
            else -> true
        }
    }
    override fun getOrientationKeyActionDefault(orientationRes: Int, keyRes: Int, actionRes: Int):String {
        return when(actionRes) {
            R.string.key_action_short_press -> availableActionValues[0]
            R.string.key_action_long_press -> availableActionValues[0]
            else -> ""
        }
    }
}