# PageTurner — Android Engineering Challenge
# Project Plan for Claude Code

> **Frameworks used:**
> - **MAARS** (Modules · Architecture · Agents · Resilience · Standards) — governs the project structure and quality bar
> - **CRAFT** (Clarify · Render · Assemble · Fortify · Tighten) — governs the multi-agent workflow
>
> **Product authority:** `docs/PAGETURNER_PRODUCT_DEFINITION.md` defines what to build and why.
> This document defines how to build it. Where they conflict, the product definition wins.

---

## 1. App Brief (MAARS: Architecture)

- **Problem:** Book discovery is broken — lists, charts, and shelves require you to already know what you want. PageTurner flips this: you react to one book at a time, and the app learns your taste from your swipes.
- **User:** The hiring committee evaluating architectural maturity — and any reader who has ever stared at a to-read list and felt paralyzed.
- **Success:** A working Tinder-style swipe experience backed by real Open Library data and real Claude AI personalization, with offline support, clean architecture, and testable code.
- **API:** Open Library (`https://openlibrary.org`) — no auth required. Claude API (`api.anthropic.com`) — key in build config.
- **Constraints:** No backend (Room for all local state), Kotlin + Jetpack Compose, multi-module, MVI + Clean Architecture throughout.

---

## 2. Module Graph (MAARS: Modules)

```
:app
├── :feature:onboarding        (genre + length preference picker, first launch only)
├── :feature:swipedeck         (main swipe experience, the home screen)
├── :feature:tasteprofile      (AI-generated taste summary screen)
├── :feature:readinglist       (saved books grid)
├── :feature:bookdetail        (full book detail, shared element entry)
├── :core:network              (Retrofit, OkHttp, interceptors, safeApiCall)
├── :core:ai                   (Claude API client + 3 UseCase wrappers)
├── :core:data                 (Room database, repositories, mappers, cache strategy)
├── :core:domain               (UseCases, domain models, repository interfaces — pure Kotlin)
├── :core:ui                   (design system: tokens, components, theme — dark only)
├── :core:analytics            (AnalyticsTracker interface + no-op impl)
├── :core:logging              (AppLogger interface + Timber impl)
└── :core:testing              (shared fakes, base test classes, test utilities)
```

**Dependency rules (strictly enforced):**
- `:feature:*` depends on `:core:domain` and `:core:ui` only — never on each other, never on `:core:data`, never on `:core:ai` directly
- `:core:ai` depends on `:core:domain` and `:core:network` — exposes UseCases, hides the Anthropic SDK
- `:core:data` depends on `:core:network` and `:core:domain`
- `:core:domain` depends on nothing (pure Kotlin, zero Android imports)
- `:core:ui` depends on nothing except Compose and Material3
- `:app` wires everything via Hilt — the only module that sees all other modules

---

## 3. Architecture Pattern (MAARS: Architecture)

**MVI + Clean Architecture**, strictly layered:

```
UI Layer (Compose Screens)
    │  emits Intent
    ▼
ViewModel (MVI: holds UiState, processes Intent → emits SideEffect)
    │  calls
    ▼
UseCase (domain logic, no Android deps — including AI UseCases from :core:ai)
    │  calls
    ▼
Repository Interface (domain)  ◄─── implemented by ─── RepositoryImpl (:core:data)
    │
    ├─── RemoteDataSource (Retrofit → Open Library)
    └─── LocalDataSource  (Room DAO)

AI layer (parallel to Repository layer):
UseCase calls AiService interface (:core:domain)
    ◄─── implemented by ─── ClaudeAiService (:core:ai)
              │
              └─── Anthropic Claude API (via Retrofit client in :core:network)
```

**The AI layer is explicitly decoupled.** Feature modules call UseCases. UseCases call interfaces. The Anthropic SDK and API key never appear outside `:core:ai`.

---

## 4. MVI Contracts — All Screens

### Onboarding
```kotlin
data class OnboardingUiState(
    val genres: List<Genre> = Genre.all(),
    val selectedGenres: Set<Genre> = emptySet(),
    val selectedLength: ReadingLength? = null,
    val canProceed: Boolean = false  // true when >= 1 genre + length selected
)

sealed class OnboardingIntent {
    data class ToggleGenre(val genre: Genre) : OnboardingIntent()
    data class SelectLength(val length: ReadingLength) : OnboardingIntent()
    object Confirm : OnboardingIntent()
}

sealed class OnboardingSideEffect {
    object NavigateToSwipeDeck : OnboardingSideEffect()
}
```

### Swipe Deck
```kotlin
data class SwipeDeckUiState(
    val cards: List<SwipeCardUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isGeneratingBrief: Boolean = false,
    val error: UiError? = null,
    val isOffline: Boolean = false,
    val swipeCount: Int = 0,
    val swipesUntilProfileUpdate: Int = 10,
    val currentCardIndex: Int = 0
)

data class SwipeCardUiModel(
    val bookKey: String,
    val title: String,
    val authors: List<String>,
    val coverUrl: String?,
    val publishYear: Int?,
    val pageCount: Int?,
    val subjects: List<String>,
    val aiBrief: String?,         // null while generating, populated async
    val matchScore: Float,        // 0.0-1.0, calculated locally
    val isWildcard: Boolean,
    val wildcardReason: String?   // Claude's one-sentence explanation
)

sealed class SwipeDeckIntent {
    data class SwipeLeft(val bookKey: String) : SwipeDeckIntent()   // Skip
    data class SwipeRight(val bookKey: String) : SwipeDeckIntent()  // Save to reading list
    data class Bookmark(val bookKey: String) : SwipeDeckIntent()    // Save to Maybe
    data class ExpandCard(val bookKey: String) : SwipeDeckIntent()  // Open detail
    object Retry : SwipeDeckIntent()
    object LoadMore : SwipeDeckIntent()
}

sealed class SwipeDeckSideEffect {
    data class NavigateToDetail(val bookKey: String) : SwipeDeckSideEffect()
    object TriggerProfileUpdate : SwipeDeckSideEffect()             // fires every 10 swipes
    data class ShowSnackbar(val message: String) : SwipeDeckSideEffect()
}
```

