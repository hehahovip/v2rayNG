package com.dd.sie.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.dd.sie.R
import com.dd.sie.databinding.ActivityVersionBinding
import com.dd.sie.util.SIEUtils

class VersionActivity : BaseActivity() {

    private lateinit var binding: ActivityVersionBinding

    private val et_version: TextView by lazy { findViewById(R.id.version_version) }
    private val et_cpu: TextView by lazy { findViewById(R.id.version_cpu) }
    private val et_wifiap: TextView by lazy { findViewById(R.id.version_wifiap) }
    private var count_version = 0
    private var count_mac = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_version)

        title = getString(R.string.title_sie_version)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding = ActivityVersionBinding.inflate(layoutInflater)

//        binding.versionCpu.setText("SystemEnv")
        et_cpu.setText("Snapdragon 870")

        et_version.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                count_version++
                if(count_version == 3) {
                    count_version = 0
                    startMainAt()
                }

            }
        })

        et_wifiap.setText(SIEUtils.readWlan0MacAddress())

        et_wifiap.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                count_mac++
                if(count_mac == 3) {
                    count_mac = 0
                    startUpgradeServerAt()
                }

            }
        })

    }

    fun startMainAt(){
        startActivity(Intent(this, MainActivity::class.java))
    }

    fun startUpgradeServerAt(){
        startActivity(Intent(this, UpgradeServerConfigActivity::class.java))
    }
}