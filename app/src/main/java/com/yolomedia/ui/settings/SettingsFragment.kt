package com.yolomedia.ui.settings

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.yolomedia.R
import com.yolomedia.data.model.BottomBarStyle
import com.yolomedia.data.model.SortOrder
import com.yolomedia.data.model.ThemeMode
import com.yolomedia.data.model.ViewMode
import com.yolomedia.data.preferences.AppPreferences
import com.yolomedia.ui.MainActivity
import com.yolomedia.ui.theme.ThemeManager
import com.yolomedia.viewmodel.SettingsViewModel

class SettingsFragment : Fragment() {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var preferences: AppPreferences

    private lateinit var settingTheme: LinearLayout
    private lateinit var tvThemeValue: TextView
    private lateinit var settingAccent: LinearLayout
    private lateinit var accentColorPreview: View
    private lateinit var settingViewMode: LinearLayout
    private lateinit var tvViewModeValue: TextView
    private lateinit var settingSort: LinearLayout
    private lateinit var tvSortValue: TextView
    private lateinit var switchAnimations: SwitchCompat
    private lateinit var settingBottomBar: LinearLayout
    private lateinit var tvBottomBarValue: TextView
    private lateinit var switchAutoPlay: SwitchCompat
    private lateinit var settingPlaybackSpeed: LinearLayout
    private lateinit var tvPlaybackSpeedValue: TextView
    private lateinit var switchConfirmDelete: SwitchCompat
    private lateinit var switchBiometric: SwitchCompat
    private lateinit var settingChangePin: LinearLayout
    private lateinit var switchHideSystemBars: SwitchCompat
    private lateinit var settingThumbnailQuality: LinearLayout
    private lateinit var tvThumbnailQualityValue: TextView
    private lateinit var settingGridColumns: LinearLayout
    private lateinit var tvGridColumnsValue: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SettingsViewModel::class.java]
        preferences = AppPreferences(requireContext())

        bindViews(view)
        setupListeners()
        observeData()
        loadPreferences()
        applyTheme()
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    private fun bindViews(view: View) {
        settingTheme = view.findViewById(R.id.setting_theme)
        tvThemeValue = view.findViewById(R.id.tv_theme_value)
        settingAccent = view.findViewById(R.id.setting_accent)
        accentColorPreview = view.findViewById(R.id.accent_color_preview)
        settingViewMode = view.findViewById(R.id.setting_view_mode)
        tvViewModeValue = view.findViewById(R.id.tv_view_mode_value)
        settingSort = view.findViewById(R.id.setting_sort)
        tvSortValue = view.findViewById(R.id.tv_sort_value)
        switchAnimations = view.findViewById(R.id.switch_animations)
        settingBottomBar = view.findViewById(R.id.setting_bottom_bar)
        tvBottomBarValue = view.findViewById(R.id.tv_bottom_bar_value)
        switchAutoPlay = view.findViewById(R.id.switch_auto_play)
        settingPlaybackSpeed = view.findViewById(R.id.setting_playback_speed)
        tvPlaybackSpeedValue = view.findViewById(R.id.tv_playback_speed_value)
        switchConfirmDelete = view.findViewById(R.id.switch_confirm_delete)
        switchBiometric = view.findViewById(R.id.switch_biometric)
        settingChangePin = view.findViewById(R.id.setting_change_pin)
        switchHideSystemBars = view.findViewById(R.id.switch_hide_system_bars)
        settingThumbnailQuality = view.findViewById(R.id.setting_thumbnail_quality)
        tvThumbnailQualityValue = view.findViewById(R.id.tv_thumbnail_quality_value)
        settingGridColumns = view.findViewById(R.id.setting_grid_columns)
        tvGridColumnsValue = view.findViewById(R.id.tv_grid_columns_value)
    }

    private fun setupListeners() {
        settingTheme.setOnClickListener { showThemeDialog() }
        settingAccent.setOnClickListener { showAccentColorDialog() }
        settingViewMode.setOnClickListener { showViewModeDialog() }
        settingSort.setOnClickListener { showSortDialog() }
        settingBottomBar.setOnClickListener { showBottomBarDialog() }
        settingPlaybackSpeed.setOnClickListener { showPlaybackSpeedDialog() }
        settingChangePin.setOnClickListener { showChangePinDialog() }
        settingThumbnailQuality.setOnClickListener { showThumbnailQualityDialog() }
        settingGridColumns.setOnClickListener { showGridColumnsDialog() }

        switchAnimations.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAnimationsEnabled(isChecked)
        }

        switchAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            preferences.autoPlayVideos = isChecked
        }

        switchConfirmDelete.setOnCheckedChangeListener { _, isChecked ->
            preferences.confirmBeforeDelete = isChecked
        }

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            preferences.biometricEnabled = isChecked
        }

        switchHideSystemBars.setOnCheckedChangeListener { _, isChecked ->
            preferences.hideSystemBars = isChecked
        }
    }

    private fun loadPreferences() {
        switchAutoPlay.isChecked = preferences.autoPlayVideos
        switchConfirmDelete.isChecked = preferences.confirmBeforeDelete
        switchBiometric.isChecked = preferences.biometricEnabled
        switchHideSystemBars.isChecked = preferences.hideSystemBars
        updatePlaybackSpeedText()
        updateThumbnailQualityText()
        updateGridColumnsText()
    }

    private fun observeData() {
        viewModel.themeMode.observe(viewLifecycleOwner) { mode ->
            tvThemeValue.text = when (mode) {
                ThemeMode.LIGHT -> getString(R.string.theme_light)
                ThemeMode.DARK -> getString(R.string.theme_dark)
                ThemeMode.SYSTEM -> getString(R.string.theme_system)
                null -> getString(R.string.theme_light)
            }
        }

        viewModel.accentColor.observe(viewLifecycleOwner) { color ->
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                setStroke(6, ThemeManager.getBackgroundColor(requireContext()))
            }
            accentColorPreview.background = drawable
        }

        viewModel.viewMode.observe(viewLifecycleOwner) { mode ->
            tvViewModeValue.text = when (mode) {
                ViewMode.GRID -> getString(R.string.view_grid)
                ViewMode.LIST -> getString(R.string.view_list)
                null -> getString(R.string.view_grid)
            }
        }

        viewModel.sortOrder.observe(viewLifecycleOwner) { order ->
            tvSortValue.text = when (order) {
                SortOrder.NAME_ASC -> getString(R.string.sort_name_asc)
                SortOrder.NAME_DESC -> getString(R.string.sort_name_desc)
                SortOrder.DATE_ASC -> getString(R.string.sort_date_asc)
                SortOrder.DATE_DESC -> getString(R.string.sort_date_desc)
                SortOrder.SIZE_ASC -> getString(R.string.sort_size_asc)
                SortOrder.SIZE_DESC -> getString(R.string.sort_size_desc)
                null -> getString(R.string.sort_date_desc)
            }
        }

        viewModel.animationsEnabled.observe(viewLifecycleOwner) { enabled ->
            switchAnimations.isChecked = enabled
        }

        viewModel.bottomBarStyle.observe(viewLifecycleOwner) { style ->
            tvBottomBarValue.text = when (style) {
                BottomBarStyle.FIXED -> getString(R.string.bar_fixed)
                BottomBarStyle.FLOATING -> getString(R.string.bar_floating)
                BottomBarStyle.COMPACT -> getString(R.string.bar_compact)
                null -> getString(R.string.bar_floating)
            }
        }
    }

    private fun showThemeDialog() {
        val options = arrayOf(
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_system)
        )
        val current = when (viewModel.themeMode.value) {
            ThemeMode.LIGHT -> 0
            ThemeMode.DARK -> 1
            ThemeMode.SYSTEM -> 2
            else -> 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_theme)
            .setSingleChoiceItems(options, current) { dialog, which ->
                val mode = when (which) {
                    0 -> ThemeMode.LIGHT
                    1 -> ThemeMode.DARK
                    2 -> ThemeMode.SYSTEM
                    else -> ThemeMode.LIGHT
                }
                viewModel.setThemeMode(mode)
                ThemeManager.applyTheme(mode)
                dialog.dismiss()
                (activity as? MainActivity)?.recreate()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAccentColorDialog() {
        val colors = intArrayOf(
            0xFF3B82F6.toInt(),
            0xFF8B5CF6.toInt(),
            0xFF10B981.toInt(),
            0xFFF59E0B.toInt(),
            0xFFEF4444.toInt(),
            0xFF14B8A6.toInt(),
            0xFFEC4899.toInt(),
            0xFF6366F1.toInt(),
            0xFF06B6D4.toInt(),
            0xFFF97316.toInt()
        )

        val colorNames = arrayOf(
            "Blue", "Purple", "Emerald", "Amber", "Red",
            "Teal", "Pink", "Indigo", "Cyan", "Orange"
        )

        val currentColor = viewModel.accentColor.value ?: colors[0]
        val currentIndex = colors.indexOf(currentColor).coerceAtLeast(0)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_accent_color)
            .setSingleChoiceItems(colorNames, currentIndex) { dialog, which ->
                viewModel.setAccentColor(colors[which])
                dialog.dismiss()
                (activity as? MainActivity)?.applyThemeColors()
                applyTheme()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showViewModeDialog() {
        val options = arrayOf(getString(R.string.view_grid), getString(R.string.view_list))
        val current = if (viewModel.viewMode.value == ViewMode.GRID) 0 else 1

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_view_mode)
            .setSingleChoiceItems(options, current) { dialog, which ->
                viewModel.setViewMode(if (which == 0) ViewMode.GRID else ViewMode.LIST)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSortDialog() {
        val options = arrayOf(
            getString(R.string.sort_name_asc), getString(R.string.sort_name_desc),
            getString(R.string.sort_date_desc), getString(R.string.sort_date_asc),
            getString(R.string.sort_size_desc), getString(R.string.sort_size_asc)
        )
        val orders = arrayOf(
            SortOrder.NAME_ASC, SortOrder.NAME_DESC,
            SortOrder.DATE_DESC, SortOrder.DATE_ASC,
            SortOrder.SIZE_DESC, SortOrder.SIZE_ASC
        )
        val current = orders.indexOf(viewModel.sortOrder.value).coerceAtLeast(0)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_sort_order)
            .setSingleChoiceItems(options, current) { dialog, which ->
                viewModel.setSortOrder(orders[which])
                dialog.dismiss()
                (activity as? MainActivity)?.refreshCurrentFragment()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showBottomBarDialog() {
        val options = arrayOf(
            getString(R.string.bar_fixed),
            getString(R.string.bar_floating),
            getString(R.string.bar_compact)
        )
        val styles = arrayOf(BottomBarStyle.FIXED, BottomBarStyle.FLOATING, BottomBarStyle.COMPACT)
        val current = styles.indexOf(viewModel.bottomBarStyle.value).coerceAtLeast(0)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_bottom_bar_style)
            .setSingleChoiceItems(options, current) { dialog, which ->
                viewModel.setBottomBarStyle(styles[which])
                dialog.dismiss()
                (activity as? MainActivity)?.applyBottomBarStyle()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPlaybackSpeedDialog() {
        val options = arrayOf(
            getString(R.string.speed_0_5x), getString(R.string.speed_0_75x),
            getString(R.string.speed_1x), getString(R.string.speed_1_25x),
            getString(R.string.speed_1_5x), getString(R.string.speed_2x)
        )
        val speeds = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val currentSpeed = preferences.defaultPlaybackSpeed
        val current = speeds.indexOfFirst { it == currentSpeed }.coerceAtLeast(2)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_default_speed)
            .setSingleChoiceItems(options, current) { dialog, which ->
                preferences.defaultPlaybackSpeed = speeds[which]
                updatePlaybackSpeedText()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showChangePinDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Enter new 4-digit PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(64, 32, 64, 16)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_change_pin)
            .setView(input)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val newPin = input.text.toString().trim()
                if (newPin.length == 4) {
                    preferences.safePin = newPin
                    Toast.makeText(requireContext(), "PIN updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showThumbnailQualityDialog() {
        val options = arrayOf(
            getString(R.string.quality_low),
            getString(R.string.quality_medium),
            getString(R.string.quality_high)
        )
        val current = preferences.thumbnailQuality

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_thumbnail_quality)
            .setSingleChoiceItems(options, current) { dialog, which ->
                preferences.thumbnailQuality = which
                updateThumbnailQualityText()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showGridColumnsDialog() {
        val options = arrayOf(
            getString(R.string.columns_2),
            getString(R.string.columns_3),
            getString(R.string.columns_4),
            getString(R.string.columns_5)
        )
        val columns = intArrayOf(2, 3, 4, 5)
        val currentCol = preferences.gridColumnCount
        val current = columns.indexOfFirst { it == currentCol }.coerceAtLeast(1)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_grid_columns)
            .setSingleChoiceItems(options, current) { dialog, which ->
                preferences.gridColumnCount = columns[which]
                updateGridColumnsText()
                dialog.dismiss()
                (activity as? MainActivity)?.refreshCurrentFragment()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updatePlaybackSpeedText() {
        val speed = preferences.defaultPlaybackSpeed
        tvPlaybackSpeedValue.text = when (speed) {
            0.5f -> getString(R.string.speed_0_5x)
            0.75f -> getString(R.string.speed_0_75x)
            1.25f -> getString(R.string.speed_1_25x)
            1.5f -> getString(R.string.speed_1_5x)
            2.0f -> getString(R.string.speed_2x)
            else -> getString(R.string.speed_1x)
        }
    }

    private fun updateThumbnailQualityText() {
        tvThumbnailQualityValue.text = when (preferences.thumbnailQuality) {
            0 -> getString(R.string.quality_low)
            2 -> getString(R.string.quality_high)
            else -> getString(R.string.quality_medium)
        }
    }

    private fun updateGridColumnsText() {
        tvGridColumnsValue.text = "${preferences.gridColumnCount} Columns"
    }

    private fun applyTheme() {
        val context = context ?: return
        val root = view as? ScrollView ?: return

        root.setBackgroundColor(ThemeManager.getBackgroundColor(context))
        applyThemeToViewGroup(root)
    }

    private fun applyThemeToViewGroup(viewGroup: ViewGroup) {
        val context = viewGroup.context
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) {
                applyThemeToViewGroup(child)
            } else if (child is TextView) {
                @Suppress("DEPRECATION")
                val size = child.textSize / context.resources.displayMetrics.scaledDensity
                when {
                    size >= 26f -> child.setTextColor(ThemeManager.getTextPrimaryColor(context))
                    size >= 15f -> child.setTextColor(ThemeManager.getTextPrimaryColor(context))
                    child.currentTextColor == context.getColor(R.color.accent_blue) -> {}
                    else -> child.setTextColor(ThemeManager.getTextSecondaryColor(context))
                }
            }
        }
    }
}
