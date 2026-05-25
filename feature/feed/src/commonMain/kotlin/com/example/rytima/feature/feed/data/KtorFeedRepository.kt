package com.example.rytima.feature.feed.data

import com.example.rytima.core.model.UserRole
import com.example.rytima.feature.feed.CreateFeedCommentRequest
import com.example.rytima.feature.feed.CreateFeedPostRequest
import com.example.rytima.feature.feed.FeedComment
import com.example.rytima.feature.feed.FeedAuthor
import com.example.rytima.feature.feed.FeedPage
import com.example.rytima.feature.feed.FeedPost
import com.example.rytima.feature.feed.FeedPostType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class KtorFeedRepository(
    private val supabaseClient: SupabaseClient,
) : FeedRepository {

    override suspend fun getCurrentAuthor(): FeedAuthor? {
        return supabaseClient.postgrest.rpc("current_feed_author")
            .decodeSingleOrNull<FeedAuthorRpcRow>()
            ?.toAuthor()
    }

    override suspend fun loadFeed(cursor: String?, limit: Int): FeedPage {
        val rows = supabaseClient.postgrest.rpc(
            function = "feed_posts_page",
            parameters = FeedPageRpcParameters(
                cursorText = cursor,
                limitCount = limit.coerceIn(1, 50),
            ),
        ).decodeList<FeedPostRpcRow>()

        return FeedPage(
            items = rows.map { it.toFeedPost() },
            nextCursor = rows.lastOrNull()?.createdAt.takeIf { rows.size >= limit },
        )
    }

    override suspend fun loadProfileFeed(
        profileType: String,
        profileId: String,
        cursor: String?,
        limit: Int,
    ): FeedPage {
        val rows = supabaseClient.postgrest.rpc(
            function = "profile_feed_posts",
            parameters = ProfileFeedPageRpcParameters(
                profileType = profileType,
                profileId = profileId,
                cursorText = cursor,
                limitCount = limit.coerceIn(1, 30),
            ),
        ).decodeList<FeedPostRpcRow>()

        return FeedPage(
            items = rows.map { it.toFeedPost() },
            nextCursor = rows.lastOrNull()?.createdAt.takeIf { rows.size >= limit },
        )
    }

    override suspend fun createPost(request: CreateFeedPostRequest): FeedPost {
        return supabaseClient.postgrest.rpc(
            function = "create_feed_post",
            parameters = CreateFeedPostRpcParameters(
                payload = CreateFeedPostRpcPayload(
                    type = request.type.name,
                    caption = request.caption,
                    mediaUrls = request.mediaUrls,
                ),
            ),
        ).decodeSingle<FeedPostRpcRow>().toFeedPost()
    }

    override suspend fun uploadMediaDataUri(dataUri: String): String {
        FeedMediaDataUri.parse(dataUri)
            ?: error("Only valid image data URIs can be uploaded.")
        error("Supabase Storage upload is not configured yet. Use a media URL for now.")
    }

    override suspend fun toggleLike(postId: String): FeedPost {
        return supabaseClient.postgrest.rpc(
            function = "toggle_feed_post_like",
            parameters = FeedPostIdRpcParameters(postId = postId),
        ).decodeSingle<FeedPostRpcRow>().toFeedPost()
    }

    override suspend fun toggleSave(postId: String): FeedPost {
        return supabaseClient.postgrest.rpc(
            function = "toggle_feed_post_save",
            parameters = FeedPostIdRpcParameters(postId = postId),
        ).decodeSingle<FeedPostRpcRow>().toFeedPost()
    }

    override suspend fun loadComments(postId: String): List<FeedComment> {
        return supabaseClient.postgrest.rpc(
            function = "feed_post_comments",
            parameters = FeedCommentPostIdRpcParameters(targetPostId = postId),
        ).decodeList<FeedCommentRpcRow>().map { it.toFeedComment() }
    }

    override suspend fun createComment(postId: String, request: CreateFeedCommentRequest): FeedComment {
        return supabaseClient.postgrest.rpc(
            function = "create_feed_post_comment",
            parameters = CreateFeedCommentRpcParameters(
                targetPostId = postId,
                commentBody = request.body,
            ),
        ).decodeSingle<FeedCommentRpcRow>().toFeedComment()
    }

    private fun FeedPostRpcRow.toFeedPost(): FeedPost =
        FeedPost(
            id = id,
            author = toAuthor(),
            type = postType.toFeedPostType(),
            caption = caption,
            mediaUrls = mediaUrls,
            commentCount = commentCount,
            likeCount = likeCount,
            saveCount = saveCount,
            isLiked = isLiked,
            isSaved = isSaved,
            createdAt = createdAt,
        )

    private fun FeedCommentRpcRow.toFeedComment(): FeedComment =
        FeedComment(
            id = id,
            postId = postId,
            author = toAuthor(),
            body = body,
            createdAt = createdAt,
        )

    private fun FeedPostRpcRow.toAuthor(): FeedAuthor =
        FeedAuthor(
            userId = authorUserId,
            role = authorRole.toUserRole(),
            displayName = authorDisplayName.ifBlank { "Rytima user" },
            handle = authorHandle?.takeIf { it.isNotBlank() },
            avatarUrl = authorAvatarUrl?.takeIf { it.isNotBlank() },
        )

    private fun FeedAuthorRpcRow.toAuthor(): FeedAuthor =
        FeedAuthor(
            userId = userId,
            role = role.toUserRole(),
            displayName = displayName.ifBlank { "Rytima user" },
            handle = handle?.takeIf { it.isNotBlank() },
            avatarUrl = avatarUrl?.takeIf { it.isNotBlank() },
        )

    private fun FeedCommentRpcRow.toAuthor(): FeedAuthor =
        FeedAuthor(
            userId = authorUserId,
            role = authorRole.toUserRole(),
            displayName = authorDisplayName.ifBlank { "Rytima user" },
            handle = authorHandle?.takeIf { it.isNotBlank() },
            avatarUrl = authorAvatarUrl?.takeIf { it.isNotBlank() },
        )

    private fun String.toUserRole(): UserRole =
        UserRole.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: UserRole.CLIENT

    private fun String.toFeedPostType(): FeedPostType =
        FeedPostType.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: FeedPostType.COACH_TIP
}

