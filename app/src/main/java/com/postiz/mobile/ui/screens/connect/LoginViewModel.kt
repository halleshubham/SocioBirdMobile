package com.postiz.mobile.ui.screens.connect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postiz.mobile.BuildConfig
import com.postiz.mobile.data.local.SessionManager
import com.postiz.mobile.data.remote.PostizApiProvider
import com.postiz.mobile.data.repository.PostizRepository
import com.postiz.mobile.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Same flow as the Postiz web app's login page: POST /auth/login with
 * {email, password, provider: "LOCAL"} returns the session JWT (as the `auth`
 * cookie). The JWT then authorises GET /users/self, whose `publicApi` field is
 * the organisation's API key (admins only) -- which is all the rest of this
 * app needs, so the user never has to find or paste a developer API key.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiProvider: PostizApiProvider,
    private val repository: PostizRepository
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    val serverUrl: String = BuildConfig.LOCKED_SERVER_URL

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    fun onEmailChange(value: String) {
        uiState = uiState.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value, error = null)
    }

    fun signIn() {
        val email = uiState.email.trim()
        val password = uiState.password
        if (email.isBlank() || password.isBlank()) {
            uiState = uiState.copy(error = "Enter your email and password")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            val outcome = try {
                withContext(Dispatchers.IO) { loginAndFetchKey(email, password) }
            } catch (e: IOException) {
                Outcome.Failure("Can't reach ${serverUrl.removePrefix("https://")}. Check your connection.")
            } catch (e: Exception) {
                Outcome.Failure(e.message ?: "Something went wrong")
            }
            when (outcome) {
                is Outcome.Failure -> uiState = uiState.copy(isLoading = false, error = outcome.message)
                is Outcome.Key -> {
                    sessionManager.saveSession(serverUrl, outcome.apiKey, isCloud = false)
                    apiProvider.invalidate()
                    when (val check = repository.checkConnection()) {
                        is Resource.Success ->
                            if (check.data) {
                                // sessionManager's Flow now emits a valid session; nav swaps to the main app.
                                uiState = uiState.copy(isLoading = false, password = "")
                            } else {
                                sessionManager.clearSession()
                                uiState = uiState.copy(isLoading = false, error = "Signed in, but the server rejected the API key")
                            }
                        is Resource.Error -> {
                            sessionManager.clearSession()
                            uiState = uiState.copy(isLoading = false, error = check.message)
                        }
                        Resource.Loading -> Unit
                    }
                }
            }
        }
    }

    private sealed interface Outcome {
        data class Key(val apiKey: String) : Outcome
        data class Failure(val message: String) : Outcome
    }

    private fun loginAndFetchKey(email: String, password: String): Outcome {
        val body = buildJsonObject {
            put("email", JsonPrimitive(email))
            put("password", JsonPrimitive(password))
            put("provider", JsonPrimitive("LOCAL"))
        }.toString().toRequestBody("application/json".toMediaType())

        val loginRequest = Request.Builder().url("$serverUrl/api/auth/login").post(body).build()
        val jwt = client.newCall(loginRequest).execute().use { response ->
            if (!response.isSuccessful) {
                val text = response.body?.string().orEmpty().trim()
                return Outcome.Failure(
                    when {
                        text.isNotEmpty() && text.length < 200 && !text.startsWith("<") -> text
                        response.code == 429 -> "Too many attempts – try again in a bit"
                        else -> "Sign-in failed (${response.code})"
                    }
                )
            }
            // Secure deployments only send the JWT as a Set-Cookie; NOT_SECURED ones also as an `auth` header.
            response.header("auth")
                ?: response.headers("Set-Cookie")
                    .firstOrNull { it.startsWith("auth=") }
                    ?.substringAfter("auth=")?.substringBefore(";")
        }
        if (jwt.isNullOrBlank()) return Outcome.Failure("Signed in, but no session token was returned")

        val selfRequest = Request.Builder().url("$serverUrl/api/user/self").header("auth", jwt).get().build()
        client.newCall(selfRequest).execute().use { response ->
            if (!response.isSuccessful) return Outcome.Failure("Couldn't load your account (${response.code})")
            val self: JsonObject = json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            val key = self["publicApi"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (key.isBlank()) {
                return Outcome.Failure("Your account's role can't access the API key. Sign in as an organisation admin.")
            }
            return Outcome.Key(key)
        }
    }
}
