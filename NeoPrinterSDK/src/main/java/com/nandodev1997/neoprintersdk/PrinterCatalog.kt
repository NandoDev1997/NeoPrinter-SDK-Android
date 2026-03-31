package com.nandodev1997.neoprintersdk

/** PrinterCatalog - Almacenamiento persistente de impresoras */

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/** Gestiona el guardado y recuperación de impresoras en SharedPreferences */
class PrinterCatalog(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /** Guarda o actualiza una impresora */
    fun save(device: PrinterDevice) {
        val list = getAll().toMutableList()
        val index = list.indexOfFirst { it.id == device.id }
        if (index >= 0) list[index] = device else list.add(device)
        persist(list)
    }

    /** Agrega una impresora (falla si el ID ya existe) */
    fun add(device: PrinterDevice): Boolean {
        if (getAll().any { it.id == device.id }) return false
        save(device)
        return true
    }

    /** Elimina una impresora por ID */
    fun remove(id: String): Boolean {
        val list = getAll().toMutableList()
        val removed = list.removeAll { it.id == id }
        if (removed) persist(list)
        return removed
    }

    /** Obtiene todas las impresoras guardadas */
    fun getAll(): List<PrinterDevice> = deserialize(prefs.getString(KEY_PRINTERS, "[]") ?: "[]")

    /** Filtra impresoras por tipo (BT o RED) */
    fun getByType(type: PrinterType): List<PrinterDevice> = getAll().filter { it.type == type }

    /** Borra todo el catálogo */
    fun clear() = prefs.edit().remove(KEY_PRINTERS).apply()

    // ── Agrupación ────────────────────────────────────────────────────────────

    /** Obtiene impresoras agrupadas por categoría */
    fun getGrouped(): LinkedHashMap<String, List<PrinterDevice>> {
        val all = getAll()
        return linkedMapOf<String, List<PrinterDevice>>().apply {
            val network = all.filter { it.type == PrinterType.NETWORK }
            val bt = all.filter { it.type == PrinterType.BLUETOOTH }
            if (network.isNotEmpty()) put(CATEGORY_NETWORK, network)
            if (bt.isNotEmpty()) put(CATEGORY_BT, bt)
        }
    }

    // ── Serialización JSON ────────────────────────────────────────────────────

    private fun persist(list: List<PrinterDevice>) {
        prefs.edit().putString(KEY_PRINTERS, serialize(list)).apply()
    }

    private fun serialize(list: List<PrinterDevice>): String {
        val array = JSONArray()
        list.forEach { d ->
            array.put(JSONObject().apply {
                put("id", d.id)
                put("label", d.label)
                put("brand", d.brand)
                put("model", d.model)
                put("type", d.type.name)
                put("address", d.address)
                put("port", d.port)
            })
        }
        return array.toString()
    }

    private fun deserialize(json: String): List<PrinterDevice> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                PrinterDevice(
                    id = o.optString("id"),
                    label = o.optString("label"),
                    brand = o.optString("brand"),
                    model = o.optString("model"),
                    type = PrinterType.valueOf(o.optString("type", "BLUETOOTH")),
                    address = o.optString("address"),
                    port = o.optInt("port", 9100),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun autoAddPairedBluetoothPrinters(context: Context) {
        val bt = BluetoothPrinter(context)
        try {
            val nuevosDispositivos: MutableList<BluetoothDevice> = mutableListOf()
            // Obtener dispositivos emparejados
            bt.getPairedPrinters().onSuccess { lista ->
                lista.forEach { device ->
                    val btClass = device.bluetoothClass
                    if (btClass != null) {
                        val maj = btClass.majorDeviceClass
                        val dev = btClass.deviceClass
                        // Filtrar solo impresoras de tipo IMAGING
                        if (maj == BluetoothClass.Device.Major.IMAGING && (dev == 1664 || dev == BluetoothClass.Device.Major.IMAGING)) {
                            nuevosDispositivos.add(device)
                        }
                    }
                }
                // Guardar solo los que no existen en este catálogo
                nuevosDispositivos.forEach { device ->
                    val exists = getAll().any { p -> p.address == device.address }
                    if (!exists) {
                        save(PrinterDevice.bluetooth(device.name ?: "BT", device.address))
                    }
                }
                Log.d("BT", "Impresoras BT agregadas: $nuevosDispositivos")
            }
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            Log.e("BT", "Error al agregar impresoras BT: ${e.message}")
        }
    }

    companion object {
        private const val PREFS_NAME = "escpos_printer_catalog"
        private const val KEY_PRINTERS = "printers"
        const val CATEGORY_NETWORK = "RED"
        const val CATEGORY_BT = "BT"
    }
}