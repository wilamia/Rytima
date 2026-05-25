package com.example.rytima.feature.feed

import com.example.rytima.core.foundation.AnalyticsEvent
import com.example.rytima.core.foundation.AnalyticsTracker
import com.example.rytima.core.designsystem.AvatarUtils
import com.example.rytima.core.mvi.SimpleMviStore
import com.example.rytima.core.navigation.FeedComposerPrefill
import com.example.rytima.feature.feed.data.FeedRepository
import kotlinx.datetime.Clock

/**
 * MVI Store for the community feed.
 *
 * Splits intent handling into two phases:
 *  - [handleImmediateIntent]: synchronous state updates for UI inputs (e.g. typing).
 *  - [handleIntent]: suspending actions involving I/O (loading, publishing, optimistic toggles).
 *
 * Optimistic updates (like / save / comment count) apply locally before the network confirms;
 * on failure, the state is rolled back transparently and a user-facing message is emitted.
 *
 * In-flight protection: per-post sets ([FeedState.pendingLikePostIds], [FeedState.pendingSavePostIds])
 * prevent double-tap spam from triggering duplicate requests.
 */
class FeedStore(
    private val repository: FeedRepository,
    private val analyticsTracker: AnalyticsTracker,
) : SimpleMviStore<FeedState, FeedIntent, FeedEffect>(FeedState()) {

    init {
        dispatch(FeedIntent.Load)
        dispatch(FeedIntent.LoadCurrentAuthor)
    }

    override fun handleImmediateIntent(intent: FeedIntent): Boolean {
        when (intent) {
            is FeedIntent.UpdateComposerCaption -> setState {
                it.copy(composer = it.composer.copy(caption = intent.value))
            }
            is FeedIntent.SelectComposerAttachmentMode -> setState {
                it.copy(composer = it.composer.copy(attachmentMode = intent.value))
            }
            is FeedIntent.UpdateComposerMediaDraft -> setState {
                it.copy(composer = it.composer.copy(mediaDraft = intent.value))
            }
            is FeedIntent.UpdateComposerCoauthorDraft -> setState {
                it.copy(composer = it.composer.copy(coauthorDraft = intent.value))
            }
            is FeedIntent.UpdateCommentDraft -> setState {
                it.copy(commentDraft = intent.value)
            }
            else -> return false
        }
        return true
    }

    override suspend fun handleIntent(intent: FeedIntent) {
        when (intent) {
            FeedIntent.Load -> load(initial = true)
            FeedIntent.LoadCurrentAuthor -> loadCurrentAuthor()
            FeedIntent.Refresh -> refresh()
            FeedIntent.LoadNextPage -> loadNextPage()
            FeedIntent.OpenComposer -> setState { it.copy(isComposerOpen = true) }
            is FeedIntent.OpenComposerWithPrefill -> openComposerWithPrefill(intent.value)
            FeedIntent.CloseComposer -> setState {
                it.copy(
                    isComposerOpen = false,
                    composer = FeedComposerState(),
                )
            }
            is FeedIntent.SelectFilter -> setState { it.copy(selectedFilter = intent.value) }
            is FeedIntent.UpdateComposerCaption -> Unit
            is FeedIntent.SelectComposerAttachmentMode -> Unit
            is FeedIntent.UpdateComposerMediaDraft -> Unit
            is FeedIntent.UpdateComposerCoauthorDraft -> Unit
            FeedIntent.AddComposerMedia -> addComposerMedia()
            is FeedIntent.AttachComposerMedia -> addComposerMedia(intent.value, clearDraft = false)
            is FeedIntent.RemoveComposerMedia -> setState {
                it.copy(
                    composer = it.composer.copy(
                        mediaUrls = it.composer.mediaUrls - intent.value,
                    )
                )
            }
            is FeedIntent.SelectComposerType -> setState {
                it.copy(composer = it.composer.copy(type = intent.value))
            }
            FeedIntent.SubmitPost -> submitPost()
            is FeedIntent.ToggleLike -> toggleLike(intent.postId)
            is FeedIntent.ToggleSave -> toggleSave(intent.postId)
            is FeedIntent.OpenComments -> openComments(intent.postId)
            FeedIntent.CloseComments -> setState {
                it.copy(
                    selectedPostId = null,
                    comments = emptyList(),
                    pendingComment = null,
                    commentDraft = "",
                    isCommentsLoading = false,
                )
            }
            is FeedIntent.UpdateCommentDraft -> Unit
            FeedIntent.SubmitComment -> submitComment()
            FeedIntent.ClearError -> setState { it.copy(errorMessage = null) }
        }
    }

    private suspend fun openComposerWithPrefill(prefill: FeedComposerPrefill) {
        setState {
            it.copy(
                isComposerOpen = true,
                composer = FeedComposerState(
                    type = prefill.type.toFeedPostType(),
                    caption = prefill.caption,
                    mediaUrls = prefill.mediaUrls.distinct(),
                ),
            )
        }
    }

    private suspend fun loadCurrentAuthor() {
        runCatching { repository.getCurrentAuthor() }
            .onSuccess { author ->
                setState { it.copy(currentAuthor = author) }
            }
    }

    private suspend fun load(initial: Boolean = false) {
        val isFirstLoad = initial && state.value.posts.isEmpty()
        setState {
            it.copy(
                isLoading = isFirstLoad,
                isRefreshing = !isFirstLoad,
                errorMessage = null,
            )
        }
        runCatching { repository.loadFeed() }
            .onSuccess { page ->
                analyticsTracker.track(AnalyticsEvent("feed_viewed"))
                setState {
                    it.copy(
                        posts = page.items,
                        nextCursor = page.nextCursor,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
            .onFailure { error ->
                setState {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Unable to load feed.",
                    )
                }
            }
    }

    private suspend fun refresh() {
        load(initial = false)
    }

    private suspend fun loadNextPage() {
        val cursor = state.value.nextCursor ?: return
        if (state.value.isLoadingMore) return

        setState { it.copy(isLoadingMore = true) }
        runCatching { repository.loadFeed(cursor = cursor) }
            .onSuccess { page ->
                setState {
                    it.copy(
                        posts = it.posts + page.items,
                        nextCursor = page.nextCursor,
                        isLoadingMore = false,
                    )
                }
            }
            .onFailure {
                setState { it.copy(isLoadingMore = false) }
                emitEffect(FeedEffect.Message(it.message ?: "Unable to load more posts."))
            }
    }

    private suspend fun submitPost() {
        val composer = state.value.composer
        val caption = composer.caption.trim()
        if (caption.isBlank()) {
            emitEffect(FeedEffect.Message("Write something before posting."))
            return
        }

        val containsPendingUploads = composer.mediaUrls.any(AvatarUtils::isDataUri)
        setState {
            it.copy(
                composer = composer.copy(
                    isSubmitting = true,
                    isUploadingMedia = containsPendingUploads,
                )
            )
        }
        runCatching {
            val resolvedMediaUrls = composer.mediaUrls.map { mediaUrl ->
                if (AvatarUtils.isDataUri(mediaUrl)) repository.uploadMediaDataUri(mediaUrl) else mediaUrl
            }
            repository.createPost(
                CreateFeedPostRequest(
                    type = composer.type,
                    caption = caption,
                    mediaUrls = resolvedMediaUrls,
                )
            )
        }.onSuccess { created ->
            analyticsTracker.track(AnalyticsEvent("feed_post_published", mapOf("type" to created.type.name)))
            setState {
                it.copy(
                    posts = listOf(created) + it.posts,
                    isComposerOpen = false,
                    composer = FeedComposerState(),
                )
            }
            emitEffect(FeedEffect.Message("Post published."))
        }.onFailure { error ->
            setState {
                it.copy(
                    composer = it.composer.copy(
                        isSubmitting = false,
                        isUploadingMedia = false,
                    )
                )
            }
            emitEffect(FeedEffect.Message(error.message ?: "Unable to publish post."))
        }
    }

    private suspend fun addComposerMedia() {
        addComposerMedia(
            rawValue = state.value.composer.mediaDraft,
            clearDraft = true,
        )
    }

    private suspend fun addComposerMedia(
        rawValue: String,
        clearDraft: Boolean,
    ) {
        val draft = rawValue.trim()
        if (draft.isBlank()) {
            emitEffect(FeedEffect.Message("Paste a media URL first."))
            return
        }

        val isPhotoDataUri = draft.startsWith("data:image", ignoreCase = true)
        val isHttpUrl = draft.startsWith("http://") || draft.startsWith("https://")
        if (!isHttpUrl && !isPhotoDataUri) {
            emitEffect(FeedEffect.Message("Use a valid media URL or image data URI."))
            return
        }

        if (state.value.composer.attachmentMode == FeedComposerAttachmentMode.VIDEO && isPhotoDataUri) {
            emitEffect(FeedEffect.Message("Use a video URL for video posts."))
            return
        }

        val existing = state.value.composer.mediaUrls
        if (draft in existing) {
            emitEffect(FeedEffect.Message("This media is already attached."))
            return
        }

        if (existing.size >= 4) {
            emitEffect(FeedEffect.Message("You can attach up to 4 images."))
            return
        }

        setState {
            it.copy(
                composer = it.composer.copy(
                    mediaDraft = if (clearDraft) "" else it.composer.mediaDraft,
                    mediaUrls = it.composer.mediaUrls + draft,
                )
            )
        }
    }

    private suspend fun toggleLike(postId: String) {
        val snapshot = state.value
        if (postId in snapshot.pendingLikePostIds) return

        val currentPost = snapshot.posts.firstOrNull { it.id == postId } ?: return
        val optimisticPost = currentPost.toggledLikeOptimistically()

        setState { current ->
            current.withReplacedPost(optimisticPost).copy(
                pendingLikePostIds = current.pendingLikePostIds + postId,
            )
        }

        runCatching { repository.toggleLike(postId) }
            .onSuccess { updated ->
                setState { current ->
                    current.withReplacedPost(updated).copy(
                        pendingLikePostIds = current.pendingLikePostIds - postId,
                    )
                }
            }
            .onFailure { error ->
                setState { current ->
                    current.withReplacedPost(currentPost).copy(
                        pendingLikePostIds = current.pendingLikePostIds - postId,
                    )
                }
                emitEffect(FeedEffect.Message(error.message ?: "Unable to update like."))
            }
    }

    private suspend fun toggleSave(postId: String) {
        val snapshot = state.value
        if (postId in snapshot.pendingSavePostIds) return

        val currentPost = snapshot.posts.firstOrNull { it.id == postId } ?: return
        val optimisticPost = currentPost.toggledSaveOptimistically()

        setState { current ->
            current.withReplacedPost(optimisticPost).copy(
                pendingSavePostIds = current.pendingSavePostIds + postId,
            )
        }

        runCatching { repository.toggleSave(postId) }
            .onSuccess { updated ->
                setState { current ->
                    current.withReplacedPost(updated).copy(
                        pendingSavePostIds = current.pendingSavePostIds - postId,
                    )
                }
            }
            .onFailure { error ->
                setState { current ->
                    current.withReplacedPost(currentPost).copy(
                        pendingSavePostIds = current.pendingSavePostIds - postId,
                    )
                }
                emitEffect(FeedEffect.Message(error.message ?: "Unable to update save."))
            }
    }

    private suspend fun openComments(postId: String) {
        setState {
            it.copy(
                selectedPostId = postId,
                isCommentsLoading = true,
                comments = emptyList(),
                pendingComment = null,
                commentDraft = "",
            )
        }
        runCatching { repository.loadComments(postId) }
            .onSuccess { comments ->
                setState { it.copy(comments = comments, isCommentsLoading = false) }
            }
            .onFailure { error ->
                setState { it.copy(isCommentsLoading = false) }
                emitEffect(FeedEffect.Message(error.message ?: "Unable to load comments."))
            }
    }

    private suspend fun submitComment() {
        val postId = state.value.selectedPostId ?: return
        if (state.value.isCommentSubmitting) return
        val body = state.value.commentDraft.trim()
        if (body.isBlank()) {
            emitEffect(FeedEffect.Message("Write a comment first."))
            return
        }

        val pendingComment = PendingFeedComment(
            localId = "pending-${postId}-${Clock.System.now().toEpochMilliseconds()}",
            body = body,
        )
        setState {
            it.withAdjustedCommentCount(postId = postId, delta = 1).copy(
                pendingComment = pendingComment,
                commentDraft = "",
                isCommentSubmitting = true,
            )
        }
        runCatching { repository.createComment(postId, CreateFeedCommentRequest(body)) }
            .onSuccess { comment ->
                analyticsTracker.track(AnalyticsEvent("feed_comment_published"))
                setState { current ->
                    current.copy(
                        comments = if (current.selectedPostId == postId) current.comments + comment else current.comments,
                        pendingComment = if (current.selectedPostId == postId) null else current.pendingComment,
                        isCommentSubmitting = false,
                    )
                }
            }
            .onFailure { error ->
                setState {
                    it.withAdjustedCommentCount(postId = postId, delta = -1).copy(
                        pendingComment = null,
                        isCommentSubmitting = false,
                    )
                }
                emitEffect(FeedEffect.Message(error.message ?: "Unable to publish comment."))
            }
    }

}

private fun String.toFeedPostType(): FeedPostType {
    return FeedPostType.entries.firstOrNull { entry ->
        entry.name.equals(this, ignoreCase = true)
    } ?: FeedPostType.COACH_TIP
}
