package aditya.wibisana.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return // Handle null action gracefully
            @Suppress("DEPRECATION")
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
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
                    updateConnectionState()
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
                    updateConnectionState()
                }
            }
        }
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> _status.value = ConnectionStatus.DISCONNECTED
                    BluetoothAdapter.STATE_TURNING_OFF -> {}
                    BluetoothAdapter.STATE_ON -> {}
                    BluetoothAdapter.STATE_TURNING_ON -> {}
                }
            }
        }
    }

    init {
        val bluetoothReceiverIntentFilter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED).apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(bluetoothReceiver, bluetoothReceiverIntentFilter)

        val bluetoothStateIntentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, bluetoothStateIntentFilter)

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
        if (bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothAdapter.STATE_CONNECTED
            || bluetoothAdapter.getProfileConnectionState(BluetoothProfile.LE_AUDIO) == BluetoothAdapter.STATE_CONNECTED
            || bluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothAdapter.STATE_CONNECTED
            || bluetoothAdapter.getProfileConnectionState(BluetoothProfile.GATT) == BluetoothAdapter.STATE_CONNECTED
            || bluetoothAdapter.getProfileConnectionState(BluetoothProfile.GATT_SERVER) == BluetoothAdapter.STATE_CONNECTED
            ) {
            _status.value = ConnectionStatus.CONNECTED
        } else {
            _status.value = ConnectionStatus.DISCONNECTED
        }
    }
}