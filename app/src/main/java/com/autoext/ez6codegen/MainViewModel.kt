package com.autoext.ez6codegen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Calendar
import java.util.Locale
import androidx.core.content.edit

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("app_prefs", Application.MODE_PRIVATE)

    private val calendar = Calendar.getInstance()

    var vin4 by mutableStateOf(prefs.getString("vin4", "").orEmpty())
    var month by mutableStateOf<Int?>(calendar.get(Calendar.MONTH) + 1)
    var day by mutableStateOf<Int?>(calendar.get(Calendar.DAY_OF_MONTH))
    var unlockCode by mutableStateOf("")
    var errorMessage by mutableStateOf("")

    init {
        if (isVinValid()) {
            generateCode()
        }
    }

    fun isVinValid() = vin4.length == 4 && vin4.all { it.isDigit() }

    fun generateCode() {
        viewModelScope.launch {
            try {
                val month = this@MainViewModel.month
                val day = this@MainViewModel.day
                errorMessage = ""
                if (!isVinValid()) {
                    throw IllegalArgumentException("VIN must be 4 digits")
                }

                if (month == null || month < 1 || month > 12) {
                    throw IllegalArgumentException("Month must be between 1 and 12")
                }

                if (day == null || day < 1 || day > 31) {
                    throw IllegalArgumentException("Day must be between 1 and 31")
                }

                prefs.edit { putString("vin4", vin4) }

                val mm = String.format(Locale.US, "%02d", month)
                val dd = String.format(Locale.US, "%02d", day)
                val input = "$vin4$mm$dd"

                val hash = sha256(input.toByteArray())
                val hex = hash.joinToString("") { String.format("%02x", it) }

                val startIndexVal = day + 1
                if (startIndexVal + 8 > hex.length) {
                    throw IllegalArgumentException("Invalid start index for hash extraction")
                }

                val code = hex.substring(startIndexVal, startIndexVal + 8)

                unlockCode = code

            } catch (e: Exception) {
                errorMessage = e.message ?: "An error occurred"
                unlockCode = ""
            }
        }
    }

    private fun sha256(input: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(input)
    }

    fun resetToCurrentDate() {
        calendar.time = Calendar.getInstance().time
        month = calendar.get(Calendar.MONTH) + 1
        day = calendar.get(Calendar.DAY_OF_MONTH)
    }
}