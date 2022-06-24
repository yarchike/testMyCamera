package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.core.content.ContextCompat.checkSelfPermission
import java.io.File
import java.util.*

class CameraService(
    private val mainActivity: MainActivity,
    private val cameraManager: CameraManager,
    private val cameraID: String
) {
    val LOG_TAG = "MyLogS"
    var mCameraDevice: CameraDevice? = null
    lateinit var mImageReader: ImageReader
    lateinit var mCaptureSession: CameraCaptureSession
    private val mFile = File(

        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
        "test1.jpg"
    )

    fun makePhoto() {
        try {
            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(mImageReader.surface)
            val CaptureCallback: CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                }
            }
            mCaptureSession.stopRepeating()
            mCaptureSession.abortCaptures()
            mCaptureSession.capture(
                captureBuilder.build(),
                CaptureCallback,
                mainActivity.mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    fun openCamera() {
        try {
            if (checkSelfPermission(
                    mainActivity.applicationContext,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                cameraManager!!.openCamera(cameraID, mCameraCallback, mainActivity.mBackgroundHandler)
            }
        } catch (e: CameraAccessException) {
            Log.i("MyLogS", e.message!!)
        }
    }
    private fun createCameraPreviewSession() {
        mImageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1)
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null)
        val texture = mainActivity.findViewById<TextureView>(R.id.textureView).surfaceTexture
        texture!!.setDefaultBufferSize(1920, 1080)
        val surface = Surface(texture)
        try {
            val builder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(surface)
            mCameraDevice!!.createCaptureSession(
                Arrays.asList(surface, mImageReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mCaptureSession = session
                        try {
                            mCaptureSession!!.setRepeatingRequest(
                                builder.build(),
                                null,
                                mainActivity.mBackgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, mainActivity.mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice!!.close()
            mCameraDevice = null
        }
    }

    val isOpen: Boolean
        get() = mCameraDevice != null

    private val mOnImageAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            mainActivity.mBackgroundHandler?.post(
                ImageSaver(
                    reader.acquireNextImage(),
                    mFile
                )
            )
        }

    private val mCameraCallback: CameraDevice.StateCallback =
        object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                mCameraDevice = camera
                Log.i(LOG_TAG, "Open camera  with id:" + mCameraDevice!!.id)
                createCameraPreviewSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                mCameraDevice!!.close()
                Log.i(LOG_TAG, "disconnect camera  with id:" + mCameraDevice!!.id)
                mCameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.i(LOG_TAG, "error! camera id:" + camera.id + " error:" + error)
            }
        }
}