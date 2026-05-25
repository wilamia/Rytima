package com.example.rytima.feature.feed

import com.example.rytima.core.model.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedFilteringTest {

    @Test
    fun `trainer filter keeps only trainer authored posts`() {
        val state = FeedState(
            posts = listOf(
                samplePost(id = "trainer", role = UserRole.TRAINER),
                samplePost(id = "athlete", role = UserRole.CLIENT),
            ),
            selectedFilter = FeedBrowseFilter.TRAINERS,
        )

        assertEquals(listOf("trainer"), state.filteredPosts().map { it.id })
    }

    @Test
    fun `pr filter keeps only pr posts`() {
        val state = FeedState(
            posts = listOf(
                samplePost(id = "tip", type = FeedPostType.COACH_TIP),
                samplePost(id = "pr", type = FeedPostType.PR),
            ),
            selectedFilter = FeedBrowseFilter.PRS,
        )

        assertEquals(listOf("pr"), state.filteredPosts().map { it.id })
    }

    @Test
    fun `tips filter keeps only coach tips`() {
        val state = FeedState(
            posts = listOf(
                samplePost(id = "tip", type = FeedPostType.COACH_TIP),
                samplePost(id = "milestone", type = FeedPostType.CLIENT_MILESTONE),
            ),
            selectedFilter = FeedBrowseFilter.TIPS,
        )

        assertEquals(listOf("tip"), state.filteredPosts().map { it.id })
    }

    private fun samplePost(
        id: String,
        role: UserRole = UserRole.CLIENT,
        type: FeedPostType = FeedPostType.COACH_TIP,
    ) = FeedPost(
        id = id,
        author = FeedAuthor(
            userId = "user-$id",
            role = role,
            displayName = "User $id",
        ),
        type = type,
        caption = "caption",
        createdAt = "2026-04-17T10:00:00Z",
    )
}
