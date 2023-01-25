package com.b22706.blecatch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.b22706.blecatch.ui.theme.BLECatchTheme
import org.altbeacon.beacon.*
import pub.devrel.easypermissions.EasyPermissions

class MainActivity :
    ComponentActivity(),
    EasyPermissions.PermissionCallbacks,
    RangeNotifier,
    MonitorNotifier{
    // https://qiita.com/kenmaeda51415/items/ac5a2d5a15783bbe9192
    companion object{
        private const val IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
    }
    private lateinit var beaconManager: BeaconManager
    private lateinit var mRegion: Region //検知対象の Beacon を識別するためのもの

    var scannerBoolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createSetContent()

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
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
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
        // Region("iBeacon", Identifier.parse("監視対象のUUID"), Major, Minor)
        mRegion = Region("iBeacon", null, null, null)
        beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(IBEACON_FORMAT)) // iBeaconのフォーマット指定
    }

    override fun onPermissionsGranted(requestCode: Int, list: List<String>) {
        // ユーザーの許可が得られたときに呼び出される
        //recreate()
    }

    override fun onPermissionsDenied(requestCode: Int, list: List<String>) {
        // ユーザーの許可が得られなかったときに呼び出される。
    }

    private fun bleScan(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            scannerBoolean = if (scannerBoolean) {
                //スキャンを停止
                beaconManager.stopMonitoring(mRegion)
                beaconManager.stopRangingBeacons(mRegion)
                false
            } else {
                //スキャンの開始
                beaconManager.addMonitorNotifier(this)
                beaconManager.addRangeNotifier(this)
                beaconManager.startMonitoring(mRegion)
                beaconManager.startRangingBeacons(mRegion)
                true
            }
        }
    }

    override fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>?, region: Region?) {
        //検知したBeaconの情報
        Log.d("iBeacon", "beacons.size ${beacons?.size}")
        beacons?.let {
            for (beacon in beacons) {
                Log.d("iBeacon", "UUID: ${beacon.id1}, major: ${beacon.id2}, minor: ${beacon.id3}, RSSI: ${beacon.rssi}, TxPower: ${beacon.txPower}, Distance: ${beacon.distance}")
            }
        }
    }

    override fun didEnterRegion(region: Region?) {
        //領域への入場を検知
        Log.d("iBeacon", "Enter Region ${region?.uniqueId}")
    }

    override fun didExitRegion(region: Region?) {
        //領域からの退場を検知
        Log.d("iBeacon", "Exit Region ${region?.uniqueId}")
    }

    override fun didDetermineStateForRegion(state: Int, region: Region?) {
        //領域への入退場のステータス変化を検知（INSIDE: 1, OUTSIDE: 0）
        Log.d("iBeacon", "Determine State: $state")
    }

    private fun createSetContent(){
        setContent {
            var buttonText by remember { mutableStateOf("BLE scan off") }

            BLECatchTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        Greeting("Android")
                        Button(onClick = {
                            bleScan()
                            buttonText = when(scannerBoolean){
                                true->{
                                    "BLE scan on"
                                }
                                false->{
                                    "BLE scan off"
                                }
                            }
                        }) {
                            Text(text = buttonText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BLECatchTheme {
        Column(modifier = Modifier.background(MaterialTheme.colors.background)) {
            Greeting("Android")
            Button(onClick = {}) {
                Text(text = "BLE scan off")
            }
        }
    }
}