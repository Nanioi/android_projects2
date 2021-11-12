package com.nanioi.cameraimageviewpractice

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import com.nanioi.cameraimageviewpractice.databinding.ActivityMainBinding
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding.cameraSurfaceView.holder.addCallback(object : SurfaceHolder.Callback{
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    if(ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED){
                        binding.cameraFocusQR.invalidate()
                    }
                    return
                }catch (e : IOException){
                    Log.e("aaa", e.message.toString())
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                TODO("Not yet implemented")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                TODO("Not yet implemented")
            }

        })
    }

}