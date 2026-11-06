package com.example.data.serial

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Android USB-host transport for CDC/ACM, FTDI, CP210x and CH34x serial adapters. */
class UsbSerialManager(private val context: Context) {
    private val usb = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var port: UsbSerialPort? = null

    fun devices(): List<UsbDevice> = UsbSerialProber.getDefaultProber().findAllDrivers(usb).map { it.device }

    fun hasPermission(device: UsbDevice): Boolean = usb.hasPermission(device)

    fun requestPermission(device: UsbDevice) {
        val intent = PendingIntent.getBroadcast(
            context, device.deviceId,
            Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        )
        usb.requestPermission(device, intent)
    }

    fun permissionReceiver(onResult: (UsbDevice?, Boolean) -> Unit): BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            val device = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            onResult(device, intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
        }
    }

    suspend fun open(device: UsbDevice, baudRate: Int) = withContext(Dispatchers.IO) {
        close()
        require(usb.hasPermission(device)) { "USB permission was not granted." }
        val driver = UsbSerialProber.getDefaultProber().findAllDrivers(usb).firstOrNull { it.device == device }
            ?: error("No supported USB-serial driver found for this device.")
        val connection = usb.openDevice(device) ?: error("Could not open USB device. Check the OTG cable and permissions.")
        val serialPort = driver.ports.firstOrNull() ?: error("USB device has no serial ports.")
        try {
            serialPort.open(connection)
            serialPort.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            port = serialPort
        } catch (error: Exception) {
            runCatching { serialPort.close() }
            connection.close()
            throw error
        }
    }

    suspend fun read(): String = withContext(Dispatchers.IO) {
        val active = port ?: return@withContext ""
        val bytes = ByteArray(512)
        val count = active.read(bytes, 100)
        if (count > 0) String(bytes, 0, count, Charsets.UTF_8) else ""
    }

    suspend fun write(text: String) = withContext(Dispatchers.IO) {
        val active = port ?: error("Serial device is not connected.")
        active.write(text.toByteArray(Charsets.UTF_8), 1000)
    }

    fun isOpen(): Boolean = port != null

    suspend fun close() = withContext(Dispatchers.IO) {
        val active = port
        port = null
        runCatching { active?.close() }
    }

    companion object { const val ACTION_USB_PERMISSION = "com.example.droidduino.USB_PERMISSION" }
}
