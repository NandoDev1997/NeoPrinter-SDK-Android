package com.nandodev1997.neoprintersdk

/** PrintBuilder - DSL para construir comandos ESC/POS */

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.nio.charset.Charset

/** Genera el buffer de bytes para enviar a la impresora */
class PrintBuilder {

    private val buffer = mutableListOf<Byte>()

    // Ancho de papel (en caracteres). 32 = 58mm, 48 = 80mm
    var paperWidth: Int = 48
    var charset: Charset = Charsets.ISO_8859_1

    // ── Texto ─────────────────────────────────────────────────────────────────

    /** Agrega texto al buffer */
    fun text(value: String): PrintBuilder {
        buffer.addAll(value.toByteArray(charset).toList())
        return this
    }

    /** Texto con salto de línea */
    fun textLine(value: String): PrintBuilder = text("$value\n")

    // ── Alineación ────────────────────────────────────────────────────────────

    /** Alineación (LEFT, CENTER, RIGHT) */
    fun align(align: Align): PrintBuilder {
        val n = when (align) {
            Align.LEFT -> 0x00
            Align.CENTER -> 0x01
            Align.RIGHT -> 0x02
        }
        return rawBytes(EscPos.ESC, 0x61, n)
    }

    // ── Negrita ───────────────────────────────────────────────────────────────

    fun bold(enabled: Boolean): PrintBuilder = rawBytes(EscPos.ESC, 0x45, if (enabled) 0x01 else 0x00)

    // ── Subrayado ─────────────────────────────────────────────────────────────

    /** Subrayado (0=off, 1=on, 2=doble) */
    @JvmOverloads
    fun underline(mode: Int = 1): PrintBuilder = rawBytes(EscPos.ESC, 0x2D, mode.coerceIn(0, 2))

    // ── Tamaño de texto ───────────────────────────────────────────────────────

    /** Tamaño del texto */
    fun size(size: TextSize): PrintBuilder {
        val n = when (size) {
            TextSize.NORMAL -> 0x00
            TextSize.DOUBLE_HEIGHT -> 0x01
            TextSize.DOUBLE_WIDTH -> 0x10
            TextSize.LARGE -> 0x11
        }
        return rawBytes(EscPos.GS, 0x21, n)
    }

    // ── Invertido (blanco sobre negro) ────────────────────────────────────────

    fun invert(enabled: Boolean): PrintBuilder = rawBytes(EscPos.GS, 0x42, if (enabled) 0x01 else 0x00)

    // ── Densidad / Calor ──────────────────────────────────────────────────────

    /** Ajusta densidad/calor de impresión */
    @JvmOverloads
    fun printDensity(heating: Int = 80, interval: Int = 2): PrintBuilder =
        rawBytes(EscPos.ESC, 0x37, heating.coerceIn(3, 255), interval.coerceIn(0, 255))

    // ── Separadores y espaciado ───────────────────────────────────────────────

    /** Línea divisora */
    @JvmOverloads
    fun divider(char: Char = '-'): PrintBuilder = textLine(char.toString().repeat(paperWidth))

    /**
     * Imprime una línea doble de '=' del ancho del papel.
     */
    fun doubleDivider(): PrintBuilder = divider('=')

    /** Salto de N líneas */
    @JvmOverloads
    fun feed(lines: Int = 1): PrintBuilder {
        repeat(lines.coerceIn(1, 10)) { buffer.add('\n'.code.toByte()) }
        return this
    }

    /** Espacio entre líneas */
    fun lineSpacing(n: Int): PrintBuilder = rawBytes(EscPos.ESC, 0x33, n.coerceIn(0, 255))

    fun lineSpacingDefault(): PrintBuilder = rawBytes(EscPos.ESC, 0x32)

    // ── Filas con dos columnas (útil para recibos) ────────────────────────────

    /** Fila con dos columnas (izquierda y derecha) */
    @JvmOverloads
    fun row(left: String, right: String, width: Int = paperWidth): PrintBuilder {
        val available = width - right.length
        val truncated = if (left.length >= available) left.take(available - 1) + " " else left
        val padding = " ".repeat((available - truncated.length).coerceAtLeast(0))
        return textLine("$truncated$padding$right")
    }

