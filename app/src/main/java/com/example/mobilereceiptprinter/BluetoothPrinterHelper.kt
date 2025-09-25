package com.example.mobilereceiptprinter

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothPrinterHelper(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    fun getPairedDevices(): Set<BluetoothDevice>? {
        return bluetoothAdapter?.bondedDevices
    }

    fun connectToDevice(device: BluetoothDevice): Boolean {
        return try {
            val uuid = device.uuids?.firstOrNull()?.uuid ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()
            outputStream = socket?.outputStream
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun printText(text: String): Boolean {
        return try {
            val bytes = convertEscPosText(text)
            outputStream?.write(bytes)
            outputStream?.flush()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun convertEscPosText(text: String): ByteArray {
        // Convert ESC/POS unicode escape sequences to actual bytes
        var processedText = text

        // ESC ! 48 (0x30) - Double height, double width, bold
        processedText = processedText.replace("\\\\u001B\\\\u0021\\\\u0030", "\u001B!\u0030")

        // ESC ! 0 - Reset to normal
        processedText = processedText.replace("\\\\u001B\\\\u0021\\\\u0000", "\u001B!\u0000")

        return processedText.toByteArray(Charsets.ISO_8859_1)
    }

    fun closeConnection() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
