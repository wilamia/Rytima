package com.example.rytima.feature.feed

import androidx.compose.runtime.Immutable
import com.example.rytima.core.navigation.FeedComposerPrefill
import com.example.rytima.core.model.UserRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FeedPostType {
    @SerialName("COACH_TIP") COACH_TIP,
    @SerialName("WORKOUT_RECAP") WORKOUT_RECAP,
    @SerialName("PR") PR,
    @SerialName("CLIENT_MILESTONE") CLIENT_MILESTONE,
    @SerialName("FORM_CHECK_REQUEST") FORM_CHECK_REQUEST,
}

@Immutable
enum class FeedBrowseFilter {
    ALL,
    TRAINERS,
    ATHLETES,
    PRS,
    TIPS,
}

@Serializable
@Immutable
data class FeedAuthor(
    val userId: String,
    val role: UserRole,
    val displayName: String,
    val handle: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
@Immutable
data class FeedPost(
    val id: String,
    val author: FeedAuthor,
    val type: FeedPostType,
    val caption: String,
    val mediaUrls: List<String> = emptyList(),
    val commentCount: Int = 0,
    val likeCount: Int = 0,
    val saveCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val createdAt: String,
)

@Serializable
@Immutable
data class FeedComment(
    val id: String,
    val postId: String,
    val author: FeedAuthor,
    val body: String,
    val createdAt: String,
)

@Immutable
data class PendingFeedComment(
    val localId: String,
    val body: String,
)

@Serializable
data class FeedPage(
    val items: List<FeedPost> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class CreateFeedPostRequest(
    val type: FeedPostType,
    val caption: String,
    val mediaUrls: List<String> = emptyList(),
)

@Serializable
data class CreateFeedCommentRequest(
    val body: String,
)

@Immutable
enum class FeedComposerAttachmentMode {
    PHOTO,
    VIDEO,
    COAUTHOR,
}

@Immutable
data class FeedComposerState(
    val type: FeedPostType = FeedPostType.COACH_TIP,
    val caption: String = "",
    val attachmentMode: FeedComposerAttachmentMode = FeedComposerAttachmentMode.PHOTO,
    val mediaDraft: String = "",
    val mediaUrls: List<String> = emptyList(),
    val coauthorDraft: String = "",
    val isUploadingMedia: Boolean = false,
    val isSubmitting: Boolean = false,
)

@Immutable
data class FeedState(
    val posts: List<FeedPost> = emptyList(),
    val currentAuthor: FeedAuthor? = null,
    val selectedFilter: FeedBrowseFilter = FeedBrowseFilter.ALL,
    val pendingLikePostIds: Set<String> = emptySet(),
    val pendingSavePostIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextCursor: String? = null,
    val errorMessage: String? = null,
    val composer: FeedComposerState = FeedComposerState(),
    val isComposerOpen: Boolean = false,
    val selectedPostId: String? = null,
    val comments: List<FeedComment> = emptyList(),
    val pendingComment: PendingFeedComment? = null,
    val isCommentsLoading: Boolean = false,
    val commentDraft: String = "",
    val isCommentSubmitting: Boolean = false,
)

sealed interface FeedIntent {
    data object Load : FeedIntent
    data object LoadCurrentAuthor : FeedIntent
    data object Refresh : FeedIntent
    data object LoadNextPage : FeedIntent
    data object OpenComposer : FeedIntent
    data class OpenComposerWithPrefill(val value: FeedComposerPrefill) : FeedIntent
    data object CloseComposer : FeedIntent
    data class SelectFilter(val value: FeedBrowseFilter) : FeedIntent
    data class UpdateComposerCaption(val value: String) : FeedIntent
    data class SelectComposerAttachmentMode(val value: FeedComposerAttachmentMode) : FeedIntent
    data class UpdateComposerMediaDraft(val value: String) : FeedIntent
    data class UpdateComposerCoauthorDraft(val value: String) : FeedIntent
    data object AddComposerMedia : FeedIntent
    data class AttachComposerMedia(val value: String) : FeedIntent
    data class RemoveComposerMedia(val value: String) : FeedIntent
    data class SelectComposerType(val value: FeedPostType) : FeedIntent
    data object SubmitPost : FeedIntent
    data class ToggleLike(val postId: String) : FeedIntent
    data class ToggleSave(val postId: String) : FeedIntent
    data class OpenComments(val postId: String) : FeedIntent
    data object CloseComments : FeedIntent
    data class UpdateCommentDraft(val value: String) : FeedIntent
    data object SubmitComment : FeedIntent
    data object ClearError : FeedIntent
}

sealed interface FeedEffect {
    data class Message(val text: String) : FeedEffect
}
