# PageTurner — Project Walkthrough

A reference document for the code review conversation. Covers every module,
its purpose, its internals, how modules connect, and where AI fits in.

---

## What the app does

PageTurner is a book discovery app built around a swipe loop. Swipe right to save
a book, left to skip, up to bookmark for later. The key idea: every swipe is a data
point. Every 10 swipes, Claude reads the full history and writes a structured taste
profile for the reader. That profile feeds back into everything — the order books
appear, how each book is described, and which "curveball" books get inserted to
stretch the reader's taste.

Three AI jobs run continuously in the background:
- A 2-sentence personal hook written for each card (brief generation)
- A structured taste profile rebuilt from swipe history every 10 swipes
- A wildcard book selection — one book every ~7 cards, from outside the user's genres,
  chosen because it shares a quality they demonstrably love

---

## Architecture overview

```
:app
  └── :feature:onboarding
  └── :feature:swipedeck
  └── :feature:tasteprofile
  └── :feature:readinglist
  └── :feature:bookdetail
       └── :core:domain        (pure Kotlin, no Android)
       └── :core:ui            (Compose design system)
       └── :core:analytics
       └── :core:logging

:core:ai     → :core:domain, :core:network
:core:data   → :core:domain, :core:network
:core:network → (no internal deps)
```

**The dependency rule:** feature modules may only import `:core:domain` and `:core:ui`.
They never touch `:core:data`, `:core:network`, or `:core:ai` directly. All of that
is wired together by `:app` through Hilt's dependency injection.

This means a feature module that needs to fetch books holds an interface
(`BookRepository`) declared in `:core:domain`, not the Room implementation
(`BookRepositoryImpl`) in `:core:data`. Swapping the data layer requires no
changes to any feature module.

**Pattern:** MVI (Model-View-Intent) applied consistently across every screen.
Each screen has a `UiState` (what to render), an `Intent` (what the user did),
and a `SideEffect` (one-shot events like navigation). ViewModels own state as
`StateFlow<UiState>` and expose side effects via a buffered `Channel`.

---

## Module reference

---

### `:core:domain`

**Plugin:** `kotlin.jvm` (no Android dependency — pure JVM)

The shared contract that every other module speaks. Contains nothing that knows
about Android, Room, Retrofit, or Claude. This is the module that keeps the
architecture honest.

**What lives here:**

- **Domain models** — `Book`, `BookDetail`, `TasteProfile`, `SwipeEvent`,
  `WildcardResult`, `OnboardingPreferences`, `Genre`, `ReadingLength`, `SwipeDirection`

- **Repository interfaces** — `BookRepository`, `SwipeRepository`, `ProfileRepository`.
  These are the contracts that `:core:data` implements and feature modules consume.

- **Service interface** — `AiService`. The only thing features ever know about AI
  is this interface. Claude is an implementation detail hidden in `:core:ai`.

- **Result types** — `Result<T>` (Success/Failure with `AppError`) for repository
  calls. `AiResult<T>` (Success/RateLimited/Failed) for AI calls — three states
  because rate limiting has a distinct user-facing response (show quota message)
  vs. a general failure (degrade silently).

- **Error types** — `AppError` sealed class: `NetworkError`, `NoInternetError`,
  `NotFoundError`, `TimeoutError`, `AiError`, `UnknownError`, `CacheExpiredError`.
  `UiError` is the presentation-layer version (title, message, isRetryable) that
  ViewModels map to before touching the UI.

**Key decision:** `AiResult` has three states, not two. `RateLimited` is explicitly
different from `Failed` because the app must surface a "quota reached" indicator
when that happens, while other failures should be invisible to the user.

---

### `:core:data`

**Purpose:** The only module that talks to Room. Implements all three repository
interfaces from `:core:domain`.

**Room database — `PageTurnerDatabase` (v2, 6 tables):**

| Table | Purpose |
|---|---|
| `books` | Cache of books fetched from Open Library. Survives across sessions. |
| `swipe_events` | Full swipe history — direction, book metadata, timestamp, wasWildcard. Fed to Claude for profile updates. |
| `taste_profile` | Single row. The most recent Claude-generated taste profile. |
| `saved_books` | Books the user swiped right or bookmarked. Has `isBookmarked` column added in migration v1→v2. |
| `ai_brief_cache` | Cached AI briefs keyed by `(bookKey, profileVersion)`. Invalidated when the profile version increments. |
| `onboarding` | Single row storing the user's selected genres and lengths. Presence determines whether to show onboarding. |

