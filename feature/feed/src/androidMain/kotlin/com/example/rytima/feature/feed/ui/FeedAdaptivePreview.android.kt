package com.example.rytima.feature.feed.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.example.rytima.core.designsystem.RytimaTheme
import com.example.rytima.core.model.UserRole
import com.example.rytima.feature.feed.FeedAuthor
import com.example.rytima.feature.feed.FeedBrowseFilter
import com.example.rytima.feature.feed.FeedPost
import com.example.rytima.feature.feed.FeedPostType
import com.example.rytima.feature.feed.FeedState

@Preview(name = "Trainer Feed Compact", widthDp = 360, heightDp = 800)
@Preview(name = "Trainer Feed Medium", widthDp = 700, heightDp = 900)
@Preview(name = "Trainer Feed Expanded", widthDp = 1000, heightDp = 900)
@Composable
private fun TrainerFeedAdaptivePreview() {
    val snack = remember { SnackbarHostState() }
    RytimaTheme {
        FeedScreen(
            state = trainerFeedPreviewState(),
            onIntent = {},
            snackbarHostState = snack,
        )
    }
}

@Preview(name = "Client Feed Compact", widthDp = 390, heightDp = 844)
@Composable
private fun ClientFeedAdaptivePreview() {
    val snack = remember { SnackbarHostState() }
    RytimaTheme {
        FeedScreen(
            state = clientFeedPreviewState(),
            onIntent = {},
            snackbarHostState = snack,
        )
    }
}

private fun trainerFeedPreviewState() = FeedState(
    selectedFilter = FeedBrowseFilter.ALL,
    posts = listOf(
        FeedPost(
            id = "trainer-post-1",
            author = FeedAuthor(
                userId = "trainer-1",
                role = UserRole.TRAINER,
                displayName = "Alex Petrov",
                avatarUrl = null,
            ),
            type = FeedPostType.COACH_TIP,
            caption = "Worked through deadlift technique with a client today.",
            mediaUrls = emptyList(),
            commentCount = 5,
            likeCount = 1,
            isLiked = false,
            createdAt = "2026-04-21T10:00:00Z",
        ),
    ),
)

private fun clientFeedPreviewState() = FeedState(
    selectedFilter = FeedBrowseFilter.ALL,
    posts = listOf(
        FeedPost(
            id = "client-post-1",
            author = FeedAuthor(
                userId = "trainer-2",
                role = UserRole.TRAINER,
                displayName = "Elena Sidorova",
                avatarUrl = null,
            ),
            type = FeedPostType.COACH_TIP,
            caption = "Light mobility after strength training helps recovery.",
            mediaUrls = emptyList(),
            commentCount = 2,
            likeCount = 14,
            isLiked = true,
            createdAt = "2026-04-24T09:00:00Z",
        ),
        FeedPost(
            id = "client-post-2",
            author = FeedAuthor(
                userId = "client-1",
                role = UserRole.CLIENT,
                displayName = "Roman Ilyin",
                avatarUrl = null,
            ),
            type = FeedPostType.CLIENT_MILESTONE,
            caption = "Finished the fourth workout this week. Technique is moving well.",
            mediaUrls = emptyList(),
            commentCount = 3,
            likeCount = 8,
            isLiked = false,
            createdAt = "2026-04-24T11:30:00Z",
        ),
    ),
)
