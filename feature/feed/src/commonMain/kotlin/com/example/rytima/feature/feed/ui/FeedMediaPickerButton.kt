package com.example.rytima.feature.feed.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun FeedMediaPickerButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onMediaPicked: (String) -> Unit,
)
