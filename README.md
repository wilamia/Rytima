# Rytima · Feed Module

A feature module from **Rytima**, a Kotlin Multiplatform fitness coaching app for Android and iOS.

The full application is private, so this repository contains only one extracted feature module as a code sample.  
It demonstrates feature-level architecture, MVI state management, Compose Multiplatform UI, asynchronous data loading, filtering, pagination, optimistic UI updates, media handling, localization, and backend communication with Ktor and Supabase.

---

## Screenshots

### Feed module

| Feed browsing and filters | Workout summary | Post-workout feed action |
|---|---|---|
| <img src="https://github.com/user-attachments/assets/1d3d229b-576a-446d-89e2-e85a9a3aeb37" alt="Feed browsing and filters" width="220" /> | <img src="https://github.com/user-attachments/assets/6beeb6d0-77b5-431d-ac0f-b57dcb815a36" alt="Workout summary" width="220" /> | <img src="https://github.com/user-attachments/assets/e3540594-5f30-4f20-829c-3fd8306d72e7" alt="Post-workout feed action" width="220" /> |

The screenshots above show the Feed module and how it connects with the post-workout flow.

After completing a workout, the user can review the workout summary and use the **Save and post to feed** action to create a feed recap.

---

## Full app context

Rytima is a larger fitness coaching app.  
The Feed module is only one part of the full product.

The screenshots below are included only to show the broader app context.  
The public repository contains the Feed module only.

| Dashboard | Active workout | Exercise details |
|---|---|---|
| <img src="https://github.com/user-attachments/assets/d5643373-2c38-477c-975f-efce5b6d7f24" alt="Dashboard" width="220" /> | <img src="https://github.com/user-attachments/assets/a01ffc8d-86eb-4ac7-ab14-4bba08854d7d" alt="Active workout" width="220" /> | <img src="https://github.com/user-attachments/assets/4e586b57-d5db-4c5e-8549-901a3a481f83" alt="Exercise details" width="220" /> |

| Calendar | Chat | Trainer profile |
|---|---|---|
| <img src="https://github.com/user-attachments/assets/df124294-77cc-41c8-b8ea-6bb30bab9da2" alt="Calendar" width="220" /> | <img src="https://github.com/user-attachments/assets/814a33ae-9a8a-49bb-ab93-85f8ab131019" alt="Chat" width="220" /> | <img src="https://github.com/user-attachments/assets/36e63749-6626-4e01-8191-7f493c548641" alt="Trainer profile" width="220" /> |

---

## What this module does

The Feed module allows trainers and athletes to share training-related content inside the app.

Users can create posts such as:

- coaching tips
- workout recaps
- personal records
- client milestones
- form-check requests

Supported post types:

```kotlin
COACH_TIP
WORKOUT_RECAP
PR
CLIENT_MILESTONE
FORM_CHECK_REQUEST
```

The module includes:

- feed browsing
- filtering by post type and author role
- pull-to-refresh
- pagination
- likes and saves
- optimistic UI updates
- basic comment count handling
- image upload through data URIs
- offline and error state handling
- a composer launcher that can be used from other modules
- localization in English, Lithuanian, and Russian using Compose Resources

---

## Why this feature matters

In a fitness coaching app, the feed is not just a social screen.

It connects trainers and athletes through real training progress, workout results, coaching content, personal records, and client achievements.

The main goal was to build a feed that:

- feels fast to use
- keeps UI logic separated from business logic
- works with asynchronous backend requests
- handles loading, pagination, refresh, empty states, and errors
- can be extended with new post types later

---

## Architecture

The module follows an MVI-style architecture.

```text
User action
   ↓
Intent
   ↓
FeedStore
   ↓
State update / Effect
   ↓
Compose UI
```

Main principles:

- UI sends intents
- Store handles business logic
- State is exposed as a Flow
- Effects are used for one-time actions
- Repository hides the data source from the UI layer
- Optimistic UI logic is separated from networking logic

---

## Module structure

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

