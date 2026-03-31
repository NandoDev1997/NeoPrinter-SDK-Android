# 🚀 NeoPrinter SDK

Una solución integral, elegante y sencilla para la impresión térmica ESC/POS en Android. NeoPrinter te permite conectar y formatear tickets en impresoras **Bluetooth** y **Red (TCP/IP)** con un mínimo esfuerzo y un código excepcionalmente limpio.

---

## 📑 Tabla de Contenidos
- [Instalación](#-instalación)
- [Configuración de Permisos](#-configuración-de-permisos)
- [Inicialización](#-inicialización)
- [Conexión y Descubrimiento](#-conexión-y-descubrimiento)
- [El Motor de Impresión (PrintBuilder)](#-el-motor-de-impresión-printbuilder)
- [Componentes del Builder](#-componentes-del-builder)
- [Diagnósticos de Red](#-diagnósticos-de-red)
- [Licencia](#-licencia)

---

## 📦 Instalación

Añade el módulo a tu proyecto de Android:

```gradle
implementation(project(":neoprinter"))
```

---

## 🛡️ Configuración de Permisos

NeoPrinter maneja la complejidad de los permisos de Bluetooth y Ubicación por ti, especialmente en Android 12+.

### 📝 Manifest (`AndroidManifest.xml`)
```xml
<!-- Bluetooth & Red -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### 🔐 Petición en Tiempo de Ejecución

#### **Kotlin**
```kotlin
val permissionHelper = PrinterPermissionHelper(this)

permissionHelper.checkAndRequest(
    onGranted = { openPicker() },
    onDenied = { /* Manejar error */ }
)
```

#### **Java**
```java
PrinterPermissionHelper permissionHelper = new PrinterPermissionHelper(this);

permissionHelper.checkAndRequest(
    () -> { openPicker(); return kotlin.Unit.INSTANCE; },
    () -> { /* error */ return kotlin.Unit.INSTANCE; }
);
```

---

## 🚀 Inicialización

El corazón de la librería es el `PrinterManager`, el cual orquesta las conexiones y el catálogo de impresoras.

#### **Kotlin**
```kotlin
val manager = PrinterManager(this)
```

#### **Java**
```java
PrinterManager manager = new PrinterManager(this);
```

---

## 🔍 Conexión y Descubrimiento

### 📱 Selector de Impresoras (UI)
Muestra un diálogo elegante para buscar o seleccionar impresoras del catálogo guardado.

#### **Kotlin**
```kotlin
manager.pickAndConnect(this) { connection ->
    printTicket(connection)
}
```

#### **Java**
```java
manager.pickAndConnect(this, error -> { /* error */ }, connection -> {
    printTicket(connection);
});
```

---

## 🧾 El Motor de Impresión (PrintBuilder)

### ✨ Ejemplo de Ticket Completo

#### **Kotlin (DSL)**
```kotlin
connection.format(this) {
    reset().align(Align.CENTER)
    bold(true).size(TextSize.LARGE).text("NEO PRINTER\n")
    bold(false).text("Ticket de Prueba\n")
    divider()
    row("SUBTOTAL", "$150.00")
    row("IVA (16%)", "$24.00")
    doubleDivider()
    row("TOTAL", "$174.00")
    feed(2).qrCode("https://nandodev.com").cut()
}
```

#### **Java (Builder Pattern)**
```java
connection.format(this, builder -> {
    builder.reset().align(Align.CENTER)
           .bold(true).size(TextSize.LARGE).text("NEO PRINTER\n")
           .bold(false).text("Ticket de Prueba\n")
           .divider()
           .row("SUBTOTAL", "$150.00")
           .row("IVA (16%)", "$24.00")
           .doubleDivider()
           .row("TOTAL", "$174.00")
           .feed(2).qrCode("https://nandodev.com", 6, 49)
           .cut();
}, (success, error) -> { /* resultado */ });
```

---

## 🛠️ Componentes del Builder

| Método | Descripción |
| :--- | :--- |
| `text(value)` | Agrega texto plano al buffer. |
| `textLine(value)` | Agrega texto seguido de un salto de línea. |
| `align(Align)` | Alineación: `LEFT`, `CENTER`, `RIGHT`. |
| `bold(boolean)` | Activa o desactiva el texto en negrita. |
| `size(TextSize)` | Tamaños: `NORMAL`, `DOUBLE_WIDTH`, `DOUBLE_HEIGHT`, `LARGE`. |
| `underline(mode)` | Subrayado: `0` (off), `1` (fino), `2` (grueso). |
| `divider(char)` | Crea una línea divisora (por defecto `-`). |
| `doubleDivider()` | Crea una línea divisora usando `=`. |
| `feed(n)` | Avanza el papel `n` líneas. |
| `row(left, right)` | Fila con dos columnas alineadas a los extremos. |
| `row3(c1, c2, c3)` | Fila con tres columnas equidistantes. |
| `barcode128(data)` | Imprime código de barras Code 128 (Optimizado). |
| `qrCode(data, size)` | Imprime un código QR (tamaño 1-8). |
| `bitmap(Bitmap)` | Imprime una imagen Bitmap monocromática. |
| `cut()` | Ejecuta un corte total del papel. |
| `openDrawer()` | Envía el pulso para abrir el cajón de dinero. |
| `reset()` | Restaura la configuración inicial de la impresora. |

---

## 📡 Diagnósticos de Red

Valida la estabilidad de tu infraestructura de red con la función `ping` integrada.

#### **Kotlin**
```kotlin
manager.ping("192.168.1.100") { reachable ->
    if (reachable) println("Conectada")
}
```

#### **Java**
```java
manager.ping("192.168.1.100", 3000, reachable -> {
    if (reachable) System.out.println("Conectada");
});
```

---

## 📜 Licencia

NeoPrinter SDK se distribuye bajo la licencia **Apache License 2.0**.

```text
Copyright 2026 NandoDev1997

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---
Hecho con ❤️ por **[NandoDev1997](https://github.com/NandoDev1997)**
