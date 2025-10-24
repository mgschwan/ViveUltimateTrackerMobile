package io.xrworkout.htctrackerdongle

import android.util.Log
import io.xrworkout.htctrackerdongle.IUsbConnectionHandler

class HTCConnectionHandler : IUsbConnectionHandler {
    val TAG = "UsbConnectionHandler"
    override fun onDeviceConnected() {
        Log.d(TAG, "HID Device connected")
    }

    fun onDeviceDIsconnected() {
        Log.d(TAG, "HID Device disconnected")
    }

    override fun onDeviceNotFound() {
        Log.d(TAG, "HID Deviec not found")
    }

    override fun onDevicePermissionDenied() {
        Log.d(TAG, "HID Device permission denied")
    }
}