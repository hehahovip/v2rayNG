package com.v2ray.ang.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.v2ray.ang.R

class VersionActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_version)

        title = getString(R.string.title_sie_version)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)


    }
}