@Serializable
private data class FeedPageRpcParameters(
    @SerialName("cursor_text") val cursorText: String? = null,
    @SerialName("limit_count") val limitCount: Int = 20,
)

@Serializable
private data class ProfileFeedPageRpcParameters(
    @SerialName("profile_type") val profileType: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("cursor_text") val cursorText: String? = null,
    @SerialName("limit_count") val limitCount: Int = 12,
)

@Serializable
private data class CreateFeedPostRpcParameters(
    val payload: CreateFeedPostRpcPayload,
)

@Serializable
private data class CreateFeedPostRpcPayload(
    val type: String,
    val caption: String,
    @SerialName("media_urls") val mediaUrls: List<String> = emptyList(),
)

@Serializable
private data class FeedPostIdRpcParameters(
    @SerialName("post_id") val postId: String,
)

@Serializable
private data class FeedCommentPostIdRpcParameters(
    @SerialName("target_post_id") val targetPostId: String,
)

@Serializable
private data class CreateFeedCommentRpcParameters(
    @SerialName("target_post_id") val targetPostId: String,
    @SerialName("comment_body") val commentBody: String,
)

@Serializable
private data class FeedAuthorRpcRow(
    @SerialName("user_id") val userId: String,
    @SerialName("role") val role: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("handle") val handle: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
private data class FeedPostRpcRow(
    val id: String,
    @SerialName("author_user_id") val authorUserId: String,
    @SerialName("author_role") val authorRole: String,
    @SerialName("author_display_name") val authorDisplayName: String,
    @SerialName("author_handle") val authorHandle: String? = null,
    @SerialName("author_avatar_url") val authorAvatarUrl: String? = null,
    @SerialName("post_type") val postType: String,
    val caption: String,
    @SerialName("media_urls") val mediaUrls: List<String> = emptyList(),
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("save_count") val saveCount: Int = 0,
    @SerialName("is_liked") val isLiked: Boolean = false,
    @SerialName("is_saved") val isSaved: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
private data class FeedCommentRpcRow(
    val id: String,
    @SerialName("post_id") val postId: String,
    @SerialName("author_user_id") val authorUserId: String,
    @SerialName("author_role") val authorRole: String,
    @SerialName("author_display_name") val authorDisplayName: String,
    @SerialName("author_handle") val authorHandle: String? = null,
    @SerialName("author_avatar_url") val authorAvatarUrl: String? = null,
    val body: String,
    @SerialName("created_at") val createdAt: String,
)
