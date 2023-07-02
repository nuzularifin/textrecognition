package com.nuzularifin.scannerimage

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.nuzularifin.scannerimage.databinding.ActivityMainBinding
import com.nuzularifin.scannerimage.utils.Constants
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val MY_CAMERA_PERMISSION_CODE: Int = 100
    private val READ_PERMISSION = 102
    private val WRITE_PERMISSION = 103

    private var camUri: Uri? = null

    private val viewModel: MainViewModel by viewModels()

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                binding.ivPicture.setImageURI(camUri)
                camUri?.let { detectText(it) }
            }
        }

    private var galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val uri = result.data?.data
                    uri?.let { binding.ivPicture.setImageURI(it) }
                    uri?.let { detectText(it) }
                } catch (e: Exception){
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.btnTakePictureCamera.setOnClickListener {
            pickImageFromCamera()
        }
        binding.btnTakePictureGallery.setOnClickListener {
            chooseImageFromGallery()
        }
        val type = Constants.FROM_GALLERY

        if (type == Constants.FROM_CAMERA) {
            binding.btnTakePictureCamera.visible()
            binding.btnTakePictureGallery.gone()
        } else {
            binding.btnTakePictureCamera.gone()
            binding.btnTakePictureGallery.visible()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        with(viewModel) {
            dataText.reObserve(this@MainActivity){
                binding.tvTextScanner.text = it
            }

            errorMessage.reObserve(this@MainActivity) {
                binding.tvTextScanner.text = it
                getString(R.string.label_error)
            }
        }
    }

    private fun chooseImageFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                && (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            ) {
                val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                val permissionCoarse = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

                requestPermissions(
                    permission,
                    READ_PERMISSION
                ) // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_READ LIKE 1001
                requestPermissions(
                    permissionCoarse,
                    WRITE_PERMISSION
                ) // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_WRITE LIKE 1002
            } else {
                pickImageFromGallery()
            }
        } else {
            pickImageFromGallery()
        }
    }

    private fun pickImageFromGallery() {
        val gallery = Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        intent.type = "image/*"
        galleryLauncher.launch(gallery)
    }

    private fun pickImageFromCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.CAMERA
                    ),
                    MY_CAMERA_PERMISSION_CODE
                )
            } else {
                val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "New Picture")
                values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
                camUri = applicationContext.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                camera.putExtra(MediaStore.EXTRA_OUTPUT, camUri)
                resultLauncher.launch(camera)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun detectText(imageUri: Uri) {
        val inputImage: InputImage = InputImage.fromFilePath(this, imageUri)
        val recognition: TextRecognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognition.process(inputImage)
            .addOnSuccessListener { visionText ->
                lifecycleScope.launch {
                    viewModel.attachToText(visionText)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}