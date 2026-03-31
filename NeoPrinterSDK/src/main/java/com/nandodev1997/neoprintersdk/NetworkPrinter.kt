package com.nandodev1997.neoprintersdk

/** NetworkPrinter - Controlador para impresoras ESC/POS vía TCP/IP */

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/** Maneja la comunicación con impresoras de red (LAN/WiFi) */
class NetworkPrinter {

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var connectTimeoutMs: Int = 8_000
    private var readTimeoutMs: Int = 5_000
    private var onConnectionLost: ((PrinterException) -> Unit)? = null

    val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false

    // ── Configuración ─────────────────────────────────────────────────────────

    fun setConnectTimeout(ms: Int): NetworkPrinter {
        connectTimeoutMs = ms; return this
    }

    fun setReadTimeout(ms: Int): NetworkPrinter {
        readTimeoutMs = ms; return this
    }

    fun onConnectionLost(cb: (PrinterException) -> Unit): NetworkPrinter {
        onConnectionLost = cb; return this
    }

    // ── Conexión ──────────────────────────────────────────────────────────────

    /** Conecta por IP y puerto */
    fun connect(host: String, port: Int = 9100, callback: (Result<Unit>) -> Unit) {
        scope.launch {
            val result = connectSuspend(host, port)
            withContext(Dispatchers.Main) { callback(result) }
        }
    }

    /** Conecta usando un objeto PrinterDevice */
    fun connect(device: PrinterDevice, callback: (Result<Unit>) -> Unit) = connect(device.address, device.port, callback)

    suspend fun connectSuspend(host: String, port: Int = 9100): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val s = Socket()
            s.connect(InetSocketAddress(host, port), connectTimeoutMs)
            s.soTimeout = readTimeoutMs
            socket = s
            outputStream = s.getOutputStream()
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(
                PrinterException(
                    PrinterError.CONNECTION_FAILED, "No se pudo conectar a $host:$port — ${e.message}", e
                )
            )
        } catch (e: Exception) {
            Result.failure(
                PrinterException(PrinterError.UNKNOWN, "Error inesperado: ${e.message}", e)
            )
        }
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    fun sendBytes(data: ByteArray, callback: (Result<Unit>) -> Unit) {
        scope.launch {
            val result = sendBytesSuspend(data)
            withContext(Dispatchers.Main) { callback(result) }
        }
    }

    suspend fun sendBytesSuspend(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val stream = outputStream ?: return@withContext Result.failure(
            PrinterException(PrinterError.NOT_CONNECTED, "Sin conexión activa")
        )
        try {
            stream.write(data)
            stream.flush()
            Result.success(Unit)
        } catch (e: IOException) {
            val error = PrinterException(PrinterError.WRITE_FAILED, "Error de escritura: ${e.message}", e)
            scope.launch(Dispatchers.Main) { onConnectionLost?.invoke(error) }
            Result.failure(error)
        }
    }

    // ── Formato DSL ───────────────────────────────────────────────────────────

    fun format(block: PrintBuilder.() -> Unit, callback: (Result<Unit>) -> Unit) {
        scope.launch {
            val result = formatSuspend(block)
            withContext(Dispatchers.Main) { callback(result) }
        }
    }

    suspend fun formatSuspend(block: PrintBuilder.() -> Unit): Result<Unit> {
        val builder = PrintBuilder()
        builder.block()
        return sendBytesSuspend(builder.build())
    }

    // ── Diagnóstico ───────────────────────────────────────────────────────────

    fun ping(host: String, timeout: Int = 3000, callback: (Result<Boolean>) -> Unit) {
        scope.launch {
            val result = pingSuspend(host, timeout)
            withContext(Dispatchers.Main) { callback(Result.success(result)) }
        }
    }

    suspend fun pingSuspend(host: String, timeout: Int = 3000): Boolean = withContext(Dispatchers.IO) {
        try {
            java.net.InetAddress.getByName(host).isReachable(timeout)
        } catch (e: Exception) {
            false
        }
    }

    // ── Desconexión ───────────────────────────────────────────────────────────

    fun disconnect() {
        try {
            outputStream?.close()
        } catch (_: IOException) {
        }
        try {
            socket?.close()
        } catch (_: IOException) {
        }
        outputStream = null
        socket = null
    }

    fun release() {
        disconnect(); scope.cancel()
    }
}