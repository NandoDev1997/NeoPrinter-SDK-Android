package com.nandodev1997.neoprintersdk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/** Ayuda a solicitar permisos de Bluetooth y Ubicación */
class PrinterPermissionHelper(private val activity: ComponentActivity) {

    private var onGrantedCallback: (() -> Unit)? = null
    private var onDeniedCallback: (() -> Unit)? = null

    // El Activity Result Launcher debe registrarse durante la creación o inicio del Activity
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            onGrantedCallback?.invoke()
        } else {
            onDeniedCallback?.invoke()
        }
        
        // Limpiamos referencias para evitar fugas de memoria (memory leaks)
        onGrantedCallback = null
        onDeniedCallback = null
    }

        /** Verifica y solicita permisos si es necesario */
    fun checkAndRequest(onGranted: () -> Unit, onDenied: () -> Unit) {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31) en adelante
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            // Versiones anteriores a Android 12
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        val alreadyHasPermissions = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (alreadyHasPermissions) {
            onGranted()
        } else {
            this.onGrantedCallback = onGranted
            this.onDeniedCallback = onDenied
            launcher.launch(requiredPermissions)
        }
    }
}