**Repositories:**

- **`BookRepositoryImpl`** — Cache-first for the swipe queue (emits Room data
  immediately, then triggers a network fetch if stock is low). `fetchNextPage`
  tracks the last-fetched page per genre internally so each replenishment call
  advances forward. `prefetchBookDetail` is a fire-and-forget background fetch
  called immediately when a user saves a book, so the detail screen is ready
  offline.

- **`SwipeRepositoryImpl`** — Records swipe events, manages saved books,
  provides swipe history for AI profile updates. `getSwipeCount()` and
  `getSavedBooks()` return Flows so the UI reacts in real time.

- **`ProfileRepositoryImpl`** — Stores and retrieves `TasteProfile` and
  `OnboardingPreferences`. `isOnboardingComplete()` is the Flow that drives
  the start destination decision at app launch.

**Migration:** v1→v2 adds `isBookmarked INTEGER NOT NULL DEFAULT 0` to
`saved_books`. Written as an explicit SQL migration (no destructive reset).

---

### `:core:network`

**Purpose:** HTTP clients and DTOs. No business logic, no Room. Two Retrofit
clients, each with its own OkHttp instance.

**Open Library client:**
- Base URL: `https://openlibrary.org/`
- No auth
- `HttpLoggingInterceptor` at `BASIC` level in debug, `NONE` in release
- `OpenLibraryApiService` exposes `searchBySubject()` and `getWorkDetail()`

**Anthropic client:**
- Base URL: `https://api.anthropic.com/`
- Auth injected by an OkHttp interceptor: `x-api-key` + `anthropic-version` headers
  added to every request. The API key is never in the service interface or its callers —
  it flows in via `@Named("anthropic_api_key")` injected from `:app`'s `AppModule`,
  which reads it from `BuildConfig.ANTHROPIC_API_KEY` (set from `local.properties`)
- `AnthropicApiService` exposes a single `createMessage()` endpoint

**DTOs:**
- `AnthropicRequestDto` — model, maxTokens, messages, system (system prompt)
- `AnthropicResponseDto` — parses the content block array, exposes `firstTextContent()`
- `SearchDocDto` / `SearchResponseDto` — Open Library search response
- `WorkDetailDto` — Open Library work detail, with a custom `WorkDescriptionAdapter`
  to handle the fact that the description field can be either a plain String or a
  `{"type": "text/plain", "value": "..."}` object

**`safeApiCall`:** A suspend inline function that wraps any Retrofit call in
`Result<T>`, mapping `SocketTimeoutException` → `TimeoutError`, `HttpException` →
`NetworkError` or `NotFoundError`, `IOException` → `NoInternetError`.

---

### `:core:ai`

**Purpose:** The Claude integration. Everything in this module is `internal` — no
feature module can reach past `:core:domain`'s `AiService` interface to touch
Anthropic directly.

**`ClaudeAiService`** — The public (but `internal`) implementation of `AiService`.
Delegates to three dedicated use cases and exposes `observeQuotaExceeded()` from
the rate limiter.

**Three use cases:**

**`GenerateBriefUseCase` (Haiku)**
Generates a 2-sentence personal book pitch. Fires once per card, before the card
reaches the top of the deck. Result cached in Room by `(bookKey, profileVersion)` —
the cache is effectively invalidated whenever Claude updates the taste profile.
Timeout: 30 seconds.

**`SummarizeProfileUseCase` (Sonnet)**
Reads the full swipe history (direction, genres, year, page count, wasWildcard for
each event) and returns a structured `TasteProfile` as JSON. Parsed with Moshi into
`ProfileSummaryDto`, then mapped to the domain model. Triggered every 10 swipes.
Timeout: 45 seconds.

**`PickWildcardUseCase` (Sonnet)**
Given the current taste profile and a pool of candidate books from off-genre subjects,
selects one and explains why in one sentence. The reason is stored on the card and
shown with a teal "WILDCARD" badge. On any failure, falls back to `candidates.random()`
with no reason shown — the wildcard slot is never left empty.
Timeout: 30 seconds.

**Prompt engineering:**
- Each use case has a **system prompt** (set in the `system` field of the request)
  for behavioral constraints: output format, no preamble, JSON-only for structured
  tasks
- User prompt contains the data: book metadata + profile summary for briefs;
  full swipe history formatted as a list + seed genres for profile; candidate book
  list + profile for wildcard
- `extractJson()` strips Markdown code fences (`\`\`\`json ... \`\`\``) from
  Claude's response before parsing — Claude sometimes adds them even when told not to

