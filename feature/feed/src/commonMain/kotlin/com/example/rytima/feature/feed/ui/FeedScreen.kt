package com.example.rytima.feature.feed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rytima.core.designsystem.NetworkImage
import com.example.rytima.core.designsystem.RytimaNoInternetScreen
import com.example.rytima.core.designsystem.RytimaAvatar
import com.example.rytima.core.designsystem.RytimaOutlinedButton
import com.example.rytima.core.designsystem.RytimaPostChip
import com.example.rytima.core.designsystem.RytimaPrimaryButton
import com.example.rytima.core.designsystem.RytimaProfileBadge
import com.example.rytima.core.designsystem.RytimaShimmer
import com.example.rytima.core.designsystem.RytimaShimmerCircle
import com.example.rytima.core.designsystem.RytimaShimmerLine
import com.example.rytima.core.designsystem.RytimaSnackbarHost
import com.example.rytima.core.designsystem.RytimaTheme
import com.example.rytima.core.designsystem.isNoInternetError
import com.example.rytima.core.navigation.FeedComposerLauncherApi
import com.example.rytima.feature.feed.FeedBrowseFilter
import com.example.rytima.feature.feed.FeedComment
import com.example.rytima.feature.feed.FeedComposerAttachmentMode
import com.example.rytima.feature.feed.FeedEffect
import com.example.rytima.feature.feed.FeedIntent
import com.example.rytima.feature.feed.PendingFeedComment
import com.example.rytima.feature.feed.FeedPost
import com.example.rytima.feature.feed.FeedPostType
import com.example.rytima.feature.feed.FeedState
import com.example.rytima.feature.feed.FeedStore
import com.example.rytima.feature.feed.filteredPosts
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import rytima.feature.feed.generated.resources.Res
import rytima.feature.feed.generated.resources.*

@Composable
fun FeedScreenRoute(
    store: FeedStore = koinViewModel(),
) {
    val state by store.state.collectAsState()
    val composerLauncher = koinInject<FeedComposerLauncherApi>()
    val pendingPrefill by composerLauncher.pendingOpenComposer.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(store) {
        store.effects.collect { effect ->
            when (effect) {
                is FeedEffect.Message -> scope.launch {
                    snackbarHostState.showSnackbar(effect.text)
                }
            }
        }
    }

    LaunchedEffect(pendingPrefill) {
        val prefill = pendingPrefill ?: return@LaunchedEffect
        store.dispatch(FeedIntent.OpenComposerWithPrefill(prefill))
        composerLauncher.consumePendingOpenComposer()
    }

    FeedScreen(
        state = state,
        onIntent = store::dispatch,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    state: FeedState,
    onIntent: (FeedIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val visiblePosts = remember(state.posts, state.selectedFilter) { state.filteredPosts() }
    val composerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val palette = com.example.rytima.core.designsystem.Redesign.palette
    Scaffold(
        containerColor = palette.bgCanvas,
        snackbarHost = {
            RytimaSnackbarHost(hostState = snackbarHostState)
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bgCanvas)
        ) {
            when {
                state.isLoading && state.posts.isEmpty() -> {
                    FeedLoadingContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    )
                }

                state.errorMessage.isNoInternetError() && state.posts.isEmpty() -> {
                    RytimaNoInternetScreen(
                        onRetry = { onIntent(FeedIntent.Load) },
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            FeedHeader(
                                onCompose = { onIntent(FeedIntent.OpenComposer) },
                            )
                        }

                        item {
                            FeedFilterRow(
                                selected = state.selectedFilter,
                                onSelect = { onIntent(FeedIntent.SelectFilter(it)) },
                            )
                        }

                        if (state.errorMessage != null && state.posts.isEmpty()) {
                            item {
                                FeedEmptyState(
                                    title = state.errorMessage,
                                    action = stringResource(Res.string.retry),
                                    onAction = { onIntent(FeedIntent.Load) },
                                )
                            }
                        }

                        if (visiblePosts.isEmpty() && state.posts.isNotEmpty()) {
                            item {
                                FeedEmptyState(
                                    title = stringResource(Res.string.empty_filter_posts),
                                    action = if (state.selectedFilter != FeedBrowseFilter.ALL) {
                                        stringResource(Res.string.show_all)
                                    } else {
                                        null
                                    },
                                    onAction = { onIntent(FeedIntent.SelectFilter(FeedBrowseFilter.ALL)) },
                                )
                            }
                        }

                        items(visiblePosts, key = { it.id }) { post ->
                            FeedPostCard(
                                post = post,
                                isLikePending = post.id in state.pendingLikePostIds,
                                isSavePending = post.id in state.pendingSavePostIds,
                                onLike = { onIntent(FeedIntent.ToggleLike(post.id)) },
                                onSave = { onIntent(FeedIntent.ToggleSave(post.id)) },
                                onComments = { onIntent(FeedIntent.OpenComments(post.id)) },
                            )
                        }

                        if (state.nextCursor != null && visiblePosts.isNotEmpty()) {
                            item {
                                RytimaOutlinedButton(
                                    text = if (state.isLoadingMore) {
                                        stringResource(Res.string.loading)
                                    } else {
                                        stringResource(Res.string.load_more)
                                    },
                                    onClick = { onIntent(FeedIntent.LoadNextPage) },
                                )
                            }
                        }
                    }
                }
            }

            if (state.isComposerOpen) {
                ModalBottomSheet(
                    onDismissRequest = { onIntent(FeedIntent.CloseComposer) },
                    sheetState = composerSheetState,
                    containerColor = RytimaTheme.colors.surface,
                ) {
                    FeedComposerSheet(
                        state = state,
                        onIntent = onIntent,
                    )
                }
            }

            if (state.selectedPostId != null) {
                ModalBottomSheet(
                    onDismissRequest = { onIntent(FeedIntent.CloseComments) },
                    containerColor = RytimaTheme.colors.surface,
                ) {
                    FeedCommentsSheet(
                        state = state,
                        onIntent = onIntent,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedHeader(
    onCompose: () -> Unit,
) {
    val palette = com.example.rytima.core.designsystem.Redesign.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.feed_title),
            color = palette.textPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
        )
        Box(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(palette.accent)
                .clickable(onClick = onCompose)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "+",
                    color = palette.textOnAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.post_button),
                    color = palette.textOnAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun FeedFilterRow(
    selected: FeedBrowseFilter,
    onSelect: (FeedBrowseFilter) -> Unit,
) {
    val palette = com.example.rytima.core.designsystem.Redesign.palette
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FeedBrowseFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) palette.textPrimary else palette.bgRaised)
                    .clickable(onClick = { onSelect(filter) })
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = filter.toReadableLabel(),
                    color = if (isSelected) palette.bgCanvas else palette.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun FeedLoadingContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = RytimaTheme.spacing.contentBottomInset),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { FeedHeaderShimmer() }
        item { FeedFilterRowShimmer() }
        items(3) { index ->
            FeedPostCardShimmer(showMedia = index == 0)
        }
    }
}