### Taste Profile
```kotlin
data class TasteProfileUiState(
    val isLoading: Boolean = false,
    val profile: TasteProfileUiModel? = null,
    val swipeStats: SwipeStats = SwipeStats(),
    val error: UiError? = null
)

data class TasteProfileUiModel(
    val aiSummary: String,            // Claude's verbatim output
    val likedGenres: List<String>,
    val avoidedGenres: List<String>,
    val preferredLength: String,
    val lastUpdatedSwipeCount: Int
)

data class SwipeStats(
    val totalSwiped: Int = 0,
    val totalSaved: Int = 0,
    val wildcardKept: Int = 0
)

sealed class TasteProfileIntent {
    object Refresh : TasteProfileIntent()
}

sealed class TasteProfileSideEffect {
    data class ShowSnackbar(val message: String) : TasteProfileSideEffect()
}
```

### Reading List
```kotlin
data class ReadingListUiState(
    val books: List<SavedBookUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false
)

data class SavedBookUiModel(
    val bookKey: String,
    val title: String,
    val coverUrl: String?,
    val authors: List<String>,
    val savedAt: Long
)

sealed class ReadingListIntent {
    data class SelectBook(val bookKey: String) : ReadingListIntent()
    data class RemoveBook(val bookKey: String) : ReadingListIntent()
}

sealed class ReadingListSideEffect {
    data class NavigateToDetail(val bookKey: String) : ReadingListSideEffect()
}
```

### Book Detail
```kotlin
data class BookDetailUiState(
    val isLoading: Boolean = false,
    val book: BookDetailUiModel? = null,
    val error: UiError? = null,
    val isOffline: Boolean = false
)

data class BookDetailUiModel(
    val bookKey: String,
    val title: String,
    val authors: List<String>,
    val coverUrl: String?,
    val publishYear: Int?,
    val pageCount: Int?,
    val subjects: List<String>,
    val description: String?,
    val aiBrief: String?,
    val isWildcard: Boolean,
    val wildcardReason: String?,
    val openLibraryUrl: String
)

sealed class BookDetailIntent {
    object NavigateBack : BookDetailIntent()
    object OpenOnOpenLibrary : BookDetailIntent()
    object Retry : BookDetailIntent()
}

sealed class BookDetailSideEffect {
    object NavigateBack : BookDetailSideEffect()
    data class OpenUrl(val url: String) : BookDetailSideEffect()
}
```

---

## 5. Tech Stack

| Concern | Library | Version |
|---------|---------|---------|
| DI | Hilt | 2.51 |
| Networking | Retrofit 2 + OkHttp 4 | latest |
| JSON | Moshi (KSP) | latest |
| Image loading | Coil 3 | latest |
| Local DB | Room (KSP) | latest |
| Async | Kotlin Coroutines + Flow | latest |
| Navigation | Compose Navigation + Shared Elements | 1.7+ |
| Testing | JUnit5 + MockK + Turbine | latest |
| Logging | Timber | latest |
| Build | Gradle Version Catalog (libs.versions.toml) | - |
| AI | Anthropic Claude API via Retrofit (claude-sonnet-4-20250514) | - |

---

## 6. Open Library API Contract

**Search by subject (queue building):**
```
GET https://openlibrary.org/search.json?subject={genre}&limit=20&page={n}
```

**Search by query (wildcard candidate fetch):**
```
GET https://openlibrary.org/search.json?q={query}&limit=20&page={n}
```

Response fields consumed:
- `docs[].key` — unique work identifier (e.g. `/works/OL45804W`)
- `docs[].title`
- `docs[].author_name[]`
- `docs[].first_publish_year`
- `docs[].cover_i` — cover image ID
- `docs[].subject[]` — genre/theme tags
- `docs[].number_of_pages_median`
- `numFound`

**Cover image:**
```
https://covers.openlibrary.org/b/id/{cover_i}-M.jpg
```

**Work detail:**
```
GET https://openlibrary.org/works/{workId}.json
```
Returns: `description` (string or `{value: string}` object — handle both), `subjects`, `first_sentence`

---

## 7. Claude API Contract (:core:ai)

All three AI jobs are Retrofit calls to `https://api.anthropic.com/v1/messages` using
`claude-sonnet-4-20250514`. The API key is injected via `BuildConfig.ANTHROPIC_API_KEY`
(set in `local.properties`, never committed to git).

Each job is a separate UseCase in `:core:ai` behind an `AiService` interface defined
in `:core:domain`.

### AI Job 1: GenerateBriefUseCase
**Trigger:** Once per book per profile version, when a book enters the swipe queue.
**Input to Claude:**
```
Book: {title} by {author} ({year}, {pageCount} pages)
Subjects: {subjects joined}
Description: {raw OL description or "no description available"}

Reader profile: {aiSummary from TasteProfile, or "new user — genres: {onboarding genres}"}

Write exactly 2 sentences. A personal pitch for why THIS reader might love this book.
Not a plot summary. A hook. Speak directly to them. Be specific to their taste.
Output only the 2 sentences. No preamble, no quotes.
```
**Output:** 2-sentence string. Cached in Room keyed by `(bookKey, profileVersion)`.
**On failure:** Return null. The card renders without a brief — never blocked.

