package com.yolomedia.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.yolomedia.data.model.BottomBarStyle
import com.yolomedia.data.model.SortOrder
import com.yolomedia.data.model.ThemeMode
import com.yolomedia.data.model.ViewMode

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("yolomedia_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name)
        set(value) = prefs.edit().putString("theme_mode", value.name).apply()

    var accentColor: Int
        get() = prefs.getInt("accent_color", 0xFF2196F3.toInt())
        set(value) = prefs.edit().putInt("accent_color", value).apply()

    var viewMode: ViewMode
        get() = ViewMode.valueOf(prefs.getString("view_mode", ViewMode.GRID.name) ?: ViewMode.GRID.name)
        set(value) = prefs.edit().putString("view_mode", value.name).apply()

    var sortOrder: SortOrder
        get() = SortOrder.valueOf(prefs.getString("sort_order", SortOrder.DATE_DESC.name) ?: SortOrder.DATE_DESC.name)
        set(value) = prefs.edit().putString("sort_order", value.name).apply()

    var animationsEnabled: Boolean
        get() = prefs.getBoolean("animations_enabled", true)
        set(value) = prefs.edit().putBoolean("animations_enabled", value).apply()

    var bottomBarStyle: BottomBarStyle
        get() = BottomBarStyle.valueOf(prefs.getString("bottom_bar_style", BottomBarStyle.FIXED.name) ?: BottomBarStyle.FIXED.name)
        set(value) = prefs.edit().putString("bottom_bar_style", value.name).apply()

    var safePin: String
        get() = prefs.getString("safe_pin", "") ?: ""
        set(value) = prefs.edit().putString("safe_pin", value).apply()

    var isSafeSetup: Boolean
        get() = prefs.getBoolean("is_safe_setup", false)
        set(value) = prefs.edit().putBoolean("is_safe_setup", value).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean("biometric_enabled", true)
        set(value) = prefs.edit().putBoolean("biometric_enabled", value).apply()

    var gridColumnCount: Int
        get() = prefs.getInt("grid_column_count", 3)
        set(value) = prefs.edit().putInt("grid_column_count", value).apply()
}
