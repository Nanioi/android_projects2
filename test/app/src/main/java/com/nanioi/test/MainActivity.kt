package com.nanioi.test

import android.os.Bundle
import android.view.SurfaceHolder


import android.content.Intent

import android.widget.Toast

import android.view.SurfaceView

import android.graphics.PixelFormat
import android.hardware.Camera
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.nanioi.test.databinding.ActivityMainBinding
import java.lang.Exception


class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {
    var camera: Camera? = null
    var surfaceView: SurfaceView? = null
    var surfaceHolder: SurfaceHolder? = null
    var button: Button? = null
    var str: String? = null
    var jpegCallback: Camera.PictureCallback? = null
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.button.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                camera?.takePicture(null, null, jpegCallback)
            }

        })

        window.setFormat(PixelFormat.UNKNOWN)
        surfaceView = findViewById<View>(R.id.surfaceView) as SurfaceView
        surfaceHolder = surfaceView!!.holder
        surfaceHolder.addCallback(this)
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        jpegCallback = object : Camera.PictureCallback() {
            fun onPictureTaken(data: ByteArray?, camera: Camera?) {
                var outStream: FileOutputStream? = null
                try {
                    str = String.format(
                        "/sdcard/%d.jpg",
                        System.currentTimeMillis()
                    )
                    outStream = FileOutputStream(str)
                    outStream.write(data)
                    outStream.close()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                }
                Toast.makeText(
                    applicationContext,
                    "Picture Saved", Toast.LENGTH_LONG
                ).show()
                refreshCamera()
                val intent = Intent(
                    this@MainActivity,
                    ResultActivity::class.java
                )
                intent.putExtra("strParamName", str)
                startActivity(intent)
            }
        }
    }

    fun refreshCamera() {
        if (surfaceHolder!!.surface == null) {
            return
        }
        try {
            camera.stopPreview()
        } catch (e: Exception) {
        }
        try {
            camera.setPreviewDisplay(surfaceHolder)
            camera.startPreview()
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        camera = Camera.open()
        camera.stopPreview()
        val param: Camera.Parameters = camera.getParameters()
        param.setRotation(90)
        camera.setParameters(param)
        try {
            camera.setPreviewDisplay(surfaceHolder)
            camera.startPreview()
        } catch (e: Exception) {
            System.err.println(e)
            return
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int, width: Int, height: Int
    ) {
        refreshCamera()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        camera.stopPreview()
        camera.release()
        camera = null
    }
}