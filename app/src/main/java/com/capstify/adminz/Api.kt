package com.capstify.adminz

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://adminz-capstify.adminzcapstify.workers.dev/"

class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs = context.getSharedPreferences("capstify_session", Context.MODE_PRIVATE)

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val existing = prefs.getStringSet("cookies", emptySet())?.toMutableSet() ?: mutableSetOf()
        cookies.forEach { c ->
            if (c.name == "__Host-capstify_session") {
                existing.removeAll { it.startsWith(c.name + "=") }
                if (!c.expiresAt.let { it <= System.currentTimeMillis() }) {
                    existing.add(c.toString())
                }
            }
        }
        prefs.edit().putStringSet("cookies", existing).apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val stored = prefs.getStringSet("cookies", emptySet()) ?: emptySet()
        val parsed = stored.mapNotNull { Cookie.parse(url, it) }
            .filter { it.expiresAt > System.currentTimeMillis() && it.matches(url) }
        if (parsed.isEmpty() && stored.isNotEmpty()) prefs.edit().remove("cookies").apply()
        return parsed
    }

    fun clear() = prefs.edit().remove("cookies").apply()
}

class CapstifyApi(context: Context) {
    private val jar = PersistentCookieJar(context)
    private val client = OkHttpClient.Builder()
        .cookieJar(jar)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    @Throws(IOException::class)
    fun get(path: String): JSONObject = request("GET", path, null)

    @Throws(IOException::class)
    fun post(path: String, body: JSONObject): JSONObject = request("POST", path, body)

    private fun request(method: String, path: String, body: JSONObject?): JSONObject {
        val url = BASE_URL.toHttpUrl().newBuilder().addPathSegments(path.removePrefix("/")).build()
        val builder = Request.Builder().url(url)
            .header("Accept", "application/json")
        if (method == "POST") {
            builder.post((body ?: JSONObject()).toString().toRequestBody(jsonType))
        } else builder.get()

        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val data = try { JSONObject(raw) } catch (_: Exception) {
                JSONObject().put("message", "Respons server tidak dapat dibaca.")
            }
            if (!response.isSuccessful) {
                throw IOException(data.optString("message").ifBlank { "Koneksi gagal (${response.code})." })
            }
            if (data.optBoolean("success", true).not()) {
                throw IOException(data.optString("message").ifBlank { "Permintaan gagal." })
            }
            return data
        }
    }

    fun clearSession() = jar.clear()
}
