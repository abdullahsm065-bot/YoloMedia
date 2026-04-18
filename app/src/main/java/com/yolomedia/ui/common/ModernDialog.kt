package com.yolomedia.ui.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.yolomedia.ui.theme.ThemeManager

object ModernDialog {

    fun confirm(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "Yes",
        negativeText: String = "Cancel",
        onPositive: () -> Unit,
        onNegative: (() -> Unit)? = null
    ) {
        val dialog = Dialog(context)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        val isDark = ThemeManager.isDarkMode(context)
        val bgColor = if (isDark) 0xFF1A2332.toInt() else 0xFFFFFFFF.toInt()
        val textPrimary = ThemeManager.getTextPrimaryColor(context)
        val textSecondary = ThemeManager.getTextSecondaryColor(context)
        val accent = ThemeManager.getAccentColor(context)
        val density = context.resources.displayMetrics.density

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 28f * density
            }
            background = bg
            setPadding((28 * density).toInt(), (28 * density).toInt(), (28 * density).toInt(), (20 * density).toInt())
            elevation = 24f * density
        }

        val tvTitle = TextView(context).apply {
            text = title
            textSize = 20f
            setTextColor(textPrimary)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        }
        container.addView(tvTitle)

        val tvMessage = TextView(context).apply {
            text = message
            textSize = 15f
            setTextColor(textSecondary)
            setPadding(0, (12 * density).toInt(), 0, (24 * density).toInt())
            setLineSpacing(0f, 1.3f)
        }
        container.addView(tvMessage)

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val btnNeg = createButton(context, negativeText, textSecondary, Color.TRANSPARENT, density)
        btnNeg.setOnClickListener {
            dialog.dismiss()
            onNegative?.invoke()
        }
        btnRow.addView(btnNeg)

        val btnPos = createButton(context, positiveText, Color.WHITE, accent, density)
        btnPos.setOnClickListener {
            dialog.dismiss()
            onPositive()
        }
        (btnPos.layoutParams as LinearLayout.LayoutParams).marginStart = (12 * density).toInt()
        btnRow.addView(btnPos)

        container.addView(btnRow)

        dialog.setContentView(container)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.88).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        animateDialogIn(container)
        dialog.show()
    }

    fun info(
        context: Context,
        title: String,
        message: String,
        buttonText: String = "OK",
        onDismiss: (() -> Unit)? = null
    ) {
        val dialog = Dialog(context)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        val isDark = ThemeManager.isDarkMode(context)
        val bgColor = if (isDark) 0xFF1A2332.toInt() else 0xFFFFFFFF.toInt()
        val textPrimary = ThemeManager.getTextPrimaryColor(context)
        val textSecondary = ThemeManager.getTextSecondaryColor(context)
        val accent = ThemeManager.getAccentColor(context)
        val density = context.resources.displayMetrics.density

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 28f * density
            }
            background = bg
            setPadding((28 * density).toInt(), (28 * density).toInt(), (28 * density).toInt(), (20 * density).toInt())
            elevation = 24f * density
        }

        val tvTitle = TextView(context).apply {
            text = title
            textSize = 20f
            setTextColor(textPrimary)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        }
        container.addView(tvTitle)

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (12 * density).toInt()
                bottomMargin = (24 * density).toInt()
            }
        }

        val tvMessage = TextView(context).apply {
            text = message
            textSize = 14f
            setTextColor(textSecondary)
            setLineSpacing(0f, 1.4f)
        }
        scrollView.addView(tvMessage)
        container.addView(scrollView)

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val btnOk = createButton(context, buttonText, Color.WHITE, accent, density)
        btnOk.setOnClickListener {
            dialog.dismiss()
            onDismiss?.invoke()
        }
        btnRow.addView(btnOk)
        container.addView(btnRow)

        dialog.setContentView(container)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.88).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        animateDialogIn(container)
        dialog.show()
    }

    fun input(
        context: Context,
        title: String,
        hint: String = "",
        initialText: String = "",
        inputType: Int = android.text.InputType.TYPE_CLASS_TEXT,
        positiveText: String = "Confirm",
        negativeText: String = "Cancel",
        onConfirm: (String) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        val isDark = ThemeManager.isDarkMode(context)
        val bgColor = if (isDark) 0xFF1A2332.toInt() else 0xFFFFFFFF.toInt()
        val textPrimary = ThemeManager.getTextPrimaryColor(context)
        val textSecondary = ThemeManager.getTextSecondaryColor(context)
        val accent = ThemeManager.getAccentColor(context)
        val density = context.resources.displayMetrics.density
        val inputBg = if (isDark) 0xFF0F1729.toInt() else 0xFFF1F5F9.toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 28f * density
            }
            background = bg
            setPadding((28 * density).toInt(), (28 * density).toInt(), (28 * density).toInt(), (20 * density).toInt())
            elevation = 24f * density
        }

        val tvTitle = TextView(context).apply {
            text = title
            textSize = 20f
            setTextColor(textPrimary)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        }
        container.addView(tvTitle)

        val editText = EditText(context).apply {
            this.hint = hint
            this.inputType = inputType
            setText(initialText)
            textSize = 16f
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
            val editBg = GradientDrawable().apply {
                setColor(inputBg)
                cornerRadius = 16f * density
            }
            background = editBg
            setPadding((18 * density).toInt(), (14 * density).toInt(), (18 * density).toInt(), (14 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (16 * density).toInt()
                bottomMargin = (24 * density).toInt()
            }
        }
        container.addView(editText)

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val btnNeg = createButton(context, negativeText, textSecondary, Color.TRANSPARENT, density)
        btnNeg.setOnClickListener { dialog.dismiss() }
        btnRow.addView(btnNeg)

        val btnPos = createButton(context, positiveText, Color.WHITE, accent, density)
        btnPos.setOnClickListener {
            dialog.dismiss()
            onConfirm(editText.text.toString().trim())
        }
        (btnPos.layoutParams as LinearLayout.LayoutParams).marginStart = (12 * density).toInt()
        btnRow.addView(btnPos)

        container.addView(btnRow)

        dialog.setContentView(container)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.88).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        animateDialogIn(container)
        dialog.show()
        editText.requestFocus()
    }

    fun singleChoice(
        context: Context,
        title: String,
        options: Array<String>,
        selectedIndex: Int = 0,
        onSelect: (Int) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        val isDark = ThemeManager.isDarkMode(context)
        val bgColor = if (isDark) 0xFF1A2332.toInt() else 0xFFFFFFFF.toInt()
        val textPrimary = ThemeManager.getTextPrimaryColor(context)
        val accent = ThemeManager.getAccentColor(context)
        val density = context.resources.displayMetrics.density
        val hoverColor = if (isDark) 0xFF253345.toInt() else 0xFFF1F5F9.toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 28f * density
            }
            background = bg
            setPadding(0, (24 * density).toInt(), 0, (12 * density).toInt())
            elevation = 24f * density
        }

        val tvTitle = TextView(context).apply {
            text = title
            textSize = 20f
            setTextColor(textPrimary)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
            setPadding((28 * density).toInt(), 0, (28 * density).toInt(), (16 * density).toInt())
        }
        container.addView(tvTitle)

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val optionsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        options.forEachIndexed { index, option ->
            val itemView = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((28 * density).toInt(), (14 * density).toInt(), (28 * density).toInt(), (14 * density).toInt())

                val indicatorSize = (20 * density).toInt()
                val indicator = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(indicatorSize, indicatorSize).apply {
                        marginEnd = (16 * density).toInt()
                    }
                    val indicatorBg = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        if (index == selectedIndex) {
                            setColor(accent)
                        } else {
                            setColor(Color.TRANSPARENT)
                            setStroke((2 * density).toInt(), if (isDark) 0xFF4A5568.toInt() else 0xFFCBD5E1.toInt())
                        }
                    }
                    background = indicatorBg
                }
                addView(indicator)

                val tv = TextView(context).apply {
                    text = option
                    textSize = 16f
                    setTextColor(if (index == selectedIndex) accent else textPrimary)
                }
                addView(tv)

                setOnClickListener {
                    dialog.dismiss()
                    onSelect(index)
                }
            }
            optionsContainer.addView(itemView)
        }

        scrollView.addView(optionsContainer)
        container.addView(scrollView)

        dialog.setContentView(container)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.88).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        animateDialogIn(container)
        dialog.show()
    }

    fun list(
        context: Context,
        title: String,
        options: Array<String>,
        onSelect: (Int) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        val isDark = ThemeManager.isDarkMode(context)
        val bgColor = if (isDark) 0xFF1A2332.toInt() else 0xFFFFFFFF.toInt()
        val textPrimary = ThemeManager.getTextPrimaryColor(context)
        val accent = ThemeManager.getAccentColor(context)
        val density = context.resources.displayMetrics.density

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 28f * density
            }
            background = bg
            setPadding(0, (24 * density).toInt(), 0, (12 * density).toInt())
            elevation = 24f * density
        }

        val tvTitle = TextView(context).apply {
            text = title
            textSize = 20f
            setTextColor(textPrimary)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
            setPadding((28 * density).toInt(), 0, (28 * density).toInt(), (16 * density).toInt())
        }
        container.addView(tvTitle)

        val scrollView = ScrollView(context)
        val listContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        options.forEachIndexed { index, option ->
            val tv = TextView(context).apply {
                text = option
                textSize = 16f
                setTextColor(textPrimary)
                setPadding((28 * density).toInt(), (14 * density).toInt(), (28 * density).toInt(), (14 * density).toInt())
                setOnClickListener {
                    dialog.dismiss()
                    onSelect(index)
                }
            }
            listContainer.addView(tv)
        }

        scrollView.addView(listContainer)
        container.addView(scrollView)

        dialog.setContentView(container)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.88).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        animateDialogIn(container)
        dialog.show()
    }

    private fun createButton(context: Context, text: String, textColor: Int, bgColor: Int, density: Float): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 15f
            setTextColor(textColor)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
            val btnBg = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 14f * density
            }
            background = btnBg
            setPadding((22 * density).toInt(), (12 * density).toInt(), (22 * density).toInt(), (12 * density).toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun animateDialogIn(view: View) {
        view.scaleX = 0.85f
        view.scaleY = 0.85f
        view.alpha = 0f
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator(0.8f))
            .start()
    }
}
