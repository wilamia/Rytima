package com.example.rytima.feature.feed

import com.example.rytima.core.model.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedOptimisticStateTest {

    @Test
    fun `optimistic like toggles on and increments count`() {
        val post = samplePost(isLiked = false, likeCount = 7)

        val updated = post.toggledLikeOptimistically()

        assertTrue(updated.isLiked)
        assertEquals(8, updated.likeCount)
    }

    @Test
    fun `optimistic like toggles off and never drops below zero`() {
        val post = samplePost(isLiked = true, likeCount = 0)

        val updated = post.toggledLikeOptimistically()

        assertFalse(updated.isLiked)
        assertEquals(0, updated.likeCount)
    }

    @Test
    fun `optimistic save toggles on and increments count`() {
        val post = samplePost(isSaved = false, saveCount = 2)

        val updated = post.toggledSaveOptimistically()

        assertTrue(updated.isSaved)
        assertEquals(3, updated.saveCount)
    }

    @Test
    fun `state replacement swaps only matching post`() {
        val first = samplePost(id = "first")
        val second = samplePost(id = "second", caption = "old")
        val state = FeedState(posts = listOf(first, second))

        val updated = state.withReplacedPost(second.copy(caption = "new"))

        assertEquals("first", updated.posts.first().id)
        assertEquals("new", updated.posts.last().caption)
    }

    @Test
    fun `comment count adjustment updates only matching post`() {
        val first = samplePost(id = "first", caption = "one")
        val second = samplePost(id = "second", caption = "two")
        val state = FeedState(posts = listOf(first, second))

        val updated = state.withAdjustedCommentCount(postId = "second", delta = 1)

        assertEquals(0, updated.posts.first().commentCount)
        assertEquals(1, updated.posts.last().commentCount)
    }

    @Test
    fun `comment count adjustment never drops below zero`() {
        val state = FeedState(posts = listOf(samplePost(id = "first", caption = "one")))

        val updated = state.withAdjustedCommentCount(postId = "first", delta = -1)

        assertEquals(0, updated.posts.first().commentCount)
    }

    private fun samplePost(
        id: String = "post-1",
        caption: String = "caption",
        isLiked: Boolean = false,
        likeCount: Int = 0,
        isSaved: Boolean = false,
        saveCount: Int = 0,
    ) = FeedPost(
        id = id,
        author = FeedAuthor(
            userId = "user-1",
            role = UserRole.CLIENT,
            displayName = "Alex Stone",
        ),
        type = FeedPostType.COACH_TIP,
        caption = caption,
        likeCount = likeCount,
        saveCount = saveCount,
        isLiked = isLiked,
        isSaved = isSaved,
        createdAt = "2026-04-17T10:00:00Z",
    )
}