**`AiRateLimiter`** — Enforces per-device quotas using a Room table of timestamps:
- 20 calls / minute
- 100 calls / hour
- 200 calls / day

`checkAndRecord()` checks all three windows atomically, records the call if allowed,
prunes entries older than 24h. Exposes `quotaExceeded: StateFlow<Boolean>` so the
UI can react. The rate limiter is a singleton shared across all three use cases —
brief generation, profile summarization, and wildcard picking all count against the
same quota.

---

### `:core:ui`

**Purpose:** The Compose design system. All visual building blocks live here so
feature modules never contain raw colour hex values or raw dimension values.

**Design tokens:**
- `PageTurnerColors` — dark theme only. Near-black background (`0xFF0F0E17`), amber
  accent (`0xFFE8A020`), teal for AI/wildcard indicators (`0xFF5DCAA5`)
- `PageTurnerType` — text styles (CardTitle, Body, BodySmall, Label)
- `PageTurnerSpacing` — spacing constants (xs=4dp through xl=32dp)

**Shared components:**
- `BookCoverImage` — Coil async image with placeholder shimmer
- `AiBriefText` — renders the generated brief with amber left-border styling
- `AiBriefShimmer` — animated shimmer shown while the brief is being generated
- `AiLearningIndicator` — teal pulsing dot shown in the top bar while AI is active
- `MatchScoreBar` — gradient bar showing how well a book matches the taste profile
- `WildcardChip` / `GenreChip` — labelling chips used on cards
- `SwipeActionButton` — the skip/bookmark/save buttons below the deck
- `ErrorState`, `EmptyShelfState`, `LoadingIndicator`, `OfflineBanner` — shared
  feedback states used across all screens

---

### `:core:logging`

**Purpose:** A single injectable `AppLogger` interface so all logging is routed
through one place. Callers call `logger.d/i/w/e(tag, message)`. What actually
happens to that log is configured once, in the `Application` class.

**`TimberAppLogger`** — the production implementation. `internal` to this module;
consumers only ever see the interface.

**`LoggingModule`** — `@Binds @Singleton`: `TimberAppLogger → AppLogger`.

**In `PageTurnerApp`:** `Timber.plant(DebugTree())` only when `BuildConfig.DEBUG`.
In release builds, no tree is planted — log calls are no-ops. A crash-reporting
tree (e.g. Crashlytics) would be added here for production.

The practical benefit: removing all debug log output from release requires changing
one line in one file.

---

### `:core:analytics`

**Purpose:** Tool-agnostic analytics. The public surface is just two things:
`AnalyticsEvent` (sealed class of typed events) and `AnalyticsTracker` (interface
with a single `track(event)` method). The tool that receives events is completely
hidden from call sites.

**`AnalyticsEvent` — 9 event types:**
- `ScreenView(screenName)` — fired from every ViewModel init
- `BookSwiped(bookKey, direction, wasWildcard)`
- `BookSaved(bookKey, isBookmarked, wasWildcard)`
- `WildcardShown(bookKey)` — when a wildcard reaches the top of the deck
- `WildcardAccepted(bookKey)` — when a wildcard is saved or bookmarked
- `AiBriefGenerated(bookKey, durationMs)` — tracks AI latency
- `AiJobFailed(job, errorType)` — "generate_brief"/"summarize_profile" + "rate_limited"/"failed"
- `ProfileUpdated(swipeCount, profileVersion)`
- `ErrorOccurred(errorType, screenName)`

**Internal pipeline:**
```
AnalyticsEvent
    → EventMapper.toPayload()    (names + property maps)
    → AnalyticsAdapter.send()    (the tool wrapper)
    → LogcatAnalyticsAdapter     (current: logs via AppLogger)
```

Swapping to Firebase Analytics means: implement `FirebaseAnalyticsAdapter`, update
one `@Binds` in `AnalyticsModule`. Zero call-site changes.

---

### `:feature:onboarding`

**Screen:** Genre selection + reading length preference. Two-step UI, single screen.

**`OnboardingViewModel`:** Manages a set of selected genres and lengths.
`canProceed` is true only when at least one of each is selected. `Confirm` writes
`OnboardingPreferences` to Room via `ProfileRepository` and emits
`NavigateToSwipeDeck` as a side effect.

**Start-up routing:** `MainViewModel` observes `profileRepository.isOnboardingComplete()`.
If false, the nav graph starts at onboarding. If true, it starts at the swipe deck.
This check happens before the first frame (using `SharingStarted.Eagerly`), so the
splash screen is shown only until Room responds.

