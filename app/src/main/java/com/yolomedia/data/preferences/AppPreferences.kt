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
        get() = prefs.getInt("accent_color", 0xFF3B82F6.toInt())
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
        get() = BottomBarStyle.valueOf(prefs.getString("bottom_bar_style", BottomBarStyle.FLOATING.name) ?: BottomBarStyle.FLOATING.name)
        set(value) = prefs.edit().putString("bottom_bar_style", value.name).apply()

    var safePin: String
        get() = prefs.getString("safe_pin", "2411") ?: "2411"
        set(value) = prefs.edit().putString("safe_pin", value).apply()

    var isSafeSetup: Boolean
        get() = prefs.getBoolean("is_safe_setup", true)
        set(value) = prefs.edit().putBoolean("is_safe_setup", value).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean("biometric_enabled", true)
        set(value) = prefs.edit().putBoolean("biometric_enabled", value).apply()

    var gridColumnCount: Int
        get() = prefs.getInt("grid_column_count", 3)
        set(value) = prefs.edit().putInt("grid_column_count", value).apply()

    var showMediaInfo: Boolean
        get() = prefs.getBoolean("show_media_info", true)
        set(value) = prefs.edit().putBoolean("show_media_info", value).apply()

    var autoPlayVideos: Boolean
        get() = prefs.getBoolean("auto_play_videos", true)
        set(value) = prefs.edit().putBoolean("auto_play_videos", value).apply()

    var defaultPlaybackSpeed: Float
        get() = prefs.getFloat("default_playback_speed", 1.0f)
        set(value) = prefs.edit().putFloat("default_playback_speed", value).apply()

    var swipeToDelete: Boolean
        get() = prefs.getBoolean("swipe_to_delete", false)
        set(value) = prefs.edit().putBoolean("swipe_to_delete", value).apply()

    var hideSystemBars: Boolean
        get() = prefs.getBoolean("hide_system_bars", true)
        set(value) = prefs.edit().putBoolean("hide_system_bars", value).apply()

    var thumbnailQuality: Int
        get() = prefs.getInt("thumbnail_quality", 1)
        set(value) = prefs.edit().putInt("thumbnail_quality", value).apply()

    var confirmBeforeDelete: Boolean
        get() = prefs.getBoolean("confirm_before_delete", true)
        set(value) = prefs.edit().putBoolean("confirm_before_delete", value).apply()

    var safePinLength: Int
        get() = prefs.getInt("safe_pin_length", 4)
        set(value) = prefs.edit().putInt("safe_pin_length", value).apply()

    fun isVideoPlayed(uri: String): Boolean {
        val played = prefs.getStringSet("played_videos", emptySet()) ?: emptySet()
        return uri in played
    }

    fun markVideoPlayed(uri: String) {
        val played = (prefs.getStringSet("played_videos", emptySet()) ?: emptySet()).toMutableSet()
        played.add(uri)
        prefs.edit().putStringSet("played_videos", played).apply()
    }

    fun clearPlayedVideos() {
        prefs.edit().remove("played_videos").apply()
    }

    var showNewBadge: Boolean
        get() = prefs.getBoolean("show_new_badge", true)
        set(value) = prefs.edit().putBoolean("show_new_badge", value).apply()

    var resumePlayback: Boolean
        get() = prefs.getBoolean("resume_playback", true)
        set(value) = prefs.edit().putBoolean("resume_playback", value).apply()

    var loopVideos: Boolean
        get() = prefs.getBoolean("loop_videos", false)
        set(value) = prefs.edit().putBoolean("loop_videos", value).apply()

    var safeAutoLockDelay: Int
        get() = prefs.getInt("safe_auto_lock_delay", 0)
        set(value) = prefs.edit().putInt("safe_auto_lock_delay", value).apply()

    var safeShowThumbnails: Boolean
        get() = prefs.getBoolean("safe_show_thumbnails", true)
        set(value) = prefs.edit().putBoolean("safe_show_thumbnails", value).apply()

    var videoGesturesEnabled: Boolean
        get() = prefs.getBoolean("video_gestures_enabled", true)
        set(value) = prefs.edit().putBoolean("video_gestures_enabled", value).apply()

    var doubleTapSeekDuration: Int
        get() = prefs.getInt("double_tap_seek_duration", 10)
        set(value) = prefs.edit().putInt("double_tap_seek_duration", value).apply()

    var safeHideFromRecents: Boolean
        get() = prefs.getBoolean("safe_hide_from_recents", false)
        set(value) = prefs.edit().putBoolean("safe_hide_from_recents", value).apply()

    var autoRotateVideo: Boolean
        get() = prefs.getBoolean("auto_rotate_video", true)
        set(value) = prefs.edit().putBoolean("auto_rotate_video", value).apply()

    var showFileSize: Boolean
        get() = prefs.getBoolean("show_file_size", true)
        set(value) = prefs.edit().putBoolean("show_file_size", value).apply()

    var showDurationBadge: Boolean
        get() = prefs.getBoolean("show_duration_badge", true)
        set(value) = prefs.edit().putBoolean("show_duration_badge", value).apply()

    var reelsAutoPlay: Boolean
        get() = prefs.getBoolean("reels_auto_play", true)
        set(value) = prefs.edit().putBoolean("reels_auto_play", value).apply()

    var reelsLoop: Boolean
        get() = prefs.getBoolean("reels_loop", true)
        set(value) = prefs.edit().putBoolean("reels_loop", value).apply()

    var cropThumbnails: Boolean
        get() = prefs.getBoolean("crop_thumbnails", true)
        set(value) = prefs.edit().putBoolean("crop_thumbnails", value).apply()

    var rememberLastFolder: Boolean
        get() = prefs.getBoolean("remember_last_folder", false)
        set(value) = prefs.edit().putBoolean("remember_last_folder", value).apply()

    fun clearReelFolders() {
        prefs.edit().putStringSet("reel_folders", emptySet()).apply()
    }

    fun getReelFolders(): Set<String> {
        return prefs.getStringSet("reel_folders", emptySet()) ?: emptySet()
    }

    fun toggleReelFolder(folderPath: String) {
        val folders = getReelFolders().toMutableSet()
        if (folders.contains(folderPath)) folders.remove(folderPath) else folders.add(folderPath)
        prefs.edit().putStringSet("reel_folders", folders).apply()
    }

    fun isReelFolder(folderPath: String): Boolean {
        return folderPath in getReelFolders()
    }
}
