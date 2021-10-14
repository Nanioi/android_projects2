package com.nanioi.imageuploadapp.photo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import com.nanioi.imageuploadapp.databinding.ActivityCameraBinding
import com.nanioi.imageuploadapp.extensions.loadCenterCrop
import com.nanioi.imageuploadapp.photo.ImageListActivity.Companion.IMAGE_LIST_REQUEST_CODE
import com.nanioi.imageuploadapp.util.PathUtil
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    private lateinit var cameraExecutor: ExecutorService
    private val cameraMainExecutor by lazy { ContextCompat.getMainExecutor(this) }

    private lateinit var imageCapture: ImageCapture
    private val cameraProviderFuture by lazy { ProcessCameraProvider.getInstance(this) } // 카메라 얻어오면 이후 실행 리스너 등록
    private val displayManager by lazy {
        getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    // Camera Config
    private var displayId: Int = -1

    private var camera: Camera? = null
    private var root: View? = null
    private var isCapturing: Boolean = false

    private var isFlashEnabled: Boolean = false

    private var uriList = mutableListOf<Uri>()

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (this@CameraActivity.displayId == displayId) {
                if (::imageCapture.isInitialized && root != null) {
                    imageCapture.targetRotation =
                            root?.display?.rotation ?: ImageOutputConfig.INVALID_ROTATION // 화면회전 시 대응
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        root = binding.root
        setContentView(binding.root)

        // 권한을 받아오면 카메라 시
        if (allPermissionsGranted()) {
            startCamera(binding.viewFinder)
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }


    }

    //권한을 얻어오는 함수
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    //카메라 시작
    private fun startCamera(viewFinder: PreviewView) {
        displayManager.registerDisplayListener(displayListener, null)

        //새로운 thread 기반으로 카메라 불러오기
        cameraExecutor = Executors.newSingleThreadExecutor()

        viewFinder.postDelayed({
            displayId = viewFinder.display.displayId // 카메라가 보여지고 있는 display의 id
            bindCameraUseCase()
        }, 10)
    }

    //카메라 사용
    private fun bindCameraUseCase() = with(binding) {
        val rotation = viewFinder.display.rotation // 회전 값 설정
        val cameraSelector =
                CameraSelector.Builder().requireLensFacing(LENS_FACING).build() // 카메라 설정(후면)

        //카메라를 정상적으로 가져올 수 있는 상태인지 확
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get() // 카메라 객체가져오기
            val preview = Preview.Builder().apply {
                setTargetAspectRatio(AspectRatio.RATIO_4_3)
                setTargetRotation(rotation)
            }.build()

            // imageCapture Init
            val builder = ImageCapture.Builder()
                    .setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY)//지연 최소화
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3) // preview 비율과 같은 비율로
                    .setTargetRotation(rotation)
                    .setFlashMode(ImageCapture.FLASH_MODE_AUTO)

            imageCapture = builder.build()

            //lifecycle
            try {
                cameraProvider.unbindAll() // 기존에 바인딩 되어 있는 카메라는 해제해주어야 함
                camera = cameraProvider.bindToLifecycle(
                        this@CameraActivity, cameraSelector, preview, imageCapture
                )
                preview.setSurfaceProvider(viewFinder.surfaceProvider) // 화면상에 매치
                bindCaptureListener()
                bindZoomListener()
                initFlashAndAddListener()
                bindPreviewImageViewClickListener()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, cameraMainExecutor)

    }

    // 촬영버튼 캡처 리스너 추가
    private fun bindCaptureListener() = with(binding) {
        captureButton.setOnClickListener {
            if (!isCapturing) {
                isCapturing = true
                captureCamera()
            }
        }
    }

    // 줌 기능 함수
    @SuppressLint("ClickableViewAccessibility")
    private fun bindZoomListener() = with(binding) {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                val delta = detector.scaleFactor // 얼마나 움직였나
                camera?.cameraControl?.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }
        val scaleGestureDetector = ScaleGestureDetector(this@CameraActivity, listener)

        viewFinder.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }
    }
    private fun initFlashAndAddListener() = with(binding) {
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() ?: false
        flashSwitch.isGone = hasFlash.not()
        if(hasFlash){
            flashSwitch.setOnCheckedChangeListener { _, isChecked ->
                isFlashEnabled = isChecked
            }
        }else{
            isFlashEnabled = false
            flashSwitch.setOnCheckedChangeListener(null)
        }
    }
    private fun bindPreviewImageViewClickListener() = with(binding) {
        previewImageView.setOnClickListener {
            startActivityForResult(
                    ImageListActivity.newIntent(this@CameraActivity, uriList),
                    IMAGE_LIST_REQUEST_CODE
            )
        }
    }
    private var contentUri: Uri? = null

    private fun captureCamera() {
        //초기화 되었는지 체크
        if (!::imageCapture.isInitialized) return
        //외장 디렉토리에 저장
        val photoFile = File(
                PathUtil.getOutputDirectory(this),
                SimpleDateFormat(
                        FILENAME_FORMAT, Locale.KOREA
                ).format(System.currentTimeMillis()) + ".jpg" // 현재 시간 기준으로 파일명 넣어주기
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        if (isFlashEnabled) flashLight(true)
        imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                        val rotation = binding.viewFinder.display.rotation // 회전 값 설정
                        contentUri = savedUri
                        updateSavedImageContent()
                    }

                    override fun onError(e: ImageCaptureException) {
                        e.printStackTrace()
                        isCapturing = false
                        flashLight(false)
                    }
                })

    }

    //플래시 켜주기
    private fun flashLight(light: Boolean) {
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() ?:false
        if (hasFlash) {
            camera?.cameraControl?.enableTorch(light)
        }
    }

    private fun updateSavedImageContent() {
        contentUri?.let {
            isCapturing = try {
                val file = File(PathUtil.getPath(this, it) ?: throw FileNotFoundException())
                MediaScannerConnection.scanFile(
                        this,
                        arrayOf(file.path),
                        arrayOf("image/jpeg"),
                        null
                )
                Handler(Looper.getMainLooper()).post {
                    binding.previewImageView.loadCenterCrop(url = it.toString(), corner = 4f)
                }
                flashLight(false)
                //if (isFlashEnabled) flashLight(false)
                uriList.add(it)
                false
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Toast.makeText(this, "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                flashLight(false)
                false
            }
        }
    }

    //권한 얻어오기
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera(binding.viewFinder)
            } else {
                Toast.makeText(this, "카메라 권한이 없습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        const val TAG = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private val LENS_FACING: Int = CameraSelector.LENS_FACING_BACK

        const val CONFIRM_IMAGE_REQUEST_CODE = 3000

        private const val URI_LIST_KEY = "uriList"

        fun newIntent(activity:Activity) = Intent(activity, CameraActivity::class.java)
    }
}