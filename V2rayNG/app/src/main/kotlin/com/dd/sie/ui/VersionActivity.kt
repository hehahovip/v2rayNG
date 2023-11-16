package com.dd.sie.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.dd.sie.R
import com.dd.sie.databinding.ActivityVersionBinding
import com.dd.sie.util.SIEUtils
import android.content.*

class VersionActivity : BaseActivity() {

    private lateinit var binding: ActivityVersionBinding

    private val et_version: TextView by lazy { findViewById(R.id.version_version) }
    private val et_wifiap: TextView by lazy { findViewById(R.id.version_wifiap) }
    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_version)

        title = getString(R.string.title_sie_version)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding = ActivityVersionBinding.inflate(layoutInflater)

        binding.versionCpu.setText("SystemEnv")

        et_version.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                count++
                if(count == 3) {
                    count = 0
                    startMainAt()
                }

            }
        })

        et_wifiap.setText(SIEUtils.readWlan0MacAddress())

    }

    fun startMainAt(){
        startActivity(Intent(this, MainActivity::class.java))
    }
}