---

### `:feature:swipedeck`

The most complex module. This is the core of the app.

**`SwipeDeckViewModel` responsibilities:**

1. **Queue management** — Loads books from Room on startup. Combines the book queue,
   wildcard pool, and current profile into a reactive `combine()`. When the deck
   drops below 5 unswiped cards, `refreshQueueIfNeeded()` fetches the next page from
   Open Library imperatively. Appends new cards without rebuilding the existing deck
   (no flash, no position reset).

2. **Wildcard interleaving** — A wildcard is inserted every 4–10 regular cards
   (randomised per gap, averaging ~7). The wildcard pool is a separate list of books
   fetched from off-genre subjects chosen at startup. If wildcards arrive after the
   initial deck is built, they are injected into the unswiped portion of the existing
   deck.

3. **AI brief scheduling** — `scheduleBrief()` is called for every card added to the
   deck. It checks the `(bookKey, profileVersion)` cache first. On a cache miss, it
   calls `aiService.generateBrief()` in a coroutine. Results update the card in-place
   via `_state.update { }`. The deck never blocks waiting for AI — `aiBrief` is
   nullable and the UI shows a shimmer while it loads.

4. **Profile update trigger** — `observeSwipeCount()` watches the Room swipe count.
   When it hits a multiple of 10, it emits `TriggerProfileUpdate` as a side effect
   AND calls `runProfileUpdate()` to kick off `aiService.summarizeProfile()`.

5. **Match score** — Computed locally (no network, no AI) from the taste profile's
   liked and avoided genres against the book's subjects. Uses a maturity factor
   (asymptotically approaching 1.0 as profileVersion increases) so early profiles
   have low confidence and late profiles are trusted more. Wildcards are capped at
   0.45 regardless of genre match.

**`SwipeDeckScreen`:**

The gesture system uses two `Animatable` values (`offsetX`, `offsetY`). Drag
gesture updates them via `snapTo` (immediate, no animation). On release:
- Past threshold in X → animate off screen → call `onSwipeLeft/Right`
- Didn't cross threshold → spring back to rest

Button-triggered swipes set `pendingButtonSwipe` state which is observed by
a `LaunchedEffect` inside the card composable — it animates the card off screen
and then calls the callback. This keeps animation logic in the composable, intent
logic in the ViewModel.

Cards are rendered back-to-front (depth 2 → 0), with background cards interpolating
toward their "promoted" position as the top card drags. `key(bookKey)` ensures Compose
reuses the same composable instance when a card rises from depth-1 to depth-0,
eliminating layout flash on transition.

---

### `:feature:tasteprofile`

**Screen:** Displays the AI-written summary, liked/avoided genres, preferred length,
recurring themes, and swipe stats (total swiped, total saved, wildcards kept).

**`TasteProfileViewModel`:** Built entirely with `combine()` — no manual state
management. Combines `profileRepository.getProfile()`, `swipeRepository.getSwipeCount()`,
`swipeRepository.getSavedBooks()`, and `aiService.observeQuotaExceeded()` into a
single `TasteProfileUiState`. Room is the source of truth — the state auto-refreshes
whenever the profile updates after a swipe milestone.

Error in the pipeline emits a non-retryable `UiError` state rather than crashing.

---

### `:feature:readinglist`

**Screen:** Two-tab pager (Liked / Bookmarked). 3-column book grid. Long-press
shows a remove confirmation bottom sheet.

**`ReadingListViewModel`:** Combines `swipeRepository.getLikedBooks()` and
`swipeRepository.getBookmarkedBooks()` into a single state. Both are Room Flows —
fully offline, zero network calls. `RemoveBook` deletes from Room; the UI updates
automatically via the Flow.

---

### `:feature:bookdetail`

**Screen:** Full book detail — cover, authors, year/pages, genre chips, full
description, AI brief if available, Open Library link. Shared element transition
on the cover image from reading list → detail.

**`BookDetailViewModel`:** Loads book detail from Room first (if description is
cached), falls back to a network fetch if not. Errors are mapped to `UiError`
and shown with a retry option. `prefetchBookDetail` (called from `SwipeDeckViewModel`
when a book is saved) means most detail screens load instantly from cache.

`isOffline` is set specifically when the error is `NoInternetError`, so the UI
can show a more helpful message than a generic error.

---

### `:app`

**Wires everything together.**

- `AppModule` — provides `@Named("anthropic_api_key")` from `BuildConfig`, which
  reads from `local.properties` at build time. The key never appears in source code.
