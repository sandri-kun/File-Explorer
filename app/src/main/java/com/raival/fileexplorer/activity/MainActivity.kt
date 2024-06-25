package com.raival.fileexplorer.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.raival.fileexplorer.App.Companion.showMsg
import com.raival.fileexplorer.R
import com.raival.fileexplorer.activity.adapter.BookmarksAdapter
import com.raival.fileexplorer.activity.model.MainViewModel
import com.raival.fileexplorer.common.dialog.CustomDialog
import com.raival.fileexplorer.common.view.BottomBarView
import com.raival.fileexplorer.common.view.TabView
import com.raival.fileexplorer.common.view.TabView.OnUpdateTabViewListener
import com.raival.fileexplorer.databinding.ActivityMainBinding
import com.raival.fileexplorer.databinding.ActivityMainDrawerBinding
import com.raival.fileexplorer.databinding.StorageDeviceItemBinding
import com.raival.fileexplorer.extension.getAvailableMemoryBytes
import com.raival.fileexplorer.extension.getShortLabel
import com.raival.fileexplorer.extension.getTotalMemoryBytes
import com.raival.fileexplorer.extension.getUsedMemoryBytes
import com.raival.fileexplorer.extension.toDp
import com.raival.fileexplorer.tab.BaseDataHolder
import com.raival.fileexplorer.tab.BaseTabFragment
import com.raival.fileexplorer.tab.apps.AppsTabDataHolder
import com.raival.fileexplorer.tab.apps.AppsTabFragment
import com.raival.fileexplorer.tab.file.FileExplorerTabDataHolder
import com.raival.fileexplorer.tab.file.FileExplorerTabFragment
import com.raival.fileexplorer.tab.file.misc.FileOpener
import com.raival.fileexplorer.tab.file.misc.FileUtils
import com.raival.fileexplorer.util.Log
import com.raival.fileexplorer.util.PrefsUtils
import com.raival.fileexplorer.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.lang.reflect.InvocationTargetException


class MainActivity : BaseActivity<ActivityMainBinding>() {
    private var confirmExit = false

    private lateinit var drawer: ActivityMainDrawerBinding

    lateinit var bottomBarView: BottomBarView
    lateinit var toolbar: MaterialToolbar
    lateinit var tabView: TabView

    override fun getViewBinding() = ActivityMainBinding.inflate(layoutInflater)

    private val mainViewModel: MainViewModel
        get() {
            return ViewModelProvider(this)[MainViewModel::class.java]
        }

    private val tabFragments: List<BaseTabFragment>
        get() {
            val list: MutableList<BaseTabFragment> = ArrayList()
            for (fragment in supportFragmentManager.fragments) {
                if (fragment is BaseTabFragment) {
                    list.add(fragment)
                }
            }
            return list
        }

    private val activeFragment: BaseTabFragment
        get() = supportFragmentManager.findFragmentById(R.id.fragment_container) as BaseTabFragment

    /**
     * Called after read & write permissions are granted
     */
    override fun init() {
        if (tabFragments.isEmpty()) {
            loadDefaultTab()
        } else {
            binding.fragmentContainer.post { restoreTabs() }
        }
    }

    private fun restoreTabs() {
        val activeFragmentTag = activeFragment.tag
        for (i in mainViewModel.getDataHolders().indices) {
            val dataHolder = mainViewModel.getDataHolders()[i]
            // The active fragment will create its own TabView, so we skip it
            if (dataHolder.tag != activeFragmentTag) {
                when (dataHolder) {
                    is FileExplorerTabDataHolder -> {
                        binding.tabs.insertNewTabAt(i, dataHolder.tag, false).setName(
                            dataHolder.activeDirectory!!.getShortLabel(
                                FileExplorerTabFragment.MAX_NAME_LENGTH
                            )
                        )
                    }
                    is AppsTabDataHolder -> {
                        binding.tabs.insertNewTabAt(i, dataHolder.tag, false).setName("Apps")
                    }
                    // handle other types of DataHolders here
                }
            }
        }
    }