    /** Fila con tres columnas */
    @JvmOverloads
    fun row3(col1: String, col2: String, col3: String, width: Int = paperWidth): PrintBuilder {
        val colWidth = width / 3
        val c1 = col1.take(colWidth).padEnd(colWidth)
        val c2 = col2.take(colWidth).padEnd(colWidth)
        val c3 = col3.take(colWidth).padStart(colWidth)
        return textLine("$c1$c2$c3")
    }

    // ── Corte de papel ────────────────────────────────────────────────────────

    /** Corte total */
    fun cut(): PrintBuilder = rawBytes(EscPos.GS, 0x56, 0x00)

    /** Corte parcial */
    fun partialCut(): PrintBuilder = rawBytes(EscPos.GS, 0x56, 0x01)

    // ── Cajón de dinero ───────────────────────────────────────────────────────

    /** Abre el cajón de dinero */
    fun openDrawer(): PrintBuilder = rawBytes(EscPos.ESC, 0x70, 0x00, 0x19, 0xFA)

    /**
     * Imprime un código de barras CODE128 (System B).
     *
     * @param data Contenido del código. Si no incluye prefijo de subset ({A, {B, {C), se añade {B por defecto.
     * @param height Altura en puntos (1-255).
     * @param width  Ancho de módulo (2-6).
     * @param hriPos Posición del texto HRI: 0=none, 1=above, 2=below, 3=both.
     */
    @JvmOverloads
    fun barcode128(
        data: String, height: Int = 60, width: Int = 2, hriPos: Int = 2
    ): PrintBuilder {
        val hasSubset = data.startsWith("{") && data.length >= 2 && data[1] in 'A'..'C'
        val finalData = if (hasSubset) data else "{B$data"
        return barcode(73, finalData, height, width, hriPos)
    }

    /**
     * Imprime un código de barras EAN13.
     */
    @JvmOverloads
    fun barcodeEAN13(data: String, height: Int = 60, width: Int = 2, hriPos: Int = 2): PrintBuilder =
        barcode(67, data, height, width, hriPos)

    /**
     * Imprime un código de barras CODE39.
     */
    @JvmOverloads
    fun barcode39(data: String, height: Int = 60, width: Int = 2, hriPos: Int = 2): PrintBuilder =
        barcode(69, data, height, width, hriPos)

    /**
     * Método genérico para imprimir códigos de barras (System B: GS k m n d1...dn).
     * @param m Tipo de código (65-73).
     * @param data Datos del código.
     */
    @JvmOverloads
    fun barcode(
        m: Int, data: String, height: Int = 60, width: Int = 2, hriPos: Int = 2
    ): PrintBuilder {
        rawBytes(EscPos.GS, 0x68, height.coerceIn(1, 255))        // Altura
        rawBytes(EscPos.GS, 0x77, width.coerceIn(2, 6))           // Ancho módulo
        rawBytes(EscPos.GS, 0x48, hriPos.coerceIn(0, 3))          // HRI
        val bytes = data.toByteArray(Charsets.US_ASCII)
        rawBytes(EscPos.GS, 0x6B, m, bytes.size)                  // Comando + Tipo + Longitud
        buffer.addAll(bytes.toList())                             // Datos
        return this
    }

    // ── QR Code ───────────────────────────────────────────────────────────────

    /** Código QR (módulo 1-8) */
    @JvmOverloads
    fun qrCode(data: String, size: Int = 4, ecc: Int = 49): PrintBuilder {
        val bytes = data.toByteArray(Charsets.UTF_8)
        val len = bytes.size + 3

        // Modelo QR
        rawBytes(EscPos.GS, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00)
        // Tamaño módulo
        rawBytes(EscPos.GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size.coerceIn(1, 8))
        // Nivel corrección
        rawBytes(EscPos.GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, ecc)
        // Datos
        rawBytes(EscPos.GS, 0x28, 0x6B, len and 0xFF, (len shr 8) and 0xFF, 0x31, 0x50, 0x30)
        buffer.addAll(bytes.toList())
        // Imprimir
        rawBytes(EscPos.GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30)
        return this
    }

    // ── Imagen bitmap ─────────────────────────────────────────────────────────

    /** Imprime una imagen Bitmap */
    fun bitmap(bitmap: Bitmap): PrintBuilder {
        val maxWidth = 384 // aunque sea 80mm BT
        val ratio = bitmap.height.toFloat() / bitmap.width

        val newHeight = (maxWidth * ratio).toInt()
        val resized = Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)

        val pixels = bitmapToMonoArray(resized)

