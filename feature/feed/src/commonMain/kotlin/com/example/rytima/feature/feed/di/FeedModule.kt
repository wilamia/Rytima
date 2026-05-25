package com.example.rytima.feature.feed.di

import com.example.rytima.core.navigation.FeedComposerLauncherApi
import com.example.rytima.feature.feed.FeedStore
import com.example.rytima.feature.feed.api.FeedComposerLauncher
import com.example.rytima.feature.feed.data.FeedRepository
import com.example.rytima.feature.feed.data.KtorFeedRepository
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val feedModule = module {
    single<FeedComposerLauncherApi> { FeedComposerLauncher() }
    single<FeedRepository> { KtorFeedRepository(supabaseClient = get()) }
    viewModel { FeedStore(get(), get()) }
}
