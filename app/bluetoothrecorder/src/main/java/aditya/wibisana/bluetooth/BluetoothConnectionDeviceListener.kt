package aditya.wibisana.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass.Device
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class BluetoothConnectionDeviceListener(
    context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
    private val bluetoothPermission: BluetoothPermission
) {
    private var _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status = _status.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTED
    }

    private val supportedProfiles = mutableListOf(
        BluetoothProfile.HEADSET,
        BluetoothProfile.A2DP,
    )

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return // Handle null action gracefully
            @Suppress("DEPRECATION")
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            Timber.v("bluetoothReceiver action: $action  device:${device?.name} deviceClass:${device?.bluetoothClass}")
            // we can check BT class here
            // example:
            // device.bluetoothClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Device found
                    // Handle the discovered device here, e.g., add it to a list
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    // Device is now connected
                    // Handle the connected device here, e.g., start data transfer
                    updateConnectionState(device, ConnectionStatus.CONNECTED)
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // Done searching
                    // Handle the end of the discovery process here, e.g., update UI
                }
                BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
                    // Device is about to disconnect
                    // Prepare for disconnection here, e.g., save any pending data
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    // Device has disconnected
                    // Handle the disconnected device here, e.g., clean up resources
                    updateConnectionState(device, ConnectionStatus.DISCONNECTED)
                }
            }
        }
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF,
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        _status.value = ConnectionStatus.DISCONNECTED
                    }
                    BluetoothAdapter.STATE_ON,
                    BluetoothAdapter.STATE_TURNING_ON -> {
                        updateConnectionState()
                    }
                }
            }
        }
    }

    init {
        Timber.v("Initialized")
        val bluetoothReceiverIntentFilter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED).apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(bluetoothReceiver, bluetoothReceiverIntentFilter)

        val bluetoothStateIntentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, bluetoothStateIntentFilter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            supportedProfiles += BluetoothProfile.LE_AUDIO
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            supportedProfiles += BluetoothProfile.GATT
            supportedProfiles += BluetoothProfile.GATT_SERVER
        }

        updateConnectionState()

        coroutineScope.launch {
            bluetoothPermission.isPermissionGranted.collect { isGranted ->
                if (isGranted) {
                    updateConnectionState()
                } else {
                    _status.value = ConnectionStatus.DISCONNECTED
                }
            }
        }
    }


    @SuppressWarnings("MissingPermission")
    private fun updateConnectionState() {
        supportedProfiles.forEach {
            if (bluetoothAdapter.getProfileConnectionState(it) == BluetoothAdapter.STATE_CONNECTED) {
                _status.value = ConnectionStatus.CONNECTED
                return@forEach
            }
        }
        _status.value = ConnectionStatus.DISCONNECTED
    }

    private fun updateConnectionState(device: BluetoothDevice?, connectionStatus: ConnectionStatus) {
        when (device?.bluetoothClass?.deviceClass) {
            Device.AUDIO_VIDEO_HEADPHONES,
            Device.AUDIO_VIDEO_HANDSFREE,
            Device.AUDIO_VIDEO_WEARABLE_HEADSET -> {
                _status.value = connectionStatus
                return
            }
        }
    }
}