package me.ilker.balance_tracker.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AuthRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(
    val token: String,
    val expiresAt: String,
    val message: String
)

@Serializable
private data class MessageResponse(val message: String)

internal class AuthApi(
    private val client: HttpClient,
    private val baseUrl: String,
    private val authRepository: AuthRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun authenticate(email: String, password: String) {
        val response = client.post("$baseUrl/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AuthRequest(email, password)))
        }
        when (response.status) {
            HttpStatusCode.Created -> {
                saveSession(response, email)
                return
            }
            HttpStatusCode.Conflict -> login(email, password)
            else -> throw AuthException(decodeMessage(response))
        }
    }

    private suspend fun login(email: String, password: String) {
        val response = client.post("$baseUrl/login") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AuthRequest(email, password)))
        }
        if (response.status != HttpStatusCode.OK) {
            throw AuthException(decodeMessage(response))
        }
        saveSession(response, email)
    }

    private suspend fun saveSession(response: HttpResponse, email: String) {
        val auth = json.decodeFromString<AuthResponse>(response.bodyAsText())
        authRepository.save(
            email = email,
            token = auth.token,
            expiresAt = Instant.parse(auth.expiresAt)
        )
    }

    private suspend fun decodeMessage(response: HttpResponse): String = try {
        json.decodeFromString<MessageResponse>(response.bodyAsText()).message
    } catch (_: Exception) {
        response.bodyAsText()
    }
}

class AuthException(message: String) : Exception(message)
