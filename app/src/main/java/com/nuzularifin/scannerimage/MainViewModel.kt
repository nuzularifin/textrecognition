package com.nuzularifin.scannerimage

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.Job

class MainViewModel() : ViewModel() {

    var job: Job? = null
    val dataText = MutableLiveData<String>()
    val errorMessage = MutableLiveData<String>()

    suspend fun attachToText(visionText: Text) {
        val stringBuilder = StringBuilder()
        for (block in visionText.textBlocks) {
            val blockText = block.text
            stringBuilder.appendLine(blockText)
        }

        val visionTextRecognition = stringBuilder.toString()

        try {
            val data = visionTextRecognition.replace("\n", "").split("[/*\\-:+X]".toRegex())
            val number1 = data[0].toInt()
            val number2 = data[1].toInt()

            val operators = findOperators(visionTextRecognition.replace("\n", ""))

            var result = 0;
            try {
                when (operators) {
                    "+" -> {
                        result = number1 + number2
                    }
                    "/", ":" -> {
                        result = number1 / number2
                    }
                    "x", "*" -> {
                        result = number1 * number2
                    }
                    "-" -> {
                        result = number1 - number2
                    }
                    else -> {
                        onErrorMessage("Is Not Numeric and operators")
                    }
                }
            } catch (e: java.lang.NumberFormatException) {
                onErrorMessage("Gagal membuat perhitungan, mohon coba kembali")
            }

            val textResult = "$number1 $operators $number2 = $result"
            dataText.postValue(textResult)
        } catch (e: Exception) {
            onErrorMessage("Gagal membuat perhitungan, mohon coba kembali")
        }
    }

    private fun findOperators(dataText: String): String {
        var operators = ""

        for (data in dataText) {
            when (data.toString()) {
                "+" -> {
                    operators = "+"
                    break
                }
                "/", ":" -> {
                    operators = "/"
                    break
                }
                "*", "X", "x" -> {
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

    private fun onErrorMessage(message: String){
        errorMessage.postValue(message)
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}