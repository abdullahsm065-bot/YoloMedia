package com.yolomedia.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
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

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var bottomNavContainer: FrameLayout
    private lateinit var mainContainer: View
    private lateinit var preferences: AppPreferences

    private var currentFragmentTag: String = TAG_VIDEOS

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
        bottomNav = findViewById(R.id.bottom_navigation)

        setupBottomNav()
        setupBackPress()
        applyThemeColors()
        applyBottomBarStyle()

        if (savedInstanceState != null) {
            currentFragmentTag = savedInstanceState.getString("current_tag", TAG_VIDEOS)
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
                    bottomNav.selectedItemId = R.id.nav_videos
                    return
                }

                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_videos -> showFragment(TAG_VIDEOS)
                R.id.nav_gallery -> showFragment(TAG_GALLERY)
                R.id.nav_safe -> showFragment(TAG_SAFE)
                R.id.nav_settings -> showFragment(TAG_SETTINGS)
            }
            true
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
        val accentColor = ThemeManager.getAccentColor(this)
        val isDark = ThemeManager.isDarkMode(this)

        mainContainer.setBackgroundColor(bgColor)

        val iconTint = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected),
                intArrayOf()
            ),
            intArrayOf(accentColor, ThemeManager.getIconTintColor(this))
        )
        val textTint = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected),
                intArrayOf()
            ),
            intArrayOf(accentColor, ThemeManager.getIconTintColor(this))
        )

        bottomNav.itemIconTintList = iconTint
        bottomNav.itemTextColor = textTint

        val style = preferences.bottomBarStyle
        if (style == BottomBarStyle.FLOATING) {
            val glassDrawable = GradientDrawable().apply {
                setColor(if (isDark) 0xB30F1729.toInt() else 0xD9FFFFFF.toInt())
                cornerRadius = 28f * resources.displayMetrics.density
                setStroke(
                    (1 * resources.displayMetrics.density).toInt(),
                    if (isDark) 0x1AFFFFFF else 0x18000000
                )
            }
            bottomNav.background = glassDrawable
        } else {
            val navBgColor = if (isDark) 0xCC0F1729.toInt() else 0xE6FFFFFF.toInt()
            bottomNav.setBackgroundColor(navBgColor)
        }

        ThemeManager.applyThemeToActivity(this)
    }

    fun applyBottomBarStyle() {
        val style = preferences.bottomBarStyle
        val params = bottomNavContainer.layoutParams as ViewGroup.MarginLayoutParams
        val density = resources.displayMetrics.density
        val isDark = ThemeManager.isDarkMode(this)

        when (style) {
            BottomBarStyle.FIXED -> {
                params.setMargins(0, 0, 0, 0)
                val navBgColor = if (isDark) 0xCC0F1729.toInt() else 0xE6FFFFFF.toInt()
                bottomNav.setBackgroundColor(navBgColor)
                bottomNav.elevation = 8f * density
            }
            BottomBarStyle.FLOATING -> {
                val hMargin = (16 * density).toInt()
                val bMargin = (16 * density).toInt()
                params.setMargins(hMargin, 0, hMargin, bMargin)

                val glassDrawable = GradientDrawable().apply {
                    setColor(if (isDark) 0xB30F1729.toInt() else 0xD9FFFFFF.toInt())
                    cornerRadius = 28f * density
                    setStroke(
                        (1 * density).toInt(),
                        if (isDark) 0x1AFFFFFF else 0x18000000
                    )
                }
                bottomNav.background = glassDrawable
                bottomNav.elevation = 16f * density
            }
            BottomBarStyle.COMPACT -> {
                params.setMargins(0, 0, 0, 0)
                val navBgColor = if (isDark) 0xCC0F1729.toInt() else 0xE6FFFFFF.toInt()
                bottomNav.setBackgroundColor(navBgColor)
                bottomNav.elevation = 4f * density
                bottomNav.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_SELECTED
            }
        }

        if (style != BottomBarStyle.COMPACT) {
            bottomNav.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_LABELED
        }

        bottomNavContainer.layoutParams = params
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