### AI Job 2: SummarizeProfileUseCase
**Trigger:** Automatically after every 10 swipes (swipe count % 10 == 0).
**Input to Claude:**
```
Swipe history (most recent {n} swipes):
{for each swipe: direction (right/left/bookmark), title, genres[], year, pageCount, wasWildcard}

The user started with genre preferences: {onboarding genres}

Analyze this reading behavior. Return a JSON object only, no preamble:
{
  "aiSummary": "1-3 sentence plain English description of their taste",
  "likedGenres": ["genre1", "genre2"],
  "avoidedGenres": ["genre1"],
  "preferredLength": "short|medium|long|any",
  "recurringThemes": ["theme1", "theme2"]
}
```
**Output:** Parsed `TasteProfile` domain object. Written to Room. `profileVersion` increments.
**On failure:** Keep existing profile unchanged. Log the error. Never reset to empty.

### AI Job 3: PickWildcardUseCase
**Trigger:** Every 7th card slot in the swipe deck.
**Input to Claude:**
```
Reader profile: {aiSummary}
Liked genres: {likedGenres}

Here are 10 candidate books from genres outside their preferences:
{for each: title, author, year, pageCount, subjects}

Pick exactly ONE book that shares a quality this reader demonstrably loves,
expressed through an unfamiliar genre. Return JSON only:
{
  "selectedIndex": 0,
  "reason": "one sentence explaining the connection to their taste"
}
```
**Output:** Selected book index + reason string. Reason displayed as `wildcardReason` on card.
**On failure:** Select a random candidate from the pool. No reason shown. Slot is never empty.

---

## 8. Design System (:core:ui)

**Dark theme only.** Single `darkColorScheme`. This is a deliberate product decision —
see `PAGETURNER_PRODUCT_DEFINITION.md` for the rationale.

```kotlin
// PageTurnerTheme.kt
object PageTurnerColors {
    val Background     = Color(0xFF0F0E17)   // near-black, warm undertone
    val Surface        = Color(0xFF1A1928)   // slightly lifted surface
    val SurfaceVariant = Color(0xFF252438)   // cards, elevated surfaces
    val OnBackground   = Color(0xFFF5F0E8)   // warm white
    val OnSurface      = Color(0xFFF5F0E8)
    val OnSurfaceMuted = Color(0x73F5F0E8)   // 45% opacity warm white
    val Accent         = Color(0xFFE8A020)   // amber — primary action color
    val OnAccent       = Color(0xFF0F0E17)
    val Teal           = Color(0xFF5DCAA5)   // wildcard badge, AI indicators
    val Error          = Color(0xFFFF4444)
    val SkipRed        = Color(0x29FF3C3C)   // swipe-left overlay tint
    val SaveGreen      = Color(0x295DCAA5)   // swipe-right overlay tint
}

object PageTurnerSpacing {
    val xs = 4.dp; val sm = 8.dp; val md = 16.dp; val lg = 24.dp; val xl = 32.dp
}

object PageTurnerType {
    val AppTitle    = TextStyle(fontFamily = FontFamily.Serif, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    val CardTitle   = TextStyle(fontFamily = FontFamily.Serif, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    val DetailTitle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    val Body        = TextStyle(fontSize = 14.sp, lineHeight = 22.sp)
    val BodySmall   = TextStyle(fontSize = 12.sp, lineHeight = 18.sp)
    val AiBrief     = TextStyle(fontSize = 13.sp, lineHeight = 20.sp, fontStyle = FontStyle.Italic)
    val Label       = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
    val Chip        = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
}
```

**Shared components in `:core:ui`:**
```
BookCoverImage(coverUrl, modifier, contentDescription)
GenreChip(label, modifier)                         — amber tint
WildcardChip(modifier)                             — teal tint, fixed label "Wildcard pick"
MatchScoreBar(score: Float, modifier)              — amber fill bar, 0.0-1.0
AiBriefText(brief: String, modifier)               — italic, amber left border
AiBriefShimmer(modifier)                           — pulsing placeholder while generating
ErrorState(message, onRetry, modifier)
LoadingIndicator(modifier)
EmptyShelfState(modifier)                          — illustrated empty state
OfflineBanner(modifier)                            — amber strip
SwipeActionButton(type: Skip|Bookmark|Save, onClick, modifier)
PageTurnerTopBar(title, onBackClick, modifier)
AiLearningIndicator(modifier)                      — teal dot + "AI learning" label
```

---

## 9. Data Layer (:core:data)

### Room Entities

```
BookEntity(
    key, title, authorNames, coverUrl, publishYear, pageCount,
    subjects, description, cachedAt
)

SwipeEventEntity(
    id, bookKey, direction (LEFT|RIGHT|BOOKMARK), timestamp,
    bookGenres, bookYear, bookPageCount, wasWildcard
)

TasteProfileEntity(
    id, aiSummary, likedGenresJson, avoidedGenresJson,
    preferredLength, recurringThemesJson, profileVersion,
    lastUpdatedSwipeCount, updatedAt
)

SavedBookEntity(
    bookKey, savedAt, aiBrief, wildcardReason, isWildcard
    -- FK to BookEntity
)

AiBriefCacheEntity(
    bookKey, profileVersion, brief, generatedAt
)

OnboardingEntity(
    completed, selectedGenresJson, selectedLength, completedAt
)
```

