package com.example.rytima.feature.feed.api

import com.example.rytima.core.navigation.FeedComposerLauncherApi
import com.example.rytima.core.navigation.FeedComposerPrefill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FeedComposerLauncher : FeedComposerLauncherApi {

    private val _pendingOpenComposer = MutableStateFlow<FeedComposerPrefill?>(null)
    override val pendingOpenComposer: StateFlow<FeedComposerPrefill?> = _pendingOpenComposer.asStateFlow()

    override fun requestOpenComposer(prefill: FeedComposerPrefill) {
        _pendingOpenComposer.value = prefill
    }

    override fun consumePendingOpenComposer(): FeedComposerPrefill? {
        val value = _pendingOpenComposer.value
        _pendingOpenComposer.value = null
        return value
    }
}
