package com.example.rytima.feature.feed

import com.example.rytima.core.model.UserRole

internal fun FeedState.filteredPosts(): List<FeedPost> =
    posts.filter(selectedFilter::matches)

internal fun FeedBrowseFilter.matches(post: FeedPost): Boolean = when (this) {
    FeedBrowseFilter.ALL -> true
    FeedBrowseFilter.TRAINERS -> post.author.role == UserRole.TRAINER
    FeedBrowseFilter.ATHLETES -> post.author.role != UserRole.TRAINER
    FeedBrowseFilter.PRS -> post.type == FeedPostType.PR
    FeedBrowseFilter.TIPS -> post.type == FeedPostType.COACH_TIP
}