### Cache Strategy

```
Books for swipe queue:
    1. Check Room for unseen books matching current profile genres
    2. If < 5 books available: fetch next page from Open Library
    3. Upsert to Room, filter against SwipeEventEntity (seen books)
    4. Emit fresh queue. Never show the same book twice.

AI briefs:
    1. Check AiBriefCacheEntity for (bookKey, profileVersion) hit
    2. On hit: return cached brief immediately
    3. On miss: call GenerateBriefUseCase async, cache result, emit when ready
    4. Card is displayed immediately — brief populates when ready, never blocks

Reading list (offline-first):
    1. Always read from SavedBookEntity + BookEntity join
    2. Fully available offline — zero network dependency
    3. No TTL — saved books never expire

Taste profile:
    1. Always read from TasteProfileEntity (Room)
    2. Refreshed only by SummarizeProfileUseCase (every 10 swipes)
    3. Available offline — profile shown from last successful update
```

---

## 10. Error Handling Contract (MAARS: Resilience)

```kotlin
sealed class AppError {
    data class NetworkError(val code: Int?) : AppError()
    object NoInternetError : AppError()
    object NotFoundError : AppError()
    data class AiError(val message: String?) : AppError()    // Claude API failure
    object AiTimeoutError : AppError()                        // Claude took too long
    data class UnknownError(val message: String?) : AppError()
    object CacheExpiredError : AppError()
}

data class UiError(
    val title: String,
    val message: String,
    val isRetryable: Boolean
)
```

**AI-specific degradation rules — these are non-negotiable:**
- `GenerateBriefUseCase` failure → card shows without brief. Never block the swipe deck.
- `SummarizeProfileUseCase` failure → keep existing profile. Never reset to empty.
- `PickWildcardUseCase` failure → use random candidate from the pool, no reason shown.
- All AI errors are logged via `:core:logging` and tracked via `AnalyticsEvent.AiError`.

---

## 11. Analytics Events (:core:analytics)

```kotlin
sealed class AnalyticsEvent {
    data class ScreenView(val screenName: String) : AnalyticsEvent()
    data class BookSwiped(val bookKey: String, val direction: String, val wasWildcard: Boolean) : AnalyticsEvent()
    data class ProfileUpdated(val swipeCount: Int, val profileVersion: Int) : AnalyticsEvent()
    data class WildcardShown(val bookKey: String) : AnalyticsEvent()
    data class WildcardAccepted(val bookKey: String) : AnalyticsEvent()
    data class AiBriefGenerated(val bookKey: String, val durationMs: Long) : AnalyticsEvent()
    data class AiJobFailed(val job: String, val errorType: String) : AnalyticsEvent()
    data class ErrorOccurred(val errorType: String, val screenName: String) : AnalyticsEvent()
}
```

---

## 12. Git Setup

### Repository structure
```
.git/
.gitignore                   (standard Android + local.properties)
AGENTS.md
README.md
CHANGELOG.md
local.properties             (gitignored — contains ANTHROPIC_API_KEY)
docs/
  ANDROID_PROJECT_PLAN.md
  PAGETURNER_PRODUCT_DEFINITION.md
  agents/
    pm-agent.md
    design-agent.md
    dev-agent.md
    test-agent.md
    review-agent.md
app/
feature/
  onboarding/
  swipedeck/
  tasteprofile/
  readinglist/
  bookdetail/
core/
  network/
  ai/
  data/
  domain/
  ui/
  analytics/
  logging/
  testing/
```

### Branch strategy
```
main              <- tagged releases only, PR required
develop           <- integration branch
feature/*         <- human feature work
agent/pm-*        <- PM agent output
agent/design-*    <- Design agent output
agent/dev-*       <- Dev agent output
agent/test-*      <- Test agent output
agent/review-*    <- Review agent output
```

### Commit convention
```
type(scope): message

Types: feat | fix | test | refactor | docs | chore | style | agent
Scope: app | onboarding | swipedeck | tasteprofile | readinglist | bookdetail |
       network | ai | data | domain | ui | analytics | logging

Examples:
feat(swipedeck): implement swipe gesture with rotation and color overlay
feat(ai): implement GenerateBriefUseCase with profile-aware prompt
feat(ai): implement SummarizeProfileUseCase with JSON response parsing
test(ai): unit tests for all three AI UseCases including failure paths
agent(dev): implement swipedeck MVI layer
```

### PR template (.github/pull_request_template.md)
```markdown
## Agent
<!-- Which CRAFT agent produced this? -->

## What changed

## MAARS checklist
- [ ] Module boundaries respected (features depend only on :core:domain and :core:ui)
- [ ] :core:ai isolated — Anthropic SDK not visible outside this module
- [ ] MVI contract followed (UiState, Intent, SideEffect defined and used)
- [ ] AI failures degrade gracefully per Section 10 rules
- [ ] Error handled via AppError sealed class
- [ ] Unit tests added including AI failure path tests
- [ ] Design system tokens used (no hardcoded colors/spacing)
- [ ] KDoc on all public APIs
- [ ] Offline behavior verified
```

---

## 13. CRAFT Agent Definitions

### Agent 1: PM Agent (C — Clarify)
**Branch:** `agent/pm-{feature}`

