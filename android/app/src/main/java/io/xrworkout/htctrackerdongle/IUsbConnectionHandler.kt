package io.xrworkout.htctrackerdongle

interface IUsbConnectionHandler {
    fun onDeviceConnected()
    fun onDeviceDisconnected()
    fun onDeviceNotFound()
    fun onDevicePermissionDenied()
}