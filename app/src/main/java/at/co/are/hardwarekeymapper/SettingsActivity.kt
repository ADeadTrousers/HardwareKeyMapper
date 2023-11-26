package at.co.are.hardwarekeymapper

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils.SimpleStringSplitter
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.preference.*

private const val TITLE_TAG = "settingsActivityTitle"

class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    companion object {
        lateinit var appContext: Context
    }

    private var optionsMenu = R.menu.settings_menu
    private lateinit var preferencesMappings: CompositeMappingsPreferenceFragmentCompat
    private lateinit var preferencesOrientationsKeys: OrientationsKeysPreferenceFragmentCompat

    private fun changeActionBar(optionsMenu: Int, headline: String = "", subtitle: String = "") {
        if (headline.isNotEmpty()) {
            title = headline
            supportActionBar?.subtitle = subtitle
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } else {
            title = getString(R.string.title_mapping)
            supportActionBar?.subtitle = subtitle
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
        this.optionsMenu = optionsMenu
        invalidateOptionsMenu()
    }

    private fun initializeNavigation() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, preferencesMappings)
            .commit()
    }

    @Suppress("BooleanMethodIsAlwaysInverted")
    private fun navigateFragment(fragment: Fragment?, args: Bundle?): Boolean {
        if (fragment != null) {
            fragment.arguments = args
            // Replace the existing Fragment with the new Fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit()
            return true
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        setContentView(R.layout.settings_activity)

        preferencesMappings = CompositeMappingsPreferenceFragmentCompat()
        preferencesOrientationsKeys = OrientationsKeysPreferenceFragmentCompat()

        if (savedInstanceState == null) {
            initializeNavigation()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                changeActionBar(R.menu.settings_menu)
                preferencesMappings.updatePreferences()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        preferencesMappings.updatePreferences()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (optionsMenu > 0) {
            val inflater: MenuInflater = menuInflater
            inflater.inflate(optionsMenu, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.settings_menu_item -> {
            if (supportFragmentManager.backStackEntryCount == 0) {
                if (navigateFragment(preferencesOrientationsKeys, null)) {
                    preferencesMappings.prepareMappingsUpdate = true
                    changeActionBar(0, item.title as String)
                }
            }
            true
        }
        R.id.detail_menu_clear_actions -> {
            getCurrentActions()?.clearPreferences(shortPress = true, longPress = true)
            true
        }
        R.id.detail_menu_clear_overlay -> {
            getCurrentActions()?.clearPreferences(app = true, intentDown = true, intentUp = true)
            true
        }
        R.id.detail_menu_clear_all -> {
            getCurrentActions()?.clearPreferences(shortPress = true, longPress = true, app = true, intentDown = true, intentUp = true)
            true
        }
        R.id.detail_menu_clear_short_press -> {
            getCurrentActions()?.clearPreferences(shortPress = true)
            true
        }
        R.id.detail_menu_clear_long_press -> {
            getCurrentActions()?.clearPreferences(longPress = true)
            true
        }
        R.id.detail_menu_clear_app -> {
            getCurrentActions()?.clearPreferences(app = true)
            true
        }
        R.id.detail_menu_clear_intent_down -> {
            getCurrentActions()?.clearPreferences(intentDown = true)
            true
        }
        R.id.detail_menu_clear_intent_up -> {
            getCurrentActions()?.clearPreferences(intentUp = true)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun getCurrentActions(): CompositeActionsPreferenceFragmentCompat? {
        val fragment = supportFragmentManager.fragments.last()
        if (fragment is CompositeActionsPreferenceFragmentCompat) {
            return fragment
        }
        return null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val fragment = preferencesMappings.findActionsFragment(pref.key)
        if (navigateFragment(fragment, pref.extras)) {
            changeActionBar(R.menu.details_menu, pref.title as String, pref.parent?.title as String)
            preferencesMappings.prepareActionsFragmentUpdate = fragment
        }
        return true
    }

    class OrientationsKeysPreferenceFragmentCompat : PreferenceFragmentCompat() {
        private lateinit var deviceSettings: DeviceSettings

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            deviceSettings = DeviceSettings.getCurrentDeviceSettings(preferenceManager.sharedPreferences!!, appContext)

            setPreferencesFromResource(R.xml.preferences_keys, rootKey)
            addPreferencesFromResource(R.xml.preferences_orientations)

            for (key in deviceSettings.availableKeys) {
                modifyKey(key)
            }
            for (orientation in deviceSettings.availableOrientations) {
                modifyOrientation(orientation)
            }
        }

        private fun modifyKey(keyRes: Int) {
            val key = deviceSettings.getKeyString(keyRes)
            val preference = findPreference<Preference>(key)

            if (preference is SwitchPreference) {
                preference.isChecked = deviceSettings.isKeyActive(keyRes)
            } else if (preference is EditTextPreference) {
                preference.text = deviceSettings.getKeyScanCode(keyRes).toString()
                preference.setOnBindEditTextListener { editText -> editText.inputType = InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_SIGNED }
            }
        }

        private fun modifyOrientation(orientationRes: Int) {
            val orientation = deviceSettings.getOrientationString(orientationRes)
            val preference = findPreference<Preference>(orientation)

            if (preference is SwitchPreference) {
                preference.isChecked = deviceSettings.isOrientationActive(orientationRes)
            }
        }

    }

    class CompositeActionsPreferenceFragmentCompat(val orientation: Int, val key: Int, private val deviceSettings: DeviceSettings) : PreferenceFragmentCompat() {

        private fun modifyPreference(actionRes: Int) {
            val action = deviceSettings.getActionString(actionRes)
            val preference = findPreference<Preference>(action)
            preference?.key = deviceSettings.getOrientationKeyActionString(orientation, key, actionRes)

            if (preference is ListPreference) {
                preference.value = deviceSettings.getOrientationKeyActionValue(orientation, key, actionRes)
            } else if (preference is EditTextPreference) {
                preference.text = deviceSettings.getOrientationKeyActionValue(orientation, key, actionRes)
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_detail, rootKey)
            for (action in deviceSettings.availableActions) {
                modifyPreference(action)
            }
        }

        fun clearPreferences(shortPress: Boolean = false, longPress: Boolean = false, app: Boolean = false, intentDown: Boolean = false, intentUp: Boolean = false) {
            clearPreference(R.string.key_action_short_press, shortPress)
            clearPreference(R.string.key_action_long_press, longPress)
            clearPreference(R.string.key_overlay_app, app)
            clearPreference(R.string.key_overlay_intent_down, intentDown)
            clearPreference(R.string.key_overlay_intent_up, intentUp)
        }

        private fun clearPreference(actionRes: Int, clear: Boolean) {
            if (clear) {
                val action = deviceSettings.getOrientationKeyActionString(orientation, key, actionRes)
                val preference = findPreference<Preference>(action)
                if (preference is ListPreference) {
                    preference.value = deviceSettings.getOrientationKeyActionDefault(orientation, key, actionRes)
                } else if (preference is EditTextPreference) {
                    preference.text = deviceSettings.getOrientationKeyActionDefault(orientation, key, actionRes)
                }
            }
        }
    }

    class CompositeMappingsPreferenceFragmentCompat : PreferenceFragmentCompat() {
        var prepareMappingsUpdate: Boolean = true
        var prepareActionsFragmentUpdate: CompositeActionsPreferenceFragmentCompat? = null

        private lateinit var deviceSettings: DeviceSettings
        private var prepareSummariesUpdate: Boolean = true
        private var subsidiaryActionsFragments = LinkedHashSet<CompositeActionsPreferenceFragmentCompat>()

        private fun modifyPreference(orientationRes: Int, keyRes: Int) {
            val key = deviceSettings.getKeyString(keyRes)
            val preference = findPreference<Preference>(key)
            val orientationKey = deviceSettings.getOrientationKeyString(orientationRes, keyRes)
            preference?.key = orientationKey
            preference?.fragment = orientationKey
            subsidiaryActionsFragments.add(CompositeActionsPreferenceFragmentCompat(orientationRes, keyRes, deviceSettings))
        }

        private fun modifyTitle(orientationRes: Int, keyRes: Int, titleRes: Int, iconRes: Int) {
            val key = deviceSettings.getKeyString(keyRes)
            val preference = findPreference<Preference>(key)
            preference?.key = deviceSettings.getOrientationKeyString(orientationRes, keyRes)
            preference?.title = getString(titleRes)
            preference?.icon = ResourcesCompat.getDrawable(resources, iconRes, requireContext().theme)
        }

        fun findActionsFragment(key: String): CompositeActionsPreferenceFragmentCompat? {
            for (fragment in subsidiaryActionsFragments) {
                val orientationKey = deviceSettings.getOrientationKeyString(fragment.orientation, fragment.key)
                if (orientationKey == key) return fragment
            }
            return null
        }

        private fun modifyOrientation(orientationRes: Int, titleRes: Int, iconRes: Int) {
            modifyTitle(orientationRes, R.string.key_title_orientation, titleRes, iconRes)
            modifyPreference(orientationRes, R.string.key_key_app_switch)
            for (key in deviceSettings.availableKeys) {
                modifyPreference(orientationRes, key)
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            deviceSettings = DeviceSettings.getCurrentDeviceSettings(preferenceManager.sharedPreferences!!, appContext)

            setPreferencesFromResource(R.xml.preferences_service, rootKey)
            addPreferencesFromResource(R.xml.preferences_mappings)
            modifyOrientation(
                R.string.orientation_portrait_bottom,
                R.string.title_portrait_bottom,
                R.drawable.device_portrait_bottom
            )
            addPreferencesFromResource(R.xml.preferences_mappings)
            modifyOrientation(
                R.string.orientation_landscape_right,
                R.string.title_landscape_right,
                R.drawable.device_landscape_right
            )
            addPreferencesFromResource(R.xml.preferences_mappings)
            modifyOrientation(
                R.string.orientation_portrait_top,
                R.string.title_portrait_top,
                R.drawable.device_portrait_top
            )
            addPreferencesFromResource(R.xml.preferences_mappings)
            modifyOrientation(
                R.string.orientation_landscape_left,
                R.string.title_landscape_left,
                R.drawable.device_landscape_left
            )
        }

        fun updatePreferences() {
            updateAccessibilityServiceSummary()

            if (prepareMappingsUpdate) {
                updateMappings()
                prepareMappingsUpdate = false
            }
            if (prepareSummariesUpdate) {
                for (fragment in subsidiaryActionsFragments) {
                    updateSummary(fragment)
                }
                prepareSummariesUpdate = false
                prepareActionsFragmentUpdate = null
            } else if (prepareActionsFragmentUpdate != null) {
                if (subsidiaryActionsFragments.contains(prepareActionsFragmentUpdate!!)) {
                    updateSummary(prepareActionsFragmentUpdate!!)
                }
                prepareActionsFragmentUpdate = null
            }
        }

        private fun updateAccessibilityServiceSummary() {
            val serviceEnabled = findPreference<Preference>(getString(R.string.key_service_switch))
            if (!accessibilityServiceEnabled()) {
                serviceEnabled?.summary = getString(R.string.summary_service_switch_enable)
            } else {
                serviceEnabled?.summary = getString(R.string.summary_service_switch_disable)
            }
        }

        private fun accessibilityServiceEnabled(): Boolean {
            val service = requireActivity().packageName + "/" + HardwareKeyMapperService::class.java.canonicalName
            val stringColonSplitter = SimpleStringSplitter(':')
            val settingValue = Settings.Secure.getString(
                requireActivity().applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                stringColonSplitter.setString(settingValue)
                while (stringColonSplitter.hasNext()) {
                    val accessibilityService = stringColonSplitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
            return false
        }

        private fun updateMappings() {
            for (orientation in deviceSettings.availableOrientations) {
                val title = deviceSettings.getOrientationTitleString(orientation)
                val preferenceTitle = findPreference<Preference>(title)
                for (key in deviceSettings.availableKeys) {
                    val orientationKey = deviceSettings.getOrientationKeyString(orientation, key)
                    val preferenceOrientationKey = findPreference<Preference>(orientationKey)
                    preferenceOrientationKey?.isVisible = deviceSettings.isOrientationKeyActive(orientation, key)
                }
                preferenceTitle?.isVisible = deviceSettings.isOrientationTitleActive(orientation)
            }
        }

        private fun updateSummary(fragment: CompositeActionsPreferenceFragmentCompat) {
            val orientationKey = deviceSettings.getOrientationKeyString(fragment.orientation, fragment.key)
            val preferenceOrientationKey = findPreference<Preference>(orientationKey)
            preferenceOrientationKey?.summary = deviceSettings.getOrientationKeySummary(fragment.orientation, fragment.key)
        }
    }

}