**System prompt:**
```
You are the Product Manager agent for the PageTurner Android app.

PageTurner is a Tinder-style book discovery app. Users swipe through book cards one
at a time. The app uses Claude AI in three distinct ways:
1. Generating personalized 2-sentence book briefs tailored to the reader's taste
2. Summarizing swipe history into a taste profile every 10 swipes
3. Picking intentional wildcard books every 7th card

Full product definition is in: docs/PAGETURNER_PRODUCT_DEFINITION.md
Read it before producing any spec.

Your role: translate feature requests into precise developer-ready specifications.
You do NOT write code. You produce:
1. User story with acceptance criteria
2. Edge cases and failure modes (always include AI failure modes)
3. API contract for the feature (which endpoints, which fields)
4. State inventory (every state the screen can be in, including AI loading states)
5. Navigation flows and back stack rules

Output format: structured Markdown, each section clearly labeled.
Sign off with: "PM SIGN-OFF: spec is ready for Design Agent."
```

**Task template:**
```
PM Agent — specify the {feature_name} feature.

Requirements from product definition:
- {bullet list from PAGETURNER_PRODUCT_DEFINITION.md}

Produce a full spec.
```

---

### Agent 2: Design Agent (R — Render)
**Branch:** `agent/design-{feature}`

**System prompt:**
```
You are the Design Agent for the PageTurner Android app.

Design system (all tokens from PageTurnerTheme in :core:ui — never hardcode values):
- Dark theme only. Background: #0F0E17. Surface: #1A1928. Accent: #E8A020 (amber).
  Teal: #5DCAA5 for wildcard and AI indicators.
- Text: #F5F0E8 primary, 45% opacity muted. Serif for titles, sans-serif for body.
- Spacing: xs=4dp, sm=8dp, md=16dp, lg=24dp, xl=32dp
- Swipe cards: layered stack (top card full, two card edges visible behind)
- AI content (briefs, taste summary): always italic with amber left border
- Wildcard badges: teal chip. Match score: amber fill bar.
- Swipe overlays: red tint on left drag, green/teal tint on right drag

You produce Jetpack Compose component specifications:
1. Component name and which module it lives in
2. Parameters (name, type, description, default)
3. Visual spec (layout description, spacing using token names, color token names)
4. All states: loading/shimmer, content, error, empty, AI-loading (brief generating)
5. Accessibility notes (content descriptions, semantics)

Sign off with: "DESIGN SIGN-OFF: components ready for Dev Agent."
```

**Task template:**
```
Design Agent — specify components for the {screen_name} screen.

PM spec: {paste PM agent output}

Produce component specs for all UI elements on this screen.
```

---

### Agent 3: Dev Agent (A — Assemble)
**Branch:** `agent/dev-{feature}`

**System prompt:**
```
You are the Dev Agent for the PageTurner Android app.

Architecture rules you must follow without exception:
- Feature modules (:feature:*) depend ONLY on :core:domain and :core:ui
- AI UseCases live in :core:ai behind AiService interface in :core:domain
- Features never import from :core:ai, :core:data, or :core:network directly
- Every screen: UiState (data class), Intent (sealed), SideEffect (sealed), ViewModel
- AI errors must degrade gracefully per Section 10 — never block the UI
- All colors/spacing/typography from PageTurnerTheme tokens only — never hardcode
- KDoc on every public class, interface, and function
- Kotlin coroutines + Flow throughout — no callbacks, no LiveData
- @HiltViewModel for all ViewModels, @Inject for all dependencies
- ANTHROPIC_API_KEY only appears in :core:ai and BuildConfig — never in logs or UI

When you produce a file, state:
- Module it belongs in
- Full package name (com.pageturner.{module}.{subpackage})
- Any new Gradle dependencies required

Sign off with: "DEV SIGN-OFF: implementation ready for Test Agent."
```

**Task template:**
```
Dev Agent — implement the {feature_name} feature.

PM spec: {paste PM output}
Design spec: {paste Design output}

Implement all files. Start with domain layer (models, interfaces), then
:core:ai if needed, then :core:data, then :feature UI layer.
```

---

### Agent 4: Test Agent (F — Fortify)
**Branch:** `agent/test-{feature}`

**System prompt:**
```
You are the Test Agent for the PageTurner Android app.

You write exhaustive unit tests. You test:
1. UseCases — every path including error paths and AI degradation paths
2. ViewModels — every Intent produces correct UiState transitions and SideEffects
3. Repositories — cache-first logic, brief caching by profileVersion, swipe recording
4. Mappers — every field mapping including null/missing/malformed values
5. AI UseCases specifically:
   - GenerateBriefUseCase: happy path, Claude failure (returns null), timeout
   - SummarizeProfileUseCase: happy path, JSON parse failure (keeps old profile), timeout
   - PickWildcardUseCase: happy path, failure (falls back to random), malformed JSON
6. Wildcard slot logic — every 7th card triggers wildcard, not every 6th or 8th
7. Profile update trigger — fires at swipe 10, 20, 30 — not 9, 11

Test naming: given_[precondition]_when_[action]_then_[outcome]
Structure: // Arrange // Act // Assert
Framework: JUnit5 + MockK + Turbine + Kotlin Coroutines Test
Fakes: use or create fakes in :core:testing

Coverage requirement: every public function in UseCases, ViewModels, and Repositories
has at minimum one happy-path test AND one error-path test. AI UseCases must have
both success and failure tested — a suite missing the failure case is incomplete.

Sign off with: "TEST SIGN-OFF: test suite ready for Review Agent."
```

