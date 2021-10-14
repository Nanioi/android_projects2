package com.nanioi.cameraapp

import android.app.Activity
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.nanioi.cameraapp.adapter.ImageViewPagerAdapter
import com.nanioi.cameraapp.databinding.ActivityImageListBinding
import com.nanioi.cameraapp.util.PathUtil
import java.io.File
import java.io.FileNotFoundException

class ImageListActivity : AppCompatActivity() {

    companion object {
        const val URI_LIST_KEY = "uriList"

        const val IMAGE_LIST_REQUEST_CODE = 100

        fun newIntent(activity: Activity, uriList: List<Uri>) =
            Intent(activity, ImageListActivity::class.java).apply {
                putExtra(URI_LIST_KEY, ArrayList<Uri>().apply { uriList.forEach { add(it) } })
            }
    }

    private lateinit var binding: ActivityImageListBinding
    private val uriList by lazy<List<Uri>> { intent.getParcelableArrayListExtra(URI_LIST_KEY)!! }
    private lateinit var imageViewPagerAdapter: ImageViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        setupImageList(uriList)
    }

    private var currentUri: Uri? = null

    //이미지 뿌려주기
    private fun setupImageList(uriList: List<Uri>) = with(binding) {
        if (::imageViewPagerAdapter.isInitialized.not()) {
            imageViewPagerAdapter = ImageViewPagerAdapter(uriList.toMutableList())
        }
        imageViewPager.adapter = imageViewPagerAdapter
        indicator.setViewPager(imageViewPager)
        imageViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() { // 현재 위치 변경 시 반영
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                toolbar.title = if (imageViewPagerAdapter.uriList.isNotEmpty()) {
                    currentUri = imageViewPagerAdapter.uriList[position]
                    getString(R.string.image_page, position + 1, imageViewPagerAdapter.uriList.size)
                } else {
                    currentUri = null
                    ""
                }
            }
        })
        deleteButton.setOnClickListener {
            currentUri?.let { uri ->
                removeImage(uri)
            }
        }
    }
    override fun onBackPressed() {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(URI_LIST_KEY, ArrayList<Uri>().apply { imageViewPagerAdapter.uriList.forEach { add(it) } })
        })
        finish()
    }

    private fun removeImage(uri: Uri) {
//        try {
//            val file = File(PathUtil.getPath(this, uri) ?: throw FileNotFoundException())
//            file.delete()
//            imageViewPagerAdapter.uriList.let{
//                val imageList = it.toMutableList()
//                imageList.remove(uri)
//                imageViewPagerAdapter.uriList = imageList
//                imageViewPagerAdapter.notifyDataSetChanged()
//            }
//            MediaScannerConnection.scanFile(this, arrayOf(file.path), arrayOf("image/jpeg"),null)
//        }catch (e : FileNotFoundException){
//            e.printStackTrace()
//            Toast.makeText(this, "이미지가 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
//        }

        val file = File(PathUtil.getPath(this, uri) ?: throw FileNotFoundException())
        file.delete()
        val removedIndex = imageViewPagerAdapter.uriList.indexOf(uri)
        imageViewPagerAdapter.uriList.removeAt(removedIndex)
        imageViewPagerAdapter.notifyItemRemoved(removedIndex)
        binding.indicator.setViewPager(binding.imageViewPager)

        if (imageViewPagerAdapter.uriList.isNotEmpty()) {
            currentUri = if (removedIndex == 0) {
                imageViewPagerAdapter.uriList[removedIndex]
            } else {
                imageViewPagerAdapter.uriList[removedIndex - 1]
            }
        }

        MediaScannerConnection.scanFile(
            this, arrayOf(file.path), arrayOf(file.name)
        ) { _, _ ->
            contentResolver.delete(uri, null, null)
        }

        if (imageViewPagerAdapter.uriList.isEmpty()) {
            Toast.makeText(this, "삭제할 수 있는 이미지가 없습니다.", Toast.LENGTH_SHORT).show()
            onBackPressed()
        } else {
            binding.toolbar.title =
                getString(R.string.image_page, removedIndex + 1, imageViewPagerAdapter.uriList.size)
        }
    }


}
