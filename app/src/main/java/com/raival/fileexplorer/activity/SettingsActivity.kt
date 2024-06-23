package com.raival.fileexplorer.activity

import android.os.Bundle
import com.google.android.material.slider.Slider
import com.raival.fileexplorer.common.dialog.CustomDialog
import com.raival.fileexplorer.common.dialog.OptionsDialog
import com.raival.fileexplorer.databinding.SettingsActivityBinding
import com.raival.fileexplorer.tab.file.misc.FileUtils
import com.raival.fileexplorer.util.PrefsUtils
import com.raival.fileexplorer.util.PrefsUtils.Settings.showBottomToolbarLabels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsActivity : BaseActivity<SettingsActivityBinding>() {
    override fun getViewBinding() = SettingsActivityBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Settings"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        binding.settingsThemeValue.text = PrefsUtils.Settings.themeMode

        binding.settingsTheme.setOnClickListener {
            OptionsDialog("Select theme mode")
                .addOption(
                    label = THEME_MODE_AUTO,
                    listener = { setThemeMode(THEME_MODE_AUTO) },
                    dismissOnClick = true
                )
                .addOption(
                    label = THEME_MODE_DARK,
                    listener = { setThemeMode(THEME_MODE_DARK) },
                    dismissOnClick = true
                )
                .addOption(
                    label = THEME_MODE_LIGHT,
                    listener = { setThemeMode(THEME_MODE_LIGHT) },
                    dismissOnClick = true
                )
                .show(supportFragmentManager, "")
        }

        binding.settingsBottomToolbarLabelsValue.isChecked = showBottomToolbarLabels
        binding.settingsBottomToolbarLabels.setOnClickListener {
            binding.settingsBottomToolbarLabelsValue.isChecked =
                !binding.settingsBottomToolbarLabelsValue.isChecked
            showBottomToolbarLabels = binding.settingsBottomToolbarLabelsValue.isChecked
        }

        binding.settingsLogModeValue.text = PrefsUtils.Settings.logMode
        binding.settingsLogMode.setOnClickListener {
            OptionsDialog("Select log mode")
                .addOption(LOG_MODE_DISABLE, { setLogMode(LOG_MODE_DISABLE) }, true)
                .addOption(
                    LOG_MODE_ERRORS_ONLY,
                    { setLogMode(LOG_MODE_ERRORS_ONLY) },
                    true
                )
                .addOption(LOG_MODE_ALL, { setLogMode(LOG_MODE_ALL) }, true)
                .show(supportFragmentManager, "")
        }

        binding.settingsDeepSearchLimitValue.text = FileUtils.getFormattedSize(
            PrefsUtils.Settings.deepSearchFileSizeLimit,
            "%.0f"
        )
        binding.settingsDeepSearchLimit.setOnClickListener {
            val seekBar = Slider(this).apply {
                valueFrom = 0f
                valueTo = 80f
                stepSize = 1f
                value = PrefsUtils.Settings.deepSearchFileSizeLimit.toFloat() / 1024 / 1024
            }

            val customDialog = CustomDialog()
            customDialog.setTitle("Max file size limit (MB)")
                .setMsg(
                    "Any file larger than "
                            + FileUtils.getFormattedSize(PrefsUtils.Settings.deepSearchFileSizeLimit)
                            + " will be ignored"
                )
                .addView(seekBar)
                .setPositiveButton("Save", {
                    PrefsUtils.Settings.deepSearchFileSizeLimit =
                        seekBar.value.toLong() * 1024 * 1024
                    binding.settingsDeepSearchLimitValue.text = FileUtils.getFormattedSize(
                        PrefsUtils.Settings.deepSearchFileSizeLimit,
                        "%.0f"
                    )
                }, true)
                .setNegativeButton("Cancel", null, true)
                .show(supportFragmentManager, "")

            seekBar.addOnChangeListener(Slider.OnChangeListener { _: Slider?, value: Float, _: Boolean ->
                customDialog.setMsg(
                    "Any file larger than "
                            + FileUtils.getFormattedSize((value * 1024 * 1024).toLong(), "%.0f")
                            + " will be ignored"
                )
            })
        }
    }

    private fun setLogMode(mode: String) {
        binding.settingsLogModeValue.text = mode
        PrefsUtils.Settings.logMode = mode
    }

    private fun setThemeMode(mode: String) {
        binding.settingsThemeValue.text = mode
        PrefsUtils.Settings.themeMode = mode
        CoroutineScope(Dispatchers.Main).launch {
            delay(150)
            recreate()
        }
    }

    companion object {
        const val LOG_MODE_DISABLE = "Disable"
        const val LOG_MODE_ERRORS_ONLY = "Errors only"
        const val LOG_MODE_ALL = "All logs"
        const val THEME_MODE_AUTO = "Auto"
        const val THEME_MODE_DARK = "Dark"
        const val THEME_MODE_LIGHT = "Light"
    }
}
