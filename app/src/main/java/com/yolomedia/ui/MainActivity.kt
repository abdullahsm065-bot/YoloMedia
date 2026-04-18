package com.yolomedia.ui

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.yolomedia.R
import com.yolomedia.data.model.BottomBarStyle
import com.yolomedia.data.preferences.AppPreferences
import com.yolomedia.ui.common.ModernDialog
import com.yolomedia.ui.gallery.GalleryFragment
import com.yolomedia.ui.safe.SafeFragment
import com.yolomedia.ui.settings.SettingsFragment
import com.yolomedia.ui.theme.ThemeManager
import com.yolomedia.ui.videos.VideosFragment
import com.yolomedia.utils.PermissionUtils

class MainActivity : AppCompatActivity() {

    private lateinit var fragmentContainer: FrameLayout
    private lateinit var bottomNavContainer: FrameLayout
    private lateinit var customBottomNav: LinearLayout
    private lateinit var mainContainer: View
    private lateinit var preferences: AppPreferences

    private lateinit var tabVideos: LinearLayout
    private lateinit var tabGallery: LinearLayout
    private lateinit var tabSafe: LinearLayout
    private lateinit var tabSettings: LinearLayout

    private lateinit var tabVideosIcon: ImageView
    private lateinit var tabGalleryIcon: ImageView
    private lateinit var tabSafeIcon: ImageView
    private lateinit var tabSettingsIcon: ImageView

    private lateinit var tabVideosLabel: TextView
    private lateinit var tabGalleryLabel: TextView
    private lateinit var tabSafeLabel: TextView
    private lateinit var tabSettingsLabel: TextView

