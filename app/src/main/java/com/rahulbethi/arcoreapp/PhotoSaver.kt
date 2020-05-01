package com.rahulbethi.arcoreapp

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.view.PixelCopy
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.ar.sceneform.ArSceneView
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class PhotoSaver(private val activity: Activity) {

    private fun generateFilename(): String? {
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)?.absolutePath +
                "/ARCoreApp/${date}_screenshot.jpg"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveBitMapToGallery(bmp: Bitmap) {
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${date}_screenshot.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/ARCoreApp")
        }
        val uri = activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        activity.contentResolver.openOutputStream( uri ?: return).use { outputStream ->
            outputStream?.let {
                try {
                    saveDataToGallery(bmp, outputStream)
                } catch(e: IOException) {
                    Toast.makeText(activity, "Error: Failed to save the Photo", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveDataToGallery(bmp: Bitmap, outputStream: OutputStream) {
        val outputData = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputData)
        outputData.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
    }

    private fun saveBitMapToGallery(bmp: Bitmap, filename: String) {
        val out = File(filename)
        if(!out.parentFile.exists()) {
            out.parentFile.mkdirs()
        }
        try {
            val outputStream = FileOutputStream(filename)
            saveDataToGallery(bmp, outputStream)
        } catch(e: IOException) {
            Toast.makeText(activity, "Error: Failed to save the Photo", Toast.LENGTH_LONG).show()
        }
    }

    fun takePhoto(arSceneView: ArSceneView) {
        val bmp = Bitmap.createBitmap(arSceneView.width, arSceneView.height, Bitmap.Config.ARGB_8888)
        val handlerThread = HandlerThread("PixelCopyThread")
        handlerThread.start()

        PixelCopy.request(arSceneView, bmp, { result ->
            if(result == PixelCopy.SUCCESS) {
                if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    val filename = generateFilename()
                    saveBitMapToGallery(bmp, filename?: return@request)
                } else {
                    saveBitMapToGallery(bmp)
                }
                activity.runOnUiThread {
                    Toast.makeText(activity, "Photo saved!", Toast.LENGTH_LONG).show()
                }
            } else {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Error: Failed to save the Photo", Toast.LENGTH_LONG).show()
                }
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }
}