@Composable
private fun FeedHeaderShimmer() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RytimaShimmerLine(width = 92.dp, height = 30.dp)
        RytimaShimmer(
            modifier = Modifier.size(width = 86.dp, height = 42.dp),
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@Composable
private fun FeedFilterRowShimmer() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(72.dp, 104.dp, 94.dp, 82.dp).forEach { width ->
            RytimaShimmer(
                modifier = Modifier.size(width = width, height = 36.dp),
                shape = RoundedCornerShape(999.dp),
            )
        }
    }
}

@Composable
private fun FeedPostCardShimmer(showMedia: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(RytimaTheme.colors.surfaceElevated)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RytimaShimmerCircle(size = 44.dp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RytimaShimmerLine(width = 132.dp, height = 18.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RytimaShimmer(
                            modifier = Modifier.size(width = 62.dp, height = 24.dp),
                            shape = RoundedCornerShape(999.dp),
                        )
                        RytimaShimmerLine(width = 78.dp, height = 12.dp)
                    }
                }
            }
            RytimaShimmerLine(width = 74.dp, height = 14.dp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RytimaShimmer(modifier = Modifier.fillMaxWidth().height(16.dp))
            RytimaShimmer(modifier = Modifier.fillMaxWidth(0.72f).height(16.dp))
        }

        if (showMedia) {
            RytimaShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(18.dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RytimaShimmer(
                modifier = Modifier.size(width = 86.dp, height = 32.dp),
                shape = RoundedCornerShape(999.dp),
            )
            RytimaShimmer(
                modifier = Modifier.size(width = 52.dp, height = 32.dp),
                shape = RoundedCornerShape(999.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RytimaShimmerCircle(size = 20.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    RytimaShimmerLine(width = 24.dp, height = 14.dp)
                }
            }
        }
    }
}

