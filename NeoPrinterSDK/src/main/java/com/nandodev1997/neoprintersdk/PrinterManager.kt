package com.nandodev1997.neoprintersdk

import android.content.Context

// ── Interfaces SAM ────────────────────────────────────────────────────────────

fun interface PrinterErrorCallback {
    fun onError(error: PrinterException)
}

fun interface PrinterReadyCallback {
    fun onReady(connection: PrinterConnection)
}

fun interface PrintAction {
    fun apply(builder: PrintBuilder)
}

fun interface PrintResultCallback {
    fun onResult(isSuccess: Boolean, error: String?)
}

// ── PrinterManager ────────────────────────────────────────────────────────────

class PrinterManager(context: Context) {

    val catalog = PrinterCatalog(context)

    private val appContext = context.applicationContext

    private var btPrinter: BluetoothPrinter? = null
    private var netPrinter: NetworkPrinter? = null

    // ── Picker + conexión ─────────────────────────────────────────────────────

    @JvmOverloads
    fun pickAndConnect(
        context: Context,
        onError: PrinterErrorCallback? = null,
        onReady: PrinterReadyCallback
    ) {
        PrinterPickerDialog(context, catalog)
            .onPrinterSelected { device ->
                connectDevice(device, onError, onReady)
            }
            .onNoPrinters {
                onError?.onError(
                    PrinterException(
                        PrinterError.NOT_CONNECTED,
                        "No hay impresoras en el catálogo"
                    )
                )
            }
            .show()
    }

    @JvmOverloads
    fun connectDevice(
        device: PrinterDevice,
        onError: PrinterErrorCallback? = null,
        onReady: PrinterReadyCallback
    ) {
        when (device.type) {

            PrinterType.BLUETOOTH -> {
                val bt = BluetoothPrinter(appContext)
                btPrinter = bt

                bt.connect(device.address) { result ->
                    result
                        .onSuccess {
                            onReady.onReady(
                                PrinterConnection.Bt(device, bt)
                            )
                        }
                        .onFailure { e ->
                            onError?.onError(
                                e as? PrinterException
                                    ?: PrinterException(
                                        PrinterError.UNKNOWN,
                                        e.message ?: "Error"
                                    )
                            )
                        }
                }
            }

            PrinterType.NETWORK -> {
                val net = NetworkPrinter()
                netPrinter = net

                net.connect(device) { result ->
                    result
                        .onSuccess {
                            onReady.onReady(
                                PrinterConnection.Net(device, net)
                            )
                        }
                        .onFailure { e ->
                            onError?.onError(
                                e as? PrinterException
                                    ?: PrinterException(
                                        PrinterError.UNKNOWN,
                                        e.message ?: "Error"
                                    )
                            )
                        }
                }
            }
        }
    }

    fun disconnectAll() {
        btPrinter?.disconnect()
        netPrinter?.disconnect()
    }

    fun release() {
        btPrinter?.release()
        netPrinter?.release()
    }

    /** Diagnóstico (ping) a una impresora de red */
    fun ping(host: String, timeout: Int = 3000, callback: (Boolean) -> Unit) {
        val net = NetworkPrinter()
        net.ping(host, timeout) { result ->
            callback(result.getOrDefault(false))
            net.release()
        }
    }
}

// ── PrinterConnection ─────────────────────────────────────────────────────────

sealed class PrinterConnection {

    abstract val device: PrinterDevice
    abstract val isConnected: Boolean

    // Internal raw methods that actually talk to the printer
    protected abstract fun doSendBytes(data: ByteArray, callback: (Result<Unit>) -> Unit)
    protected abstract fun doFormat(block: PrintBuilder.() -> Unit, callback: (Result<Unit>) -> Unit)
    abstract fun disconnect()

    // ── Public API - Kotlin ───────────────────────────────────────────────────

    @JvmSynthetic
    fun sendBytes(context: Context? = null, data: ByteArray, callback: (Result<Unit>) -> Unit) {
        if (context != null) {
            val dialog = PrintingProgressDialog(context)
            dialog.show()
            doSendBytes(data) { result ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    dialog.dismiss()
                    callback(result)
                }
            }
        } else {
            doSendBytes(data, callback)
        }
    }

    @JvmSynthetic
    fun format(context: Context? = null, block: PrintBuilder.() -> Unit, callback: (Result<Unit>) -> Unit) {
        if (context != null) {
            val dialog = PrintingProgressDialog(context)
            dialog.show()
            doFormat(block) { result ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    dialog.dismiss()
                    callback(result)
                }
            }
        } else {
            doFormat(block, callback)
        }
    }

    // ── Public API - Java ─────────────────────────────────────────────────────

    // Sin Context
    fun sendBytes(data: ByteArray, callback: PrintResultCallback) {
        sendBytes(null, data) { result ->
            if (result.isSuccess) callback.onResult(true, null)
            else callback.onResult(false, result.exceptionOrNull()?.message)
        }
    }

    // Con Context
    fun sendBytes(context: Context, data: ByteArray, callback: PrintResultCallback) {
        sendBytes(context, data) { result ->
            if (result.isSuccess) callback.onResult(true, null)
            else callback.onResult(false, result.exceptionOrNull()?.message)
        }
    }

    // Sin Context
    fun format(action: PrintAction, callback: PrintResultCallback) {
        format(null, { action.apply(this) }) { result ->
            if (result.isSuccess) callback.onResult(true, null)
            else callback.onResult(false, result.exceptionOrNull()?.message)
        }
    }

    // Con Context
    fun format(context: Context, action: PrintAction, callback: PrintResultCallback) {
        format(context, { action.apply(this) }) { result ->
            if (result.isSuccess) callback.onResult(true, null)
            else callback.onResult(false, result.exceptionOrNull()?.message)
        }
    }

    // ── Implementaciones ──────────────────────────────────────────────────────

    class Bt(
        override val device: PrinterDevice,
        private val printer: BluetoothPrinter
    ) : PrinterConnection() {

        override val isConnected get() = printer.isConnected

        override fun doSendBytes(data: ByteArray, callback: (Result<Unit>) -> Unit) =
            printer.sendBytes(data, callback)

        override fun doFormat(block: PrintBuilder.() -> Unit, callback: (Result<Unit>) -> Unit) =
            printer.format(block, callback)

        override fun disconnect() = printer.disconnect()
    }

    class Net(
        override val device: PrinterDevice,
        private val printer: NetworkPrinter
    ) : PrinterConnection() {

        override val isConnected get() = printer.isConnected

        override fun doSendBytes(data: ByteArray, callback: (Result<Unit>) -> Unit) =
            printer.sendBytes(data, callback)

        override fun doFormat(block: PrintBuilder.() -> Unit, callback: (Result<Unit>) -> Unit) =
            printer.format(block, callback)

        override fun disconnect() = printer.disconnect()
    }
}