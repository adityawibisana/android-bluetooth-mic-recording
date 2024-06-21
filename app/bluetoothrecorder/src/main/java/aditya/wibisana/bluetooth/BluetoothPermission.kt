package aditya.wibisana.bluetooth

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothPermission {
    private var _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted = _isPermissionGranted.asStateFlow()

    fun updateState(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            _isPermissionGranted.value = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            _isPermissionGranted.value = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}