**Task template:**
```
Test Agent — write tests for the {feature_name} feature.

Implementation files:
{paste list of files produced by Dev Agent}

Dev sign-off context: {paste Dev Agent sign-off}

Write full test suites for all UseCases, ViewModels, and Repositories.
```

---

### Agent 5: Review Agent (T — Tighten)
**Branch:** `agent/review-{feature}`

**System prompt:**
```
You are the Review Agent for the PageTurner Android app.

You audit code before it merges. You check:

1. ARCHITECTURE
   - Are module boundaries respected? (features only on :core:domain + :core:ui)
   - Is :core:ai fully isolated? Anthropic SDK not visible in any feature module?
   - Is MVI correctly applied on every screen?
   - Any layer violations (UI calling repository directly, etc.)?

2. AI INTEGRATION
   - Do all 3 AI jobs degrade gracefully on failure?
   - Is ANTHROPIC_API_KEY never exposed in logs, error messages, or UI strings?
   - Are briefs cached correctly by (bookKey, profileVersion)?
   - Does brief generation never block the swipe deck?
   - Does profile update failure leave the existing profile intact?

3. CODE QUALITY
   - Readable? Consistent naming? No magic numbers?
   - No duplicated logic? Single responsibility?

4. RESILIENCE
   - Every AppError variant handled? Including AiError and AiTimeoutError?
   - No unhandled exceptions reaching the ViewModel?

5. TESTING
   - AI failure paths tested for all 3 UseCases?
   - Profile update trigger boundary tested (swipe 10, not 9 or 11)?
   - Are tests testing behavior, not implementation details?

6. DESIGN SYSTEM
   - Any hardcoded color, spacing, or text style values?
   - Dark theme only enforced?

7. ANDROID
   - Memory leaks? Lifecycle issues? Recomposition storms?
   - Room queries on IO dispatcher?
   - AI calls on IO dispatcher, not Main?

8. PERFORMANCE
   - Brief generation async and non-blocking?
   - Swipe animation smooth (no heavy work on composition)?

Output format:
## Review Report: {feature_name}
### Approved
### Warnings (should fix)
### Blockers (must fix before merge)
### Suggestions (optional)

Sign off with "REVIEW APPROVED: ready to merge to develop"
or "REVIEW BLOCKED: {n} blockers to resolve before resubmitting"
```

**Task template:**
```
Review Agent — audit the {feature_name} feature.

PM spec: {paste}
Design spec: {paste}
Implementation files: {paste or list}
Test files: {paste or list}

Produce a full review report.
```

---

## 14. Screen Specifications

### Screen 0: Onboarding (`:feature:onboarding`)
**Shown:** Once on first launch only. All subsequent launches skip directly to swipe deck.
**States:** content only (fully local, no loading or error states)
**UI elements:**
- PageTurner wordmark (serif, animated fade + scale in)
- Tagline: "Your next great read is waiting."
- Genre chip grid (multi-select): Literary Fiction · Science Fiction · Fantasy · Thriller · Historical Fiction · Mystery · Non-Fiction · Biography · Horror · Philosophy · Classics · Short Stories
- Length selector (single-select, 3 options): Short (< 200p) · Medium (200–400p) · Long (400p+)
- "Start discovering" button — disabled until ≥1 genre and 1 length selected
- No back button. No skip.

---

### Screen 1: Swipe Deck (`:feature:swipedeck`)
**States:** loading (full-screen shimmer stack), content (card stack), ai-loading (card visible, brief shimmer), error (with retry), offline (amber banner + use cached queue)
**UI elements:**
- TopAppBar: "PageTurner" wordmark (serif) + teal AI learning dot + "AI learning" label
- Card stack: top card full-size, two card edges visible behind creating depth
- Per card: cover art (fills upper 60% of card), title, author, year, page count, up to 3 genre chips, match score bar (amber), AI brief (italic, amber left border) or shimmer placeholder, wildcard badge if applicable (teal)
- Swipe directional hints: "← SKIP" (faded red, left side) and "ADD TO LIST →" (faded teal, right side)
- Three action buttons row: X / bookmark / checkmark
- Swipe count footer: "23 swipes · profile updating in 7"
- Swipe gesture: drag with card rotation transform + red/green tint overlay that intensifies with drag distance

---

### Screen 2: Taste Profile (`:feature:tasteprofile`)
**States:** loading (shimmer), content, insufficient-data (< 10 swipes, show prompt to keep swiping), error
**UI elements:**
- "My taste profile" section header (label style, uppercase, muted)
- AI summary: quoted paragraph, italic, amber left border — Claude's verbatim text
- "What you love" genre chips (amber)
- "You tend to skip" chips (faded red)
- Stats grid: total swiped · total saved · wildcards kept
- "Profile last updated after Nth swipe" footnote (muted)

---

### Screen 3: Reading List (`:feature:readinglist`)
**States:** loading (shimmer grid), content (cover grid), empty (illustrated shelf)
**UI elements:**
- LazyVerticalGrid, 3 columns, cover art fills each cell with rounded corners
- Long-press context: remove from list confirmation bottom sheet
- Empty state: illustrated bare wooden shelf + "Your next great read is waiting."
- Fully offline — Room only, zero network calls

---

### Screen 4: Book Detail (`:feature:bookdetail`)
**Entry:** Shared element transition — cover image morphs from card/grid into full-width hero
**States:** content (always — data already loaded), loading shimmer fallback for cold nav
**UI elements:**
- Full-width hero cover (shared element transition target)
- Dark gradient scrim over bottom of hero, title + author overlaid on it
- Year + page count chips
- Subject chips (all of them, horizontally scrollable row)
- AI brief (full text, italic, amber left border)
- Wildcard reason if applicable (teal left border, "We picked this because...")
- Description (truncated to 3 lines, "Show more" tap expands)
- "View on Open Library" text link
- Back arrow in TopAppBar

