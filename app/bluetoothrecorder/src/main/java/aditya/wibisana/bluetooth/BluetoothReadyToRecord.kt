package aditya.wibisana.bluetooth

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BluetoothReadyToRecord(
    private val audioManager: AudioManager,
    private val bluetoothConnectionDeviceListener: BluetoothConnectionDeviceListener
) {
    private val worker = CoroutineScope(Dispatchers.IO)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    init {
        updateReadyState()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.addOnModeChangedListener({}, {
                updateReadyState()
            })
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.addOnCommunicationDeviceChangedListener({},{
                updateReadyState()
            })
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            worker.launch {
                bluetoothConnectionDeviceListener.status.collect {
                    while(it == BluetoothConnectionDeviceListener.ConnectionStatus.CONNECTED) {
                        updateReadyState()
                        delay(1000)
                    }
                    if (it == BluetoothConnectionDeviceListener.ConnectionStatus.DISCONNECTED) {
                        _isReady.value = false
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun updateReadyState() {
        _isReady.value = audioManager.isBluetoothScoOn && audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
    }
}