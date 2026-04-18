package com.yolomedia.ui.settings

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
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
import com.yolomedia.ui.common.ModernDialog
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
    private lateinit var switchNewBadge: SwitchCompat
    private lateinit var switchResumePlayback: SwitchCompat
    private lateinit var switchLoopVideos: SwitchCompat
    private lateinit var switchVideoGestures: SwitchCompat
    private lateinit var settingDoubleTapSeek: LinearLayout
    private lateinit var tvDoubleTapSeekValue: TextView
    private lateinit var settingClearNewBadges: LinearLayout
    private lateinit var settingSafeAutoLock: LinearLayout
    private lateinit var tvSafeAutoLockValue: TextView
    private lateinit var switchSafeThumbnails: SwitchCompat
    private lateinit var switchSafeHideRecents: SwitchCompat
    private lateinit var switchAutoRotateVideo: SwitchCompat
    private lateinit var switchShowFileSize: SwitchCompat
    private lateinit var switchShowDuration: SwitchCompat
    private lateinit var switchReelsAutoPlay: SwitchCompat
    private lateinit var switchReelsLoop: SwitchCompat
    private lateinit var settingClearReelTags: LinearLayout
    private lateinit var switchCropThumbnails: SwitchCompat
    private lateinit var switchRememberFolder: SwitchCompat
    private lateinit var switchHapticFeedback: SwitchCompat
    private lateinit var switchSoundEffects: SwitchCompat
    private lateinit var settingClearRecent: LinearLayout

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
        switchNewBadge = view.findViewById(R.id.switch_new_badge)
        switchResumePlayback = view.findViewById(R.id.switch_resume_playback)
        switchLoopVideos = view.findViewById(R.id.switch_loop_videos)
        switchVideoGestures = view.findViewById(R.id.switch_video_gestures)
        settingDoubleTapSeek = view.findViewById(R.id.setting_double_tap_seek)
        tvDoubleTapSeekValue = view.findViewById(R.id.tv_double_tap_seek_value)
        settingClearNewBadges = view.findViewById(R.id.setting_clear_new_badges)
        settingSafeAutoLock = view.findViewById(R.id.setting_safe_auto_lock)
        tvSafeAutoLockValue = view.findViewById(R.id.tv_safe_auto_lock_value)
        switchSafeThumbnails = view.findViewById(R.id.switch_safe_thumbnails)
        switchSafeHideRecents = view.findViewById(R.id.switch_safe_hide_recents)
        switchAutoRotateVideo = view.findViewById(R.id.switch_auto_rotate_video)
        switchShowFileSize = view.findViewById(R.id.switch_show_file_size)
        switchShowDuration = view.findViewById(R.id.switch_show_duration)
        switchReelsAutoPlay = view.findViewById(R.id.switch_reels_auto_play)
        switchReelsLoop = view.findViewById(R.id.switch_reels_loop)
        settingClearReelTags = view.findViewById(R.id.setting_clear_reel_tags)
        switchCropThumbnails = view.findViewById(R.id.switch_crop_thumbnails)
        switchRememberFolder = view.findViewById(R.id.switch_remember_folder)
        switchHapticFeedback = view.findViewById(R.id.switch_haptic_feedback)
        switchSoundEffects = view.findViewById(R.id.switch_sound_effects)
        settingClearRecent = view.findViewById(R.id.setting_clear_recent)
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

        switchNewBadge.setOnCheckedChangeListener { _, isChecked ->
            preferences.showNewBadge = isChecked
        }

        switchResumePlayback.setOnCheckedChangeListener { _, isChecked ->
            preferences.resumePlayback = isChecked
        }

        switchLoopVideos.setOnCheckedChangeListener { _, isChecked ->
            preferences.loopVideos = isChecked
        }

        switchVideoGestures.setOnCheckedChangeListener { _, isChecked ->
            preferences.videoGesturesEnabled = isChecked
        }

        settingDoubleTapSeek.setOnClickListener { showDoubleTapSeekDialog() }

        settingClearNewBadges.setOnClickListener {
            ModernDialog.confirm(
                context = requireContext(),
                title = "Clear New Badges",
                message = "Mark all videos as played? This will remove all NEW badges.",
                positiveText = "Clear",
                negativeText = "Cancel",
                onPositive = {
                    preferences.clearPlayedVideos()
                    Toast.makeText(requireContext(), "All badges cleared", Toast.LENGTH_SHORT).show()
                }
            )
        }

        settingSafeAutoLock.setOnClickListener { showSafeAutoLockDialog() }

        switchSafeThumbnails.setOnCheckedChangeListener { _, isChecked ->
            preferences.safeShowThumbnails = isChecked
        }

        switchSafeHideRecents.setOnCheckedChangeListener { _, isChecked ->
            preferences.safeHideFromRecents = isChecked
        }

        switchAutoRotateVideo.setOnCheckedChangeListener { _, isChecked ->
            preferences.autoRotateVideo = isChecked
        }

        switchShowFileSize.setOnCheckedChangeListener { _, isChecked ->
            preferences.showFileSize = isChecked
        }

        switchShowDuration.setOnCheckedChangeListener { _, isChecked ->
            preferences.showDurationBadge = isChecked
        }

        switchReelsAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            preferences.reelsAutoPlay = isChecked
        }

        switchReelsLoop.setOnCheckedChangeListener { _, isChecked ->
            preferences.reelsLoop = isChecked
        }

        settingClearReelTags.setOnClickListener {
            ModernDialog.confirm(
                context = requireContext(),
                title = "Clear Reel Tags",
                message = "Remove reel designation from all folders? They will open normally again.",
                positiveText = "Clear",
                negativeText = "Cancel",
                onPositive = {
                    preferences.clearReelFolders()
                    Toast.makeText(requireContext(), "All reel tags cleared", Toast.LENGTH_SHORT).show()
                }
            )
        }

        switchCropThumbnails.setOnCheckedChangeListener { _, isChecked ->
            preferences.cropThumbnails = isChecked
        }

        switchRememberFolder.setOnCheckedChangeListener { _, isChecked ->
            preferences.rememberLastFolder = isChecked
        }

        switchHapticFeedback.setOnCheckedChangeListener { _, isChecked ->
            preferences.hapticFeedbackEnabled = isChecked
        }

        switchSoundEffects.setOnCheckedChangeListener { _, isChecked ->
            preferences.soundEffectsEnabled = isChecked
        }

        settingClearRecent.setOnClickListener {
            ModernDialog.confirm(
                context = requireContext(),
                title = "Clear Recently Viewed",
                message = "Remove all recently viewed history?",
                positiveText = "Clear",
                negativeText = "Cancel",
                onPositive = {
                    preferences.clearRecentlyViewed()
                    Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun loadPreferences() {
        switchAutoPlay.isChecked = preferences.autoPlayVideos
        switchConfirmDelete.isChecked = preferences.confirmBeforeDelete
        switchBiometric.isChecked = preferences.biometricEnabled
        switchHideSystemBars.isChecked = preferences.hideSystemBars
        switchNewBadge.isChecked = preferences.showNewBadge
        switchResumePlayback.isChecked = preferences.resumePlayback
        switchLoopVideos.isChecked = preferences.loopVideos
        switchVideoGestures.isChecked = preferences.videoGesturesEnabled
        switchSafeThumbnails.isChecked = preferences.safeShowThumbnails
        switchSafeHideRecents.isChecked = preferences.safeHideFromRecents
        switchAutoRotateVideo.isChecked = preferences.autoRotateVideo
        switchShowFileSize.isChecked = preferences.showFileSize
        switchShowDuration.isChecked = preferences.showDurationBadge
        switchReelsAutoPlay.isChecked = preferences.reelsAutoPlay
        switchReelsLoop.isChecked = preferences.reelsLoop
        switchCropThumbnails.isChecked = preferences.cropThumbnails
        switchRememberFolder.isChecked = preferences.rememberLastFolder
        switchHapticFeedback.isChecked = preferences.hapticFeedbackEnabled
        switchSoundEffects.isChecked = preferences.soundEffectsEnabled
        updatePlaybackSpeedText()
        updateThumbnailQualityText()
        updateGridColumnsText()
        updateDoubleTapSeekText()
        updateSafeAutoLockText()
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

        ModernDialog.singleChoice(
            context = requireContext(),
            title = getString(R.string.settings_theme),
            options = options,
            selectedIndex = current,
            onSelect = { which ->
                val mode = when (which) {
                    0 -> ThemeMode.LIGHT
                    1 -> ThemeMode.DARK
                    2 -> ThemeMode.SYSTEM
                    else -> ThemeMode.LIGHT
                }
                viewModel.setThemeMode(mode)
                ThemeManager.applyTheme(mode)
                (activity as? MainActivity)?.recreate()
            }
        )
    }

    private fun showAccentColorDialog() {
        val colors = intArrayOf(
            0xFF2F80ED.toInt(),
            0xFF8B5CF6.toInt(),
            0xFF22C55E.toInt(),
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

        ModernDialog.singleChoice(
            context = requireContext(),
            title = getString(R.string.settings_accent_color),
            options = colorNames,
            selectedIndex = currentIndex,
            onSelect = { which ->
                viewModel.setAccentColor(colors[which])
                (activity as? MainActivity)?.applyThemeColors()
                applyTheme()
            }
        )
    }

    private fun showViewModeDialog() {
        val options = arrayOf(getString(R.string.view_grid), getString(R.string.view_list))
        val current = if (viewModel.viewMode.value == ViewMode.GRID) 0 else 1

        ModernDialog.singleChoice(
            context = requireContext(),
            title = getString(R.string.settings_view_mode),
            options = options,
            selectedIndex = current,
            onSelect = { which ->
                viewModel.setViewMode(if (which == 0) ViewMode.GRID else ViewMode.LIST)
            }
        )
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

        ModernDialog.singleChoice(
            context = requireContext(),
            title = getString(R.string.settings_sort_order),
            options = options,
            selectedIndex = current,
            onSelect = { which ->
                viewModel.setSortOrder(orders[which])
                (activity as? MainActivity)?.refreshCurrentFragment()
            }
        )
    }

    private fun showBottomBarDialog() {
        val options = arrayOf(
            getString(R.string.bar_fixed),
            getString(R.string.bar_floating),
            getString(R.string.bar_compact)
        )
        val styles = arrayOf(BottomBarStyle.FIXED, BottomBarStyle.FLOATING, BottomBarStyle.COMPACT)
        val current = styles.indexOf(viewModel.bottomBarStyle.value).coerceAtLeast(0)

        ModernDialog.singleChoice(
            context = requireContext(),
            title = getString(R.string.settings_bottom_bar_style),
            options = options,
            selectedIndex = current,
            onSelect = { which ->
                viewModel.setBottomBarStyle(styles[which])
                (activity as? MainActivity)?.applyBottomBarStyle()
            }
        )
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

        ModernDialog.singleChoice(
            context = requireContext(),
            title = getString(R.string.settings_default_speed),
            options = options,
            selectedIndex = current,
            onSelect = { which ->
                preferences.defaultPlaybackSpeed = speeds[which]
                updatePlaybackSpeedText()
            }
        )
    }

    private fun showChangePinDialog() {
        ModernDialog.input(
            context = requireContext(),
            title = getString(R.string.settings_change_pin),
            hint = "Enter new 4-digit PIN",
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD,
            positiveText = getString(R.string.confirm),
            onConfirm = { newPin ->
                if (newPin.length == 4) {
                    preferences.safePin = newPin
                    Toast.makeText(requireContext(), "PIN updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showThumbnailQualityDialog() {
        val options = arrayOf(
            getString(R.string.quality_low),
            getString(R.string.quality_medium),
            getString(R.string.quality_high)
        )
        val current = preferences.thumbnailQuality

        ModernDialog.singleChoice(
            context = requireContext(),
            title = getString(R.string.settings_thumbnail_quality),
            options = options,
            selectedIndex = current,
            onSelect = { which ->
                preferences.thumbnailQuality = which
                updateThumbnailQualityText()
            }
        )
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

        ModernDialog.singleChoice(
            context = requireContext(),
            title = getString(R.string.settings_grid_columns),
            options = options,
            selectedIndex = current,
            onSelect = { which ->
                preferences.gridColumnCount = columns[which]
                updateGridColumnsText()
                (activity as? MainActivity)?.refreshCurrentFragment()
            }
        )
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

    private fun updateDoubleTapSeekText() {
        tvDoubleTapSeekValue.text = "${preferences.doubleTapSeekDuration} seconds"
    }

    private fun updateSafeAutoLockText() {
        tvSafeAutoLockValue.text = when (preferences.safeAutoLockDelay) {
            0 -> "Immediately"
            30 -> "30 seconds"
            60 -> "1 minute"
            300 -> "5 minutes"
            else -> "Immediately"
        }
    }

    private fun showDoubleTapSeekDialog() {
        val options = arrayOf("5 seconds", "10 seconds", "15 seconds", "30 seconds")
        val values = intArrayOf(5, 10, 15, 30)
        val current = values.indexOfFirst { it == preferences.doubleTapSeekDuration }.coerceAtLeast(1)

        ModernDialog.singleChoice(
            context = requireContext(),
            title = "Double Tap Seek Duration",
            options = options,
            selectedIndex = current,
            onSelect = { which ->
                preferences.doubleTapSeekDuration = values[which]
                updateDoubleTapSeekText()
            }
        )
    }

    private fun showSafeAutoLockDialog() {
        val options = arrayOf("Immediately", "30 seconds", "1 minute", "5 minutes")
        val values = intArrayOf(0, 30, 60, 300)
        val current = values.indexOfFirst { it == preferences.safeAutoLockDelay }.coerceAtLeast(0)

        ModernDialog.singleChoice(
            context = requireContext(),
            title = "Auto Lock Delay",
            options = options,
            selectedIndex = current,
            onSelect = { which ->
                preferences.safeAutoLockDelay = values[which]
                updateSafeAutoLockText()
            }
        )
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
