package com.dd.sie.ui

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.res.ColorStateList
import android.net.VpnService
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.tbruyelle.rxpermissions.RxPermissions
import com.tencent.mmkv.MMKV
import com.dd.sie.AppConfig.ANG_PACKAGE
import com.dd.sie.R
import com.dd.sie.databinding.ActivitySieMainBinding
import com.dd.sie.extension.toast
import com.dd.sie.helper.SimpleItemTouchHelperCallback
import com.dd.sie.service.V2RayServiceManager
import com.dd.sie.util.*
import com.dd.sie.viewmodel.MainViewModel
import kotlinx.coroutines.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MainSieActivity : BaseActivity() {
    private lateinit var binding: ActivitySieMainBinding

    private val adapter by lazy { MainSieRecyclerAdapter(this) }
    private val mainStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_MAIN, MMKV.MULTI_PROCESS_MODE) }
    private val settingsStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SETTING, MMKV.MULTI_PROCESS_MODE) }
    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            startV2Ray()
        }
    }
    private var mItemTouchHelper: ItemTouchHelper? = null
    val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySieMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        title = getString(R.string.title_server)
        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener {
            if (mainViewModel.isRunning.value == true) {
                Utils.stopVService(this)
            } else if (settingsStorage?.decodeString(com.dd.sie.AppConfig.PREF_MODE) ?: "VPN" == "VPN") {
                val intent = VpnService.prepare(this)
                if (intent == null) {
                    startV2Ray()
                } else {
                    requestVpnPermission.launch(intent)
                }
            } else {
                startV2Ray()
            }
        }
        binding.layoutTest.setOnClickListener {
            if (mainViewModel.isRunning.value == true) {
                setTestState(getString(R.string.connection_test_testing))
                mainViewModel.testCurrentServerRealPing()
            } else {
//                tv_test_state.text = getString(R.string.connection_test_fail)
            }
        }

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        val callback = SimpleItemTouchHelperCallback(adapter)
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper?.attachToRecyclerView(binding.recyclerView)

        setupViewModel()
        copyAssets()
        migrateLegacy()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RxPermissions(this)
                .request(Manifest.permission.POST_NOTIFICATIONS)
                .subscribe {
                    if (!it)
                        toast(R.string.toast_permission_denied)
                }
        }

        SIEUtils.askRootPermission()

        initLoadNodesConfig()

        checkAutoStart()

        wifiFunc()
    }

    private fun checkAutoStart() {
        val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val autoStartFlag = defaultSharedPreferences.getBoolean(com.dd.sie.AppConfig.PREF_AUTO_START, false)

        if(autoStartFlag) {
            Log.d("SIE", "AutoStarting...")
            mainViewModel.reloadServerList()
            mainViewModel.testAllRealPinginMain()
            MmkvManager.findFastestServer()
            V2RayServiceManager.startV2Ray(this)
        }
    }

    private fun wifiFunc() {
        var wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if(wifiManager.isWifiEnabled) {
            Log.d("SIE","wifiManager is enabled!")
        } else {
            Log.d("SIE","wifiManager is disabled!")
        }

        var p2pManager = applicationContext.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager

    }

    private fun initLoadNodesConfig() {
        var checkInitFlag = mainStorage.getBoolean("init", false)
        if(!checkInitFlag) {
            loadNodesConfig()
        }
    }

    private fun updateSub() {
        // delete old nodes
        MmkvManager.removeAllServer()


        toast("更新线路中")
        loadNodesConfig()
        setupViewModel()
        mainViewModel.reloadServerList()
//        mainViewModel.reloadServerList()
    }

    private fun loadNodesConfig() {

        val scope = CoroutineScope(Job() + Dispatchers.IO)
        val job = scope.launch {
            // 写入粘贴板数据，导入节点信息
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            var flag = true
            externalCacheDir?.let {  flag = SIEUtils.downloadToFile(it.path ) }

            if(flag) {
                var file = File((externalCacheDir?.path ?: "") + SIEUtils.DOWNLOAD_FILE_SUFFIX)
                var nodesByteArray = SIEUtils.doCipher(file.readBytes())
                launch(Dispatchers.Main) {
                    if(nodesByteArray != null) {
                        var nodes = String(nodesByteArray)

                        ClipData.newPlainText("label", nodes)?.let{clipboardManager.setPrimaryClip(it)}
                        importClipboard()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            clipboardManager.clearPrimaryClip()
                        }

                        mainStorage.encode("init", true)
                        mainStorage.sync()
                    }
                }

            }
        }
    }

    private fun setupViewModel() {
        mainViewModel.updateListAction.observe(this) { index ->
            if (index >= 0) {
                adapter.notifyItemChanged(index)
            } else {
                adapter.notifyDataSetChanged()
            }
        }
        mainViewModel.updateTestResultAction.observe(this) { setTestState(it) }
        mainViewModel.isRunning.observe(this) { isRunning ->
            adapter.isRunning = isRunning
            if (isRunning) {
                binding.fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorSelected))
                setTestState(getString(R.string.connection_connected))
                binding.layoutTest.isFocusable = true
            } else {
                binding.fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorUnselected))
                setTestState(getString(R.string.connection_not_connected))
                binding.layoutTest.isFocusable = false
            }
            hideCircle()
        }
        mainViewModel.startListenBroadcast()
    }

    private fun copyAssets() {
        val extFolder = Utils.userAssetPath(this)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geo = arrayOf("geosite.dat", "geoip.dat")
                assets.list("")
                        ?.filter { geo.contains(it) }
                        ?.filter { !File(extFolder, it).exists() }
                        ?.forEach {
                            val target = File(extFolder, it)
                            assets.open(it).use { input ->
                                FileOutputStream(target).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            Log.i(ANG_PACKAGE, "Copied from apk assets folder to ${target.absolutePath}")
                        }
            } catch (e: Exception) {
                Log.e(ANG_PACKAGE, "asset copy failed", e)
            }
        }
    }

    private fun migrateLegacy() {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = AngConfigManager.migrateLegacyConfig(this@MainSieActivity)
            if (result != null) {
                launch(Dispatchers.Main) {
                    if (result) {
                        toast(getString(R.string.migration_success))
                        mainViewModel.reloadServerList()
                    } else {
                        toast(getString(R.string.migration_fail))
                    }
                }
            }
        }
    }

    fun startV2Ray() {
        if (mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER).isNullOrEmpty()) {
            return
        }
        showCircle()