---

## 15. Implementation Order for Claude Code

Follow this exact order. Each step is one full CRAFT cycle (all 5 agents if needed, at minimum Dev + Test + Review).

```
Step 1:  Project scaffold
         - Multi-module Gradle structure (all modules from Section 2)
         - libs.versions.toml with all dependencies + Anthropic API via Retrofit
         - Empty modules with package names com.pageturner.*
         - Hilt ApplicationComponent in :app
         - local.properties entry: ANTHROPIC_API_KEY= (gitignored)
         - BuildConfig field wired to read the key
         - Commit: chore(app): initial multi-module project scaffold

Step 2:  :core:domain
         - Domain models: Book, BookDetail, Author, SwipeEvent, TasteProfile,
           ReadingLength (SHORT/MEDIUM/LONG), Genre, SwipeDirection
         - Repository interfaces: BookRepository, SwipeRepository, ProfileRepository
         - AiService interface with 3 method signatures
         - AppError sealed class (include AiError, AiTimeoutError)
         - Result<T> type alias
         - Commit: feat(domain): domain models, interfaces, error types

Step 3:  :core:ui
         - PageTurnerTheme with full darkColorScheme
         - PageTurnerColors, PageTurnerSpacing, PageTurnerType objects
         - All shared components listed in Section 8
         - Commit: feat(ui): design system tokens and shared components

Step 4:  :core:network
         - OpenLibraryApiService (Retrofit interface + DTOs)
         - AnthropicApiService (Retrofit — /v1/messages endpoint only)
         - Moshi adapters for Anthropic request/response shapes
         - safeApiCall {} wrapper
         - NetworkMonitor (Flow<Boolean> connectivity state)
         - Commit: feat(network): Open Library and Anthropic Retrofit clients

Step 5:  :core:ai
         - ClaudeAiService implementing AiService from :core:domain
         - GenerateBriefUseCase (prompt from Section 7, null on failure)
         - SummarizeProfileUseCase (prompt from Section 7, keep old profile on failure)
         - PickWildcardUseCase (prompt from Section 7, random fallback on failure)
         - Hilt module binding AiService -> ClaudeAiService
         - Commit: feat(ai): Claude AI service and three use cases with graceful degradation

Step 6:  :core:data
         - Room database with all entities from Section 9
         - DAOs for each entity
         - Mappers: OL DTO -> Domain, Room Entity -> Domain
         - BookRepositoryImpl, SwipeRepositoryImpl, ProfileRepositoryImpl
         - Cache strategy per Section 9
         - Hilt module bindings
         - Commit: feat(data): Room database, repositories, cache-first strategy

Step 7:  :feature:onboarding
         - OnboardingViewModel (MVI per Section 4)
         - OnboardingScreen composable
         - Writes OnboardingEntity to Room on confirm
         - Commit: feat(onboarding): genre and length preference onboarding screen

Step 8:  :feature:swipedeck
         - SwipeDeckViewModel: queue building, brief generation trigger (async),
           profile update trigger (every 10 swipes), wildcard slot logic (every 7th)
         - SwipeDeckScreen + SwipeCard composable with drag gesture and overlays
         - Commit: feat(swipedeck): swipe deck with gesture, AI brief, and wildcard logic

Step 9:  :feature:tasteprofile
         - TasteProfileViewModel (MVI per Section 4)
         - TasteProfileScreen composable
         - Commit: feat(tasteprofile): taste profile screen with AI summary display

Step 10: :feature:readinglist
         - ReadingListViewModel (MVI per Section 4)
         - ReadingListScreen composable (3-col grid, offline-first)
         - Commit: feat(readinglist): reading list grid with offline support

Step 11: :feature:bookdetail
         - BookDetailViewModel (MVI per Section 4)
         - BookDetailScreen with SharedTransitionLayout shared element
         - Commit: feat(bookdetail): book detail with shared element cover transition

Step 12: :app wiring
         - NavGraph with SharedTransitionLayout wrapping the NavHost
         - Bottom nav: Discover (swipe deck) · My Taste · Reading List
         - Onboarding check on launch (read OnboardingEntity, navigate accordingly)
         - All Hilt module bindings wired
         - Commit: feat(app): navigation graph, bottom nav, launch routing

Step 13: Tests (Test Agent)
         - :core:ai — all 3 UseCase happy path + failure path tests
         - :core:data — repository cache logic, brief caching, swipe recording
         - :feature:swipedeck ViewModel — swipe intents, wildcard slot, profile trigger
         - :feature:tasteprofile ViewModel — profile load, insufficient data state
         - :feature:readinglist ViewModel — load, remove
         - :feature:bookdetail ViewModel — load, navigation
         - :core:domain mappers
         - Commit: test(*): full unit test suite including AI degradation paths

Step 14: Review pass (Review Agent)
         - Full MAARS audit including AI isolation and degradation rules
         - Commit: docs(review): architectural review report and fixes applied
```

---

## 16. AGENTS.md (place at project root)