    /**
     * The default fragment cannot be deleted, and its tag is unique (starts with "0_")
     */
    private fun loadDefaultTab() {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                FileExplorerTabFragment(),
                BaseTabFragment.DEFAULT_TAB_FRAGMENT_PREFIX + generateRandomTag()
            )
            .setReorderingAllowed(true)
            .commit()
    }

    fun generateRandomTag(): String {
        return Utils.getRandomString(16)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drawer = binding.drawerLayout

        bottomBarView = binding.bottomBarView
        toolbar = binding.toolbar
        tabView = binding.tabs

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_round_menu_24)
        binding.toolbar.setNavigationOnClickListener(null)

        val toggle: ActionBarDrawerToggle = object :
            ActionBarDrawerToggle(
                this,
                binding.drawer,
                binding.toolbar,
                R.string.app_name,
                R.string.app_name
            ) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                refreshBookmarks()
            }
        }

        binding.drawer.addDrawerListener(toggle)
        toggle.syncState()

        binding.tabs.setOnUpdateTabViewListener(object : OnUpdateTabViewListener {
            override fun onUpdate(tab: TabView.Tab?, event: Int) {
                if (tab == null) return
                if (event == TabView.ON_SELECT) {
                    if (tab.tag.startsWith(BaseTabFragment.FILE_EXPLORER_TAB_FRAGMENT_PREFIX)
                        || tab.tag.startsWith(BaseTabFragment.DEFAULT_TAB_FRAGMENT_PREFIX)
                    ) {
                        if (supportFragmentManager.findFragmentById(R.id.fragment_container)?.tag != tab.tag) {
                            supportFragmentManager.beginTransaction()
                                .replace(
                                    R.id.fragment_container,
                                    FileExplorerTabFragment(),
                                    tab.tag
                                )
                                .setReorderingAllowed(true)
                                .commit()
                        }
                    }
                    if (tab.tag.startsWith(BaseTabFragment.APPS_TAB_FRAGMENT_PREFIX)) {
                        if (supportFragmentManager.findFragmentById(R.id.fragment_container)?.tag != tab.tag) {
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, AppsTabFragment(), tab.tag)
                                .setReorderingAllowed(true)
                                .commit()
                        }
                    }
                    // Handle other types of tabs here...
                } else if (event == TabView.ON_LONG_CLICK) {
                    val popupMenu = PopupMenu(this@MainActivity, tab.view)
                    popupMenu.inflate(R.menu.tab_menu)
                    // Default tab is un-closable
                    if (tab.tag.startsWith(BaseTabFragment.DEFAULT_TAB_FRAGMENT_PREFIX)) {
                        popupMenu.menu.findItem(R.id.close).isVisible = false
                        popupMenu.menu.findItem(R.id.close_all).isVisible = false
                    }
                    popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                        when (item.itemId) {
                            R.id.close -> {
                                val activeFragment = activeFragment
                                if (tab.tag == activeFragment.tag) {
                                    activeFragment.closeTab()
                                } else {
                                    mainViewModel.getDataHolders()
                                        .removeIf { dataHolder1: BaseDataHolder -> dataHolder1.tag == tab.tag }
                                    closeTab(tab.tag)
                                }
                                return@setOnMenuItemClickListener true
                            }
                            R.id.close_all -> {
                                val activeFragment = activeFragment
                                // Remove unselected tabs
                                for (tag in binding.tabs.tags) {
                                    if (!tag.startsWith(BaseTabFragment.DEFAULT_TAB_FRAGMENT_PREFIX) && tag != activeFragment.tag) {
                                        mainViewModel.getDataHolders()
                                            .removeIf { dataHolder1: BaseDataHolder -> dataHolder1.tag == tag }
                                        closeTab(tag)
                                    }
                                }
                                // Remove the active tab
                                activeFragment.closeTab()
                                return@setOnMenuItemClickListener true
                            }
                            R.id.close_others -> {
                                val activeFragment = activeFragment
                                for (tag in binding.tabs.tags) {
                                    if (!tag.startsWith(BaseTabFragment.DEFAULT_TAB_FRAGMENT_PREFIX) && tag != activeFragment.tag && tag != tab.tag) {
                                        mainViewModel.getDataHolders()
                                            .removeIf { dataHolder1: BaseDataHolder -> dataHolder1.tag == tag }
                                        closeTab(tag)
                                    }
                                }
                                if (activeFragment.tag != tab.tag) activeFragment.closeTab()
                                return@setOnMenuItemClickListener true
                            }
                            else -> false
                        }
                    }
                    popupMenu.show()
                }
            }
        })

        binding.tabsOptions.setOnClickListener {
            addNewTab()
        }

        checkPermissions()
        setupDrawer()
    }

    override fun onResume() {
        super.onResume()
        val newTheme = PrefsUtils.Settings.themeMode
        if (newTheme != currentTheme) {
            recreate()
        } else {
            binding.bottomBarView.onUpdatePrefs()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshBookmarks() {
        drawer.bookmarksList.adapter?.notifyDataSetChanged()
    }

    fun onBookmarkSelected(file: File) {
        if (file.isDirectory) {
            val fragment = FileExplorerTabFragment(file)
            addNewTab(
                fragment,
                BaseTabFragment.FILE_EXPLORER_TAB_FRAGMENT_PREFIX + generateRandomTag()
            )
        } else {
            FileOpener(this).openFile(file)
        }
        if (binding.drawer.isDrawerOpen(drawer.root)) binding.drawer.close()
    }

    private fun setupDrawer() {
        drawer.apps.setOnClickListener {
            addNewTab(
                AppsTabFragment(),
                BaseTabFragment.APPS_TAB_FRAGMENT_PREFIX + generateRandomTag()
            )
            binding.drawer.close()
        }

        drawer.bookmarksList.adapter = BookmarksAdapter(this)

        drawer.toolbar.apply {
            setTitle(R.string.app_name)
            subtitle = LINK
            drawer.toolbar.menu.apply {
                clear()
                add("GitHub")
                    .setOnMenuItemClickListener { openGithubPage() }
                    .setIcon(R.drawable.ic_baseline_open_in_browser_24)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                add("Settings")
                    .setOnMenuItemClickListener { openSettings() }
                    .setIcon(R.drawable.ic_round_settings_24)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
        }

        addStorageSpace("Root Directory", Environment.getRootDirectory())
        addStorageSpace("Storage Directory", Environment.getExternalStorageDirectory())
        val storageManager = getSystemService(StorageManager::class.java)!!
        storageManager.storageVolumes.forEach { volume ->
            if (volume.isRemovable)
                addStorageSpace(volume.getDescription(this), getVolumePath(volume), true)
        }
    }

    private fun getVolumePath(storageVolume: StorageVolume): File {
        if (VERSION.SDK_INT >= VERSION_CODES.R) return storageVolume.directory!!
        try {
            val storageVolumeClazz: Class<*> = StorageVolume::class.java
            val getPath = storageVolumeClazz.getMethod("getPathFile")
            return getPath.invoke(storageVolume) as File
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        return File("/mnt/" + storageVolume.uuid)
    }

    @SuppressLint("SetTextI18n")
    private fun addStorageSpace(name: String, root: File, isVolume: Boolean = false) {
        if (isVolume && root.exists() && root.canRead().not()) {
            val usbManager = getSystemService(StorageManager::class.java)
            val volume = usbManager.getStorageVolume(root)
            if (volume == null) {
                showMsg("Failed to get volume")
                return
            }

            val intent = if (VERSION.SDK_INT >= VERSION_CODES.Q) {
                volume.createOpenDocumentTreeIntent()
            } else {
                volume.createAccessIntent(null)!!
            }
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    Snackbar.make(
                        binding.root,
                        "Access granted",
                        Snackbar.LENGTH_SHORT
                    ).show()

                    result.data?.data?.let { treeUri ->
                        contentResolver.takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }

                    addStorageSpace(name, root)
                } else {
                    Snackbar.make(
                        binding.root,
                        "Access denied",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }.launch(intent)
            return
        }

        val storageDeviceItemBinding = StorageDeviceItemBinding.inflate(layoutInflater)

        storageDeviceItemBinding.storageSpaceTitle.text = name

        val used = root.getUsedMemoryBytes()
        val total = root.getTotalMemoryBytes()
        val available = root.getAvailableMemoryBytes()
        storageDeviceItemBinding.storageSpaceProgress.progress =
            (used.toDouble() / total.toDouble() * 100).toInt()
        storageDeviceItemBinding.storageSpace.text = (FileUtils.getFormattedSize(used)
                + " used, "
                + FileUtils.getFormattedSize(available)
                + " available")

        storageDeviceItemBinding.root.setOnClickListener {
            addNewTab(
                FileExplorerTabFragment(root),
                BaseTabFragment.FILE_EXPLORER_TAB_FRAGMENT_PREFIX + generateRandomTag()
            )
            binding.drawer.close()
        }

        drawer.storageSpaces.addView(storageDeviceItemBinding.root)
    }

    private fun openSettings(): Boolean {
        startActivity(Intent().setClass(this, SettingsActivity::class.java))
        return true
    }

    private fun openGithubPage(): Boolean {
        startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(LINK)))
        return true
    }

    fun addNewTab(fragment: BaseTabFragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .setReorderingAllowed(true)
            .commit()
    }

    @SuppressLint("SetTextI18n")
    private fun addNewTab() {
        val customDialog = CustomDialog()
        val input = customDialog.createInput(this, "e.g. /sdcard/...")
        input.editText?.setSingleLine()
        val textView = MaterialTextView(this)
        textView.setPadding(0, 8.toDp(), 0, 0)
        textView.alpha = 0.7f
        textView.text = "Quick Links:"
        val layout = ChipGroup(this).apply {
            isScrollContainer = true
        }

        // Chips
        layout.addView(createChip("Internal Storage") {
            addNewTab(
                FileExplorerTabFragment(),
                BaseTabFragment.FILE_EXPLORER_TAB_FRAGMENT_PREFIX + generateRandomTag()
            )
            customDialog.dismiss()
        })

        layout.addView(createChip("Downloads") {
            addNewTab(
                FileExplorerTabFragment(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)),
                BaseTabFragment.FILE_EXPLORER_TAB_FRAGMENT_PREFIX + generateRandomTag()
            )
            customDialog.dismiss()
        })

        layout.addView(createChip("Documents") {
            addNewTab(
                FileExplorerTabFragment(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)),
                BaseTabFragment.FILE_EXPLORER_TAB_FRAGMENT_PREFIX + generateRandomTag()
            )
            customDialog.dismiss()
        })

        layout.addView(createChip("DCIM") {
            addNewTab(
                FileExplorerTabFragment(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)),
                BaseTabFragment.FILE_EXPLORER_TAB_FRAGMENT_PREFIX + generateRandomTag()
            )
            customDialog.dismiss()
        })

        layout.addView(createChip("Music") {
            addNewTab(
                FileExplorerTabFragment(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)),
                BaseTabFragment.FILE_EXPLORER_TAB_FRAGMENT_PREFIX + generateRandomTag()
            )
            customDialog.dismiss()
        })

        customDialog.setTitle("Set tab path")
            .addView(input)
            .addView(textView)
            .addView(HorizontalScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(layout)
            })
            .setPositiveButton("Go", {
                val file = File(
                    input.editText!!.text.toString()
                )
                if (file.exists()) {
                    if (file.canRead()) {
                        if (file.isFile) {
                            FileOpener(this).openFile(file)
                        } else {
                            addNewTab(
                                FileExplorerTabFragment(file),
                                BaseTabFragment.FILE_EXPLORER_TAB_FRAGMENT_PREFIX + generateRandomTag()
                            )
                        }
                    } else {
                        showMsg(Log.UNABLE_TO + " read the provided file")
                    }
                } else {
                    showMsg("The destination path doesn't exist!")
                }
            }, true)
            .show(supportFragmentManager, "")
    }

    private fun createChip(title: String, onClick: () -> Unit): View {
        return Chip(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = title
            setOnClickListener {
                onClick.invoke()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        (menu as MenuBuilder).setOptionalIconsVisible(true)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val title = item.title.toString()
        if (title == "Logs") {
            showLogFile()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showLogFile() {
        val logFile = File(getExternalFilesDir(null)!!.absolutePath + "/debug/log.txt")
        if (logFile.exists() && logFile.isFile) {
            val intent = Intent()
            intent.setClass(this, TextEditorActivity::class.java)
            intent.putExtra("file", logFile.absolutePath)
            startActivity(intent)
            return
        }
        showMsg("No logs found")
    }

    override fun onBackPressed() {
        if (binding.drawer.isDrawerOpen(drawer.root)) {
            binding.drawer.close()
            return
        }
        if (activeFragment.onBackPressed()) {
            return
        }
        if (!confirmExit) {
            confirmExit = true
            showMsg("Press again to exit")
            CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                confirmExit = false
            }
            return
        }
        super.onBackPressed()
    }

    fun closeTab(tag: String) {
        // Remove the tab from TabView. TabView will select another tab which will replace the corresponding fragment.
        // The DataHolder must be removed by the fragment itself, as deletion process differs for each tab.

        // Default fragment (the one added when the app is opened) won't be closed.
        if (tag.startsWith("0_")) return
        binding.tabs.removeTab(tag)
    }

    companion object {
        private const val LINK = "https://github.com/Raival-e/File-Explorer"
    }
}
