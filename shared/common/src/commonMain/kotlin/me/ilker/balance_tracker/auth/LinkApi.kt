package me.ilker.balance_tracker.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LinkTokenResponse(val token: String)

@Serializable
data class LinkRequest(val token: String)

@Serializable
data class LinkResponse(val message: String)

class LinkApi(
    private val client: HttpClient,
    private val baseUrl: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    @Throws(Exception::class)
    suspend fun getLinkToken(): String {
        val response = client.get("$baseUrl/link/token")
        if (response.status != HttpStatusCode.OK) {
            throw LinkException(decodeMessage(response))
        }
        return json.decodeFromString<LinkTokenResponse>(response.bodyAsText()).token
    }

    @Throws(Exception::class)
    suspend fun link(token: String) {
        val response = client.post("$baseUrl/link") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(LinkRequest(token)))
        }
        if (response.status != HttpStatusCode.OK) {
            throw LinkException(decodeMessage(response))
        }
    }

    private suspend fun decodeMessage(response: HttpResponse): String = try {
        json.decodeFromString<LinkResponse>(response.bodyAsText()).message
    } catch (_: Exception) {
        response.bodyAsText()
    }
}

class LinkException(message: String) : Exception(message)
