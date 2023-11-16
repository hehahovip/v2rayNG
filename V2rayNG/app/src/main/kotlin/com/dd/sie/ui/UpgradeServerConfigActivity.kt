package com.dd.sie.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.dd.sie.R

class UpgradeServerConfigActivity : AppCompatActivity() {

    private val et_server: EditText by lazy { findViewById(R.id.et_sie_upgrade_server_address) }
    private val btn_server: Button by lazy { findViewById(R.id.sie_upgradeserver_btn) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upgrade_server_config)
    }
}