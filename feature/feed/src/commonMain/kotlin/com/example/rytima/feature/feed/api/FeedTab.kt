package com.example.rytima.feature.feed.api

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.example.rytima.feature.feed.ui.FeedScreenRoute
import org.jetbrains.compose.resources.stringResource
import rytima.feature.feed.generated.resources.Res
import rytima.feature.feed.generated.resources.feed_tab_title

object FeedTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Outlined.DynamicFeed)
            val title = stringResource(Res.string.feed_tab_title)
            return remember(title) {
                TabOptions(
                    index = 1u,
                    title = title,
                    icon = icon,
                )
            }
        }

    @Composable
    override fun Content() {
        FeedScreenRoute()
    }
}
