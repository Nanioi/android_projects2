package com.nanioi.cameraimageviewpractice

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.nanioi.cameraimageviewpractice.databinding.ActivityMainBinding
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var surface : BaseSurface

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surface = binding.baseSurface as BaseSurface

//        binding.cameraSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {
//                try {
//                    if (ActivityCompat.checkSelfPermission(
//                            this@MainActivity,
//                            Manifest.permission.CAMERA
//                        )
//                        == PackageManager.PERMISSION_GRANTED
//                    ) {
//                        binding.cameraFocusQR.invalidate()
//                    }
//                    return
//                } catch (e: IOException) {
//                    Log.e("aaa", e.message.toString())
//                }
//            }
//
//            override fun surfaceChanged(
//                holder: SurfaceHolder,
//                format: Int,
//                width: Int,
//                height: Int
//            ) {
//                TODO("Not yet implemented")
//            }
//
//            override fun surfaceDestroyed(holder: SurfaceHolder) {
//                TODO("Not yet implemented")
//            }
//
//        })
    }

    private fun getPermission() {
        if ((ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                ) ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                Toast.makeText(this, "????????? ???????????? ???????????????.", Toast.LENGTH_LONG).show()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    101
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    101
                )
                //?????? ?????? ??????
            }
        } else {
            //?????? ?????? ??????
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "??? ????????? ?????? ????????? ?????? ???????????????.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    this,
                    "????????? ???????????? ?????? ?????? ??? ????????? ???????????????.\n????????? ???????????? ????????? ?????? ??? ?????????.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        // start the drawing
        surface.startDrawThread()
    }

    override fun onPause() {
        // stop the drawing to save cpu time
        surface.stopDrawThread()
        super.onPause()
    }
}