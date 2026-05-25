# Rytima · Feed Module

This repository contains one feature module from **Rytima**, a Kotlin Multiplatform fitness coaching app for Android and iOS.

The full project is private, so this module is shared separately as a code sample. It shows how I structure one feature in the app, including UI state, data loading, filtering, optimistic actions, media handling, and integration with the backend.

## What the feed does

The feed is used for posts from trainers and athletes. Users can share things like workout recaps, personal records, coaching tips, client progress, or form-check requests.

Supported post types:

- `COACH_TIP`
- `WORKOUT_RECAP`
- `PR`
- `CLIENT_MILESTONE`
- `FORM_CHECK_REQUEST`

The module includes:

- feed browsing
- filtering by post type or author role
- pull-to-refresh
- pagination
- likes and saves
- optimistic UI updates
- basic comment count handling
- image upload through data URIs
- offline/error state handling
- a composer launcher that can be called from other modules
- localization (English, Lithuanian, Russian) via Compose Resources

## Structure

```text
feature/feed/src/commonMain/kotlin/com/example/rytima/feature/feed/
├── FeedContracts.kt
├── FeedStore.kt
├── FeedRepository.kt
├── KtorFeedRepository.kt
├── FeedScreen.kt
├── FeedTab.kt
├── FeedFiltering.kt
├── FeedOptimisticState.kt
├── FeedMediaDataUri.kt
├── FeedMediaPickerButton.kt
├── FeedComposerLauncher.kt
└── FeedModule.kt
```

Short overview:

| File | Purpose |
|---|---|
| `FeedContracts.kt` | Intent, State and Effect definitions for the MVI flow |
| `FeedStore.kt` | Main business logic and state updates |
| `FeedRepository.kt` | Repository interface |
| `KtorFeedRepository.kt` | Network implementation using Ktor and Supabase |
| `FeedScreen.kt` | Compose screen entry point |
| `FeedTab.kt` | Voyager tab integration |
| `FeedFiltering.kt` | Filtering logic |
| `FeedOptimisticState.kt` | Helpers for optimistic likes/saves/comments |
| `FeedMediaDataUri.kt` | Media encoding helpers |
| `FeedMediaPickerButton.kt` | UI for choosing media |
| `FeedComposerLauncher.kt` | API for opening the composer from other modules |
| `FeedModule.kt` | Koin module |

## Architecture

The module follows an MVI-style structure:

- UI sends intents
- Store handles business logic
- State is exposed as a Flow
- Effects are used for one-time actions
- Repository hides the data source from the UI layer

The goal was to keep the feed logic separated from the Compose UI and make the feature easier to test and extend.

Main tools used:

- Kotlin Multiplatform
- Compose Multiplatform
- Coroutines and Flow
- Koin
- Ktor
- Supabase Postgrest
- kotlinx.serialization
- Voyager

## Optimistic UI

Likes and saves are updated immediately in the local state before the backend responds.

If the request fails, the previous state is restored.

Example:

```kotlin
internal fun FeedPost.toggledLikeOptimistically(): FeedPost {
    val nextLiked = !isLiked
    return copy(
        isLiked = nextLiked,
        likeCount = (likeCount + if (nextLiked) 1 else -1).coerceAtLeast(0),
    )
}
```

This makes the feed feel faster and avoids waiting for every network request before showing feedback to the user.

## Dependencies

This module was extracted from a larger multi-module project, so some internal modules are not included here.

| Module | Used for |
|---|---|
| `core/designsystem` | Shared UI components and loading placeholders |
| `core/foundation` | Analytics and common app utilities |
| `core/model` | Shared domain models |
| `core/mvi` | Base MVI store |
| `core/navigation` | Cross-module navigation contracts |
| `core/network` | Ktor and Supabase configuration |

## Why only this module is public

The full Rytima project is private because it contains production configuration, backend-related code, payment setup, and other parts that are not meant to be public.

This repository is only meant to show a focused part of the codebase:

- feature module structure
- MVI implementation
- Compose Multiplatform UI
- async state handling
- optimistic updates
- backend communication with Ktor and Supabase