@Composable
private fun FeedPostCard(
    post: FeedPost,
    isLikePending: Boolean,
    isSavePending: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComments: () -> Unit,
) {
    val palette = com.example.rytima.core.designsystem.Redesign.palette
    val isPr = post.type == FeedPostType.PR

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.bgSurface),
    ) {
        if (isPr) {
            FeedPostPrHero(post = post, palette = palette)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Author header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isPr) 32.dp else 40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(palette.accent, palette.accentSecondarySoft),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    RytimaAvatar(
                        avatarUrl = post.author.avatarUrl,
                        defaultSeed = post.author.displayName.ifBlank { post.author.userId },
                        modifier = Modifier
                            .size(if (isPr) 32.dp else 40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = post.author.displayName,
                            color = palette.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (post.author.role.name == "TRAINER") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(palette.accentSoft)
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.trainer_badge),
                                    color = palette.accent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Text(
                        text = "${post.type.toReadableLabel()} · ${formatPostDate(post.createdAt)}",
                        color = palette.textTertiary,
                        fontSize = 13.sp,
                    )
                }
            }

            // Title (extract first line if it's short — heuristic since model has no title field)
            val firstLine = post.caption.lineSequence().firstOrNull().orEmpty()
            val showTitle = !isPr && firstLine.length in 1..70 && post.caption.contains("\n")
            val body = if (showTitle) post.caption.substringAfter("\n").trim() else post.caption

            if (showTitle) {
                Text(
                    text = firstLine,
                    color = palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                )
            }

            if (body.isNotBlank()) {
                Text(
                    text = body,
                    color = palette.textSecondary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
            }

            if (post.mediaUrls.isNotEmpty()) {
                FeedMediaGrid(
                    mediaUrls = post.mediaUrls,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Hashtags extracted from caption
            val hashtags = remember(post.caption) {
                Regex("#[\\p{L}0-9_]+").findAll(post.caption).map { it.value }.toList()
            }
            if (hashtags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    hashtags.take(4).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(palette.bgRaised)
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = tag,
                                color = palette.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // Divider before actions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.borderSubtle),
            )

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeedAction(
                    icon = Icons.Outlined.FavoriteBorder,
                    label = "${post.likeCount}",
                    active = post.isLiked || isLikePending,
                    isPending = isLikePending,
                    onClick = onLike,
                )
                FeedAction(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = "${post.commentCount}",
                    active = false,
                    isPending = false,
                    onClick = onComments,
                )
                FeedAction(
                    icon = Icons.Outlined.BookmarkBorder,
                    label = "${post.saveCount}",
                    active = post.isSaved || isSavePending,
                    isPending = isSavePending,
                    onClick = onSave,
                )
            }
        }
    }
}

@Composable
private fun FeedPostPrHero(
    post: FeedPost,
    palette: com.example.rytima.core.designsystem.RedesignPalette,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(palette.accent, palette.accentSoft),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x2E0A0A0E))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "🏆 ${stringResource(Res.string.new_record)}",
                    color = palette.textOnAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = formatPostDate(post.createdAt),
                color = palette.textOnAccent.copy(alpha = 0.75f),
                fontSize = 13.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val firstLine = post.caption.lineSequence().firstOrNull().orEmpty()
                Text(
                    text = if (firstLine.isNotBlank()) firstLine else stringResource(Res.string.record_fallback),
                    color = palette.textOnAccent.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                )
                Text(
                    text = "PR",
                    color = palette.textOnAccent,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.sp,
                    lineHeight = 48.sp,
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0x2E0A0A0E)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = post.author.displayName.firstOrNull()?.uppercase() ?: "?",
                    color = palette.textOnAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun formatPostDate(iso: String): String {
    val datePart = iso.substringBefore("T")
    val parts = datePart.split("-")
    if (parts.size < 3) return datePart
    val day = parts[2].trimStart('0')
    val monthInt = parts[1].toIntOrNull() ?: return datePart
    val months = monthShortLabels()
    val m = months.getOrNull(monthInt - 1) ?: return datePart
    return "$day $m"
}

@Composable
private fun monthShortLabels(): List<String> = listOf(
    stringResource(Res.string.month_short_jan),
    stringResource(Res.string.month_short_feb),
    stringResource(Res.string.month_short_mar),
    stringResource(Res.string.month_short_apr),
    stringResource(Res.string.month_short_may),
    stringResource(Res.string.month_short_jun),
    stringResource(Res.string.month_short_jul),
    stringResource(Res.string.month_short_aug),
    stringResource(Res.string.month_short_sep),
    stringResource(Res.string.month_short_oct),
    stringResource(Res.string.month_short_nov),
    stringResource(Res.string.month_short_dec),
)

