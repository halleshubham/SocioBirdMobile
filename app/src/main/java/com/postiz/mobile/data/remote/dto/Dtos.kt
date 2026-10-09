package com.postiz.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/*
 * Schema provenance, so nobody mistakes a guess for a documented fact:
 *
 * VERIFIED against the actual backend source
 * (shacky-postiz apps/backend/src/public-api/routes/v1/public.integrations.controller.ts
 * and libraries/nestjs-libraries/src/dtos/posts, the per-provider settings dtos):
 *   - ConnectionStatusDto, IntegrationDto (GET /integrations)
 *   - GetPostsResponseDto, PostDto, PostIntegrationSummaryDto (GET /posts)
 *   - CreatePostRequestDto, PostRequestItemDto (POST /posts) --
 *     startDate/endDate are REQUIRED (@IsDateString(), no @IsOptional()) on
 *     GET /posts; shortLink/tags are REQUIRED (@IsDefined()) on POST /posts
 *     even though they're commonly empty/false.
 *   - the upload response shape (POST /upload)
 *   - GroupDto (GET /groups, reuses CustomerDto: {id, name})
 *   - IntegrationSettingsResponseDto (GET /integration-settings/:id)
 *   - FindSlotResponseDto (GET /find-slot/:id)
 *   - ChangePostStatusRequestDto (PUT /posts/:id/status)
 *   - UploadFromUrlRequestDto (POST /upload-from-url)
 *   - SocialConnectResponseDto (GET /social/:integration)
 *   - AnalyticsDataDto (GET /analytics/:integration, GET /analytics/post/:postId)
 *
 * Response bodies whose shape is genuinely unknown (notifications) are left
 * as raw JsonElement in PostizApiService rather than force-fit into a class.
 */

/**
 * Providers the backend's create-post settings discriminator accepts
 * (libraries/nestjs-libraries/src/dtos/posts/providers-settings/all.providers.settings.ts).
 * There's no public "list available provider types" endpoint, so this is
 * used to populate the Add Channel picker; the server still has the final
 * say (GET /social/:id 400s with "Integration not allowed" for anything it
 * doesn't actually support on this instance).
 */
val KNOWN_PROVIDER_IDENTIFIERS = listOf(
    "x", "linkedin", "linkedin-page", "instagram", "instagram-standalone",
    "facebook", "threads", "mastodon", "bluesky", "telegram", "reddit",
    "lemmy", "youtube", "pinterest", "dribbble", "tiktok", "discord", "slack",
    "medium", "devto", "wordpress", "hashnode", "listmonk", "gmb", "wrapcast",
    "nostr", "vk"
)

@Serializable
data class ConnectionStatusDto(
    val connected: Boolean
)

@Serializable
data class CustomerDto(
    val id: String,
    val name: String
)

@Serializable
data class IntegrationDto(
    val id: String,
    val name: String,
    val identifier: String,
    val picture: String? = null,
    val disabled: Boolean = false,
    val profile: String? = null,
    val customer: CustomerDto? = null
)

@Serializable
data class UploadResponseDto(
    val id: String,
    val path: String
)

@Serializable
data class PostImageDto(
    val id: String,
    val path: String
)

@Serializable
data class PostValueDto(
    val content: String,
    val image: List<PostImageDto> = emptyList()
)

@Serializable
data class PostIntegrationRefDto(
    val id: String
)

@Serializable
data class PostRequestItemDto(
    val integration: PostIntegrationRefDto,
    val value: List<PostValueDto>,
    /** Must contain at least {"__type": "<platform identifier>"}; platforms
     *  like x/linkedin/instagram accept extra keys documented per-provider
     *  at https://docs.postiz.com/public-api/providers/<platform>. */
    val settings: JsonObject
)

@Serializable
data class CreatePostRequestDto(
    /** "now" | "schedule" | "draft" */
    val type: String,
    /** ISO-8601, e.g. 2025-01-01T10:00:00.000Z */
    val date: String,
    val shortLink: Boolean = false,
    val tags: List<String> = emptyList(),
    val posts: List<PostRequestItemDto>
)

