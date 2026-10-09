package com.postiz.mobile.data.remote

import com.postiz.mobile.data.local.Session
import com.postiz.mobile.data.local.SessionManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds (and rebuilds, if the stored server URL / token changes) the
 * Retrofit client at runtime, because -- unlike a normal app -- the base
 * URL isn't known at compile time. It's whatever the self-hoster typed
 * into the Connect screen.
 */
@Singleton
class PostizApiProvider @Inject constructor(
    private val sessionManager: SessionManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
        // The backend validates several request fields with @IsDefined()
        // even when they're commonly empty/false (e.g. CreatePostRequestDto's
        // shortLink/tags) -- encodeDefaults=false (the kotlinx default) would
        // silently drop them from the JSON body whenever they equal their
        // Kotlin default, causing a 400 the server-side validator can't
        // explain any better than "Bad Request".
        encodeDefaults = true
    }

    /**
     * Client for the presigned storage URLs the multipart upload hands out.
     * It deliberately has no Authorization interceptor: the URL is already
     * signed, and an extra header would be rejected by the storage.
     */
    val storageClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .build()
    }

    @Volatile private var cachedKey: String? = null
    @Volatile private var cachedService: PostizApiService? = null

    suspend fun getService(): PostizApiService {
        val session = sessionManager.currentSession()
        val key = "${session.serverUrl}|${session.apiToken}|${session.isCloud}"
        val existing = cachedService
        if (existing != null && cachedKey == key) return existing

        val built = build(session)
        cachedKey = key
        cachedService = built
        return built
    }

    /** Force a rebuild on the next call, e.g. right after saving a new session. */
    fun invalidate() {
        cachedKey = null
        cachedService = null
    }

    private fun build(session: Session): PostizApiService {
        val authInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", session.apiToken)
                .build()
            chain.proceed(request)
        }
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            // Video uploads can legitimately take minutes on a slow mobile
            // connection; 30s (the old value) was tight enough to abort a
            // real reel upload mid-transfer. These only bound how long a
            // single read/write op may stall, not the request as a whole,
            // so ordinary JSON calls aren't affected.
            .readTimeout(15, TimeUnit.MINUTES) // server may process/transfer a big file before replying
            .writeTimeout(10, TimeUnit.MINUTES)
            // No overall call deadline: a 1 GB video on mobile data can take
            // far longer than any fixed cap, as long as bytes keep moving.
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(session.serverUrl, session.isCloud))
            .client(client)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()

        return retrofit.create(PostizApiService::class.java)
    }

    companion object {
        /**
         * Self-hosted docs: https://{your-domain}/api/public/v1
         * Cloud docs:       https://api.postiz.com/public/v1
         * Users just type a bare domain/IP; we add scheme + path.
         */
        fun normalizeBaseUrl(raw: String, isCloud: Boolean): String {
            var url = raw.trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            url = url.trimEnd('/')

            val suffix = if (isCloud) "/public/v1" else "/api/public/v1"
            if (!url.endsWith(suffix)) {
                url += suffix
            }
            return "$url/"
        }
    }
}
