package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat.LOG_TAG
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val LOG_TAG = "MyLogS"
    lateinit var mCameraManager: CameraManager
    lateinit var mBackgroundThread: HandlerThread
    var mBackgroundHandler: Handler? = null
    private val CAMERA1 = 0
    private val CAMERA2 = 1
    var myCameras: Array<CameraService?> = arrayOf()

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread.start()
        mBackgroundHandler = Handler(mBackgroundThread.looper)
    }
    private fun stopBackgroundThread() {
        mBackgroundThread.quitSafely()
        try {
            mBackgroundThread.join()
            mBackgroundThread.quit()
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
    @RequiresApi(Build.VERSION_CODES.M)
    fun checkPermission(){
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            ||
            ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 1
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(LOG_TAG, "Запрашиваем разрешение")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission()
        }
        mCameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        button1.setOnClickListener {
            if (myCameras[CAMERA2]!!.isOpen) {
                myCameras[CAMERA2]!!.closeCamera()
            }
            if (myCameras[CAMERA1] != null) {
                if (!myCameras[CAMERA1]!!.isOpen) myCameras[CAMERA1]!!.openCamera()
            }
        }
        button2.setOnClickListener {
            if (myCameras[CAMERA1]!!.isOpen) {
                myCameras[CAMERA1]!!.closeCamera()
            }
            if (myCameras[CAMERA2] != null) {
                if (!myCameras[CAMERA2]!!.isOpen) myCameras[CAMERA2]!!.openCamera()
            }
        }
        button3.setOnClickListener {
            if (myCameras[CAMERA1]!!.isOpen) myCameras[CAMERA1]!!.makePhoto()
            if (myCameras[CAMERA2]!!.isOpen) myCameras[CAMERA2]!!.makePhoto()
        }
        Log.d(LOG_TAG, "Получение списка камер с устройства")
        try {
            // Получение списка камер с устройства
            myCameras = arrayOfNulls(mCameraManager.cameraIdList.size)
            for (cameraID in mCameraManager.cameraIdList) {
                Log.i(LOG_TAG, "cameraID: $cameraID")
                val id = cameraID.toInt()
                // создаем обработчик для камеры
                myCameras[id] = CameraService(mainActivity = this, cameraManager =  mCameraManager, cameraID = cameraID)
            }

        }catch (e: CameraAccessException){
            Log.e(LOG_TAG, e.message!!)
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        startBackgroundThread()
    }

    override fun onStop() {
        super.onStop()
        stopBackgroundThread()
    }
}