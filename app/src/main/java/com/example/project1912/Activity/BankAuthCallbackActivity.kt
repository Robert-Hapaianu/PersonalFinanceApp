package com.example.personalfinanceapp.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.personalfinanceapp.Utils.SecureTokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class BankAuthCallbackActivity : AppCompatActivity() {
    private lateinit var secureTokenStorage: SecureTokenStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        secureTokenStorage = SecureTokenStorage(this)

        val data = intent?.data
        if (data != null) {
            val requisitionId = secureTokenStorage.getRequisitionId()
            if (requisitionId != null) {
                checkRequisitionStatus(requisitionId)
            }
        }

        finish()
    }

    private fun checkRequisitionStatus(requisitionId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val accessToken = secureTokenStorage.getAccessToken()
                if (accessToken == null) {
                    withContext(Dispatchers.Main) {
                        // Handle missing access token
                        return@withContext
                    }
                }

                val url = URL("https://bankaccountdata.gocardless.com/api/v2/requisitions/$requisitionId/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Content-Type", "application/json")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    
                    // Handle the response based on the requisition status
                    val status = jsonResponse.optString("status")
                    println("Requisition Status: $status")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
} 
