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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptions
import com.nuzularifin.scannerimage.databinding.FragmentFirstBinding
import kotlinx.coroutines.*


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.btnTakePicture.setOnClickListener {
            chooseImage()
        }

        binding.btnSeeDetail.gone()
        binding.btnSeeDetail.setOnClickListener {
            openDetail()
        }
    }

    private fun openDetail() {
        Log.d("distanceFromPI", "distance: $distanceFromStartLocation")
        var bundle = Bundle()
        bundle.putString(SecondFragment.TEXT_DATA, dataText)
        bundle.putString(SecondFragment.DISTANCE, distanceFromStartLocation.toString())
        findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment, bundle)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun chooseImage() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)  {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                    MY_LOCATION_PERMISSION_CODE
                )
                return
            }

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
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
                camUri = requireContext().contentResolver.insert(
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
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            fusedLocationProviderClient.lastLocation.addOnCompleteListener {
                val location: Location? = it.result
                if (location != null){
                    binding.tvInfo.text = "Latitude ${location.latitude} , Longitude : ${location.longitude}"
                    val plazaIndonesiaLocation = Location("plaza_indonesia")
                    plazaIndonesiaLocation.latitude = -6.1938597
                    plazaIndonesiaLocation.longitude = 106.8197775
                    distanceFromStartLocation = location.distanceTo(plazaIndonesiaLocation)/1000.toDouble()
                    location.time
                    binding.btnSeeDetail.visible()
                }
            }
        }
    }

    private fun detectText(imageUri: Uri) {
        val inputImage: InputImage = InputImage.fromFilePath(requireContext(), imageUri)
        val recognition: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognition.process(inputImage)
            .addOnSuccessListener { visionText ->
                attachToText(visionText)
            }
            .addOnFailureListener { e ->
                Log.d("FailedRecognition", "detectText: ${e.printStackTrace()}")
            }
    }

    private fun attachToText(visionText: Text) {
        val stringBuilder = StringBuilder()
        for (block in visionText.textBlocks) {
            val blockText = block.text
            stringBuilder.appendLine(blockText)
        }

        dataText = stringBuilder.toString()
        Log.d("attachToText", "attachToText: $dataText")
    }
}