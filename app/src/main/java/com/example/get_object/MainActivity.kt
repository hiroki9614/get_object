package com.example.get_object

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileDescriptor

class MainActivity : AppCompatActivity() {

    companion object{
        const val DOWNLOAD_KEY = "modelDownload"
    }
    private var mCustomSharedPreferences: CustomSharedPreferences? = null
    private var mSaveMediaFile: SaveMediaFile? = null
    private var resultArray: ArrayList<ResultData> = ArrayList()
    private var downloadFlg: Boolean = false

    //画像選択
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()){ uri: Uri? ->
        if (uri != null) {
            //bitmapで取得
            val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            val bitmap = rotateImageIfRequired(fileDescriptor!!)
            fileDescriptor.close()
            getImageLabel(bitmap)
        }
    }
    //カメラ
    private val getCamera = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == RESULT_OK){
            val fileDescriptor = mSaveMediaFile?.getSaveUri()?.let { contentResolver.openFileDescriptor(it, "r") }
            val bitmap = rotateImageIfRequired(fileDescriptor!!)
            fileDescriptor.close()
            getImageLabel(bitmap)
        }
    }
    //権限
    private val checkPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
        if(!result){
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mCustomSharedPreferences = CustomSharedPreferences(this)
        mSaveMediaFile = SaveMediaFile(this)
        downloadFlg = mCustomSharedPreferences!!.getBooleanShared(DOWNLOAD_KEY)

        //権限チェック
        checkPermission.launch(WRITE_EXTERNAL_STORAGE)
        //モデルダウンロード状況によるレイアウト変更
        setDownloadResult(downloadFlg)

        //画像選択
        picture_btn.setOnClickListener{
            getContent.launch("image/*")
        }

        //カメラ選択
        camera_btn.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mSaveMediaFile!!.saveToPublish())
            Log.d("Test", mSaveMediaFile!!.getSaveUri().toString())
            getCamera.launch(intent)
        }

        //ダウンロード
        download_btn.setOnClickListener{
            modelDownload()
        }
    }

    private fun getImageLabel(bitmap: Bitmap){
        //画面に画像を表示
        imageView.setImageBitmap(bitmap)
        Log.d("Test", "setImageView")
        val image = InputImage.fromBitmap(bitmap, 0)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        labeler.process(image)
                //成功
                .addOnSuccessListener { labels ->
                    labels.forEach{ label ->
                        resultArray.add(
                                ResultData(
                                        label.text,
                                        label.confidence
                                ))
                    }
                    var text = ""
                    resultArray.forEach { data ->
                        text += data.label + ","
                    }
                    getTranslation(text)
                }
                //失敗
                .addOnFailureListener { e ->
                    result_txt.text = "Image Label Faild"
                    Log.e("getImageLabel", "err=$e");
                }
    }

    private fun getTranslation(text: String){
        if(text.isEmpty()){
            result_txt.text = "Image Label Faild"
            return
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.JAPANESE)
            .build()
        val translator = Translation.getClient(options)

        translator.translate(text)
                //成功
            .addOnSuccessListener { translatedText ->
                var resultText = ""
                val split = translatedText.split("、")
                final_result_txt.text = ""

                for((i, data) in resultArray.withIndex()) {
                    Log.d("getTranslation", resultArray[i].label)
                     resultText += split[i] + "(" + data.label + ") : " +
                                data.confidence + System.getProperty("line.separator")
                }
                result_txt.text = resultText
                final_result_txt.text = "この写真は「" + split[0] + "」"
                resultArray.clear()
            }
                //失敗
            .addOnFailureListener { e ->
                result_txt.text = "Translation Faild"
                final_result_txt.text = "画像を選んでください"
                Log.e("getTranslation", "err=$e");
            }
    }

    //モデルダウンロード
    private fun modelDownload(){
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.JAPANESE)
            .build()
        val translator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder()
            //.requireWifi()
            .build()

        download_btn.isEnabled = false
        progress_bar.visibility = View.VISIBLE

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                setDownloadResult(true)
                Log.d("getTranslation", "download success");
            }
            .addOnFailureListener { e ->
                result_txt.text = "Model Download Faild"
                download_btn.isEnabled = true
                setDownloadResult(false)
                Log.e("getTranslation", "err=$e");
            }
    }

    //モデルダウンロード状況によるレイアウト設定
    private fun setDownloadResult(result: Boolean){
        mCustomSharedPreferences!!.setBooleanShared(DOWNLOAD_KEY, result)
        downloadFlg = result
        if(result) {
            progress_bar.visibility = View.GONE
            picture_btn.visibility = View.VISIBLE
            camera_btn.visibility = View.VISIBLE
            download_btn.visibility = View.GONE
            final_result_txt.text = "画像を選んでください"
            analysis_txt.text = "分析結果"
        }else{
            progress_bar.visibility = View.GONE
            picture_btn.visibility = View.GONE
            camera_btn.visibility = View.GONE
            download_btn.visibility = View.VISIBLE
            final_result_txt.text = "モデルデータをダウンロードしてください"
            analysis_txt.text = ""
        }
    }

    //画像の向きチェック
    private fun rotateImageIfRequired(parcelFileDescriptor: ParcelFileDescriptor): Bitmap {
        val descriptor = parcelFileDescriptor.fileDescriptor
        var bitmap = BitmapFactory.decodeFileDescriptor(descriptor)

        val ei = ExifInterface(descriptor)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        Log.d("Test", orientation.toString())

        when(orientation){
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                bitmap = rotateImage(bitmap, 90)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                bitmap = rotateImage(bitmap, 180)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                bitmap = rotateImage(bitmap, 270)
            }
        }
        return bitmap
    }

    //画像の向き変更
    private fun rotateImage(bitmap: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotateImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return rotateImage
    }

}