package com.example.rytima.feature.feed

internal fun FeedState.withReplacedPost(updated: FeedPost): FeedState = copy(
    posts = posts.map { post -> if (post.id == updated.id) updated else post },
)

internal fun FeedState.withAdjustedCommentCount(
    postId: String,
    delta: Int,
): FeedState = copy(
    posts = posts.map { post ->
        if (post.id == postId) {
            post.copy(commentCount = (post.commentCount + delta).coerceAtLeast(0))
        } else {
            post
        }
    },
)

internal fun FeedPost.toggledLikeOptimistically(): FeedPost {
    val nextLiked = !isLiked
    return copy(
        isLiked = nextLiked,
        likeCount = (likeCount + if (nextLiked) 1 else -1).coerceAtLeast(0),
    )
}

internal fun FeedPost.toggledSaveOptimistically(): FeedPost {
    val nextSaved = !isSaved
    return copy(
        isSaved = nextSaved,
        saveCount = (saveCount + if (nextSaved) 1 else -1).coerceAtLeast(0),
    )
}
