package com.b22706.blecatch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.b22706.blecatch.ui.theme.BLECatchTheme
import org.altbeacon.beacon.*
import pub.devrel.easypermissions.EasyPermissions

class MainActivity :
    ComponentActivity(),
    EasyPermissions.PermissionCallbacks
{
    // https://qiita.com/kenmaeda51415/items/ac5a2d5a15783bbe9192
    companion object{
        private const val IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
    }
    private lateinit var iBeacon: IBeacon

    var externalFilePath = ""
    var csvBoolean = false

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

        externalFilePath = this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()

        iBeacon = IBeacon(this)
        iBeacon.setRegion(null, "11", "")
    }

    override fun onPermissionsGranted(requestCode: Int, list: List<String>) {
        // ユーザーの許可が得られたときに呼び出される
        //recreate()
    }

    override fun onPermissionsDenied(requestCode: Int, list: List<String>) {
        // ユーザーの許可が得られなかったときに呼び出される。
        finish()
    }

    private fun createSetContent(){
        setContent {
            var buttonText by remember { mutableStateOf("BLE scan off") }
            var csvButtonText by remember { mutableStateOf("csv start") }
            val beaconList by iBeacon.beaconLiveData.observeAsState()
            var majorText by remember { mutableStateOf("") }

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
                        OnClickButton(text = buttonText) {
                            buttonText = when(iBeacon.scannerBoolean){
                                true->{
                                    iBeacon.stopScan()
                                    "BLE scan off"
                                }
                                false->{
                                    iBeacon.startScan()
                                    "BLE scan on"
                                }
                            }
                        }
                        Button(onClick = {
                            csvButtonText = when(csvBoolean){
                                true->{
                                    Toast.makeText(
                                        this@MainActivity,
                                        "wait...",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    iBeacon.csvWriter(externalFilePath,System.currentTimeMillis().toString()).let {
                                        when(it){
                                            true ->{
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "csv success",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            false -> {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "csv defeat",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                Log.d("csvWrite","defeat")
                                            }
                                        }
                                    }
                                    csvBoolean = false
                                    "csv start"
                                }
                                false->{
                                    iBeacon.resetQueue()
                                    csvBoolean = true
                                    "csw write"
                                }
                            }
                        }) {
                            Text(text = csvButtonText)
                        }
                        TextField(
                            value = majorText,
                            onValueChange = { majorText = it },
                            label = { Text("major") }
                        )
                        OnClickButton(text = "change major"){
                            Log.d("MainActivity", majorText)
                            Toast.makeText(
                                this@MainActivity,
                                "major change: $majorText",
                                Toast.LENGTH_SHORT
                            ).show()
                            iBeacon.setRegion(null, majorText, null)
                        }
                        BeaconList(beaconList)
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

@Composable
fun BeaconList(messages: List<Beacon>?) {
    LazyColumn(reverseLayout = true) {
        messages?.forEach {
            item { BeaconListRow(it) }
        }
    }
}

@Composable
fun BeaconListRow(beacon: Beacon) {
    Text(text = "UUID: ${beacon.id1}, major: ${beacon.id2}, minor: ${beacon.id3}, RSSI: ${beacon.rssi}, TxPower: ${beacon.txPower}, Distance: ${beacon.distance}")
}

@Composable
fun OnClickButton(text: String, onClick: () -> Unit){
    Button(onClick = onClick
    ) {
        Text(text = text)
    }
}

@Composable
fun RegionChange(bText: String, onClick: () -> Unit){
    var text by remember { mutableStateOf("") }

    TextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("major") }
    )
    OnClickButton(text = bText, onClick = onClick)
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