//        toast(R.string.toast_services_start)
        V2RayServiceManager.startV2Ray(this)
        hideCircle()
    }

    fun restartV2Ray() {
        if (mainViewModel.isRunning.value == true) {
            Utils.stopVService(this)
        }
        Observable.timer(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    startV2Ray()
                }
    }

    public override fun onResume() {
        super.onResume()
        mainViewModel.reloadServerList()
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_sie, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.sie_real_ping_all -> {
            mainViewModel.testAllRealPing()
            true
        }

        R.id.sie_service_restart -> {
            restartV2Ray()
            true
        }

        R.id.sie_sub_update -> {
            updateSub()
            true
        }

        R.id.sie_setting -> {
            setting()
            true
        }

        R.id.sie_version -> {
            version()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    /**
     * import config from clipboard
     */
    fun importClipboard()
            : Boolean {
        try {
            val clipboard = Utils.getClipboard(this)
            importBatchConfig(clipboard)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun importBatchConfig(server: String?, subid: String = "") {
        val subid2 = if(subid.isNullOrEmpty()){
            mainViewModel.subscriptionId
        }else{
            subid
        }
        val append = subid.isNullOrEmpty()

        var count = AngConfigManager.importBatchConfig(server, subid2, append)
        if (count <= 0) {
            count = AngConfigManager.importBatchConfig(Utils.decode(server!!), subid2, append)
        }
        if (count > 0) {
            toast(R.string.toast_success)
            mainViewModel.reloadServerList()
        } else {
            toast(R.string.toast_failure)
        }
    }

    fun setTestState(content: String?) {
        binding.tvTestState.text = content
    }

//    val mConnection = object : ServiceConnection {
//        override fun onServiceDisconnected(name: ComponentName?) {
//        }
//
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            sendMsg(AppConfig.MSG_REGISTER_CLIENT, "")
//        }
//    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    fun showCircle() {
        binding.fabProgressCircle.show()
    }

    fun hideCircle() {
        try {
            Observable.timer(300, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        try {
                            if (binding.fabProgressCircle.isShown) {
                                binding.fabProgressCircle.hide()
                            }
                        } catch (e: Exception) {
                            Log.w(ANG_PACKAGE, e)
                        }
                    }
        } catch (e: Exception) {
            Log.d(ANG_PACKAGE, e.toString())
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
//        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
//            binding.drawerLayout.closeDrawer(GravityCompat.START)
//        } else {
            //super.onBackPressed()
            onBackPressedDispatcher.onBackPressed()
//        }
    }

    fun version() {
        startActivity(Intent(this, VersionActivity::class.java))
    }

    fun setting() {
        startActivity(Intent(this, SettingsSieActivity::class.java))
    }

}
