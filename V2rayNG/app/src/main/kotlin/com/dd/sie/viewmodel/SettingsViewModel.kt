package com.dd.sie.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.tencent.mmkv.MMKV
import com.dd.sie.util.MmkvManager

class SettingsViewModel(application: Application) : AndroidViewModel(application), SharedPreferences.OnSharedPreferenceChangeListener {

    private val settingsStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SETTING, MMKV.MULTI_PROCESS_MODE) }

    fun startListenPreferenceChange() {
        PreferenceManager.getDefaultSharedPreferences(getApplication()).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        PreferenceManager.getDefaultSharedPreferences(getApplication()).unregisterOnSharedPreferenceChangeListener(this)
        Log.i(com.dd.sie.AppConfig.ANG_PACKAGE, "Settings ViewModel is cleared")
        super.onCleared()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        Log.d(com.dd.sie.AppConfig.ANG_PACKAGE, "Observe settings changed: $key")
        when(key) {
            com.dd.sie.AppConfig.PREF_MODE,
            com.dd.sie.AppConfig.PREF_VPN_DNS,
            com.dd.sie.AppConfig.PREF_REMOTE_DNS,
            com.dd.sie.AppConfig.PREF_DOMESTIC_DNS,
            com.dd.sie.AppConfig.PREF_LOCAL_DNS_PORT,
            com.dd.sie.AppConfig.PREF_SOCKS_PORT,
            com.dd.sie.AppConfig.PREF_HTTP_PORT,
            com.dd.sie.AppConfig.PREF_LOGLEVEL,
            com.dd.sie.AppConfig.PREF_LANGUAGE,
            com.dd.sie.AppConfig.PREF_ROUTING_DOMAIN_STRATEGY,
            com.dd.sie.AppConfig.PREF_ROUTING_MODE,
            com.dd.sie.AppConfig.PREF_V2RAY_ROUTING_AGENT,
            com.dd.sie.AppConfig.PREF_V2RAY_ROUTING_BLOCKED,
            com.dd.sie.AppConfig.PREF_V2RAY_ROUTING_DIRECT, -> {
                settingsStorage?.encode(key, sharedPreferences.getString(key, ""))
            }
            com.dd.sie.AppConfig.PREF_SPEED_ENABLED,
            com.dd.sie.AppConfig.PREF_PROXY_SHARING,
            com.dd.sie.AppConfig.PREF_LOCAL_DNS_ENABLED,
            com.dd.sie.AppConfig.PREF_FAKE_DNS_ENABLED,
            com.dd.sie.AppConfig.PREF_ALLOW_INSECURE,
            com.dd.sie.AppConfig.PREF_PREFER_IPV6,
            com.dd.sie.AppConfig.PREF_PER_APP_PROXY,
            com.dd.sie.AppConfig.PREF_BYPASS_APPS,
            com.dd.sie.AppConfig.PREF_CONFIRM_REMOVE,
            com.dd.sie.AppConfig.PREF_START_SCAN_IMMEDIATE, -> {
                settingsStorage?.encode(key, sharedPreferences.getBoolean(key, false))
            }
            com.dd.sie.AppConfig.PREF_SNIFFING_ENABLED -> {
                settingsStorage?.encode(key, sharedPreferences.getBoolean(key, true))
            }
            com.dd.sie.AppConfig.PREF_PER_APP_PROXY_SET -> {
                settingsStorage?.encode(key, sharedPreferences.getStringSet(key, setOf()))
            }
        }
    }
}
