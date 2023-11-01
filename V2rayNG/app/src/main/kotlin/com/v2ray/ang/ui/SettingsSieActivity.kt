package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.activity.viewModels
import androidx.preference.*
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.util.Utils
import com.v2ray.ang.viewmodel.SettingsViewModel

class SettingsSieActivity : BaseActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sie_settings)

        title = getString(R.string.title_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        settingsViewModel.startListenPreferenceChange()
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private val mode by lazy { findPreference<ListPreference>(AppConfig.PREF_MODE) }

        override fun onCreatePreferences(bundle: Bundle?, s: String?) {
            addPreferencesFromResource(R.xml.pref_settings_sie)

            mode?.setOnPreferenceChangeListener { _, newValue ->
                updateMode(newValue.toString())
                true
            }
            mode?.dialogLayoutResource = R.layout.preference_with_help_link
            //loglevel.summary = "LogLevel"
        }

        override fun onStart() {
            super.onStart()
            val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
            updateMode(defaultSharedPreferences.getString(AppConfig.PREF_MODE, "VPN"))
            var remoteDnsString = defaultSharedPreferences.getString(AppConfig.PREF_REMOTE_DNS, "")

            if (TextUtils.isEmpty(remoteDnsString)) {
                remoteDnsString = AppConfig.DNS_AGENT
            }

        }

        private fun updateMode(mode: String?) {
            val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
            val vpn = mode == "VPN"
        }

    }

    fun onModeHelpClicked(view: View) {
        Utils.openUri(this, AppConfig.v2rayNGWikiMode)
    }
}