/**
 * The nested `integration` object on a post is a DIFFERENT, narrower shape
 * than the top-level IntegrationDto returned by GET /integrations -- notably
 * the platform key is `providerIdentifier`, not `identifier`, and there's no
 * `disabled`/`profile`/`customer`.
 */
@Serializable
data class PostIntegrationSummaryDto(
    val id: String,
    val providerIdentifier: String? = null,
    val name: String? = null,
    val picture: String? = null
)

@Serializable
data class PostDto(
    val id: String,
    val state: String? = null,
    val publishDate: String? = null,
    val integration: PostIntegrationSummaryDto? = null,
    val content: String? = null
)

/** GET /posts replies with an envelope, not a bare array. */
@Serializable
data class GetPostsResponseDto(
    val posts: List<PostDto> = emptyList()
)

/**
 * GET /integration-settings/:id -- per-provider validation rules. `settings`
 * and `tools` vary by provider (arbitrary JSON schema / tool list), so they're
 * kept raw rather than force-fit into a class; only maxLength is used today
 * (character counter in the composer).
 */
@Serializable
data class IntegrationSettingsResponseDto(
    val output: IntegrationSettingsOutputDto
)

@Serializable
data class IntegrationSettingsOutputDto(
    val rules: String? = null,
    val maxLength: Int? = null,
    val settings: JsonElement? = null,
    val tools: JsonElement? = null
)

/** GET /find-slot/:id -- a suggested free ISO-8601 publish time for that channel. */
@Serializable
data class FindSlotResponseDto(
    val date: String
)

/** PUT /posts/:id/status -- only these two transitions are valid server-side. */
@Serializable
data class ChangePostStatusRequestDto(
    val status: String
)

@Serializable
data class UploadFromUrlRequestDto(
    val url: String
)

/*
 * POST /upload/<step> -- direct-to-storage multipart upload for large files.
 * The file's parts are PUT straight to the presigned URLs, so they never
 * pass through the server or the proxy in front of it (which caps request
 * bodies). Mirrors the web app's flow in the backend's r2.uploader.ts.
 */
@Serializable
data class MultipartFileDto(
    val name: String,
    val type: String
)

@Serializable
data class CreateMultipartUploadRequestDto(
    val file: MultipartFileDto,
    val contentType: String,
    val fileHash: String = ""
)

@Serializable
data class CreateMultipartUploadResponseDto(
    val uploadId: String,
    val key: String
)

@Serializable
data class SignPartRequestDto(
    val key: String,
    val uploadId: String,
    val partNumber: Int
)

@Serializable
data class SignPartResponseDto(
    val url: String
)

@Serializable
data class UploadedPartDto(
    @SerialName("PartNumber") val partNumber: Int,
    @SerialName("ETag") val eTag: String
)

@Serializable
data class CompleteMultipartUploadRequestDto(
    val key: String,
    val uploadId: String,
    val parts: List<UploadedPartDto>,
    val file: MultipartFileDto
)

/** Only the saved media record is read; the rest of the body is the storage response. */
@Serializable
data class CompleteMultipartUploadResponseDto(
    val saved: UploadResponseDto
)

@Serializable
data class AbortMultipartUploadRequestDto(
    val key: String,
    val uploadId: String
)

/** GET /social/:integration -- the OAuth URL to open in a browser to connect a new channel. */
@Serializable
data class SocialConnectResponseDto(
    val url: String
)

@Serializable
data class AnalyticsPointDto(
    val total: String,
    val date: String
)

@Serializable
data class AnalyticsDataDto(
    val label: String,
    val data: List<AnalyticsPointDto> = emptyList(),
    val percentageChange: Double = 0.0
)

/**
 * Error response bodies are inconsistent server-side: most handlers throw
 * HttpException({msg: "..."}, code), but post-creation validation throws
 * {statusCode, provider, name, message}. Both fields are optional here so one
 * parse attempt covers either shape.
 */
@Serializable
data class ApiErrorBodyDto(
    val msg: String? = null,
    val message: String? = null
)
