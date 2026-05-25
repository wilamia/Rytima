package com.example.rytima.feature.feed.api

import com.example.rytima.core.navigation.FeedComposerPrefill
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeedComposerLauncherTest {

    @Test
    fun `launcher stores and consumes pending composer prefill`() {
        val launcher = FeedComposerLauncher()
        val prefill = FeedComposerPrefill(
            type = "PR",
            caption = "Bench press PR",
        )

        launcher.requestOpenComposer(prefill)

        assertEquals(prefill, launcher.pendingOpenComposer.value)
        assertEquals(prefill, launcher.consumePendingOpenComposer())
        assertNull(launcher.pendingOpenComposer.value)
    }
}