- `MainActivity` — single activity, sets up Compose with `PageTurnerNavGraph`
- `PageTurnerApp` — Hilt entry point, plants Timber `DebugTree` in debug builds only
- `MainViewModel` — determines start destination by observing `isOnboardingComplete()`
- `NavGraph` — five destinations. Onboarding and book detail are not in the bottom nav.
  Back stack management uses `saveState`/`restoreState` so tab state is preserved
  when switching bottom nav tabs.

---

## How the AI integrates with user actions

```
User swipes right
    → SwipeDeckViewModel.performSwipe()
        → swipeRepository.recordSwipe()         (Room write)
        → swipeRepository.saveBook()            (Room write)
        → bookRepository.prefetchBookDetail()   (background network)
        → analytics.track(BookSaved, BookSwiped, WildcardAccepted?)

Every 10 swipes (via observeSwipeCount)
    → aiService.summarizeProfile(history, onboardingGenres)
        → Anthropic Sonnet API call
        → JSON parsed → TasteProfile
        → profileRepository.saveProfile()      (Room write)
        → TasteProfileViewModel auto-updates   (observing Room Flow)
        → brief cache invalidated by profileVersion bump

For each new card added to deck
    → scheduleBrief(card, profileSummary, profileVersion)
        → check AiBriefCacheDao first
        → on miss: aiService.generateBrief(book, profileSummary)
            → Anthropic Haiku API call
            → result cached in Room
            → card updated in-place, no rebuild
```

---

## Key design decisions worth discussing

**Snapshot seen keys, not reactive deck rebuild**
The swipe queue is fetched once at startup. Each swipe advances an index, not
a reactive filter. A reactive approach (re-query Room on every swipe) caused
7–8 second hangs due to cache misses triggering network fetches. The current
approach fetches new pages imperatively when the deck runs low.

**AI degrades on every axis**
Brief not ready → shimmer, then loads in. Brief generation fails → no brief shown,
card is still usable. Profile update rate-limited → existing profile preserved, quota
indicator shown. Wildcard pick fails → random fallback, no reason shown. Nothing
blocks the swipe loop.

**`AiResult` vs `Result`**
Two distinct result types for a reason: `Result<T>` has Success/Failure.
`AiResult<T>` has Success/RateLimited/Failed. The three-way distinction lets
the ViewModel respond differently to quota exhaustion (surface a banner) vs.
other AI failures (silent degradation).

**Brief cache keyed by `(bookKey, profileVersion)`**
The brief is personalised to the profile. If the profile updates, old briefs
are stale — they were written for a different reader model. Incrementing
`profileVersion` effectively invalidates the entire brief cache without
needing an explicit purge.

**`internal` on everything in `:core:ai` and `:core:analytics`**
The only public surface is the interface in `:core:domain`. This is enforced
by the compiler, not convention — a feature module literally cannot import
`ClaudeAiService` or `LogcatAnalyticsAdapter`. Swapping implementations
requires changing one `@Binds` binding.

**R8 enabled for release**
The Anthropic API key lives in `BuildConfig`. Without code shrinking it would
be readable in plain text from the APK. ProGuard rules cover Retrofit (reflection
on service interfaces), Moshi (generated adapters), Hilt components, and Room.

---

## Test coverage

Each layer has its own test scope:

| Area | What's tested |
|---|---|
| Domain mappers | `BookMapper`, `SwipeEventMapper` round-trips |
| `:core:data` | `BookRepositoryImpl` cache-first behaviour; `SwipeRepositoryImpl` swipe recording |
| `:core:ai` | All three use cases — success, timeout, rate-limited, parse failure, blank response |
| `:feature:swipedeck` | ViewModel swipe loop, index advancement, saveBook calls, profile trigger at 10 swipes, replenishment flag, navigation side effect |
| `:feature:tasteprofile` | State from combined flows, null profile, quota exceeded state |
| `:feature:onboarding` | Genre/length toggle state machine, confirm guard, side effect |
| `:feature:readinglist` | State from Room flows, SelectBook/RemoveBook intents |
| `:feature:bookdetail` | Load success/failure, isSaved, all intents |

Tests use JUnit 5 + MockK. All ViewModel tests use `StandardTestDispatcher`
with `Dispatchers.setMain` so coroutines are fully controlled. `AiRateLimiter`
and `AppLogger` are `@MockK(relaxed = true)` — relaxed mocks silently absorb
all calls to void-returning infrastructure so tests stay focused on behaviour.
