package com.nandodev1997.neoprintersdk

/** PrinterDevice - Modelos de datos para impresoras */

import java.util.UUID

/** Tipos de conexión soportados */
enum class PrinterType {
    BLUETOOTH, NETWORK
}

/** Representa una impresora en el catálogo con sus propiedades */
data class PrinterDevice(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val brand: String = "",
    val model: String = "",
    val type: PrinterType,
    val address: String,           // MAC o IP
    val port: Int = 9100,          // solo relevante para NETWORK
) {
    /** Nombre para mostrar en la interfaz */
    val displayName: String
        get() = if (label.isNotBlank()) label else if (brand.isNotBlank()) "$brand $model".trim() else address

    /** Categoría de la impresora */
    val category: String
        get() = when (type) {
            PrinterType.BLUETOOTH -> "Bluetooth"
            PrinterType.NETWORK -> "Red"
        }

    /** Nombre del icono según tipo */
    val iconRes: String
        get() = when (type) {
            PrinterType.BLUETOOTH -> "ic_bluetooth"
            PrinterType.NETWORK -> "ic_lan"
        }

    /** Subtítulo con detalles técnicos */
    val subtitle: String
        get() = when (type) {
            PrinterType.BLUETOOTH -> buildString {
                if (brand.isNotBlank()) append("$brand $model  •  ")
                append(address)
            }

            PrinterType.NETWORK -> buildString {
                if (brand.isNotBlank()) append("$brand $model  •  ")
                append("$address:$port")
            }
        }

    companion object {
        /** Crea objeto para Bluetooth */
        fun bluetooth(
            label: String, macAddress: String, brand: String = "", model: String = "",
        ) = PrinterDevice(
            label = label,
            brand = brand,
            model = model,
            type = PrinterType.BLUETOOTH,
            address = macAddress,
        )

        /** Crea objeto para Red */
        fun network(
            label: String,
            ip: String,
            port: Int = 9100,
            brand: String = "",
            model: String = "",
        ) = PrinterDevice(
            label = label,
            brand = brand,
            model = model,
            type = PrinterType.NETWORK,
            address = ip,
            port = port,
        )
    }
}