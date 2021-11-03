package com.example.realtodoapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.realtodoapp.R
import com.example.realtodoapp.service.CertService
import com.example.realtodoapp.util.AppUtil
import com.example.realtodoapp.util.TfliteModelUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL
import kr.co.shineware.nlp.komoran.core.Komoran
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class MainActivity : AppCompatActivity(){

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // CertService Foreground 실행
        val intent = Intent(this, CertService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(intent)
        } else {
            this.startService(intent)
        }
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION)
        requirePermissions(permissions, 999)

        // 테스트
        AppUtil.checkForPermission(this)

        //komoran 테스트
//        var komoran = Komoran(DEFAULT_MODEL.FULL)
//        var sample = "좋은 아침입니다"
//        var komoranResult = komoran.analyze(sample)
//        var tokenList = komoranResult.tokenList
//        var morphList = mutableListOf<String>()
//        for(token in tokenList){
//            morphList.add(token.morph)
//        }
//        Log.d("komoran", morphList.toString())


        // Ai 테스트
        var tfModel = TfliteModelUtil.loadTfModel(this)
        var input = Array(1){"오늘도 비타민을 꾸준히 먹었다! 기분이 한결 좋아지는듯?"}
        var output: ByteBuffer = ByteBuffer.allocate(2*4).order(ByteOrder.nativeOrder())
        tfModel.run(input, output)

        // bytebuffer float 변환
        output.rewind()
        var pro = output.asFloatBuffer()
        Log.d("AITEST", pro.get(0).toString() + " "+ pro.get(1).toString())

    }

    // 위치 권한 부여
    fun requirePermissions(permissions: Array<String>, requestCode: Int) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        } else {
            val isAllPermissionsGranted = permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
            if (isAllPermissionsGranted) {
            } else {
                ActivityCompat.requestPermissions(this, permissions, requestCode)
            }
        }
    }


}


