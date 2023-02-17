package com.nuzularifin.scannerimage

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.nuzularifin.scannerimage.databinding.FragmentSecondBinding
import java.text.SimpleDateFormat

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var estimateTimeWithDistance = null

    companion object {
        var DISTANCE = "distance"
        var TEXT_DATA = "text_data"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(TEXT_DATA, "")?.let {
            binding.tvTextData.text = it

        }
        arguments?.getString(DISTANCE, "")?.let {
            binding.tvDistance.text = "Jarak dengan Plaza Indonesia dari posisi anda = \n $it km"
        }

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun timeToSeconds(time: String): Long {
        val dateFormat = SimpleDateFormat("mm:ss")
        val reference = dateFormat.parse("00:00")
        val date = dateFormat.parse(time)

        return (date.time - reference.time) / 1000L
    }

}