        var width=resized.width
        var height=resized.height


        val fixedWidth = (width + 7) / 8 * 8

        rawBytes(EscPos.ESC, 0x33, 0x18) // line spacing 24

        val bands = (height + 23) / 24

        for (band in 0 until bands) {

            rawBytes(
                EscPos.ESC, 0x2A, 0x21,
                (fixedWidth and 0xFF),
                (fixedWidth shr 8) and 0xFF
            )

            for (x in 0 until fixedWidth) {

                for (b in 0 until 3) {

                    var byte = 0

                    for (bit in 0 until 8) {

                        val y = band * 24 + b * 8 + bit

                        if (y < height && x < width) {
                            val index = y * width + x

                            if (index < pixels.size && pixels[index]) {
                                byte = byte or (0x80 shr bit)
                            }
                        }
                    }

                    buffer.add(byte.toByte())
                }
            }

            buffer.add(0x0A)
        }

        lineSpacingDefault()
        return this
    }

    internal fun bitmapToMonoArray(bitmap: Bitmap): BooleanArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = BooleanArray(width * height)

        // Creamos una copia de la imagen en flotantes para el error difuso
        val lum = Array(height) { FloatArray(width) }

        // Calculamos luminancia inicial y aplicamos transparencia
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = bitmap.getPixel(x, y)
                val alpha = (color shr 24) and 0xFF
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                // Luminancia perceptual
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toFloat()

                // Transparencia → blanco (255)
                lum[y][x] = if (alpha <= 128) 255f else gray
            }
        }

        // Floyd–Steinberg dithering
        for (y in 0 until height) {
            for (x in 0 until width) {
                val oldPixel = lum[y][x]
                val newPixel = if (oldPixel < 128) 0f else 255f
                val error = oldPixel - newPixel
                lum[y][x] = newPixel
                pixels[y * width + x] = newPixel == 0f

                // Distribuimos el error
                if (x + 1 < width) lum[y][x + 1] += error * 7f / 16f
                if (y + 1 < height) {
                    if (x > 0) lum[y + 1][x - 1] += error * 3f / 16f
                    lum[y + 1][x] += error * 5f / 16f
                    if (x + 1 < width) lum[y + 1][x + 1] += error * 1f / 16f
                }
            }
        }

        return pixels
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    /** Inicializa la configuración */
    fun reset(): PrintBuilder = rawBytes(EscPos.ESC, 0x40)

    // ── Bytes raw ─────────────────────────────────────────────────────────────

    /** Agrega bytes directamente */
    fun rawBytes(vararg bytes: Int): PrintBuilder {
        bytes.forEach { buffer.add(it.toByte()) }
        return this
    }

    fun rawBytes(bytes: ByteArray): PrintBuilder {
        buffer.addAll(bytes.toList())
        return this
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    /** Genera el ByteArray final */
    fun build(): ByteArray = buffer.toByteArray()
}

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class Align { LEFT, CENTER, RIGHT }

enum class TextSize {
    NORMAL, DOUBLE_HEIGHT, DOUBLE_WIDTH, LARGE          // doble alto + doble ancho
}

// ── Constantes ESC/POS ────────────────────────────────────────────────────────

object EscPos {
    const val ESC: Int = 0x1B
    const val GS: Int = 0x1D
    const val DLE: Int = 0x10
    const val FS: Int = 0x1C
    const val LF: Int = 0x0A
    const val FF: Int = 0x0C

    // Comandos listos para usar como ByteArray
    @JvmField val INITIALIZE = byteArrayOf(ESC.toByte(), 0x40)
    @JvmField val CUT_FULL = byteArrayOf(GS.toByte(), 0x56, 0x00)
    @JvmField val CUT_PARTIAL = byteArrayOf(GS.toByte(), 0x56, 0x01)
    @JvmField val BOLD_ON = byteArrayOf(ESC.toByte(), 0x45, 0x01)
    @JvmField val BOLD_OFF = byteArrayOf(ESC.toByte(), 0x45, 0x00)
    @JvmField val ALIGN_LEFT = byteArrayOf(ESC.toByte(), 0x61, 0x00)
    @JvmField val ALIGN_CENTER = byteArrayOf(ESC.toByte(), 0x61, 0x01)
    @JvmField val ALIGN_RIGHT = byteArrayOf(ESC.toByte(), 0x61, 0x02)
}