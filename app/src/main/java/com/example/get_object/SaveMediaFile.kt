package com.example.get_object

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SaveMediaFile(private val context: Context) {

    private var saveUri: Uri? = null

    //SDKバージョンにより
    fun saveToPublish(): Uri? {
        if(Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                saveUri = saveFileUriQ()
            }else {
                saveUri = saveFileUriQLassThen()
            }
            return saveUri
        }
        return null
    }

    //ファイル名作成処理
    private fun getFileName(): String {
        val fileDate = SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPAN).format(Date())
        return String.format("%s.png", fileDate)
    }

    //AndroidSDK　Q以上
    private fun saveFileUriQ(): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, getFileName())
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        return context.contentResolver.insert(collection, values)
    }

    //AndroidSDK　Q未満
    private fun saveFileUriQLassThen(): Uri? {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val outputFile = File(directory, getFileName())

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put("_data", outputFile.absolutePath)
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        return FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", outputFile)
    }

    fun getSaveUri(): Uri? {
        return saveUri
    }
}