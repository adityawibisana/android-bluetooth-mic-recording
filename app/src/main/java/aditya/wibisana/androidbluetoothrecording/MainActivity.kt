package aditya.wibisana.androidbluetoothrecording

import aditya.wibisana.androidbluetoothrecording.ui.theme.AndroidBluetoothRecordingTheme
import aditya.wibisana.bluetooth.BluetoothConnectionDeviceListener
import aditya.wibisana.bluetooth.BluetoothPermission
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val bluetoothPermission = BluetoothPermission()
        bluetoothPermission.updateState(this.applicationContext)
        val bluetoothListener = BluetoothConnectionDeviceListener(
            context = applicationContext,
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(),
            bluetoothPermission = bluetoothPermission
        )

        setContent {
            AndroidBluetoothRecordingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DeviceStatus(
                        status = bluetoothListener.status,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceStatus(
    status: StateFlow<BluetoothConnectionDeviceListener.ConnectionStatus>,
    modifier: Modifier = Modifier
) {
    val currentStatus = status.collectAsState()
    Text(
        text = "Device is: ${currentStatus.value.name}!",
        modifier = modifier
    )
}