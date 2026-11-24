package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SupabaseApiException(val statusCode: Int, val rawBody: String, message: String) : Exception(message)

class SupabaseClient {
    companion object {
        const val SUPABASE_URL = "https://ngbxvudichaooqxhjvei.supabase.co"
        const val SUPABASE_KEY = "sb_publishable_Iuni12vkgScCnAvrteXBwg_KrnOxYAr"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun extractErrorMessage(raw: String, code: Int): String {
        if (raw.isBlank()) return "HTTP-Fehler $code"
        return try {
            val obj = JSONObject(raw)
            obj.optString("message").takeIf { it.isNotBlank() }
                ?: obj.optString("error_description").takeIf { it.isNotBlank() }
                ?: obj.optString("error").takeIf { it.isNotBlank() }
                ?: obj.optString("hint").takeIf { it.isNotBlank() }
                ?: raw.take(200)
        } catch (_: Exception) {
            raw.take(200)
        }
    }

    suspend fun restGetArray(path: String, accessToken: String? = null): JSONArray =
        withContext(Dispatchers.IO) {
            val token = accessToken?.takeIf { it.isNotBlank() } ?: SUPABASE_KEY
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/$path")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    throw SupabaseApiException(resp.code, body, extractErrorMessage(body, resp.code))
                }
                if (resp.code == 204 || body.isBlank()) JSONArray() else JSONArray(body)
            }
        }

    suspend fun restMutate(
        method: String,
        path: String,
        bodyJson: String? = null,
        accessToken: String? = null
    ): JSONArray = withContext(Dispatchers.IO) {
        val token = accessToken?.takeIf { it.isNotBlank() } ?: SUPABASE_KEY
        val reqBuilder = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/$path")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")

        val body = (bodyJson ?: "").toRequestBody(JSON_MEDIA_TYPE)
        when (method.uppercase()) {
            "POST" -> reqBuilder.post(body)
            "PATCH" -> reqBuilder.patch(body)
            "DELETE" -> if (bodyJson != null) reqBuilder.delete(body) else reqBuilder.delete()
            else -> throw IllegalArgumentException("Unsupported method $method")
        }

        client.newCall(reqBuilder.build()).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw SupabaseApiException(resp.code, raw, extractErrorMessage(raw, resp.code))
            }
            if (resp.code == 204 || raw.isBlank()) JSONArray()
            else if (raw.trimStart().startsWith("[")) JSONArray(raw)
            else JSONArray().put(JSONObject(raw))
        }
    }

    suspend fun rpcRaw(
        functionName: String,
        payload: JSONObject? = null,
        accessToken: String? = null
    ): String = withContext(Dispatchers.IO) {
        val token = accessToken?.takeIf { it.isNotBlank() } ?: SUPABASE_KEY
        val bodyStr = payload?.toString() ?: "{}"
        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/rpc/$functionName")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(bodyStr.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw SupabaseApiException(resp.code, raw, extractErrorMessage(raw, resp.code))
            }
            raw
        }
    }

    suspend fun authPasswordToken(email: String, password: String): JSONObject =
        withContext(Dispatchers.IO) {
            val payload = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { resp ->
                val raw = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    throw SupabaseApiException(resp.code, raw, extractErrorMessage(raw, resp.code))
                }
                JSONObject(raw)
            }
        }

    suspend fun callEdgeFunction(
        functionName: String,
        payload: JSONObject,
        accessToken: String? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val reqBuilder = Request.Builder()
            .url("$SUPABASE_URL/functions/v1/$functionName")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

        if (!accessToken.isNullOrBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $accessToken")
        }

        client.newCall(reqBuilder.build()).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw SupabaseApiException(resp.code, raw, extractErrorMessage(raw, resp.code))
            }
            if (raw.isBlank()) JSONObject() else JSONObject(raw)
        }
    }
}
