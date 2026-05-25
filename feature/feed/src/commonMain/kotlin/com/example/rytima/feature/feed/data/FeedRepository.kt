package com.example.rytima.feature.feed.data

import com.example.rytima.feature.feed.CreateFeedCommentRequest
import com.example.rytima.feature.feed.CreateFeedPostRequest
import com.example.rytima.feature.feed.FeedAuthor
import com.example.rytima.feature.feed.FeedComment
import com.example.rytima.feature.feed.FeedPage
import com.example.rytima.feature.feed.FeedPost

interface FeedRepository {
    suspend fun getCurrentAuthor(): FeedAuthor?
    suspend fun loadFeed(cursor: String? = null, limit: Int = 20): FeedPage
    suspend fun loadProfileFeed(profileType: String, profileId: String, cursor: String? = null, limit: Int = 12): FeedPage
    suspend fun createPost(request: CreateFeedPostRequest): FeedPost
    suspend fun uploadMediaDataUri(dataUri: String): String
    suspend fun toggleLike(postId: String): FeedPost
    suspend fun toggleSave(postId: String): FeedPost
    suspend fun loadComments(postId: String): List<FeedComment>
    suspend fun createComment(postId: String, request: CreateFeedCommentRequest): FeedComment
}