```markdown
# Agent Definitions — PageTurner

This project uses the CRAFT framework for AI-assisted development.
Each agent has a defined role, context, and output contract.

## CRAFT Pipeline
C -> Clarify (PM Agent)     -> produces: feature spec
R -> Render (Design Agent)  -> produces: component specs
A -> Assemble (Dev Agent)   -> produces: implementation
F -> Fortify (Test Agent)   -> produces: test suite
T -> Tighten (Review Agent) -> produces: review report and sign-off

## Branch naming
agent/pm-{feature}
agent/design-{feature}
agent/dev-{feature}
agent/test-{feature}
agent/review-{feature}

## Agent system prompts
Section 13 of docs/ANDROID_PROJECT_PLAN.md contains full system prompts.
Individual prompt files are stored in docs/agents/:
  pm-agent.md
  design-agent.md
  dev-agent.md
  test-agent.md
  review-agent.md

## How to invoke an agent in Claude Code
1. git checkout -b agent/{role}-{feature} from develop
2. Paste the agent's system prompt as the first message
3. Provide the task template with context filled in
4. Commit the output: git commit -m "agent({role}): {description}"
5. Open PR to develop with MAARS checklist completed
```

---

## 17. README.md

```markdown
# PageTurner

Tinder for books — but the swipes teach it your taste.

Swipe right to save a book, swipe left to pass. Every 10 swipes, AI reads your
history and writes a profile of you as a reader. That profile shapes everything:
the books you see next, how they're described to you, and the occasional intentional
curveball designed to expand your reading.

Built for the Mistplay Android Engineering Challenge.

## Architecture

- **Pattern:** MVI + Clean Architecture, strictly layered
- **Modules:** Multi-module Gradle
- **Stack:** Kotlin, Jetpack Compose, Hilt, Room, Retrofit, Coil, Coroutines + Flow
- **AI:** Claude API (claude-sonnet-4-20250514) — brief generation, taste profiling,
  wildcard selection. Isolated in :core:ai, degrades gracefully on failure.
- **Frameworks:** MAARS (architecture standards) + CRAFT (AI agent workflow)

## Module graph

:app
  -> :feature:onboarding, :feature:swipedeck, :feature:tasteprofile,
     :feature:readinglist, :feature:bookdetail
  -> :core:domain, :core:ui, :core:analytics, :core:logging

:feature:* -> :core:domain, :core:ui (only)
:core:ai   -> :core:domain, :core:network
:core:data -> :core:domain, :core:network

## How the AI works

Three distinct Claude API calls, each isolated in :core:ai behind an interface:

1. Brief generator — rewrites book descriptions as 2-sentence personal hooks,
   tailored to the current reader profile
2. Profile summariser — reads the full swipe history every 10 swipes,
   produces a plain-English taste profile stored in Room
3. Wildcard picker — every 7th card, selects one book from outside the user's
   comfort zone that shares a quality they demonstrably love

All three degrade gracefully. The swipe deck never blocks waiting for AI.

## Running the project

1. Clone the repo
2. Add ANTHROPIC_API_KEY=sk-ant-... to local.properties (never commit this)
3. Open in Android Studio Iguana or later (Compose 1.7+ required)
4. Run on emulator or device (min SDK 26)
5. Open Library is public — no other API keys needed

## Testing

./gradlew test                       -- full unit test suite
./gradlew :core:ai:test              -- AI UseCase tests (includes failure paths)
./gradlew :feature:swipedeck:test    -- swipe deck ViewModel + logic tests

## AI Agent workflow

Built with the CRAFT multi-agent pipeline. See AGENTS.md and docs/agents/ for
agent system prompts. See git log — agent/* branches show each agent's contribution
to each feature, from spec through implementation through review.
```

---

## 18. Kickoff Prompt for Claude Code

**Paste this verbatim as your first message to Claude Code:**

```
You are building an Android app called PageTurner for an engineering interview challenge.

You have two documents in the docs/ folder. Read both completely before writing
any code. Do not begin Step 1 until you have confirmed you have read both.

---

DOCUMENT 1: docs/PAGETURNER_PRODUCT_DEFINITION.md
Read this first. It defines WHAT you are building and WHY.
- App concept: Tinder-style book discovery with AI-driven personalisation
- All 5 screens: Onboarding, Swipe Deck, Taste Profile, Reading List, Book Detail
- The 3 AI jobs: Brief Generator, Profile Summariser, Wildcard Picker
- Explicit non-goals — treat each one as a hard constraint, do not build them
- Definition of done — use this as your acceptance criteria for every feature

If this document conflicts with the plan document, the product definition wins.

---

DOCUMENT 2: docs/ANDROID_PROJECT_PLAN.md
Read this second. It defines HOW to build it.
- MAARS framework: module structure and dependency rules (Section 2)
- MVI contracts for all 5 screens (Section 4)
- Claude API prompt templates for all 3 AI jobs (Section 7)
- Design system tokens (Section 8)
- Room entities and cache strategy (Section 9)
- Error handling and AI degradation rules (Section 10) — read carefully
- CRAFT agent definitions (Section 13)
- Git branch and commit conventions (Section 12)
- Implementation order: follow Section 15 exactly, one step at a time

---

Begin with Step 1 from Section 15: project scaffold.
- Create the multi-module Gradle structure (all modules from Section 2)
- Set up libs.versions.toml with all dependencies
- Wire the Anthropic API key: local.properties -> BuildConfig.ANTHROPIC_API_KEY
- Create empty modules with package names com.pageturner.*
- Set up Hilt ApplicationComponent in :app
- Commit: chore(app): initial multi-module project scaffold

When you finish each step:
1. State which step you completed
2. List every file you created or modified
3. Wait for explicit confirmation before starting the next step
```
