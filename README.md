# 🚀 NeoPrinter SDK

Una solución integral, robusta y elegante para la impresión térmica **ESC/POS** en Android. NeoPrinter SDK te permite conectar, gestionar y formatear tickets en impresoras **Bluetooth (RFCOMM/SPP)** y de **Red (TCP/IP - Wi-Fi/Ethernet)** con un mínimo esfuerzo y un código excepcionalmente limpio.

---

## 📑 Tabla de Contenidos
- [📦 Instalación](#-instalación)
- [🛡️ Configuración de Permisos](#️-configuración-de-permisos)
- [🗂️ Gestión de Impresoras (Catálogo)](#️-gestión-de-impresoras-catálogo)
  - [🌐 Agregar una Impresora de Red (TCP/IP)](#-agregar-una-impresora-de-red-tcpip)
  - [📶 Agregar o Auto-descubrir Impresoras Bluetooth](#-agregar-o-auto-descubrir-impresoras-bluetooth)
  - [⚙️ Operaciones CRUD del Catálogo](#️-operaciones-crud-del-catálogo)
- [🔄 Flujo de Interacción y Arquitectura](#-flujo-de-interacción-y-arquitectura)
- [🚀 Inicialización y Selección de Impresoras](#-inicialización-y-selección-de-impresoras)
- [🧾 El Motor de Impresión (PrintBuilder)](#-el-motor-de-impresión-printbuilder)
- [🛠️ Tabla Completa de Métodos del Builder](#️-tabla-completa-de-métodos-del-builder)
- [📡 Diagnósticos de Red](#-diagnósticos-de-red)
- [⚠️ Manejo de Errores y Reintentos](#️-manejo-de-errores-y-reintentos)
- [🔮 Roadmap: Funcionalidades Faltantes y Oportunidades de Mejora](#-roadmap-funcionalidades-faltantes-y-oportunidades-de-mejora)
- [📜 Licencia](#-licencia)

---

## 📦 Instalación

Añade el módulo SDK a tu proyecto de Android dentro del archivo `build.gradle.kts` de tu aplicación (`:app`):

```kotlin
dependencies {
    implementation(project(":NeoPrinterSDK"))
}
```

---

## 🛡️ Configuración de Permisos

NeoPrinter simplifica la gestión de permisos en tiempo de ejecución para Bluetooth y Ubicación, adaptándose automáticamente a Android 12+ (API 31+).

### 📝 Manifest (`AndroidManifest.xml`)
Agrega los siguientes permisos en el archivo `AndroidManifest.xml` de tu aplicación:

```xml
<!-- Permisos de Red -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Permisos de Bluetooth -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<!-- Requeridos para Android 12 (API 31) y superiores -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<!-- Requerido para descubrimiento Bluetooth en Android 11 o inferior -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### 🔐 Petición en Tiempo de Ejecución
Utiliza `PrinterPermissionHelper` dentro de tu `Activity` para solicitar los permisos requeridos antes de buscar o conectar impresoras.

#### **Kotlin**
```kotlin
val permissionHelper = PrinterPermissionHelper(this)

permissionHelper.checkAndRequest(
    onGranted = {
        // Permisos concedidos, abrir selector o conectar
        manager.pickAndConnect(this) { connection ->
            printTicket(connection)
        }
    },
    onDenied = {
        Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()
    }
)
```

#### **Java**
```java
PrinterPermissionHelper permissionHelper = new PrinterPermissionHelper(this);

permissionHelper.checkAndRequest(
    () -> {
        manager.pickAndConnect(this, error -> {}, connection -> {
            printTicket(connection);
        });
        return kotlin.Unit.INSTANCE;
    },
    () -> {
        Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show();
        return kotlin.Unit.INSTANCE;
    }
);
```

---

## 🗂️ Gestión de Impresoras (Catálogo)

El catálogo (`PrinterCatalog`) es el encargado de almacenar y recuperar persistentemente las impresoras configuradas mediante `SharedPreferences`.

### 🌐 Agregar una Impresora de Red (TCP/IP)

Para conectar con una impresora de red (Wi-Fi o cable Ethernet LAN), debes especificar su **dirección IP** y el **puerto** (por defecto `9100` en la mayoría de impresoras térmicas ESC/POS).

#### **Ejemplo de Registro de Impresora de Red:**

#### **Kotlin**
```kotlin
val catalog = PrinterCatalog(context)

// Crear objeto de impresora de red
val networkPrinter = PrinterDevice.network(
    label = "Cocina Principal",
    ip = "192.168.1.200",
    port = 9100, // Opcional, valor por defecto: 9100
    brand = "Epson",
    model = "TM-T20III"
)

// Guardar o actualizar en el catálogo
catalog.save(networkPrinter)
```

#### **Java**
```java
PrinterCatalog catalog = new PrinterCatalog(context);

PrinterDevice networkPrinter = PrinterDevice.Companion.network(
    "Cocina Principal", // label
    "192.168.1.200",    // ip
    9100,               // port
    "Epson",            // brand
    "TM-T20III"         // model
);

catalog.save(networkPrinter);
```

---

### 📶 Agregar o Auto-descubrir Impresoras Bluetooth

Para impresoras Bluetooth, puedes agregarlas manualmente proporcionando su dirección MAC o utilizar el método de auto-descubrimiento para importar las impresoras vinculadas al dispositivo Android.

#### **1. Agregar Manualmente:**
```kotlin
val btPrinter = PrinterDevice.bluetooth(
    label = "Caja Portátil 1",
    macAddress = "00:11:22:33:44:55",
    brand = "Zebra",
    model = "iMZ320"
)
catalog.save(btPrinter)
```

#### **2. Auto-descubrir e importar impresoras BT emparejadas:**
`PrinterCatalog` incluye la función `autoAddPairedBluetoothPrinters(context)` que analiza los dispositivos Bluetooth emparejados en el sistema operativo, filtra los de clase de impresión (`IMAGING`) y los registra automáticamente en el catálogo si aún no existen.

```kotlin
// Importa automáticamente las impresoras BT vinculadas
catalog.autoAddPairedBluetoothPrinters(context)
```

---

### ⚙️ Operaciones CRUD del Catálogo

| Método | Descripción |
| :--- | :--- |
| `save(device)` | Guarda o actualiza una impresora en el catálogo. |
| `add(device)` | Agrega una impresora (devuelve `false` si ya existe su ID). |
| `remove(id)` | Elimina una impresora por su ID único. |
| `getAll()` | Obtiene la lista completa de `PrinterDevice`. |
| `getByType(PrinterType)` | Filtra las impresoras por tipo (`PrinterType.BLUETOOTH` o `PrinterType.NETWORK`). |
| `getGrouped()` | Obtiene un mapa ordenado agrupando las impresoras por categorías ("RED" y "BT"). |
| `autoAddPairedBluetoothPrinters(context)` | Escanea y registra impresoras Bluetooth vinculadas en Android. |
| `clear()` | Elimina todas las impresoras del catálogo. |

---

## 🔄 Flujo de Interacción y Arquitectura

NeoPrinter SDK utiliza una arquitectura desacoplada para separar el almacenamiento del catálogo, la selección en la interfaz de usuario, los controladores físicos de comunicación y el generador de comandos ESC/POS.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Aplicación Cliente                            │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                        1. Solicitar Permisos
                                     ▼
                   ┌───────────────────────────────────┐
                   │     PrinterPermissionHelper       │
                   └─────────────────┬─────────────────┘
                                     │ Permisos concedidos
                                     ▼
                   ┌───────────────────────────────────┐
                   │          PrinterManager           │
                   └───────┬───────────────────┬───────┘
                           │                   │
             2. Seleccionar│                   │3. Conectar directamente
              vía Diálogo  ▼                   ▼
      ┌───────────────────────────┐   ┌──────────────────────────┐
      │   PrinterPickerDialog     │   │     connectDevice()      │
      └────────────┬──────────────┘   └────────────┬─────────────┘
                   │                               │
                   │  Carga datos desde            │
                   ▼                               │
      ┌───────────────────────────┐                │
      │      PrinterCatalog       │                │
      └───────────────────────────┘                │
                                                   │
                                     ┌─────────────┴─────────────┐
                                     ▼                           ▼
                        ┌────────────────────────┐  ┌────────────────────────┐
                        │    BluetoothPrinter    │  │     NetworkPrinter     │
                        │   (Socket RFCOMM/SPP)  │  │      (Socket TCP)      │
                        └────────────┬───────────┘  └────────────┬───────────┘
                                     │                           │
                                     └─────────────┬─────────────┘
                                                   │
                                                   ▼
                                     ┌──────────────────────────┐
                                     │    PrinterConnection     │
                                     └─────────────┬────────────┘
                                                   │
                                                   ▼
                                     ┌──────────────────────────┐
                                     │       PrintBuilder       │
                                     │  (Generador de ESC/POS)  │
                                     └─────────────┬────────────┘
                                                   │
                                                   ▼
                                     ┌──────────────────────────┐
                                     │    Impresora Física      │
                                     └──────────────────────────┘
```

### 💬 Descripción del Flujo:
1. **Validación de Permisos:** Se solicita la aprobación de permisos mediante `PrinterPermissionHelper`.
2. **Selección/Catálogo:** Se consulta `PrinterCatalog` o se despliega `PrinterPickerDialog` para presentar al usuario las impresoras disponibles ordenadas por categorías (Red o Bluetooth).
3. **Conexión:** `PrinterManager` inicializa el controlador correspondiente (`NetworkPrinter` o `BluetoothPrinter`), establece la conexión Socket en un hilo secundario (`Dispatchers.IO`) y retorna una instancia unificada de `PrinterConnection`.
4. **Construcción e Impresión:** Se utiliza el DSL `PrintBuilder` para armar el búfer de comandos ESC/POS. Durante el proceso de envío se muestra un diálogo modal `PrintingProgressDialog`.
5. **Cierre de Conexión:** Una vez completada la transferencia de bytes o en caso de error, se liberan o cierran los recursos.

---

## 🚀 Inicialización y Selección de Impresoras

El punto de entrada principal para interactuar con la librería es `PrinterManager`.

### 1. Inicialización
```kotlin
val manager = PrinterManager(context)
```

### 2. Seleccionar y Conectar con Selector UI (`pickAndConnect`)
Si hay varias impresoras en el catálogo, `pickAndConnect` muestra un diálogo estilizado (`PrinterPickerDialog`) para que el usuario elija. Si solo hay una, se conecta automáticamente.

#### **Kotlin**
```kotlin
manager.pickAndConnect(
    context = this,
    onError = { exception ->
        Log.e("Printer", "Error de conexión: ${exception.message}")
    },
    onReady = { connection ->
        // Conexión exitosa, proceder a imprimir
        imprimirComprobante(connection)
    }
)
```

#### **Java**
```java
manager.pickAndConnect(
    this,
    error -> Log.e("Printer", "Error: " + error.getMessage()),
    connection -> {
        imprimirComprobante(connection);
    }
);
```

### 3. Conectar a una Impresora Específica (`connectDevice`)
Si ya conoces la impresora a la que deseas conectar sin mostrar el selector UI:

```kotlin
val device = catalog.getAll().first { it.type == PrinterType.NETWORK }

manager.connectDevice(
    device = device,
    onError = { error -> /* Manejar error */ },
    onReady = { connection -> /* Utilizar conexión */ }
)
```

---

## 🧾 El Motor de Impresión (PrintBuilder)

`PrintBuilder` es un potente motor DSL que construye secuencias de comandos ESC/POS en formato de arreglo de bytes.

### ✨ Ejemplo Completo de Ticket

#### **Kotlin (DSL)**
```kotlin
connection.format(context) {
    // Configuración general del ticket
    paperWidth = 48 // 48 caracteres para papel de 80mm (32 para 58mm)
    
    reset()
    align(Align.CENTER)
    bold(true).size(TextSize.LARGE).textLine("RESTAURANTE NEO")
    bold(false).size(TextSize.NORMAL).textLine("Av. Principal #123, Ciudad")
    textLine("TEL: (555) 019-2831")
    divider('=')

    align(Align.LEFT)
    textLine("Mesa: 05               Mesero: Carlos")
    textLine("Fecha: 28/03/2026      Hora: 14:30")
    divider()

    // Encabezado de items
    row3("CANT/DESC", "P.UNIT", "TOTAL")
    divider('-')

    // Filas del pedido
    row3("1x Hamburguesa", "85.00", "85.00")
    row3("2x Refrescos", "25.00", "50.00")
    row3("1x Papas Fritas", "39.00", "39.00")
    doubleDivider()

    // Totales
    align(Align.RIGHT)
    row("SUBTOTAL", "$174.00")
    row("IVA (16%)", "$27.84")
    bold(true).size(TextSize.DOUBLE_HEIGHT)
    row("TOTAL", "$201.84")
    bold(false).size(TextSize.NORMAL)

    divider()
    align(Align.CENTER)
    feed(1)
    
    // Código QR para comprobante fiscal
    qrCode("https://facturacion.neoprinter.com/ticket?id=849201", size = 5)
    feed(1)
    
    // Código de barras
    barcode128("84920192831")
    feed(2)
    
    // Abrir cajón de dinero y cortar papel
    openDrawer()
    cut()
}
```

#### **Java (Builder Pattern)**
```java
connection.format(context, builder -> {
    builder.setPaperWidth(48);
    builder.reset().align(Align.CENTER);
    builder.bold(true).size(TextSize.LARGE).textLine("RESTAURANTE NEO");
    builder.bold(false).size(TextSize.NORMAL).textLine("Av. Principal #123, Ciudad");
    builder.divider('=');

    builder.row("SUBTOTAL", "$174.00");
    builder.row("IVA (16%)", "$27.84");
    builder.doubleDivider();
    builder.row("TOTAL", "$201.84");

    builder.feed(2)
           .qrCode("https://facturacion.neoprinter.com/ticket?id=849201", 5, 49)
           .barcode128("84920192831", 60, 2, 2)
           .openDrawer()
           .cut();
}, (isSuccess, error) -> {
    if (isSuccess) {
        Log.d("Printer", "Impresión exitosa");
    } else {
        Log.e("Printer", "Error: " + error);
    }
});
```

---

## 🛠️ Tabla Completa de Métodos del Builder

| Método | Argumentos | Descripción |
| :--- | :--- | :--- |
| **`paperWidth`** | `Int` | Ajusta el ancho de papel en número de caracteres (ej. `32` para 58mm, `48` para 80mm). |
| **`charset`** | `Charset` | Codificación de caracteres (por defecto `ISO_8859_1`). |
| **`text(val)`** | `String` | Escribe texto plano en el búfer. |
| **`textLine(val)`** | `String` | Escribe texto seguido de un salto de línea (`\n`). |
| **`align(mode)`** | `Align` | Alineación del texto: `Align.LEFT`, `Align.CENTER`, `Align.RIGHT`. |
| **`bold(enabled)`** | `Boolean` | Activa (`true`) o desactiva (`false`) el modo en negrita. |
| **`underline(mode)`**| `Int` | Estilo de subrayado: `0` (desactivado), `1` (normal), `2` (grueso). |
| **`size(textSize)`** | `TextSize` | Tamaños de fuente: `NORMAL`, `DOUBLE_HEIGHT`, `DOUBLE_WIDTH`, `LARGE`. |
| **`invert(enabled)`** | `Boolean` | Imprime texto invertido (blanco sobre fondo negro). |
| **`printDensity()`** | `heating, interval` | Modifica el tiempo de calentamiento del cabezal térmico (densidad de impresión). |
| **`divider(char)`** | `Char` | Imprime una línea separadora del ancho del papel con el carácter especificado (`-` por defecto). |
| **`doubleDivider()`**| - | Imprime una línea separadora doble con el carácter `=`. |
| **`feed(lines)`** | `Int` | Avanza el papel la cantidad de líneas indicadas. |
| **`lineSpacing(n)`** | `Int` | Configura el espaciado vertical personalizado entre líneas. |
| **`lineSpacingDefault()`**| - | Restaura el espaciado entre líneas por defecto del fabricante. |
| **`row(left, right)`**| `left, right` | Fila con dos columnas alineadas a los extremos izquierdo y derecho. |
| **`row3(c1,c2,c3)`** | `c1, c2, c3` | Fila con tres columnas equidistantes. |
| **`barcode128(data)`**| `data, h, w, hri` | Imprime código de barras **Code 128** con formateo dinámico. |
| **`barcodeEAN13(data)`**| `data, h, w, hri` | Imprime código de barras **EAN-13**. |
| **`barcode39(data)`** | `data, h, w, hri` | Imprime código de barras **Code 39**. |
| **`qrCode(data)`** | `data, size, ecc` | Genera e imprime un código QR nativo ESC/POS (tamaño módulo de 1 a 8). |
| **`bitmap(bitmap)`** | `Bitmap` | Imprime una imagen Android ajustándola mediante el algoritmo de tramado difuso **Floyd–Steinberg**. |
| **`cut()`** | - | Realiza un corte total del papel. |
| **`partialCut()`** | - | Realiza un corte parcial del papel. |
| **`openDrawer()`** | - | Envía un pulso eléctrico para abrir el cajón monedero. |
| **`reset()`** | - | Restaura los comandos iniciales y estilos predeterminados de la impresora. |
| **`rawBytes(bytes)`** | `ByteArray` | Envía secuencias arbitrarias de bytes directo a la impresora. |

---

## 📡 Diagnósticos de Red

Valida la conectividad IP y latencia de impresoras de red mediante la utilidad `ping`.

#### **Kotlin**
```kotlin
manager.ping("192.168.1.200", timeout = 3000) { isReachable ->
    if (isReachable) {
        println("La impresora de red responde adecuadamente")
    } else {
        println("No se pudo alcanzar la dirección IP")
    }
}
```

#### **Java**
```java
manager.ping("192.168.1.200", 3000, isReachable -> {
    if (isReachable) {
        System.out.println("Impresora en línea");
    } else {
        System.out.println("Impresora fuera de línea");
    }
});
```

---

## ⚠️ Manejo de Errores y Reintentos

Las operaciones de conexión y envío retornan excepciones de tipo `PrinterException`, permitiendo identificar el tipo exacto de error mediante `PrinterError`.

```kotlin
try {
    // Operación de impresión
} catch (e: PrinterException) {
    when (e.error) {
        PrinterError.CONNECTION_FAILED -> Log.e("Printer", "Fallo al conectar con el socket IP/MAC")
        PrinterError.CONNECTION_TIMEOUT -> Log.e("Printer", "Tiempo de espera agotado")
        PrinterError.PERMISSION_DENIED -> Log.e("Printer", "Faltan permisos de Bluetooth")
        PrinterError.NOT_CONNECTED -> Log.e("Printer", "Intento de impresión sin conexión activa")
        PrinterError.WRITE_FAILED -> Log.e("Printer", "Error al escribir bytes en el socket")
        else -> Log.e("Printer", "Error no determinado: ${e.message}")
    }

    // Verificar si la falla es reintentable
    if (e.isRetryable) {
        // Ejecutar lógica de reintento automático
    }
}
```


---
Hecho con ❤️ por **[NandoDev1997](https://github.com/NandoDev1997)**
