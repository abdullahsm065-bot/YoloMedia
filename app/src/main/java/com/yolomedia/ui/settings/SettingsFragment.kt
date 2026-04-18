package com.yolomedia.ui.settings

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.yolomedia.R
import com.yolomedia.data.model.BottomBarStyle
import com.yolomedia.data.model.SortOrder
import com.yolomedia.data.model.ThemeMode
import com.yolomedia.data.model.ViewMode
import com.yolomedia.ui.MainActivity
import com.yolomedia.ui.theme.ThemeManager
import com.yolomedia.viewmodel.SettingsViewModel

class SettingsFragment : Fragment() {

    private lateinit var viewModel: SettingsViewModel

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SettingsViewModel::class.java]

        bindViews(view)
        setupListeners()
        observeData()
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
    }

    private fun setupListeners() {
        settingTheme.setOnClickListener { showThemeDialog() }
        settingAccent.setOnClickListener { showAccentColorDialog() }
        settingViewMode.setOnClickListener { showViewModeDialog() }
        settingSort.setOnClickListener { showSortDialog() }
        settingBottomBar.setOnClickListener { showBottomBarDialog() }

        switchAnimations.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAnimationsEnabled(isChecked)
        }
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
                null -> getString(R.string.bar_fixed)
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

                // Refresh activity theme
                (activity as? MainActivity)?.let {
                    it.recreate()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAccentColorDialog() {
        val colors = intArrayOf(
            0xFF2196F3.toInt(), // Blue
            0xFF9C27B0.toInt(), // Purple
            0xFF4CAF50.toInt(), // Green
            0xFFFF9800.toInt(), // Orange
            0xFFF44336.toInt(), // Red
            0xFF009688.toInt(), // Teal
            0xFFE91E63.toInt(), // Pink
            0xFF3F51B5.toInt(), // Indigo
            0xFF00BCD4.toInt(), // Cyan
            0xFFFFEB3B.toInt()  // Yellow
        )

        val colorNames = arrayOf(
            "Blue", "Purple", "Green", "Orange", "Red",
            "Teal", "Pink", "Indigo", "Cyan", "Yellow"
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
        val options = arrayOf(
            getString(R.string.view_grid),
            getString(R.string.view_list)
        )
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
            getString(R.string.sort_name_asc),
            getString(R.string.sort_name_desc),
            getString(R.string.sort_date_desc),
            getString(R.string.sort_date_asc),
            getString(R.string.sort_size_desc),
            getString(R.string.sort_size_asc)
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

    private fun applyTheme() {
        val context = context ?: return
        val root = view as? ScrollView ?: return

        root.setBackgroundColor(ThemeManager.getBackgroundColor(context))

        // Style all TextViews based on theme
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
                    child.currentTextColor == context.getColor(R.color.accent_blue) -> {} // Keep accent
                    else -> child.setTextColor(ThemeManager.getTextSecondaryColor(context))
                }
            }
        }
    }
}
