package com.postiz.mobile.data.remote

import com.postiz.mobile.data.remote.dto.AbortMultipartUploadRequestDto
import com.postiz.mobile.data.remote.dto.AnalyticsDataDto
import com.postiz.mobile.data.remote.dto.ChangePostStatusRequestDto
import com.postiz.mobile.data.remote.dto.ConnectionStatusDto
import com.postiz.mobile.data.remote.dto.CompleteMultipartUploadRequestDto
import com.postiz.mobile.data.remote.dto.CompleteMultipartUploadResponseDto
import com.postiz.mobile.data.remote.dto.CreateMultipartUploadRequestDto
import com.postiz.mobile.data.remote.dto.CreateMultipartUploadResponseDto
import com.postiz.mobile.data.remote.dto.CreatePostRequestDto
import com.postiz.mobile.data.remote.dto.CustomerDto
import com.postiz.mobile.data.remote.dto.FindSlotResponseDto
import com.postiz.mobile.data.remote.dto.GetPostsResponseDto
import com.postiz.mobile.data.remote.dto.IntegrationDto
import com.postiz.mobile.data.remote.dto.IntegrationSettingsResponseDto
import com.postiz.mobile.data.remote.dto.SignPartRequestDto
import com.postiz.mobile.data.remote.dto.SignPartResponseDto
import com.postiz.mobile.data.remote.dto.SocialConnectResponseDto
import com.postiz.mobile.data.remote.dto.UploadFromUrlRequestDto
import com.postiz.mobile.data.remote.dto.UploadResponseDto
import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mirrors https://docs.postiz.com/public-api, verified against the actual
 * PublicIntegrationsController source rather than the docs alone (see
 * Dtos.kt's provenance header). All paths are relative to
 * PostizApiProvider's base URL, which already ends in
 * ".../api/public/v1/" (self-hosted) or ".../public/v1/" (cloud).
 */
interface PostizApiService {

    @GET("is-connected")
    suspend fun checkConnection(): ConnectionStatusDto

    @GET("integrations")
    suspend fun getIntegrations(
        @Query("group") group: String? = null
    ): List<IntegrationDto>

    @DELETE("integrations/{id}")
    suspend fun deleteIntegration(@Path("id") id: String): Response<Unit>

    @GET("integration-settings/{id}")
    suspend fun getIntegrationSettings(@Path("id") id: String): IntegrationSettingsResponseDto

    /** The OAuth URL to open in a browser to connect a new channel of this provider type. */
    @GET("social/{integration}")
    suspend fun getSocialConnectUrl(@Path("integration") integration: String): SocialConnectResponseDto

    /** Brand/customer groups (same concept as the web app's brand filter). */
    @GET("groups")
    suspend fun getGroups(): List<CustomerDto>

    /** startDate/endDate are REQUIRED server-side (@IsDateString(), not @IsOptional()). */
    @GET("posts")
    suspend fun getPosts(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("customer") customer: String? = null
    ): GetPostsResponseDto

    @POST("posts")
    suspend fun createPost(@Body request: CreatePostRequestDto): JsonElement

    @DELETE("posts/{id}")
    suspend fun deletePost(@Path("id") id: String): Response<Unit>

    @PUT("posts/{id}/status")
    suspend fun changePostStatus(
        @Path("id") id: String,
        @Body request: ChangePostStatusRequestDto
    ): Response<Unit>

    /** id is required by the route even though it's typed optional server-side (see PostizRepository). */
    @GET("find-slot/{id}")
    suspend fun findSlot(@Path("id") integrationId: String): FindSlotResponseDto

    @Multipart
    @POST("upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): UploadResponseDto

    @POST("upload/create-multipart-upload")
    suspend fun createMultipartUpload(@Body request: CreateMultipartUploadRequestDto): CreateMultipartUploadResponseDto

    @POST("upload/sign-part")
    suspend fun signPart(@Body request: SignPartRequestDto): SignPartResponseDto

    @POST("upload/complete-multipart-upload")
    suspend fun completeMultipartUpload(@Body request: CompleteMultipartUploadRequestDto): CompleteMultipartUploadResponseDto

    @POST("upload/abort-multipart-upload")
    suspend fun abortMultipartUpload(@Body request: AbortMultipartUploadRequestDto): Response<Unit>

    @POST("upload-from-url")
    suspend fun uploadFromUrl(@Body request: UploadFromUrlRequestDto): UploadResponseDto

    /** date = number of days of history, e.g. "7", "30" (matches the web app's dateRange). */
    @GET("analytics/{integration}")
    suspend fun getChannelAnalytics(
        @Path("integration") integrationId: String,
        @Query("date") days: String
    ): List<AnalyticsDataDto>

    /** Returns AnalyticsData[] normally, but {"missing": true} when the post has no releaseId yet. */
    @GET("analytics/post/{postId}")
    suspend fun getPostAnalytics(
        @Path("postId") postId: String,
        @Query("date") days: String
    ): JsonElement

    /** Shape not yet confirmed -- returned raw so parsing never crashes the app. */
    @GET("notifications")
    suspend fun getNotifications(@Query("page") page: Int? = null): JsonElement
}
