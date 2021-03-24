package com.jontiloCode.latihancamerax

import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.jar.Manifest
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        if (allPermissionGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION)
        }

        camera_capture_button.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()

        cameraExecutor  = Executors.newSingleThreadExecutor()
    }


    private fun getOutputDirectory() : File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it,resources.getString(R.string.app_name)).apply { mkdirs() }
        }

        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir

    }

    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


    private fun takePhoto() {
        //Toast.makeText(this, "Capture Photo", Toast.LENGTH_SHORT).show()

        val imageCapture = imageCapture ?: return

        val photoFile = File(outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + " .jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)

                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo Capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()

                }
            }
        )

    }
    private fun startCamera() {

        //#buat inisialisasi untuk ProcessCameraProvider
        //#object ini di gunakan untuk bind lifecycle Camera ke lifecycle owner (MainActivity)
        //#berfungsi untuk peoses membuka dan menutup camera
        val cameraFutureProvider = ProcessCameraProvider.getInstance(this)


        //#membuat listener dari cameraFutureProvider
        cameraFutureProvider.addListener(Runnable {

            val cameraProvider : ProcessCameraProvider = cameraFutureProvider.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.createSurfaceProvider())
            }


            imageCapture = ImageCapture.Builder().build()

            val cameraSelector =  CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            }catch (exc : Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    companion object {
        private const val TAG  = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSION = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        //#ecek jika code request sudah sesuai, lewati jika salah
        if (requestCode == REQUEST_CODE_PERMISSION) {

            //#cek jika permission sudah diizinkan
            //#panggil fungsi startCamera()
            //#jika permission di tolak, maka akan menampilkan info Toast
            if (allPermissionGranted()) {
                startCamera()

            } else {
                Toast.makeText(this, "Permission not granted by the user", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}