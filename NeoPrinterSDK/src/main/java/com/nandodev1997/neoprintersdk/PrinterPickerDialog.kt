package com.nandodev1997.neoprintersdk

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Diálogo para seleccionar una impresora del catálogo */
class PrinterPickerDialog(
    private val context: Context, private val catalog: PrinterCatalog
) {

    private var onSelected: ((PrinterDevice) -> Unit)? = null
    private var onNone: (() -> Unit)? = null
    private var dialogTitle: String = "Seleccionar Impresora"

    fun onPrinterSelected(callback: (PrinterDevice) -> Unit) = apply { onSelected = callback }
    fun onNoPrinters(callback: () -> Unit) = apply { onNone = callback }
    fun withTitle(t: String) = apply { dialogTitle = t }

    fun show() {
        val all = catalog.getAll()
        when {
            all.isEmpty() -> onNone?.invoke()
            all.size == 1 -> onSelected?.invoke(all.first())
            else -> {
                val grouped = catalog.getGrouped()
                val hasNetwork = grouped.containsKey(PrinterCatalog.CATEGORY_NETWORK)
                val hasBt = grouped.containsKey(PrinterCatalog.CATEGORY_BT)

                when {
                    hasNetwork && !hasBt -> showListDialog(grouped[PrinterCatalog.CATEGORY_NETWORK]!!, false)
                    !hasNetwork && hasBt -> showListDialog(grouped[PrinterCatalog.CATEGORY_BT]!!, false)
                    else -> showCategoryDialog(grouped)
                }
            }
        }
    }

    private fun makeDialog(): Dialog {
        val themedContext = ContextThemeWrapper(context, R.style.KPrinterDialogTheme)
        val dialog = Dialog(themedContext)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return dialog
    }

    private fun showCategoryDialog(grouped: LinkedHashMap<String, List<PrinterDevice>>) {
        val dialog = makeDialog()
        val dp = context.resources.displayMetrics.density

        val root = buildShell(dp, dialogTitle, dialog)
        val body = root.getChildAt(1) as LinearLayout

        body.setPadding(40, 80, 40, 80)

        if (grouped.containsKey(PrinterCatalog.CATEGORY_NETWORK)) {
            body.addView(buildCategoryCard(dp, R.drawable.printer, "Impresoras Fijas (Red)", "Wi-Fi / Ethernet") {
                dialog.dismiss()
                showListDialog(grouped[PrinterCatalog.CATEGORY_NETWORK]!!, true)
            })
            body.addView(spacer(dp, 20f))
        }

        if (grouped.containsKey(PrinterCatalog.CATEGORY_BT)) {
            body.addView(buildCategoryCard(dp, R.drawable.ic_printer_bt, "Impresoras Portátiles (BT)", "Emparejamiento inalámbrico") {
                dialog.dismiss()
                showListDialog(grouped[PrinterCatalog.CATEGORY_BT]!!, true)
            })
        }



        dialog.setContentView(root)
        dialog.show()
    }

    private fun showListDialog(printers: List<PrinterDevice>, showBack: Boolean) {
        val dialog = makeDialog()
        val dp = context.resources.displayMetrics.density

        val root = buildShell(dp, "Seleccionar Impresora", dialog)
        val body = root.getChildAt(1) as LinearLayout

        val scroll = ScrollView(context)
        val listContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(listContainer)

        printers.forEach {
            listContainer.addView(buildPrinterListRow(dp, it, dialog))
            listContainer.addView(buildDivider(dp))
        }

        body.addView(scroll)

        body.addView(TextView(context).apply {
            text = "Cancelar"
            textSize = 14f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER
            val pad = (10 * dp).toInt()
            setPadding(0, pad, 0, pad)
            setOnClickListener { dialog.dismiss() }
        })

        dialog.setContentView(root)
        dialog.show()
    }

    private fun buildShell(dp: Float, headerTitle: String, dialog: Dialog): LinearLayout {
        val outer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRect(Color.WHITE, 16 * dp)
        }

        val header = FrameLayout(context).apply {
            background = roundedRectTop(Color.parseColor("#2D2D2D"), 16 * dp)
            val pad = (20 * dp).toInt()
            setPadding(pad, pad, pad, pad)
        }

        header.addView(TextView(context).apply {
            text = headerTitle
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })

        val close = ImageView(context).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.WHITE) // fuerza color
            layoutParams = FrameLayout.LayoutParams(40, 40, Gravity.END)
            setOnClickListener { dialog.dismiss() }
        }
        header.addView(close)

        outer.addView(header)

        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * dp).toInt()
            setPadding(pad, pad, pad, pad)
        }

        outer.addView(body)
        return outer
    }

    private fun buildCategoryCard(
        dp: Float, iconRes: Int, title: String, subtitle: String, onClick: () -> Unit
    ): View {

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = roundedRect(Color.parseColor("#F3F3F3"), 12 * dp)
            val pad = (16 * dp).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isClickable = true
            setOnClickListener { onClick() }
        }

        val iconCircle = FrameLayout(context).apply {
            background = circle(Color.parseColor("#DDDDDD"), 48 * dp)
            layoutParams = LinearLayout.LayoutParams(
                (48 * dp).toInt(), (48 * dp).toInt()
            ).also {
                it.marginEnd = (16 * dp).toInt()
            }
        }

        val icon = ImageView(context).apply {
            setImageResource(iconRes)
            layoutParams = FrameLayout.LayoutParams(
                (24 * dp).toInt(), (24 * dp).toInt(), Gravity.CENTER
            )
        }

        iconCircle.addView(icon)
        card.addView(iconCircle)

        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        textCol.addView(TextView(context).apply {
            text = title
            textSize = 15f
            setTextColor(Color.parseColor("#111111"))
            typeface = Typeface.DEFAULT_BOLD
        })

        textCol.addView(TextView(context).apply {
            text = subtitle
            textSize = 13f
            setTextColor(Color.parseColor("#888888"))
            val tPad = (4 * dp).toInt()
            setPadding(0, tPad, 0, 0)
        })

        card.addView(textCol)

        val chevron = ImageView(context).apply {
            setImageResource(R.drawable.ic_chevron_right)
            layoutParams = LinearLayout.LayoutParams(
                (20 * dp).toInt(), (20 * dp).toInt()
            )
        }

        card.addView(chevron)

        return card
    }

    private fun buildPrinterListRow(dp: Float, printer: PrinterDevice, dialog: Dialog): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val vPad = (14 * dp).toInt()
            setPadding(0, vPad, 0, vPad)
            isClickable = true
        }

        val iconCircle = FrameLayout(context).apply {
            background = circle(Color.parseColor("#EEEEEE"), 40 * dp)
            layoutParams = LinearLayout.LayoutParams(
                (40 * dp).toInt(), (40 * dp).toInt()
            ).also {
                it.marginEnd = (14 * dp).toInt()
            }
        }

        val icon = ImageView(context).apply {
            setImageResource(
                if (printer.type == PrinterType.BLUETOOTH) R.drawable.ic_printer_bt
                else R.drawable.ic_printer_network
            )
            layoutParams = FrameLayout.LayoutParams(
                (20 * dp).toInt(), (20 * dp).toInt(), Gravity.CENTER
            )
        }

        iconCircle.addView(icon)
        row.addView(iconCircle)

        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        textCol.addView(TextView(context).apply {
            text = printer.displayName
            textSize = 14f
            setTextColor(Color.parseColor("#111111"))
            typeface = Typeface.DEFAULT_BOLD
        })

        textCol.addView(TextView(context).apply {
            text = printer.subtitle
            textSize = 12f
            setTextColor(Color.parseColor("#888888"))
        })

        row.addView(textCol)

        val chevron = ImageView(context).apply {
            setImageResource(R.drawable.ic_chevron_right)
            layoutParams = LinearLayout.LayoutParams(
                (20 * dp).toInt(), (20 * dp).toInt()
            )
        }

        row.addView(chevron)

        row.setOnClickListener {
            dialog.dismiss()
            onSelected?.invoke(printer)
        }

        return row
    }

    private fun circle(color: Int, sizePx: Float) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setSize(sizePx.toInt(), sizePx.toInt())
    }

    private fun buildDivider(dp: Float) = View(context).apply {
        setBackgroundColor(Color.LTGRAY)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
        )
    }

    private fun spacer(dp: Float, h: Float) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (h * dp).toInt()
        )
    }

    private fun roundedRect(color: Int, r: Float) = GradientDrawable().apply {
        cornerRadius = r
        setColor(color)
    }

    private fun roundedRectTop(color: Int, r: Float) = GradientDrawable().apply {
        cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        setColor(color)
    }
}