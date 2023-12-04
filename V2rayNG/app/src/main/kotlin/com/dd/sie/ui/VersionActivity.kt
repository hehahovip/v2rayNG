package com.dd.sie.ui

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.dd.sie.AppConfig
import com.dd.sie.R
import com.dd.sie.databinding.ActivityVersionBinding
import com.dd.sie.util.SIEUtils
import java.math.BigDecimal
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class VersionActivity : BaseActivity() {

    private lateinit var binding: ActivityVersionBinding

    private val etVersion: TextView by lazy { findViewById(R.id.version_version) }
    private val etMacAddress: TextView by lazy { findViewById(R.id.version_wifiap) }
    private val etCurrentTraffic: TextView by lazy { findViewById(R.id.current_traffic_value) }
    private val etHistoryTraffic: TextView by lazy { findViewById(R.id.history_traffic_value) }
    private var countVersion = 0
    private var countMac = 0

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_version)

        title = getString(R.string.title_sie_version)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding = ActivityVersionBinding.inflate(layoutInflater)

//        binding.versionCpu.setText("SystemEnv")
        et_cpu.setText("Snapdragon 870")

        etVersion.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                countVersion++
                if (countVersion == 3) {
                    countVersion = 0
                    startMainAt()
                }

            }
        })

        etMacAddress.text = (SIEUtils.readWlan0MacAddress())

        etMacAddress.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                countMac++
                if (countMac == 3) {
                    countMac = 0
                    startUpgradeServerAt()
                }

            }
        })

        // 计算移动流量并显示
        getDataUsage()

    }

    fun startMainAt() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    fun startUpgradeServerAt() {
        startActivity(Intent(this, UpgradeServerConfigActivity::class.java))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getDataUsage() {
        val networkStatsManager =
            applicationContext.getSystemService(NETWORK_STATS_SERVICE) as NetworkStatsManager
        val timezone = TimeZone.getDefault()

        var calendar: Calendar = Calendar.getInstance()
        calendar.set(2023,1,1)
        calendar.timeZone = timezone
        var startHistoryDate = calendar.timeInMillis

        var rightNow: Calendar = Calendar.getInstance()
        rightNow.timeZone = timezone
        rightNow.set(Calendar.DAY_OF_MONTH, 1)
        rightNow.set(Calendar.HOUR, 0)
        rightNow.set(Calendar.MINUTE, 0)
        rightNow.set(Calendar.SECOND, 0)
        rightNow.set(Calendar.MILLISECOND, 0)
        var startCurrentMonthDate = rightNow.timeInMillis

        var endCalendar: Calendar = Calendar.getInstance()
        endCalendar.timeZone = timezone
        var endDate = endCalendar.timeInMillis

        Log.d(
            AppConfig.ANG_PACKAGE,
            "Mobile traffic size Start Date: " + String.format("%s", TimeZone.getDefault().toString())
        )
        Log.d(
            AppConfig.ANG_PACKAGE,
            "Mobile traffic size Start Date: " + String.format("%s", startCurrentMonthDate)
        )
        Log.d(
            AppConfig.ANG_PACKAGE,
            "Mobile traffic size End Date: " + String.format("%s", endDate)
        )
        var currentBucketData = networkStatsManager.querySummaryForDevice(
            ConnectivityManager.TYPE_MOBILE,
            null,
            startCurrentMonthDate,
            endDate
        )
        var historyBucketData = networkStatsManager.querySummaryForDevice(
            ConnectivityManager.TYPE_MOBILE,
            null,
            startHistoryDate,
            endDate
        )
        Log.d(
            AppConfig.ANG_PACKAGE,
            "Mobile traffic size: " + String.format(
                "%d",
                (currentBucketData.txBytes + currentBucketData.rxBytes)
            )
        )

        etCurrentTraffic.text = formatData(currentBucketData)
        etHistoryTraffic.text = formatData(historyBucketData)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun formatData(data: NetworkStats.Bucket): String {
        var finalResult = "0 KB"
        if (data != null) {
            var total = data.rxBytes + data.txBytes
            var unit = " B"
            val data1024 = 1024.0
            var result: BigDecimal = BigDecimal((total/data1024)).setScale(2, BigDecimal.ROUND_HALF_UP)
            var backupResult = result
            if (result.toLong() > 0) {
                unit = " KB"
                result = BigDecimal((result.toLong()/data1024).toString()).setScale(2, BigDecimal.ROUND_HALF_UP)
                if (result.toLong() > 0) {
                    backupResult = result
                    unit = " MB"
                    result = BigDecimal((result.toLong()/data1024).toString()).setScale(2, BigDecimal.ROUND_HALF_UP)
                    if (result.toLong() > 0) {
                        backupResult = result
                        unit = " GB"
                        return backupResult.toDouble().toString() + unit
                    } else {
                        return backupResult.toDouble().toString() + unit
                    }
                } else {
                    return backupResult.toDouble().toString() + unit
                }
            } else {
                return backupResult.toDouble().toString() + unit
            }
        }
        return finalResult

    }

}