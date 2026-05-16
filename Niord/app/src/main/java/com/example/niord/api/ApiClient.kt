package com.example.niord.api

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.date.GMTDate
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap


object ApiClient {
    private const val BASE_URL = "http://192.168.100.25/"
    fun createHttpClient(context: Context): HttpClient {
        return HttpClient(OkHttp) {
            // 1. Serialization (JSON to Data Classes)
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }

            install(HttpCookies) {
                storage = PersistentCookieStorage(context)
            }

            // 2. Logging (Great for debugging)
            install(Logging) {
                level = LogLevel.BODY
            }

            // 3. Optional: Default Request (Base URL)
            defaultRequest {
                url(BASE_URL)
            }
        }
    }
}


class PersistentCookieStorage(context: Context) : CookiesStorage {
    private val sharedPrefs = context.getSharedPreferences("app_cookies", Context.MODE_PRIVATE)
    private val cookiesCache = ConcurrentHashMap<String, Cookie>()

    init {
        loadCookieFromStorage("access_token")
        loadCookieFromStorage("refresh_token")
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        val now = System.currentTimeMillis()

        return cookiesCache.values.filter { cookie ->
            matchesDomain(cookie.domain, requestUrl.host) &&
                    matchesPath(cookie.path, requestUrl.encodedPath) &&
                    (cookie.expires == null || cookie.expires!!.timestamp > now)
        }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        //if (cookie.name != "access_token" && cookie.name != "refresh_token") return

        // Fallback to request host/path if the cookie doesn't explicitly define them
        val cookieDomain = cookie.domain ?: requestUrl.host
        val cookiePath = cookie.path ?: "/"

        val normalizedCookie = cookie.copy(domain = cookieDomain, path = cookiePath)

        cookiesCache[cookie.name] = normalizedCookie
        saveCookieToStorage(normalizedCookie)
    }

    override fun close() {
        cookiesCache.clear()
    }

    //   Private Helpers

    private fun loadCookieFromStorage(name: String) {
        val value = sharedPrefs.getString("${name}_value", null) ?: return
        val domain = sharedPrefs.getString("${name}_domain", null)
        val path = sharedPrefs.getString("${name}_path", "/")
        val expiresTimestamp = sharedPrefs.getLong("${name}_expires", -1L)

        val expiresDate = if (expiresTimestamp != -1L) GMTDate(expiresTimestamp) else null

        cookiesCache[name] = Cookie(
            name = name,
            value = value,
            domain = domain,
            path = path,
            expires = expiresDate
        )
    }

    private fun saveCookieToStorage(cookie: Cookie) {
        sharedPrefs.edit().apply {
            putString("${cookie.name}_value", cookie.value)
            putString("${cookie.name}_domain", cookie.domain)
            putString("${cookie.name}_path", cookie.path)
            if (cookie.expires != null) {
                putLong("${cookie.name}_expires", cookie.expires!!.timestamp)
            } else {
                remove("${cookie.name}_expires")
            }
            apply()
        }
    }

    private fun matchesDomain(cookieDomain: String?, requestHost: String): Boolean {
        if (cookieDomain == null) return true
        val host = requestHost.lowercase()
        val domain = cookieDomain.lowercase().removePrefix(".")
        return host == domain || host.endsWith(".$domain")
    }

    private fun matchesPath(cookiePath: String?, requestPath: String): Boolean {
        val path = cookiePath ?: "/"
        val reqPath = requestPath.ifEmpty { "/" }
        return reqPath.startsWith(path)
    }
}