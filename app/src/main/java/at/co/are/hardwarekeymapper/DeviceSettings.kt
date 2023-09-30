package at.co.are.hardwarekeymapper

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences

abstract class DeviceSettings(
    private val sharedPreferences: SharedPreferences,
    private val context: Context
    ) {

    val availableOrientations = intArrayOf(
        R.string.orientation_portrait_bottom,
        R.string.orientation_landscape_left,
        R.string.orientation_portrait_top,
        R.string.orientation_landscape_right)
    val availableKeys = intArrayOf(
        R.string.key_key_home,
        R.string.key_key_back,
        R.string.key_key_menu,
        R.string.key_key_search,
        R.string.key_key_app_switch,
        R.string.key_key_camera,
        R.string.key_key_volume_up,
        R.string.key_key_volume_down,
        R.string.key_key_unknown)
    val availableActions = intArrayOf(
        R.string.key_action_short_press,
        R.string.key_action_long_press,
        R.string.key_overlay_app,
        R.string.key_overlay_intent_down,
        R.string.key_overlay_intent_up)

    var availableActionValues: Array<String> = context.resources.getStringArray(R.array.action_values)
    private var availableActionEntries: Array<String> = context.resources.getStringArray(R.array.action_entries)

    companion object {
        fun getCurrentDeviceSettings(
            sharedPreferences: SharedPreferences,
            context: Context
        ) : DeviceSettings{
            return when (android.os.Build.MODEL) {
                "Atom_XL" -> IntercomDeviceSettings(sharedPreferences,context)
                "Atom_L" -> PTTDeviceSettings(sharedPreferences,context)
                "Armor_3WT" -> IntercomDeviceSettings(sharedPreferences,context)
                "Armor_3W" -> PTTDeviceSettings(sharedPreferences,context)
                else -> DefaultDeviceSettings(sharedPreferences,context)
            }
        }
    }

    @Suppress("BooleanMethodIsAlwaysInverted")
    fun isOrientationActive(orientationRes: Int): Boolean {
        return sharedPreferences.getBoolean(getResourceString(orientationRes),getDefaultActive(orientationRes))
    }

    @Suppress("BooleanMethodIsAlwaysInverted")
    fun isKeyActive(keyRes: Int): Boolean {
        return when (keyRes) {
            R.string.key_key_unknown -> {
                getKeyScanCode(keyRes) != 0
            }
            else -> sharedPreferences.getBoolean(getResourceString(keyRes),getDefaultActive(keyRes))
        }
    }

    fun getKeyScanCode(keyRes: Int): Int {
        return sharedPreferences.getString(getResourceString(keyRes),getDefaultScanCode(keyRes))?.toInt() ?: 0
    }

    fun isOrientationKeyActive(orientationRes: Int,keyRes: Int): Boolean {
        return isOrientationActive(orientationRes) && isKeyActive(keyRes)
    }

    fun isOrientationTitleActive(orientationRes: Int): Boolean {
        if (isOrientationActive(orientationRes)) {
            for (key in availableKeys) {
                if (isKeyActive(key)) return true
            }
        }
        return false
    }

    private fun getResourceString(res: Int):String {
        return context.resources.getString(res)
    }
    fun getKeyString(keyRes: Int): String {
        return getResourceString(keyRes)
    }
    fun getActionString(actionRes: Int): String {
        return getResourceString(actionRes)
    }
    fun getOrientationString(orientationRes: Int): String {
        return getResourceString(orientationRes)
    }
    fun getOrientationTitleString(orientationRes: Int): String {
        return getResourceString(orientationRes)+"_"+getResourceString(R.string.key_title_orientation)
    }
    fun getOrientationKeyString(orientationRes: Int, keyRes: Int): String {
        return getOrientationString(orientationRes)+"_"+getResourceString(keyRes)
    }
    fun getOrientationKeyActionString(orientationRes: Int, keyRes: Int, actionRes: Int): String {
        return getOrientationKeyString(orientationRes,keyRes)+"_"+getResourceString(actionRes)
    }

    abstract fun getOrientationKeyActionDefault(ignoredOrientationRes: Int, keyRes: Int, actionRes: Int):String

    fun getOrientationKeyActionValue(orientationRes: Int, keyRes: Int, actionRes: Int): String? {
        return sharedPreferences.getString(getOrientationKeyActionString(orientationRes,keyRes,actionRes),getOrientationKeyActionDefault(orientationRes,keyRes,actionRes))
    }
    private fun getOrientationKeyActionEntry(orientationRes: Int, keyRes: Int, actionRes: Int): String {
        val value = getOrientationKeyActionValue(orientationRes,keyRes,actionRes)
        val index = availableActionValues.indexOf(value)
        return availableActionEntries[index]
    }
    fun getOrientationKeyActionGlobal(orientationRes: Int, keyRes: Int, actionRes: Int): Int {
        return when (getOrientationKeyActionValue(orientationRes,keyRes,actionRes)) {
            availableActionValues[1] -> AccessibilityService.GLOBAL_ACTION_BACK
            availableActionValues[2] -> AccessibilityService.GLOBAL_ACTION_HOME
            availableActionValues[3] -> AccessibilityService.GLOBAL_ACTION_RECENTS
            availableActionValues[4] -> AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
            availableActionValues[5] -> AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
            availableActionValues[6] -> AccessibilityService.GLOBAL_ACTION_POWER_DIALOG
            availableActionValues[7] -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            availableActionValues[8] -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
            availableActionValues[9] -> AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN
            //deviceSettings.availableActionValues[10] -> return GLOBAL_ACTION_KEYCODE_HEADSETHOOK
            availableActionValues[10] -> HardwareKeyMapperService.LOCAL_ACTION_TOGGLE_FLASH_LIGHT
            else -> 0
        }
    }
    fun getOrientationKeySummary(orientationRes: Int, keyRes: Int): String {
        var summaryString = getResourceString(R.string.title_action_short_press) + ": "
        summaryString += getOrientationKeyActionEntry(orientationRes,keyRes,R.string.key_action_short_press)
        summaryString += "\n"+getResourceString(R.string.title_action_long_press) + ": "
        summaryString += getOrientationKeyActionEntry(orientationRes,keyRes,R.string.key_action_long_press)

        val valueOverlayApp = getOrientationKeyActionValue(orientationRes,keyRes,R.string.key_overlay_app)
        val valueOverlayIntentDown = getOrientationKeyActionValue(orientationRes,keyRes,R.string.key_overlay_intent_down)
        val valueOverlayIntentUp = getOrientationKeyActionValue(orientationRes,keyRes,R.string.key_overlay_intent_up)

        if (!valueOverlayApp.isNullOrEmpty()) {
            if (!valueOverlayIntentDown.isNullOrEmpty() || !valueOverlayIntentUp.isNullOrEmpty()) {
                summaryString += "\n"+getResourceString(R.string.title_detail_overlay)+": "+getResourceString(R.string.title_detail_overlay_active)
            }
        }

        return summaryString
    }

    protected abstract fun getDefaultScanCode(res: Int):String
    @Suppress("BooleanMethodIsAlwaysInverted")
    protected abstract fun getDefaultActive(res: Int):Boolean
}

