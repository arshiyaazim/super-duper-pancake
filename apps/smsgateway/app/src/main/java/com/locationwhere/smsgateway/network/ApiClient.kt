package com.locationwhere.smsgateway.network

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun forwardSms(
        url: String,
        secret: String,
        sender: String,
        body: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val json = JSONObject().apply {
            put("secret", secret)
            put("from", sender)
            put("message", body)
        }

        val request = Request.Builder()
            .url("$url/api/v1/gateway/sms")
            .post(json.toString().toRequestBody(JSON))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Timber.e(e, "SMS forward API call failed")
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        Timber.d("SMS forward API success: code=${response.code}")
                        callback(true, responseBody)
                    } else {
                        Timber.w("SMS forward API error: code=${response.code}")
                        callback(false, responseBody ?: "Error: ${response.code}")
                    }
                }
            }
        })
    }

    fun testConnection(url: String, secret: String, callback: (Boolean, String?) -> Unit) {
        val json = JSONObject().apply {
            put("secret", secret)
        }

        Timber.d("Testing connection to backend")

        val request = Request.Builder()
            .url("$url/api/v1/gateway/test")
            .post(json.toString().toRequestBody(JSON))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Timber.e(e, "Connection test failed")
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = response.body?.string()
                    Timber.d("Connection test response: code=${response.code}")
                    callback(response.isSuccessful, responseBody)
                }
            }
        })
    }
}
