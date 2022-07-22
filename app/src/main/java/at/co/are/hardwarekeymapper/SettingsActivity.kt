package at.co.are.hardwarekeymapper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

private const val TITLE_TAG = "settingsActivityTitle"

class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    companion object {
        lateinit  var appContext: Context
    }

    private var settingsVisible = true
    private lateinit var preferencesMappings : CompositeMappingsPreferenceFragmentCompat
    private lateinit var preferencesOrientationsKeys : OrientationsKeysPreferenceFragmentCompat

    private fun changeActionBar(headline: String = "", subtitle: String = "") {
        if (headline.isNotEmpty()) {
            title = headline
            supportActionBar?.subtitle = subtitle
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            settingsVisible = false
        } else {
            title = getString(R.string.title_mapping)
            supportActionBar?.subtitle = subtitle
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            settingsVisible = true
        }
        invalidateOptionsMenu()
    }

    private fun initializeNavigation() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, preferencesMappings)
            .commit()
    }

    private fun navigateFragment(fragment: Fragment?, caller: Fragment?, args: Bundle?): Boolean {
        if (fragment != null) {
            fragment.arguments = args
            fragment.setTargetFragment(caller, 0)
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
        //PreferenceManager.setDefaultValues(this, R.xml.preferences_keys, false)
        //PreferenceManager.setDefaultValues(this, R.xml.preferences_orientations, false)

        if (savedInstanceState == null) {
            initializeNavigation()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                changeActionBar()
                preferencesMappings.updatePreferences()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        preferencesMappings.updatePreferences()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.settings_menu, menu)
        menu.findItem(R.id.settings_menu_item)?.isVisible = settingsVisible
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem)= when (item.itemId) {
        R.id.settings_menu_item -> {
            if (supportFragmentManager.backStackEntryCount == 0) {
                val currentFragment =
                    supportFragmentManager.fragments[supportFragmentManager.backStackEntryCount]
                if (navigateFragment(preferencesOrientationsKeys,currentFragment,null)) {
                    changeActionBar(item.title as String)
                    preferencesMappings.prepareMappingsUpdate = true
                }
            }
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
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

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val fragment = preferencesMappings.findActionsFragment(pref.key)
        if (navigateFragment(fragment,caller,pref.extras)) {
            changeActionBar(pref.title as String,pref.parent?.title as String)
            preferencesMappings.prepareActionsFragmentUpdate = fragment
        }
        return true
    }

    class OrientationsKeysPreferenceFragmentCompat : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_keys, rootKey)
            addPreferencesFromResource(R.xml.preferences_orientations)
        }
    }

    class CompositeActionsPreferenceFragmentCompat(private val orientationKey: String) : PreferenceFragmentCompat() {
        companion object {
            lateinit var availableActionValues: Array<String>
            lateinit var availableActionEntries: Array<String>
        }
        init {
            availableActionValues = appContext.resources.getStringArray(R.array.action_values)
            availableActionEntries = appContext.resources.getStringArray(R.array.action_entries)
        }

        private fun modifyPreference(actionRes: Int, orientationKey: String, default: Any? = null) {
            val action = getString(actionRes)
            val preference = findPreference<Preference>(action)
            preference?.key = orientationKey+"_"+action
            if (default != null) {
                preference?.setDefaultValue(default)
            }
        }
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_detail, rootKey)
            modifyPreference(R.string.key_action_short_press,availableActionValues[0])
            modifyPreference(R.string.key_action_long_press,orientationKey,availableActionValues[0])
            modifyPreference(R.string.key_overlay_app,orientationKey)
            modifyPreference(R.string.key_overlay_intent_down,orientationKey)
            modifyPreference(R.string.key_overlay_intent_up,orientationKey)
        }
    }

    class CompositeMappingsPreferenceFragmentCompat : PreferenceFragmentCompat() {
        var prepareMappingsUpdate: Boolean = false
        var prepareActionsFragmentUpdate: CompositeActionsPreferenceFragmentCompat? = null

        private var prepareSummariesUpdate: Boolean = true
        private var subsidiaryActionsFragments =
            LinkedHashMap<String, CompositeActionsPreferenceFragmentCompat>()

        private fun modifyPreference(orientation: String, keyRes: Int) {
            val key = getString(keyRes)
            val preference = findPreference<Preference>(key)
            val orientationKey = orientation + "_" + key
            preference?.key = orientationKey
            preference?.fragment = orientationKey
            subsidiaryActionsFragments[orientationKey] =
                CompositeActionsPreferenceFragmentCompat(orientationKey)
        }

        private fun modifyTitle(orientation: String, keyRes: Int, title: String, icon: Drawable) {
            val key = getString(keyRes)
            val preference = findPreference<Preference>(key)
            preference?.key = orientation + "_" + key
            preference?.title = title
            preference?.icon = icon
        }

        fun findActionsFragment(key: String): CompositeActionsPreferenceFragmentCompat? {
            return subsidiaryActionsFragments[key]
        }

        private fun modifyOrientation(orientationRes: Int, titleRes: Int, iconRes: Int) {
            val orientation = getString(orientationRes)
            val icon = appContext.resources.getDrawable(iconRes, requireContext().theme)
            modifyTitle(orientation, R.string.key_title_orientation, getString(titleRes), icon)
            modifyPreference(orientation, R.string.key_key_app_switch)
            modifyPreference(orientation, R.string.key_key_back)
            modifyPreference(orientation, R.string.key_key_camera)
            modifyPreference(orientation, R.string.key_key_home)
            modifyPreference(orientation, R.string.key_key_menu)
            modifyPreference(orientation, R.string.key_key_search)
            modifyPreference(orientation, R.string.key_key_volume_down)
            modifyPreference(orientation, R.string.key_key_volume_up)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_mappings, rootKey)
            modifyOrientation(
                R.string.orientation_portrait_bottom,
                R.string.title_portrait_bottom,
                R.drawable.device_portrait_bottom
            )
            addPreferencesFromResource(R.xml.preferences_mappings)
            modifyOrientation(
                R.string.orientation_landscape_right,
                R.string.title_landscape_left,
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
                R.string.title_landscape_right,
                R.drawable.device_landscape_left
            )
        }

        fun updatePreferences() {
            val sharedPreferences = preferenceManager?.sharedPreferences
                ?: return

            if (prepareMappingsUpdate) {
                updateMappings(sharedPreferences)
                prepareMappingsUpdate = false
            }
            if (prepareSummariesUpdate) {
                for (actionFragment in subsidiaryActionsFragments) {
                    updateSummary(sharedPreferences, actionFragment.key,true)
                }
                prepareSummariesUpdate = false
                prepareActionsFragmentUpdate = null
            } else if (prepareActionsFragmentUpdate != null) {
                val keys =
                    subsidiaryActionsFragments.filterValues { it == prepareActionsFragmentUpdate }.keys
                for (key in keys) {
                    updateSummary(sharedPreferences, key,false)
                }
                prepareActionsFragmentUpdate = null
            }
        }

        private fun updateMappings(sharedPreferences: SharedPreferences) {
            val orientationStates = LinkedHashMap<String, Boolean>()
            val keyStates = LinkedHashMap<String, Boolean>()

            fun addOrientation(orientationRes: Int, default: Boolean) {
                val orientation = getString(orientationRes)
                orientationStates[orientation] = sharedPreferences.getBoolean(orientation, default)
            }

            fun addKey(keyRes: Int, default: Boolean) {
                val key = getString(keyRes)
                keyStates[key] = sharedPreferences.getBoolean(key, default)
            }

            addOrientation(R.string.orientation_portrait_bottom, true)
            addOrientation(R.string.orientation_landscape_left, true)
            addOrientation(R.string.orientation_portrait_top, false)
            addOrientation(R.string.orientation_landscape_right, true)

            addKey(R.string.key_key_home, true)
            addKey(R.string.key_key_back, true)
            addKey(R.string.key_key_menu, false)
            addKey(R.string.key_key_search, true)
            addKey(R.string.key_key_app_switch, true)
            addKey(R.string.key_key_camera, false)
            addKey(R.string.key_key_volume_up, true)
            addKey(R.string.key_key_volume_down, true)

            val orientationTitle = getString(R.string.key_title_orientation)
            for (orientation in orientationStates) {
                val preferenceTitle =
                    findPreference<Preference>(orientation.key + "_" + orientationTitle)
                preferenceTitle?.isVisible = orientation.value
                for (key in keyStates) {
                    val preferenceMapping =
                        findPreference<Preference>(orientation.key + "_" + key.key)
                    preferenceMapping?.isVisible = (orientation.value and key.value)
                }
            }
        }

        private fun updateSummary(
            sharedPreferences: SharedPreferences,
            key: String,
            updateValues: Boolean
        ) {
            val preferenceMapping = findPreference<Preference>(key)

            val keyShortPress = key+"_"+getString(R.string.key_action_short_press)
            val keyLongPress = key+"_"+getString(R.string.key_action_long_press)
            val keyOverlayApp = key+"_"+getString(R.string.key_overlay_app)
            val keyOverlayIntentDown = key+"_"+getString(R.string.key_overlay_intent_down)
            val keyOverlayIntentUp = key+"_"+getString(R.string.key_overlay_intent_up)

            val valueShortPress = sharedPreferences.getString(keyShortPress,CompositeActionsPreferenceFragmentCompat.availableActionValues[0])
            val valueLongPress = sharedPreferences.getString(keyLongPress,CompositeActionsPreferenceFragmentCompat.availableActionValues[0])
            val valueOverlayApp = sharedPreferences.getString(keyOverlayApp,"")
            val valueOverlayIntentDown = sharedPreferences.getString(keyOverlayIntentDown,"")
            val valueOverlayIntentUp = sharedPreferences.getString(keyOverlayIntentUp,"")

            var summaryString = getString(R.string.title_action_short_press) + ": "
            summaryString += getActionListTitle(valueShortPress!!)
            summaryString += ", "+getString(R.string.title_action_long_press) + ": "
            summaryString += getActionListTitle(valueLongPress!!)

            if (valueOverlayApp!!.isNotEmpty()) {
                if ((valueOverlayIntentDown!!.isNotEmpty()) || (valueOverlayIntentUp!!.isNotEmpty())) {
                    summaryString += ", "+getString(R.string.title_detail_overlay)+": "+getString(R.string.title_detail_overlay_active)
                }
            }

            preferenceMapping?.summary = summaryString

            if (updateValues) {
                sharedPreferences.edit()
                    .putString(keyShortPress,valueShortPress)
                    .putString(keyLongPress,valueShortPress)
                    .putString(keyOverlayApp,valueOverlayApp)
                    .putString(keyOverlayIntentDown,valueOverlayIntentDown)
                    .putString(keyOverlayIntentUp,valueOverlayIntentUp)
                    .apply()
            }
        }

        private fun getActionListTitle(
            value: String,
        ) : String {
            val index = CompositeActionsPreferenceFragmentCompat.availableActionValues.indexOf(value)
            return CompositeActionsPreferenceFragmentCompat.availableActionEntries[index]
        }
    }

}


