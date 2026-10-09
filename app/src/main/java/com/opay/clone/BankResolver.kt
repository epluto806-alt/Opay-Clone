package com.opay.clone

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object BankResolver {

    private const val TAG = "BankResolver"
    private const val ENDPOINT = "https://api.spendjuice.com/payment-methods/resolve-bank-account"
    private const val API_KEY  = "test_r3m3mb3r2pu70nasm1l3"

    // Cache: key = "accountNumber|bankCode", value = Pair(name, expiryMillis)
    private val cache = ConcurrentHashMap<String, Pair<String, Long>>()
    private const val CACHE_TTL_MS = 10 * 60 * 1000L  // 10 minutes
    private const val CONNECT_TIMEOUT_MS = 4000
    private const val READ_TIMEOUT_MS = 3000

    fun resolve(accountNumber: String, bankCode: String, callback: (String?) -> Unit) {
        val key = "$accountNumber|$bankCode"
        val now = System.currentTimeMillis()

        // Cache hit — instant, no thread, no network
        cache[key]?.let { (name, expiry) ->
            if (expiry > now) {
                Log.d(TAG, "cache HIT for $key")
                Handler(Looper.getMainLooper()).post { callback(name) }
                return
            } else {
                cache.remove(key)
            }
        }

        Thread {
            val start = System.currentTimeMillis()
            var name: String? = null
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Authorization", API_KEY)
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Connection", "keep-alive")
                    setRequestProperty("Accept", "application/json")
                }

                val body = """{"account_number":"$accountNumber","bank_code":"$bankCode"}"""
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                if (code in 200..299) {
                    val text = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val obj = JSONObject(text)
                    val data = obj.optJSONObject("data")
                    name = data?.optString("account_name")?.takeIf { it.isNotBlank() }
                    if (!name.isNullOrBlank()) {
                        cache[key] = name to (System.currentTimeMillis() + CACHE_TTL_MS)
                    }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() }
                }
            } catch (e: Exception) {
                Log.d(TAG, "resolve failed for $key: ${e.message}")
                name = null
            } finally {
                conn?.disconnect()
            }

            val ms = System.currentTimeMillis() - start
            Log.d(TAG, "resolve $key took ${ms}ms, result=${name ?: "null"}")

            Handler(Looper.getMainLooper()).post { callback(name) }
        }.start()
    }

    fun clearCache() {
        cache.clear()
    }
}