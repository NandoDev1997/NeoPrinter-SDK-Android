package com.nandodev1997.neoprintersdk

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/** Muestra un diálogo de "Imprimiendo..." que no se puede cancelar */
class PrintingProgressDialog(private val context: Context) {
    private var dialog: Dialog? = null
    private var message: String = "Imprimiendo..."

    fun withMessage(msg: String) = apply { this.message = msg }

    fun show() {
        val dp = context.resources.displayMetrics.density
        val themedContext = ContextThemeWrapper(context, R.style.KPrinterDialogTheme)

        dialog = Dialog(themedContext).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = 16 * dp
                setColor(Color.WHITE)
            }
            val pad = (32 * dp).toInt()
            setPadding(pad, pad, pad, pad)
            minimumWidth = (240 * dp).toInt() // <- Forzar ancho para que no se corte el texto
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val progress = ProgressBar(context).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(
                (48 * dp).toInt(), (48 * dp).toInt()
            ).also {
                it.bottomMargin = (16 * dp).toInt()
            }
        }
        card.addView(progress)

        val text = TextView(context).apply {
            this.text = message
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        card.addView(text)

        dialog?.setContentView(card)
        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}