| File | Purpose |
|---|---|
| `FeedContracts.kt` | Defines intents, state, effects, post models, filters, and UI contracts |
| `FeedStore.kt` | Handles business logic, intents, loading, pagination, and state updates |
| `FeedRepository.kt` | Repository interface used by the store |
| `KtorFeedRepository.kt` | Network implementation using Ktor and Supabase PostgREST |
| `FeedScreen.kt` | Main Compose screen entry point |
| `FeedTab.kt` | Voyager tab integration |
| `FeedFiltering.kt` | Feed filter definitions and filtering helpers |
| `FeedOptimisticState.kt` | Optimistic update helpers for likes, saves, and comments |
| `FeedMediaDataUri.kt` | Media encoding helpers |
| `FeedMediaPickerButton.kt` | Media picker UI integration |
| `FeedComposerLauncher.kt` | Public API for opening the feed composer from other modules |
| `FeedModule.kt` | Koin dependency injection module |

---

## Main technologies

- Kotlin Multiplatform
- Compose Multiplatform
- Coroutines
- Flow
- Koin
- Ktor
- Supabase PostgREST
- kotlinx.serialization
- Voyager
- Compose Resources

---

## Data flow

The UI does not call the backend directly.

User actions are converted into intents and handled by the store.

```text
FeedScreen
   ↓ sends intents
FeedStore
   ↓ calls
FeedRepository
   ↓ implemented by
KtorFeedRepository
   ↓ communicates with
Supabase PostgREST
```

This keeps the screen focused on rendering state, while the store controls loading, refreshing, pagination, optimistic updates, and error handling.

---

## Optimistic UI

Likes and saves are updated immediately in local state before the backend response arrives.

If the backend request fails, the previous state is restored.

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

This makes the feed feel faster because the user gets instant feedback instead of waiting for every network request to finish.

---

## Pagination and refresh

The feed supports:

- initial loading
- pull-to-refresh
- loading more posts
- end-of-list handling
- empty state handling
- error state handling
- retry actions

Pagination state is handled inside the store, not inside the UI.

The screen only reacts to the current state and displays content, loading indicators, empty states, or error states.

---

## Filtering

The feed can be filtered by post type or author role.

Examples of supported filters:

- all posts
- trainers only
- athletes only
- personal records
- coaching tips
- workout recaps
- form-check requests

Filtering logic is kept outside the main screen so it can be changed without rewriting UI code.

---

## Media handling

The module supports basic image upload through data URIs.

Media-related logic is separated into helper files:

```text
FeedMediaDataUri.kt
FeedMediaPickerButton.kt
```

This keeps media encoding and picker UI away from the main business logic.

---

## Composer launcher

The feed composer can be opened from other parts of the app.

For example, after finishing a workout, the app can open the composer and prepare a workout recap post.

```text
Workout summary
   ↓
FeedComposerLauncher
   ↓
Feed post composer
```

This connects the Feed module with the wider training flow of the app.

---

## Localization

The feature supports localization using Compose Resources.

Current supported languages:

- English
- Lithuanian
- Russian

The goal was to avoid hardcoded UI text and prepare the feature for users in different markets.

---

## Dependencies from the full project

This module was extracted from a larger private multi-module app.  
Some internal modules are referenced but not included in this public repository.

| Module | Used for |
|---|---|
| `core/designsystem` | Shared UI components, buttons, cards, and loading placeholders |
| `core/foundation` | Analytics and common app utilities |
| `core/model` | Shared domain models |
| `core/mvi` | Base MVI store |
| `core/navigation` | Cross-module navigation contracts |
| `core/network` | Ktor and Supabase setup |

Because of that, this repository is intended as a **code sample**, not as a fully runnable standalone application.

---

## Why only this module is public

The full Rytima project is private because it contains:

- production configuration
- backend-related code
- Firebase setup
- Supabase setup
- payment-related logic
- private app modules
- internal implementation details

This repository focuses on one feature so the code can be reviewed safely.

It shows:

- feature module structure
- MVI implementation
- Compose Multiplatform UI
- async state handling
- optimistic UI updates
- pagination and refresh handling
- backend communication with Ktor and Supabase
- separation between UI, store, and repository

---

## What this repository demonstrates

This repository demonstrates:

- Kotlin Multiplatform feature module structure
- Compose Multiplatform UI implementation
- MVI-style state management
- Coroutines and Flow usage
- separation of UI, business logic, and networking
- optimistic UI updates
- loading, empty, and error state handling
- backend integration with Ktor and Supabase
- modular feature code that can be extended later

---

## Status

This is an extracted showcase module from an active private project.

The goal of this repository is to demonstrate code organization, feature architecture, and implementation style.
