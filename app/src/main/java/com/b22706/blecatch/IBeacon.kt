package com.b22706.blecatch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.altbeacon.beacon.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.FileWriter
import java.io.IOException
import java.util.*

class IBeacon(private val context: Context): RangeNotifier, MonitorNotifier {
    companion object{
        private const val LOG_NAME = "iBeacon"
        private const val IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
    }

    private var queue: ArrayDeque<IBeaconData> = ArrayDeque(listOf())
    fun resetQueue(){queue.clear()}
    data class IBeaconData (
        val time: Long,
        val iBeacon: Beacon
    )
    var csvRun = false

    val beaconList: MutableList<Beacon> = mutableListOf()
    private var _beaconLiveData = MutableLiveData(listOf<Beacon>())
    val beaconLiveData: LiveData<List<Beacon>> = _beaconLiveData
    private fun beaconListAdd(data: Beacon){
        for (i in 0 until beaconList.size){
            if(beaconList.size == 0)break
            val beacon = beaconList[i]
            if(beacon.id1 == data.id1 && beacon.id2 == data.id2 && beacon.id3 == data.id3){
                beaconList.removeAt(i)
                beaconList.add(i,data)
                _beaconLiveData.postValue(beaconList.toList())
                return
            }
        }

        beaconList.add(data)
        _beaconLiveData.postValue(beaconList.toList())
    }

    private val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(context)
    private var mRegion: Region = Region("iBeacon", null, null, null) //検知対象の Beacon を識別するためのもの

    var scannerBoolean = false
    private set

    init {
        //Bluetoothアダプターを初期化する
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(IBEACON_FORMAT)) // iBeaconのフォーマット指定

        beaconManager.addMonitorNotifier(this)
        beaconManager.addRangeNotifier(this)
    }

    fun setRegion(uuid: String?, major: String?, minor: String?){
        // Region("iBeacon", Identifier.parse("監視対象のUUID"), Major, Minor)
        val id1 = when(uuid) {
            null, "" -> null
            else -> Identifier.parse(uuid)
        }
        val id2 = when(major) {
            null, "" -> null
            else -> Identifier.parse(major)
        }
        val id3 = when(minor) {
            null, "" -> null
            else -> Identifier.parse(minor)
        }
        if(scannerBoolean){
            beaconManager.stopMonitoring(mRegion)
            beaconManager.stopRangingBeacons(mRegion)
            scannerBoolean = false
        }
        mRegion = Region("iBeacon", id1, id2, id3)
    }

    fun startScan(){
        Log.d(LOG_NAME, "$mRegion")
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (!scannerBoolean){
                //スキャンの開始
                beaconManager.startMonitoring(mRegion)
                beaconManager.startRangingBeacons(mRegion)
                scannerBoolean = true
            }
        }
    }

    fun stopScan(){
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (scannerBoolean) {
                //スキャンを停止
                beaconManager.stopMonitoring(mRegion)
                beaconManager.stopRangingBeacons(mRegion)
                scannerBoolean = false
            }
        }
    }

    override fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>, region: Region) {
        //検知したBeaconの情報
        Log.d(LOG_NAME, "beacons.size: ${beacons.size}")// 検出したビーコンの数
        beacons.let {
            for (beacon in beacons) {
                Log.d(LOG_NAME, "UUID: ${beacon.id1}, major: ${beacon.id2}, minor: ${beacon.id3}, RSSI: ${beacon.rssi}, TxPower: ${beacon.txPower}, Distance: ${beacon.distance}")
                beaconListAdd(beacon)
                queue.add(IBeaconData(System.currentTimeMillis(), beacon))
            }
        }
    }

    override fun didEnterRegion(region: Region) {
        //領域への入場を検知
        Log.d(LOG_NAME, "Enter Region: ${region.uniqueId}")
    }

    override fun didExitRegion(region: Region) {
        //領域からの退場を検知
        Log.d(LOG_NAME, "Exit Region: ${region.uniqueId}")
    }

    override fun didDetermineStateForRegion(state: Int, region: Region) {
        //領域への入退場のステータス変化を検知（INSIDE: 1, OUTSIDE: 0）
        Log.d(LOG_NAME, "Determine State: $state")
    }

    fun csvWriter(path: String, fileName: String): Boolean {
        //CSVファイルの書き出し
        try{
            //書込み先指定
            val writer = FileWriter("${path}/${fileName}-iBeacon.csv")

            //書き込み準備
            val csvPrinter = CSVPrinter(
                writer, CSVFormat.DEFAULT
                    .withHeader(
                        "time",
                        "uuid",
                        "major",
                        "minor",
                        "rssi",
                        "distance"
                    )
            )
            val hnd = Handler(Looper.getMainLooper())
            queue.clear()
            csvRun = true
            // こいつ(rnb0) が何回も呼ばれる
            val rnb = object : Runnable {
                override fun run() {
                    val dequeClone = queue.clone()
                    //書き込み開始
                    for(data in dequeClone){
                        //データ保存
                        csvPrinter.printRecord(
                            data.time.toString(),
                            data.iBeacon.id1,
                            data.iBeacon.id2,
                            data.iBeacon.id3,
                            data.iBeacon.rssi.toString(),
                            data.iBeacon.distance.toString()
                        )
                    }
                    queue.clear()

                    // stop用のフラグ
                    when(csvRun) {
                        true -> {
                            // 指定時間後に自分自身を呼ぶ
                            hnd.postDelayed(this, 1000)
                        }
                        false -> {
                            //データ保存の終了処理
                            csvPrinter.flush()
                            csvPrinter.close()
                        }
                    }
                }
            }
            // 初回の呼び出し
            hnd.post(rnb)
            return true
        }catch (e: IOException){
            //エラー処理
            Log.d(LOG_NAME, "${e}:${e.message!!}")
            return false
        }
    }
}