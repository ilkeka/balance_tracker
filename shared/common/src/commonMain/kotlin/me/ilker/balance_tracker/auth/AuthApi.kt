package me.ilker.balance_tracker.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AuthRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val message: String)

class AuthApi(private val baseUrl: String) {
    private val client = HttpClient {
        install(HttpCookies)
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _sessionEmail = MutableStateFlow<String?>(null)
    val sessionEmail: StateFlow<String?> = _sessionEmail.asStateFlow()

    suspend fun authenticate(email: String, password: String) {
        val response = client.post("$baseUrl/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AuthRequest(email, password)))
        }
        if (response.status == HttpStatusCode.Created) {
            _sessionEmail.value = email
            return
        }
        if (response.status == HttpStatusCode.Conflict) {
            login(email, password)
            return
        }
        val msg = try {
            json.decodeFromString<AuthResponse>(response.bodyAsText()).message
        } catch (_: Exception) {
            response.bodyAsText()
        }
        throw AuthException(msg)
    }

    private suspend fun login(email: String, password: String) {
        val response = client.post("$baseUrl/login") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AuthRequest(email, password)))
        }
        if (response.status != HttpStatusCode.OK) {
            val msg = try {
                json.decodeFromString<AuthResponse>(response.bodyAsText()).message
            } catch (_: Exception) {
                response.bodyAsText()
            }
            throw AuthException(msg)
        }
        _sessionEmail.value = email
    }
}

class AuthException(message: String) : Exception(message)
