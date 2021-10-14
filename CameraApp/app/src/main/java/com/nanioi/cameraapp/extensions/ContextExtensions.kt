package com.nanioi.cameraapp.extensions

import android.content.Context
import com.nanioi.cameraapp.R
import java.io.File

internal fun Context.getAppCachePictureDirectory() : File {
    val mediaDir = filesDir?.let {
        File(it, getString(R.string.app_name)).apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists()) {
        mediaDir
    } else {
        filesDir
    }
}
