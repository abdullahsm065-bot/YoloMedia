package com.yolomedia.ui.theme

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.yolomedia.R
import com.yolomedia.data.model.ThemeMode
import com.yolomedia.data.preferences.AppPreferences

object ThemeManager {

    fun applyTheme(mode: ThemeMode) {
        when (mode) {
            ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeMode.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun isDarkMode(context: Context): Boolean {
        val prefs = AppPreferences(context)
        return when (prefs.themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> {
                val nightModeFlags = context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun getBackgroundColor(context: Context): Int {
        return if (isDarkMode(context)) {
            ContextCompat.getColor(context, R.color.dark_background)
        } else {
            ContextCompat.getColor(context, R.color.light_background)
        }
    }

    fun getSurfaceColor(context: Context): Int {
        return if (isDarkMode(context)) {
            ContextCompat.getColor(context, R.color.dark_surface)
        } else {
            ContextCompat.getColor(context, R.color.light_surface)
        }
    }

    fun getCardColor(context: Context): Int {
        return if (isDarkMode(context)) {
            ContextCompat.getColor(context, R.color.dark_card)
        } else {
            ContextCompat.getColor(context, R.color.light_card)
        }
    }

    fun getTextPrimaryColor(context: Context): Int {
        return if (isDarkMode(context)) {
            ContextCompat.getColor(context, R.color.dark_text_primary)
        } else {
            ContextCompat.getColor(context, R.color.light_text_primary)
        }
    }

    fun getTextSecondaryColor(context: Context): Int {
        return if (isDarkMode(context)) {
            ContextCompat.getColor(context, R.color.dark_text_secondary)
        } else {
            ContextCompat.getColor(context, R.color.light_text_secondary)
        }
    }

    fun getAccentColor(context: Context): Int {
        return AppPreferences(context).accentColor
    }

    fun getAccentColorLight(context: Context): Int {
        val accent = getAccentColor(context)
        val r = Color.red(accent)
        val g = Color.green(accent)
        val b = Color.blue(accent)
        return if (isDarkMode(context)) {
            Color.argb(40, r, g, b)
        } else {
            Color.argb(25, r, g, b)
        }
    }

    fun getAccentColorMedium(context: Context): Int {
        val accent = getAccentColor(context)
        val r = Color.red(accent)
        val g = Color.green(accent)
        val b = Color.blue(accent)
        return Color.argb(100, r, g, b)
    }

    fun getIconTintColor(context: Context): Int {
        return if (isDarkMode(context)) {
            ContextCompat.getColor(context, R.color.dark_icon_tint)
        } else {
            ContextCompat.getColor(context, R.color.light_icon_tint)
        }
    }

    fun getDividerColor(context: Context): Int {
        return if (isDarkMode(context)) {
            ContextCompat.getColor(context, R.color.dark_divider)
        } else {
            ContextCompat.getColor(context, R.color.light_divider)
        }
    }

    fun getNavBarColor(context: Context): Int {
        return if (isDarkMode(context)) {
            ContextCompat.getColor(context, R.color.dark_nav_bar)
        } else {
            ContextCompat.getColor(context, R.color.light_nav_bar)
        }
    }

    fun applyThemeToActivity(activity: Activity) {
        val isDark = isDarkMode(activity)
        val bgColor = getBackgroundColor(activity)
        val navColor = getNavBarColor(activity)

        activity.window.statusBarColor = bgColor
        activity.window.navigationBarColor = navColor

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = activity.window.insetsController
            if (isDark) {
                controller?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                controller?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
            } else {
                controller?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
                controller?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } else {
            @Suppress("DEPRECATION")
            if (!isDark) {
                activity.window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                activity.window.decorView.systemUiVisibility = 0
            }
        }
    }

    fun styleTitle(textView: TextView, context: Context) {
        textView.setTextColor(getTextPrimaryColor(context))
    }

    fun styleSubtitle(textView: TextView, context: Context) {
        textView.setTextColor(getTextSecondaryColor(context))
    }

    fun tintIcon(imageView: ImageView, context: Context) {
        imageView.setColorFilter(getTextPrimaryColor(context))
    }

    fun tintIconAccent(imageView: ImageView, context: Context) {
        imageView.setColorFilter(getAccentColor(context))
    }

    fun applyAccentToSeekBar(seekBar: SeekBar, context: Context) {
        val accentColor = getAccentColor(context)
        seekBar.progressTintList = android.content.res.ColorStateList.valueOf(accentColor)
        seekBar.thumbTintList = android.content.res.ColorStateList.valueOf(accentColor)
    }

    fun createAccentDrawable(context: Context, cornerRadius: Float = 12f): GradientDrawable {
        return GradientDrawable().apply {
            setColor(getAccentColor(context))
            this.cornerRadius = cornerRadius * context.resources.displayMetrics.density
        }
    }

    fun createAccentOutlineDrawable(context: Context, cornerRadius: Float = 12f): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(
                (1.5f * context.resources.displayMetrics.density).toInt(),
                getAccentColor(context)
            )
            this.cornerRadius = cornerRadius * context.resources.displayMetrics.density
        }
    }
}
