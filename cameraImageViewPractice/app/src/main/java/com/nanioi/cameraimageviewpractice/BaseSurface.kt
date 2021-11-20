package com.nanioi.cameraimageviewpractice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.ETC1.getHeight

import android.opengl.ETC1.getWidth
import android.os.Build
import android.util.AttributeSet
import android.util.Log

import android.view.MotionEvent

import android.view.SurfaceHolder

import android.view.SurfaceView
import android.view.View
import java.lang.Exception


class BaseSurface(context: Context?) :
    SurfaceView(context), SurfaceHolder.Callback, View.OnTouchListener, Runnable {
    /**
     * Holds the surface frame
     */
    private var mHolder: SurfaceHolder? = null
    /**
     * Draw thread
     */
    private var drawThread: Thread? = null

    /**
     * True when the surface is ready to draw
     */
    private var surfaceReady = false

    /**
     * Drawing thread flag
     */
    private var drawingActive = false

    /**
     * Paint for drawing the sample rectangle
     */
    private val samplePaint: Paint = Paint()
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (width == 0 || height == 0) {
            return
        }

        // resize your UI
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mHolder = holder
        if (drawThread != null) {
            Log.d(BaseSurface.Companion.LOGTAG, "draw thread still active..")
            drawingActive = false
            try {
                drawThread!!.join()
            } catch (e: InterruptedException) { // do nothing
            }
        }
        surfaceReady = true
        startDrawThread()
        Log.d(BaseSurface.Companion.LOGTAG, "Created")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface is not used anymore - stop the drawing thread
        stopDrawThread()
        // and release the surface
        holder.surface.release()
        mHolder = null
        surfaceReady = false
        Log.d(BaseSurface.Companion.LOGTAG, "Destroyed")
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        // Handle touch events
        return true
    }

    /**
     * Stops the drawing thread
     */
    fun stopDrawThread() {
        if (drawThread == null) {
            Log.d(BaseSurface.Companion.LOGTAG, "DrawThread is null")
            return
        }
        drawingActive = false
        while (true) {
            try {
                Log.d(BaseSurface.Companion.LOGTAG, "Request last frame")
                drawThread!!.join(5000)
                break
            } catch (e: Exception) {
                Log.e(BaseSurface.Companion.LOGTAG, "Could not join with draw thread")
            }
        }
        drawThread = null
    }

    /**
     * Creates a new draw thread and starts it.
     */
    fun startDrawThread() {
        if (surfaceReady && drawThread == null) {
            drawThread = Thread(this, "Draw thread")
            drawingActive = true
            drawThread!!.start()
        }
    }

    override fun run() {
        Log.d(BaseSurface.Companion.LOGTAG, "Draw thread started")
        var frameStartTime: Long
        var frameTime: Long

        /*
         * In order to work reliable on Nexus 7, we place ~500ms delay at the start of drawing thread
         * (AOSP - Issue 58385)
         */if (Build.BRAND.equals("google", ignoreCase = true) && Build.MANUFACTURER.equals(
                "asus",
                ignoreCase = true
            ) && Build.MODEL.equals("Nexus 7", ignoreCase = true)
        ) {
            Log.w(BaseSurface.Companion.LOGTAG, "Sleep 500ms (Device: Asus Nexus 7)")
            try {
                Thread.sleep(500)
            } catch (ignored: InterruptedException) {
            }
        }
        try {
            while (drawingActive) {
                if (holder == null) {
                    return
                }
                frameStartTime = System.nanoTime()
                val canvas: Canvas? = holder!!.lockCanvas()
                if (canvas != null) {
                    // clear the screen using black
                    canvas.drawARGB(255, 0, 0, 0)
                    try {
                        // Your drawing here
                        canvas.drawRect(
                            0F, 0F, (width / 2).toFloat(),
                            (height / 2).toFloat(), samplePaint)
                    } finally {
                        holder!!.unlockCanvasAndPost(canvas)
                    }
                }

                // calculate the time required to draw the frame in ms
                frameTime = (System.nanoTime() - frameStartTime) / 1000000
                if (frameTime < BaseSurface.Companion.MAX_FRAME_TIME) // faster than the max fps - limit the FPS
                {
                    try {
                        Thread.sleep(BaseSurface.Companion.MAX_FRAME_TIME - frameTime)
                    } catch (e: InterruptedException) {
                        // ignore
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(BaseSurface.Companion.LOGTAG, "Exception while locking/unlocking")
        }
        Log.d(BaseSurface.Companion.LOGTAG, "Draw thread finished")
    }

    companion object {
        /**
         * Time per frame for 60 FPS
         */
        private const val MAX_FRAME_TIME = (1000.0 / 60.0).toInt()
        private const val LOGTAG = "surface"
    }

    init {
        val holder = getHolder()
        holder.addCallback(this)
        setOnTouchListener(this)

        // red
        samplePaint.setColor(-0x10000)
        // smooth edges
        samplePaint.setAntiAlias(true)
    }
}