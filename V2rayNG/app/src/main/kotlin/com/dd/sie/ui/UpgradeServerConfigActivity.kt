package com.dd.sie.ui

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import com.dd.sie.R

class UpgradeServerConfigActivity : AppCompatActivity() {

    private val et_server: EditText by lazy { findViewById(R.id.et_sie_upgrade_server_address) }
    private val btn_server: Button by lazy { findViewById(R.id.sie_upgradeserver_btn) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upgrade_server_config)
        val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        et_server.setText(defaultSharedPreferences.getString(com.dd.sie.AppConfig.UPGRADE_SERVER_ADDR, ""))

        btn_server.setOnClickListener {

            Log.i("SIE", et_server.text.toString().trim())

            var editor = defaultSharedPreferences.edit()
            editor.putString(com.dd.sie.AppConfig.UPGRADE_SERVER_ADDR, et_server.text.toString().trim())
            editor.commit()

            finish()
        }
    }
}