package com.yolomedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.yolomedia.data.model.BottomBarStyle
import com.yolomedia.data.model.SortOrder
import com.yolomedia.data.model.ThemeMode
import com.yolomedia.data.model.ViewMode
import com.yolomedia.data.preferences.AppPreferences

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val preferences = AppPreferences(application)

    private val _themeMode = MutableLiveData(preferences.themeMode)
    val themeMode: LiveData<ThemeMode> = _themeMode

    private val _accentColor = MutableLiveData(preferences.accentColor)
    val accentColor: LiveData<Int> = _accentColor

    private val _viewMode = MutableLiveData(preferences.viewMode)
    val viewMode: LiveData<ViewMode> = _viewMode

    private val _sortOrder = MutableLiveData(preferences.sortOrder)
    val sortOrder: LiveData<SortOrder> = _sortOrder

    private val _animationsEnabled = MutableLiveData(preferences.animationsEnabled)
    val animationsEnabled: LiveData<Boolean> = _animationsEnabled

    private val _bottomBarStyle = MutableLiveData(preferences.bottomBarStyle)
    val bottomBarStyle: LiveData<BottomBarStyle> = _bottomBarStyle

    fun setThemeMode(mode: ThemeMode) {
        preferences.themeMode = mode
        _themeMode.value = mode
    }

    fun setAccentColor(color: Int) {
        preferences.accentColor = color
        _accentColor.value = color
    }

    fun setViewMode(mode: ViewMode) {
        preferences.viewMode = mode
        _viewMode.value = mode
    }

    fun setSortOrder(order: SortOrder) {
        preferences.sortOrder = order
        _sortOrder.value = order
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        preferences.animationsEnabled = enabled
        _animationsEnabled.value = enabled
    }

    fun setBottomBarStyle(style: BottomBarStyle) {
        preferences.bottomBarStyle = style
        _bottomBarStyle.value = style
    }
}