    private var currentFragmentTag: String = TAG_VIDEOS
    private var currentTabIndex = 0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            loadCurrentFragment()
        } else {
            showPermissionDialog()
        }
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (PermissionUtils.hasStoragePermissions(this)) {
            loadCurrentFragment()
        } else {
            showPermissionDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = AppPreferences(this)
        ThemeManager.applyTheme(preferences.themeMode)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainContainer = findViewById(R.id.main_container)
        fragmentContainer = findViewById(R.id.fragment_container)
        bottomNavContainer = findViewById(R.id.bottom_nav_container)
        customBottomNav = findViewById(R.id.custom_bottom_nav)

        tabVideos = findViewById(R.id.tab_videos)
        tabGallery = findViewById(R.id.tab_gallery)
        tabSafe = findViewById(R.id.tab_safe)
        tabSettings = findViewById(R.id.tab_settings)

        tabVideosIcon = findViewById(R.id.tab_videos_icon)
        tabGalleryIcon = findViewById(R.id.tab_gallery_icon)
        tabSafeIcon = findViewById(R.id.tab_safe_icon)
        tabSettingsIcon = findViewById(R.id.tab_settings_icon)

        tabVideosLabel = findViewById(R.id.tab_videos_label)
        tabGalleryLabel = findViewById(R.id.tab_gallery_label)
        tabSafeLabel = findViewById(R.id.tab_safe_label)
        tabSettingsLabel = findViewById(R.id.tab_settings_label)

        setupBottomNav()
        setupBackPress()
        applyThemeColors()
        applyBottomBarStyle()

        if (savedInstanceState != null) {
            currentFragmentTag = savedInstanceState.getString("current_tag", TAG_VIDEOS)
            currentTabIndex = when (currentFragmentTag) {
                TAG_VIDEOS -> 0
                TAG_GALLERY -> 1
                TAG_SAFE -> 2
                TAG_SETTINGS -> 3
                else -> 0
            }
        }

        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        applyThemeColors()
        applyBottomBarStyle()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("current_tag", currentFragmentTag)
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentByTag(currentFragmentTag)

                when (currentFragment) {
                    is VideosFragment -> {
                        if (currentFragment.handleBackPress()) return
                    }
                    is SafeFragment -> {
                        if (currentFragment.handleBackPress()) return
                    }
                }

                if (currentFragmentTag != TAG_VIDEOS) {
                    selectTab(0)
                    return
                }

                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun setupBottomNav() {
        tabVideos.setOnClickListener { selectTab(0) }
        tabGallery.setOnClickListener { selectTab(1) }
        tabSafe.setOnClickListener { selectTab(2) }
        tabSettings.setOnClickListener { selectTab(3) }
    }

    private fun selectTab(index: Int) {
        if (index == currentTabIndex && supportFragmentManager.findFragmentByTag(getTagForIndex(index)) != null) {
            return
        }

        performHapticFeedback()

        currentTabIndex = index
        val tag = getTagForIndex(index)
        showFragment(tag)
        animateTabSelection(index)
    }

    private fun getTagForIndex(index: Int): String {
        return when (index) {
            0 -> TAG_VIDEOS
            1 -> TAG_GALLERY
            2 -> TAG_SAFE
            3 -> TAG_SETTINGS
            else -> TAG_VIDEOS
        }
    }

    private fun animateTabSelection(selectedIndex: Int) {
        val tabs = listOf(tabVideos, tabGallery, tabSafe, tabSettings)
        val icons = listOf(tabVideosIcon, tabGalleryIcon, tabSafeIcon, tabSettingsIcon)
        val labels = listOf(tabVideosLabel, tabGalleryLabel, tabSafeLabel, tabSettingsLabel)

        val accentColor = ThemeManager.getAccentColor(this)
        val inactiveColor = ThemeManager.getIconTintColor(this)

        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            val icon = icons[index]
            val label = labels[index]

            if (preferences.animationsEnabled) {
                val scaleX = ObjectAnimator.ofFloat(icon, "scaleX", icon.scaleX, if (isSelected) 1.15f else 1.0f)
                val scaleY = ObjectAnimator.ofFloat(icon, "scaleY", icon.scaleY, if (isSelected) 1.15f else 1.0f)
                val alphaAnim = ObjectAnimator.ofFloat(label, "alpha", label.alpha, if (isSelected) 1.0f else 0.6f)

                AnimatorSet().apply {
                    playTogether(scaleX, scaleY, alphaAnim)
                    duration = 250
                    interpolator = OvershootInterpolator(2f)
                    start()
                }

                if (isSelected) {
                    val bounce = ObjectAnimator.ofFloat(tab, "translationY", 0f, -4f, 0f)
                    bounce.duration = 300
                    bounce.interpolator = OvershootInterpolator(3f)
                    bounce.start()
                }
            } else {
                icon.scaleX = if (isSelected) 1.15f else 1.0f
                icon.scaleY = if (isSelected) 1.15f else 1.0f
                label.alpha = if (isSelected) 1.0f else 0.6f
            }

            icon.setColorFilter(if (isSelected) accentColor else inactiveColor)
            label.setTextColor(if (isSelected) accentColor else inactiveColor)
            label.typeface = android.graphics.Typeface.create(
                "sans-serif",
                if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
            )
        }
    }

    private fun performHapticFeedback() {
        if (!preferences.hapticFeedbackEnabled) return
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadCurrentFragment()
            } else {
                showPermissionDialog()
            }
        } else {
            val permissions = PermissionUtils.getRequiredPermissions()
            val notGranted = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (notGranted.isEmpty()) {
                loadCurrentFragment()
            } else {
                permissionLauncher.launch(notGranted.toTypedArray())
            }
        }
    }

    private fun showPermissionDialog() {
        ModernDialog.confirm(
            context = this,
            title = getString(R.string.permission_required),
            message = getString(R.string.permission_desc),
            positiveText = getString(R.string.grant_permission),
            negativeText = getString(R.string.cancel),
            onPositive = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    manageStorageLauncher.launch(intent)
                } else {
                    permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
                }
            },
            onNegative = { loadCurrentFragment() }
        )
    }

    private fun loadCurrentFragment() {
        showFragment(currentFragmentTag)
        animateTabSelection(currentTabIndex)
    }

    private fun showFragment(tag: String) {
        currentFragmentTag = tag
        val fm = supportFragmentManager
        val existing = fm.findFragmentByTag(tag)

        val transaction = fm.beginTransaction()

        if (preferences.animationsEnabled) {
            transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        }

        fm.fragments.forEach { transaction.hide(it) }

        if (existing != null) {
            transaction.show(existing)
        } else {
            val fragment = createFragment(tag)
            transaction.add(R.id.fragment_container, fragment, tag)
        }

        transaction.commit()
    }

    private fun createFragment(tag: String): Fragment {
        return when (tag) {
            TAG_VIDEOS -> VideosFragment()
            TAG_GALLERY -> GalleryFragment()
            TAG_SAFE -> SafeFragment()
            TAG_SETTINGS -> SettingsFragment()
            else -> VideosFragment()
        }
    }

    fun applyThemeColors() {
        val bgColor = ThemeManager.getBackgroundColor(this)
        val isDark = ThemeManager.isDarkMode(this)

        mainContainer.setBackgroundColor(bgColor)

        val style = preferences.bottomBarStyle
        val density = resources.displayMetrics.density

        when (style) {
            BottomBarStyle.FLOATING -> {
                val glassDrawable = GradientDrawable().apply {
                    setColor(if (isDark) 0xB30F1729.toInt() else 0xD9FFFFFF.toInt())
                    cornerRadius = 28f * density
                    setStroke(
                        (1 * density).toInt(),
                        if (isDark) 0x1AFFFFFF else 0x18000000
                    )
                }
                customBottomNav.background = glassDrawable
                customBottomNav.elevation = 16f * density
            }
            BottomBarStyle.COMPACT, BottomBarStyle.FIXED -> {
                val navBgColor = if (isDark) 0xCC0F1729.toInt() else 0xE6FFFFFF.toInt()
                customBottomNav.setBackgroundColor(navBgColor)
                customBottomNav.elevation = if (style == BottomBarStyle.FIXED) 8f * density else 4f * density
            }
        }

        animateTabSelection(currentTabIndex)
        ThemeManager.applyThemeToActivity(this)
    }

    fun applyBottomBarStyle() {
        val style = preferences.bottomBarStyle
        val params = bottomNavContainer.layoutParams as ViewGroup.MarginLayoutParams
        val density = resources.displayMetrics.density
        val isDark = ThemeManager.isDarkMode(this)

        val navHeight = when (style) {
            BottomBarStyle.COMPACT -> 56
            else -> 72
        }

        val navParams = customBottomNav.layoutParams
        navParams.height = (navHeight * density).toInt()
        customBottomNav.layoutParams = navParams

        val fragmentParams = fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams

        when (style) {
            BottomBarStyle.FIXED -> {
                params.setMargins(0, 0, 0, 0)
                fragmentParams.bottomMargin = (navHeight * density).toInt()
                val navBgColor = if (isDark) 0xCC0F1729.toInt() else 0xE6FFFFFF.toInt()
                customBottomNav.setBackgroundColor(navBgColor)
                customBottomNav.elevation = 8f * density
            }
            BottomBarStyle.FLOATING -> {
                val hMargin = (16 * density).toInt()
                val bMargin = (16 * density).toInt()
                params.setMargins(hMargin, 0, hMargin, bMargin)
                fragmentParams.bottomMargin = ((navHeight + 32) * density).toInt()

                val glassDrawable = GradientDrawable().apply {
                    setColor(if (isDark) 0xB30F1729.toInt() else 0xD9FFFFFF.toInt())
                    cornerRadius = 28f * density
                    setStroke(
                        (1 * density).toInt(),
                        if (isDark) 0x1AFFFFFF else 0x18000000
                    )
                }
                customBottomNav.background = glassDrawable
                customBottomNav.elevation = 16f * density
            }
            BottomBarStyle.COMPACT -> {
                params.setMargins(0, 0, 0, 0)
                fragmentParams.bottomMargin = (navHeight * density).toInt()
                val navBgColor = if (isDark) 0xCC0F1729.toInt() else 0xE6FFFFFF.toInt()
                customBottomNav.setBackgroundColor(navBgColor)
                customBottomNav.elevation = 4f * density

                listOf(tabVideosLabel, tabGalleryLabel, tabSafeLabel, tabSettingsLabel).forEachIndexed { i, label ->
                    label.visibility = if (i == currentTabIndex) View.VISIBLE else View.GONE
                }
            }
        }

        if (style != BottomBarStyle.COMPACT) {
            listOf(tabVideosLabel, tabGalleryLabel, tabSafeLabel, tabSettingsLabel).forEach {
                it.visibility = View.VISIBLE
            }
        }

        bottomNavContainer.layoutParams = params
        fragmentContainer.layoutParams = fragmentParams
    }

    fun refreshCurrentFragment() {
        val tag = currentFragmentTag
        val fragment = supportFragmentManager.findFragmentByTag(tag)
        when (fragment) {
            is VideosFragment -> fragment.refreshData()
            is GalleryFragment -> fragment.refreshData()
        }
    }

    companion object {
        const val TAG_VIDEOS = "videos"
        const val TAG_GALLERY = "gallery"
        const val TAG_SAFE = "safe"
        const val TAG_SETTINGS = "settings"
    }
}
