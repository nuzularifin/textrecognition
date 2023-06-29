package com.nuzularifin.scannerimage

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptions
import com.nuzularifin.scannerimage.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val MY_CAMERA_PERMISSION_CODE: Int = 100
    private val MY_LOCATION_PERMISSION_CODE: Int = 101

    private var camUri: Uri? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var distanceFromStartLocation: Double? = null
    private var dataText: String = ""

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                GlobalScope.launch {
                    withContext(Dispatchers.Main){
                        async {
                            binding.ivPicture.setImageURI(camUri)
                            camUri?.let { detectText(it) }
                            getLocation()
                        }
                    }
                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.btnTakePicture.setOnClickListener {
            chooseImage()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun chooseImage() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)  {
                ActivityCompat.requestPermissions(this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                    MY_LOCATION_PERMISSION_CODE
                )
                return
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(
                        Manifest.permission.CAMERA
                    ),
                    MY_CAMERA_PERMISSION_CODE
                )
            } else {
                val galleryIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "New Picture")
                values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
                camUri = applicationContext.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                galleryIntent.putExtra(MediaStore.EXTRA_OUTPUT, camUri)
                resultLauncher.launch(galleryIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            fusedLocationProviderClient.lastLocation.addOnCompleteListener {
                val location: Location? = it.result
                if (location != null){
                    binding.tvInfo.text = "Latitude ${location.latitude} , Longitude : ${location.longitude}"
                    val plazaIndonesiaLocation = Location("plaza_indonesia")
                    plazaIndonesiaLocation.latitude = -6.1938597
                    plazaIndonesiaLocation.longitude = 106.8197775
                    distanceFromStartLocation = location.distanceTo(plazaIndonesiaLocation)/1000.toDouble()
                    location.time
                }
            }
        }
    }

    private fun attachToText(visionText: Text) {
        val stringBuilder = StringBuilder()
        for (block in visionText.textBlocks) {
            val blockText = block.text
            stringBuilder.appendLine(blockText)
        }

        dataText = stringBuilder.toString()
        binding.tvTextScanner.text = dataText
        try {
            val operatorsMath = listOf("+","/","-",":")
            val data = dataText.replace("\n","").split("[/*\\-+X]".toRegex())
            val number1 = data[0].toInt()
            val number2 = data[1].toInt()

            val operators = findOperators(dataText.replace("\n",""))

            var result = 0;
            try {
                when (operators){
                    "+" -> {
                        result = number1 + number2
                    }
                    "/",":" -> {
                        result = number1 / number2
                    }
                    "x","*" -> {
                        result = number1 * number2
                    }
                    "-" -> {
                        result = number1 - number2
                    }
                    else -> {
                        Toast.makeText(this, "is not numeric and operators", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception){
                Toast.makeText(this, "${e.printStackTrace()}", Toast.LENGTH_SHORT).show()
            }

            binding.tvTextScanner.text = "$number1 $operators $number2 = $result"

        } catch (e: Exception){
            binding.tvTextScanner.text = getString(R.string.label_error)
            e.printStackTrace()
        }
    }

    private fun findOperators(dataText: String): String {
        var operators = ""

        for (data in dataText){
            when (data.toString()) {
                "+" ->{
                    operators = "+"
                    break
                }
                "/",":" -> {
                    operators = "/"
                    break
                }
                "*","X" -> {
                    operators = "*"
                    break
                }
                "-" -> {
                    operators = "-"
                    break
                }
                else -> {

                }
            }
        }

        return operators
    }

    private fun detectText(imageUri: Uri) {
        val inputImage: InputImage = InputImage.fromFilePath(this, imageUri)
        val recognition: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognition.process(inputImage)
            .addOnSuccessListener { visionText ->
                attachToText(visionText)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}