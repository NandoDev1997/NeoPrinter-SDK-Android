package com.nandodev1997.neoprintersdk

/** BluetoothPrinter - Controlador para impresoras Bluetooth ESC/POS */

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/** Maneja la conexión y envío de datos por Bluetooth */
class BluetoothPrinter(private val context: Context) {

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val DEFAULT_TIMEOUT_MS = 10_000L
        private const val DEFAULT_BUFFER_SIZE = 4096
    }

    // ── Estado interno ────────────────────────────────────────────────────────

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var connectionTimeoutMs: Long = DEFAULT_TIMEOUT_MS
    private var onConnectionLost: ((PrinterException) -> Unit)? = null

    /** Verifica si hay una conexión activa */
    val isConnected: Boolean
        get() = socket?.isConnected == true

    // ── Configuración ─────────────────────────────────────────────────────────

    /** Configura el tiempo de espera de conexión */
    fun setConnectionTimeout(ms: Long): BluetoothPrinter {
        connectionTimeoutMs = ms
        return this
    }

    /** Define qué hacer si la conexión se pierde */
    fun onConnectionLost(callback: (PrinterException) -> Unit): BluetoothPrinter {
        onConnectionLost = callback
        return this
    }

    // ── Conexión ──────────────────────────────────────────────────────────────

    /** Conecta a una impresora por su MAC */
    fun connect(address: String, callback: (Result<Unit>) -> Unit) {
        scope.launch {
            val result = connectSuspend(address)
            withContext(Dispatchers.Main) { callback(result) }
        }
    }

    /** Conecta usando un objeto BluetoothDevice */
    fun connect(device: BluetoothDevice, callback: (Result<Unit>) -> Unit) {
        connect(device.address, callback)
    }

    /** Conecta de forma suspendida */
    suspend fun connectSuspend(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()

            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(
                    PrinterException(PrinterError.BLUETOOTH_NOT_AVAILABLE, "Bluetooth no disponible en este dispositivo")
                )

            if (!adapter.isEnabled) {
                return@withContext Result.failure(
                    PrinterException(PrinterError.BLUETOOTH_DISABLED, "Bluetooth está desactivado")
                )
            }

            if (!isValidMacAddress(address)) {
                return@withContext Result.failure(
                    PrinterException(PrinterError.INVALID_ADDRESS, "Dirección MAC inválida: $address")
                )
            }

            val device = try {
                adapter.getRemoteDevice(address)
            } catch (e: IllegalArgumentException) {
                return@withContext Result.failure(
                    PrinterException(PrinterError.INVALID_ADDRESS, "Dirección MAC inválida: $address", e)
                )
            }

            val newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            adapter.cancelDiscovery()

            withTimeout(connectionTimeoutMs) {
                newSocket.connect()
            }

            socket = newSocket
            outputStream = newSocket.outputStream

            Result.success(Unit)

        } catch (e: TimeoutCancellationException) {
            Result.failure(
                PrinterException(PrinterError.CONNECTION_TIMEOUT, "Timeout al conectar con $address (${connectionTimeoutMs}ms)", e)
            )
        } catch (e: IOException) {
            Result.failure(
                PrinterException(PrinterError.CONNECTION_FAILED, "No se pudo conectar con $address: ${e.message}", e)
            )
        } catch (e: SecurityException) {
            Result.failure(
                PrinterException(PrinterError.PERMISSION_DENIED, "Permiso Bluetooth denegado. Verifica BLUETOOTH_CONNECT en Android 12+", e)
            )
        } catch (e: Exception) {
            Result.failure(
                PrinterException(PrinterError.UNKNOWN, "Error inesperado: ${e.message}", e)
            )
        }
    }

    /** Devuelve los dispositivos ya vinculados */
    fun getPairedPrinters(): Result<List<BluetoothDevice>> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return Result.failure(
                    PrinterException(PrinterError.BLUETOOTH_NOT_AVAILABLE, "Bluetooth no disponible")
                )
            if (!adapter.isEnabled) {
                return Result.failure(
                    PrinterException(PrinterError.BLUETOOTH_DISABLED, "Bluetooth está desactivado")
                )
            }
            Result.success(adapter.bondedDevices.toList())
        } catch (e: SecurityException) {
            Result.failure(
                PrinterException(PrinterError.PERMISSION_DENIED, "Permiso denegado al listar dispositivos", e)
            )
        }
    }

    // ── Escritura directa en bytes ────────────────────────────────────────────

    /** Envía bytes puros a la impresora */
    fun sendBytes(data: ByteArray, callback: (Result<Unit>) -> Unit) {
        scope.launch {
            val result = sendBytesSuspend(data)
            withContext(Dispatchers.Main) { callback(result) }
        }
    }

    /** Envía bytes de forma suspendida */
    suspend fun sendBytesSuspend(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val stream = outputStream
            ?: return@withContext Result.failure(
                PrinterException(PrinterError.NOT_CONNECTED, "No hay conexión activa con la impresora")
            )
        try {
            // Dividimos en chunks (ej. 1024 bytes) para evitar desbordar el búfer BT (MTU)
            val chunkSize = 1024
            var offset = 0
            while (offset < data.size) {
                val end = minOf(offset + chunkSize, data.size)
                stream.write(data, offset, end - offset)
                stream.flush()
                offset = end

                // Pequeño delay de gracia entre chunks grandes para imágenes
                if (offset < data.size) {
                    kotlinx.coroutines.delay(100)
                }
            }
            Result.success(Unit)
        } catch (e: IOException) {
            val error = PrinterException(PrinterError.WRITE_FAILED, "Error al escribir en la impresora: ${e.message}", e)
            scope.launch(Dispatchers.Main) { onConnectionLost?.invoke(error) }
            Result.failure(error)
        }
    }

    /** Envía varios bloques de bytes */
    suspend fun sendBytesSuspend(vararg chunks: ByteArray): Result<Unit> {
        for (chunk in chunks) {
            val result = sendBytesSuspend(chunk)
            if (result.isFailure) return result
        }
        return Result.success(Unit)
    }

    // ── API de formato (DSL) ──────────────────────────────────────────────────

    /** Imprime usando el constructor de formato (DSL) */
    fun format(block: PrintBuilder.() -> Unit, callback: (Result<Unit>) -> Unit) {
        scope.launch {
            val result = formatSuspend(block)
            withContext(Dispatchers.Main) { callback(result) }
        }
    }

    /** Formato suspendido */
    suspend fun formatSuspend(block: PrintBuilder.() -> Unit): Result<Unit> {
        val builder = PrintBuilder()
        builder.block()
        return sendBytesSuspend(builder.build())
    }

    // ── Desconexión ───────────────────────────────────────────────────────────

    /** Cierra la conexión */
    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (_: IOException) {
        } finally {
            outputStream = null
            socket = null
        }
    }

    /** Libera recursos y cancela procesos */
    fun release() {
        disconnect()
        scope.cancel()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isValidMacAddress(address: String): Boolean =
        address.matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$"))
}