@Composable
private fun FeedAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    isPending: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = !isPending, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPending) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = RytimaTheme.colors.primary,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) RytimaTheme.colors.primary else RytimaTheme.colors.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (active) RytimaTheme.colors.primary else RytimaTheme.colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun FeedComposerSheet(
    state: FeedState,
    onIntent: (FeedIntent) -> Unit,
) {
    val scrollState = rememberScrollState()
    val author = state.currentAuthor
    val authorName = author?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.your_profile)
    val authorRoleLabel = when (author?.role?.name) {
        "TRAINER" -> stringResource(Res.string.trainer_badge)
        "CLIENT" -> stringResource(Res.string.client_badge)
        else -> stringResource(Res.string.profile_label)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.new_post),
                style = MaterialTheme.typography.headlineSmall,
                color = RytimaTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = { onIntent(FeedIntent.CloseComposer) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(RytimaTheme.colors.surfaceElevated),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.close),
                    tint = RytimaTheme.colors.onSurface,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RytimaAvatar(
                avatarUrl = author?.avatarUrl,
                defaultSeed = authorName.ifBlank { author?.userId.orEmpty() },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(RytimaTheme.colors.primary),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = authorName,
                    style = MaterialTheme.typography.titleMedium,
                    color = RytimaTheme.colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = authorRoleLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = RytimaTheme.colors.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = state.composer.caption,
            onValueChange = { onIntent(FeedIntent.UpdateComposerCaption(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
            shape = RoundedCornerShape(18.dp),
            placeholder = { Text(stringResource(Res.string.composer_placeholder)) },
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeedPostType.entries.forEach { type ->
                FeedComposerTypeChip(
                    label = type.toReadableLabel(),
                    selected = state.composer.type == type,
                    onClick = { onIntent(FeedIntent.SelectComposerType(type)) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(RytimaTheme.colors.surfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = when (state.composer.type) {
                        FeedPostType.COACH_TIP -> "#strength #progress #motivation"
                        FeedPostType.WORKOUT_RECAP -> "#workout #recap #training"
                        FeedPostType.PR -> "#pr #strength #milestone"
                        FeedPostType.CLIENT_MILESTONE -> "#client #result #progress"
                        FeedPostType.FORM_CHECK_REQUEST -> "#form #help #technique"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = RytimaTheme.colors.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ComposerShortcutChip(
                    label = stringResource(Res.string.photo),
                    selected = state.composer.attachmentMode == FeedComposerAttachmentMode.PHOTO,
                    onClick = {
                        onIntent(FeedIntent.SelectComposerAttachmentMode(FeedComposerAttachmentMode.PHOTO))
                    },
                    modifier = Modifier.weight(1f),
                )
                ComposerShortcutChip(
                    label = stringResource(Res.string.video),
                    selected = state.composer.attachmentMode == FeedComposerAttachmentMode.VIDEO,
                    onClick = {
                        onIntent(FeedIntent.SelectComposerAttachmentMode(FeedComposerAttachmentMode.VIDEO))
                    },
                    modifier = Modifier.weight(1f),
                )
                ComposerShortcutChip(
                    label = stringResource(Res.string.coauthor),
                    selected = state.composer.attachmentMode == FeedComposerAttachmentMode.COAUTHOR,
                    onClick = {
                        onIntent(FeedIntent.SelectComposerAttachmentMode(FeedComposerAttachmentMode.COAUTHOR))
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            when (state.composer.attachmentMode) {
                FeedComposerAttachmentMode.PHOTO,
                FeedComposerAttachmentMode.VIDEO -> {
                    val isVideoMode = state.composer.attachmentMode == FeedComposerAttachmentMode.VIDEO
                    OutlinedTextField(
                        value = state.composer.mediaDraft,
                        onValueChange = { onIntent(FeedIntent.UpdateComposerMediaDraft(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        placeholder = {
                            Text(
                                if (isVideoMode) {
                                    stringResource(Res.string.video_link_placeholder)
                                } else {
                                    stringResource(Res.string.photo_link_placeholder)
                                },
                            )
                        },
                        singleLine = true,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RytimaOutlinedButton(
                            text = when {
                                state.composer.isUploadingMedia -> stringResource(Res.string.loading)
                                isVideoMode -> stringResource(Res.string.add_video)
                                else -> stringResource(Res.string.add_photo)
                            },
                            onClick = { onIntent(FeedIntent.AddComposerMedia) },
                            modifier = Modifier.weight(1f),
                        )
                        if (!isVideoMode) {
                            FeedMediaPickerButton(
                                modifier = Modifier.weight(1f),
                                enabled = !state.composer.isSubmitting,
                                onMediaPicked = { onIntent(FeedIntent.AttachComposerMedia(it)) },
                            )
                        }
                    }
                }
                FeedComposerAttachmentMode.COAUTHOR -> {
                    OutlinedTextField(
                        value = state.composer.coauthorDraft,
                        onValueChange = { onIntent(FeedIntent.UpdateComposerCoauthorDraft(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        placeholder = { Text(stringResource(Res.string.coauthor_placeholder)) },
                        singleLine = true,
                    )
                }
            }

            if (state.composer.mediaUrls.isNotEmpty()) {
                FeedComposerMediaStrip(
                    mediaUrls = state.composer.mediaUrls,
                    onRemove = { onIntent(FeedIntent.RemoveComposerMedia(it)) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RytimaOutlinedButton(
                text = stringResource(Res.string.close),
                onClick = { onIntent(FeedIntent.CloseComposer) },
                modifier = Modifier.weight(1f),
            )
            RytimaPrimaryButton(
                text = when {
                    state.composer.isUploadingMedia -> stringResource(Res.string.loading)
                    state.composer.isSubmitting -> stringResource(Res.string.publishing)
                    else -> stringResource(Res.string.publish)
                },
                onClick = { onIntent(FeedIntent.SubmitPost) },
                enabled = !state.composer.isSubmitting,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ComposerShortcutChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    RytimaTheme.colors.primary.copy(alpha = 0.16f)
                } else {
                    RytimaTheme.colors.surfaceElevated
                },
            )
            .border(
                width = 1.dp,
                color = if (selected) RytimaTheme.colors.primary else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) RytimaTheme.colors.primary else RytimaTheme.colors.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun FeedMediaGrid(
    mediaUrls: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        mediaUrls.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { mediaUrl ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(132.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(RytimaTheme.colors.surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (mediaUrl.isVideoMediaUrl()) {
                            VideoMediaPlaceholder()
                        } else {
                            NetworkImage(
                                model = mediaUrl,
                                contentDescription = stringResource(Res.string.attachment_content_description),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
                repeat(2 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FeedComposerMediaStrip(
    mediaUrls: List<String>,
    onRemove: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        mediaUrls.forEach { mediaUrl ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(RytimaTheme.colors.surfaceElevated),
                contentAlignment = Alignment.Center,
            ) {
                if (mediaUrl.isVideoMediaUrl()) {
                    VideoMediaPlaceholder()
                } else {
                    NetworkImage(
                        model = mediaUrl,
                        contentDescription = stringResource(Res.string.composer_attachment_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                IconButton(
                    onClick = { onRemove(mediaUrl) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(RytimaTheme.colors.surface.copy(alpha = 0.9f)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.remove_attachment_content_description),
                        tint = RytimaTheme.colors.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoMediaPlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(Res.string.video),
            style = MaterialTheme.typography.titleMedium,
            color = RytimaTheme.colors.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.video_added),
            style = MaterialTheme.typography.bodySmall,
            color = RytimaTheme.colors.onSurfaceVariant,
        )
    }
}

private fun String.isVideoMediaUrl(): Boolean {
    val value = lowercase()
    return value.contains(".mp4") ||
        value.contains(".mov") ||
        value.contains(".webm") ||
        value.contains("video")
}

@Composable
private fun FeedCommentsSheet(
    state: FeedState,
    onIntent: (FeedIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.comments_title),
            style = MaterialTheme.typography.headlineSmall,
            color = RytimaTheme.colors.onSurface,
            fontWeight = FontWeight.Bold,
        )

        if (state.isCommentsLoading) {
            FeedCommentsShimmer()
        } else if (state.comments.isEmpty()) {
            FeedEmptyState(
                title = stringResource(Res.string.no_comments),
                action = null,
                onAction = {},
            )
        }

        if (state.comments.isNotEmpty() || state.pendingComment != null) {
            LazyColumn(
                modifier = Modifier.height(260.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.pendingComment?.let { pending ->
                    item(key = pending.localId) {
                        PendingCommentRow(pending)
                    }
                }
                items(state.comments, key = { it.id }) { comment ->
                    FeedCommentRow(comment)
                }
            }
        }

        OutlinedTextField(
            value = state.commentDraft,
            onValueChange = { onIntent(FeedIntent.UpdateCommentDraft(it)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            placeholder = { Text(stringResource(Res.string.comment_placeholder)) },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { onIntent(FeedIntent.CloseComments) }) {
                Text(stringResource(Res.string.close), color = RytimaTheme.colors.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.weight(1f))
            RytimaPrimaryButton(
                text = if (state.isCommentSubmitting) {
                    stringResource(Res.string.sending)
                } else {
                    stringResource(Res.string.send)
                },
                onClick = { onIntent(FeedIntent.SubmitComment) },
                enabled = !state.isCommentSubmitting,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun FeedCommentRow(comment: FeedComment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(RytimaTheme.colors.surfaceElevated)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RytimaAvatar(
                avatarUrl = comment.author.avatarUrl,
                defaultSeed = comment.author.displayName.ifBlank { comment.author.userId },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = comment.author.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = RytimaTheme.colors.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            RytimaProfileBadge(
                label = if (comment.author.role.name == "TRAINER") {
                    stringResource(Res.string.trainer_badge)
                } else {
                    stringResource(Res.string.client_badge)
                },
            )
        }
        Text(
            text = comment.body,
            style = MaterialTheme.typography.bodyMedium,
            color = RytimaTheme.colors.onSurface,
        )
    }
}

@Composable
private fun FeedCommentsShimmer() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(RytimaTheme.colors.surfaceElevated)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RytimaShimmerCircle(size = 36.dp)
                    RytimaShimmerLine(width = 118.dp, height = 16.dp)
                    RytimaShimmer(
                        modifier = Modifier.size(width = 62.dp, height = 24.dp),
                        shape = RoundedCornerShape(999.dp),
                    )
                }
                RytimaShimmer(modifier = Modifier.fillMaxWidth(0.88f).height(16.dp))
            }
        }
    }
}

@Composable
private fun PendingCommentRow(comment: PendingFeedComment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(RytimaTheme.colors.primary.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = RytimaTheme.colors.primary.copy(alpha = 0.30f),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RytimaTheme.colors.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = RytimaTheme.colors.primary,
                )
            }
            Text(
                text = stringResource(Res.string.sending),
                style = MaterialTheme.typography.titleSmall,
                color = RytimaTheme.colors.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = comment.body,
            style = MaterialTheme.typography.bodyMedium,
            color = RytimaTheme.colors.onSurface,
        )
    }
}

@Composable
private fun FeedEmptyState(
    title: String,
    action: String?,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(RytimaTheme.colors.surfaceElevated)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = RytimaTheme.colors.onSurfaceVariant,
        )
        if (action != null) {
            RytimaOutlinedButton(text = action, onClick = onAction)
        }
    }
}

@Composable
private fun FeedComposerTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                color = if (selected) {
                    RytimaTheme.colors.primary.copy(alpha = 0.16f)
                } else {
                    RytimaTheme.colors.surfaceElevated
                },
            )
            .border(
                width = 1.dp,
                color = if (selected) {
                    RytimaTheme.colors.primary.copy(alpha = 0.55f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = if (selected) RytimaTheme.colors.primary else RytimaTheme.colors.onSurface,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
    )
}

@Composable
private fun FeedPostType.toReadableLabel(): String = when (this) {
    FeedPostType.COACH_TIP -> stringResource(Res.string.post_type_coach_tip)
    FeedPostType.WORKOUT_RECAP -> stringResource(Res.string.post_type_workout_recap)
    FeedPostType.PR -> stringResource(Res.string.post_type_pr)
    FeedPostType.CLIENT_MILESTONE -> stringResource(Res.string.post_type_client_milestone)
    FeedPostType.FORM_CHECK_REQUEST -> stringResource(Res.string.post_type_form_check)
}

@Composable
private fun FeedBrowseFilter.toReadableLabel(): String = when (this) {
    FeedBrowseFilter.ALL -> stringResource(Res.string.filter_all)
    FeedBrowseFilter.TRAINERS -> stringResource(Res.string.filter_trainers)
    FeedBrowseFilter.ATHLETES -> stringResource(Res.string.filter_athletes)
    FeedBrowseFilter.PRS -> stringResource(Res.string.filter_prs)
    FeedBrowseFilter.TIPS -> stringResource(Res.string.filter_tips)
}
