package com.nandodev1997.neoprintersdk

/** Errores y excepciones de la librería */

/** Tipos de error posibles */
enum class PrinterError {
    /** El dispositivo no tiene hardware Bluetooth */
    BLUETOOTH_NOT_AVAILABLE,

    /** Bluetooth está desactivado en el dispositivo */
    BLUETOOTH_DISABLED,

    /** Dirección MAC inválida */
    INVALID_ADDRESS,

    /** No se pudo establecer la conexión */
    CONNECTION_FAILED,

    /** Timeout al intentar conectar */
    CONNECTION_TIMEOUT,

    /** Permiso BLUETOOTH_CONNECT denegado (Android 12+) */
    PERMISSION_DENIED,

    /** Se intentó escribir sin conexión activa */
    NOT_CONNECTED,

    /** Error al escribir datos en el socket */
    WRITE_FAILED,

    /** Error desconocido */
    UNKNOWN
}

/** Excepción personalizada para la impresora */
class PrinterException(
    val error: PrinterError,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /** Indica si el error permite reintentar */
    val isRetryable: Boolean
        get() = error in setOf(
            PrinterError.CONNECTION_FAILED,
            PrinterError.CONNECTION_TIMEOUT,
            PrinterError.WRITE_FAILED
        )

    override fun toString(): String =
        "PrinterException(error=$error, message=$message)"
}