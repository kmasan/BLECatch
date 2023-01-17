package com.b22706.blecatch

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.b22706.blecatch.ui.theme.BLECatchTheme
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import kotlin.experimental.and


class MainActivity : ComponentActivity(), EasyPermissions.PermissionCallbacks {
    private lateinit var adapter: BluetoothAdapter
    lateinit var scanner: BluetoothLeScanner

    var scannerBoolean = false

    private val scanCallback = object: ScanCallback(){
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            // スキャン結果が返ってきます
            // このメソッドかonBatchScanResultsのいずれかが呼び出されます。
            // 通常はこちらが呼び出されます。
            if (result == null) {
                return
            }
            val bDevice: BluetoothDevice = result.device
            val record: ScanRecord? = result.scanRecord
            val bytes = if (record != null) record.bytes else ByteArray(0)
            Log.d("Beacon", "${bDevice.address}, $bytes")
//            Log.d("Beacon", "address:${bDevice.address}"
//                .plus(", UUID:${getUUID(bytes)}")
//                .plus(", Major:${getMajor(bytes)}")
//                .plus(", Minor:${getMinor(bytes)}")
//                .plus(", Power:${bytes[29]}")
//                .plus(", RSSI:${result.rssi}"))
//            textView.append(
//                "UUID: " + getUUID(bytes) + " major: " + getMajor(bytes) + " minor: " + getMinor(
//                    bytes
//                ) +
//                        " Power: " + bytes[29].toString() + " Rssi: " + java.lang.String.valueOf(
//                    result.getRssi()
//                ) + "\n"
//            )
        }

        private fun getUUID(scanRecord: ByteArray): String {
            return (IntToHex2(scanRecord[9] and 0xff.toByte())
                    + IntToHex2(scanRecord[10] and 0xff.toByte())
                    + IntToHex2(scanRecord[11] and 0xff.toByte())
                    + IntToHex2(scanRecord[12] and 0xff.toByte())
                    + "-"
                    + IntToHex2(scanRecord[13] and 0xff.toByte())
                    + IntToHex2(scanRecord[14] and 0xff.toByte())
                    + "-"
                    + IntToHex2(scanRecord[15] and 0xff.toByte())
                    + IntToHex2(scanRecord[16] and 0xff.toByte())
                    + "-"
                    + IntToHex2(scanRecord[17] and 0xff.toByte())
                    + IntToHex2(scanRecord[18] and 0xff.toByte())
                    + "-"
                    + IntToHex2(scanRecord[19] and 0xff.toByte())
                    + IntToHex2(scanRecord[20] and 0xff.toByte())
                    + IntToHex2(scanRecord[21] and 0xff.toByte())
                    + IntToHex2(scanRecord[22] and 0xff.toByte())
                    + IntToHex2(scanRecord[23] and 0xff.toByte())
                    + IntToHex2(scanRecord[24] and 0xff.toByte()))
        }

        private fun getMajor(scanRecord: ByteArray): String {
            val hexMajor = IntToHex2(scanRecord[25] and 0xff.toByte()) +
                    IntToHex2(scanRecord[26] and 0xff.toByte())
            return hexMajor.toInt(16).toString()
        }

        private fun getMinor(scanRecord: ByteArray): String {
            val hexMinor = IntToHex2(scanRecord[27] and 0xff.toByte()) +
                    IntToHex2(scanRecord[28] and 0xff.toByte())
            return hexMinor.toInt(16).toString()
        }

        private fun IntToHex2(i: Byte): String {
            val hex_2 = charArrayOf(
//                Character.forDigit((i shl 4) and 0x0f, 16),
//                Character.forDigit(i and 0x0f, 16)
            )
            val hex_2_str = String(hex_2)
            return hex_2_str.uppercase(Locale.getDefault())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BLECatchTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting("Android")
                }
            }
        }

        //BLE対応端末かどうかを調べる。対応していない場合はメッセージを出して終了
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "R.string.ble_not_supported", Toast.LENGTH_SHORT).show()
            finish()
        }

        val permissions = when{
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            else -> arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        if (!EasyPermissions.hasPermissions(this, *permissions)) {
            // パーミッションが許可されていない時の処理
            EasyPermissions.requestPermissions(this, "パーミッションに関する説明", 0, *permissions)
            return
        }

        //Bluetoothアダプターを初期化する
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = manager.adapter
        scanner = adapter.bluetoothLeScanner
    }

    override fun onPermissionsGranted(requestCode: Int, list: List<String>) {
        // ユーザーの許可が得られたときに呼び出される
        //recreate()
    }

    override fun onPermissionsDenied(requestCode: Int, list: List<String>) {
        // ユーザーの許可が得られなかったときに呼び出される。
    }

    override fun onResume() {
        super.onResume()
        //スキャンの開始
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            scanner.startScan(scanCallback)
        }
    }

    override fun onPause() {
        super.onPause()
        //スキャンを停止
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            scanner.stopScan(scanCallback)
        }
    }

    private fun bleScan(){
        if(scannerBoolean){
            //scanner.stopScan(scanCallback)
            setContent { ButtonText(name = "BLE scan Off") }
        }else{
            //scanner.startScan(scanCallback)
            setContent { ButtonText(name = "BLE scan On") }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}


@Composable
fun ButtonText(name: String){
    Text(text = name)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Column{
        Greeting("Android")
        Button(onClick = {}){
            ButtonText(name = "BLE scan Off")
        }
    }
}