package at.co.are.hardwarekeymapper

import android.content.Context
import android.content.SharedPreferences

class AtomLXLDeviceSettings(sharedPreferences: SharedPreferences,
                            context: Context
) : DeviceSettings(sharedPreferences, context) {

    companion object {
        const val INTERCOM_APP = "com.agold.intercom"
        const val INTERCOM_INTENT_UP = "android.intent.action.PTT.up"
        const val INTERCOM_INTENT_DOWN = "android.intent.action.PTT.down"
    }

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
                    R.string.key_key_search -> INTERCOM_APP
                    R.string.key_key_unknown -> INTERCOM_APP
                    else -> ""
                }
            R.string.key_overlay_intent_down ->
                when (keyRes) {
                    R.string.key_key_search -> INTERCOM_INTENT_DOWN
                    R.string.key_key_unknown -> INTERCOM_INTENT_DOWN
                    else -> ""
                }
            R.string.key_overlay_intent_up ->
                when (keyRes) {
                    R.string.key_key_search -> INTERCOM_INTENT_UP
                    R.string.key_key_unknown -> INTERCOM_INTENT_UP
                    else -> ""
                }
            else -> ""
        }
    }
}