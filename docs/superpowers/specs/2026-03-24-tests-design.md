# PageTurner Test Suite Design

**Date:** 2026-03-24
**Approach:** Behavioral specification — tests describe expected state and behavior, not implementation details.
**Narrative:** Tests are written as the specification that drove each layer's design.
**Style:** Kotlin backtick test names, `@Nested` classes for scenario grouping, JUnit 5 + MockK + Turbine.

---

## Guiding Principle

Every test describes what the system *should do*, not how it does it. No test ever asserts that a specific internal method was called — only that the observable state or output is correct. This keeps tests resilient to refactoring and readable as a living spec.

---

## Layer 1 — Pure Functions

**Modules:** `:core:domain` (`ResultExtensionsTest`), `:core:data` (`CalculateMatchScoreTest`), `:core:ai` (`ExtractJsonTest`)
**Dependencies:** None — no mocking required. `Converters` is a pure helper, no Android runtime needed.

### `ResultExtensionsTest` (`:core:domain`)

Specifies the `Result<T>` sealed type and its extension functions.

| Scenario | Expected state |
|---|---|
| `getOrNull` on Success | returns the wrapped data |
| `getOrNull` on Failure | returns null |
| `errorOrNull` on Failure | returns the AppError |
| `errorOrNull` on Success | returns null |
| `map` on Success | transforms the data, returns new Success |
| `map` on Failure | passes Failure through unchanged |

### `CalculateMatchScoreTest` (`:core:data` — `BookMapper.kt`)

Specifies the local taste-alignment score function. Takes a JSON-serialized subjects string and a `TasteProfile?`.

| Scenario | Expected state |
|---|---|
| null profile | 0.5 (neutral) |
| empty liked and avoided genres | 0.5 (neutral) |
| one liked subject match | score > 0.5 |
| one avoided subject match | score < 0.5 |
| multiple liked matches | score increases proportionally, capped at 0.99 |
| multiple avoided matches | score decreases proportionally, floored at 0.05 |
| liked and avoided both match (cancel) | score near 0.5 |
| partial string containment (e.g. "fiction" in "literary fiction") | counts as a match |

### `ExtractJsonTest` (`:core:ai` — `JsonExtractor.kt`)

Specifies Claude response normalisation. Must be in package `com.pageturner.core.ai.usecase` to access `internal` visibility.

| Scenario | Expected state |
|---|---|
| plain JSON string | returned unchanged (trimmed) |
| JSON wrapped in ` ```json ``` ` fences | fences stripped, JSON returned |
| JSON wrapped in plain ` ``` ``` ` fences | fences stripped, JSON returned |
| empty string | empty string returned |
| fences with extra whitespace inside | JSON trimmed correctly |

---

## Layer 2 — Mappers

**Module:** `:core:data`
**Test source:** `core/data/src/test/`
**Dependencies:** `Converters` (pure, no mocking needed).

### `BookMapperTest`

Specifies DTO → Entity → Domain transformation chain.

| Scenario | Expected state |
|---|---|
| `SearchDocDto` with all fields present | `BookEntity` fields populated correctly |
| `SearchDocDto` with null key | entity key is empty string |
| `SearchDocDto` with null title | entity title is "Unknown Title" |
| `SearchDocDto` with null subjects | entity subjectsJson encodes empty list |
| `BookEntity.toDomain()` | domain `Book` subjects round-trip through Converters |
| `BookEntity.toDomain()` with explicit aiBrief | domain book carries the brief |
| `SavedBookWithDetail.toDomain()` | matchScore fixed at 1.0; isWildcard and wildcardReason from savedBook |
| `BookEntity.toDetailDomain()` | openLibraryUrl is `https://openlibrary.org` + key |

### `SwipeEventMapperTest`

Specifies SwipeEvent ↔ entity round-trip.

| Scenario | Expected state |
|---|---|
| `SwipeEvent.toEntity()` | direction serialized as enum name |
| `SwipeEventEntity.toDomain()` | direction deserialized back to correct enum value |
| genres round-trip | list survives JSON serialize → deserialize |
| all SwipeDirection values (LEFT, RIGHT, BOOKMARK) | each serializes and deserializes correctly |

---

## Layer 3 — ViewModels (Behavioral Heart)

**Modules:** `:feature:onboarding`, `:feature:swipedeck`, `:feature:readinglist`, `:feature:bookdetail`, `:feature:tasteprofile`
**Test source:** `src/test/java/` within each feature module
**Dependencies:** MockK for repository/service interfaces; Turbine for Flow/Channel assertions; `kotlinx-coroutines-test` for `runTest` + `StandardTestDispatcher`.

All ViewModel tests use `@ExtendWith(MockKExtension::class)` and run with `TestScope`.

### `OnboardingViewModelTest`

Specifies the onboarding state machine — the simplest, clearest MVI spec.

**Nested: `initial state`**
- no genres are selected
- no lengths are selected
- canProceed is false

**Nested: `when a genre is toggled on`**
- it is added to selectedGenres
- canProceed remains false if no length is selected

**Nested: `when a genre is toggled off`**
- it is removed from selectedGenres
- canProceed becomes false if no genres remain

**Nested: `when canProceed is true`**
- at least one genre and one length must both be selected
- confirm saves preferences to repository and emits NavigateToSwipeDeck

**Nested: `when canProceed is false`**
- confirm does nothing (no side effect emitted, repository never called)

### `SwipeDeckViewModelTest`

Specifies the swipe loop — the core product behavior.

**Nested: `when onboarding is not complete`**
- state shows a non-retryable error
- isLoading becomes false

**Nested: `when swiping left`**
- currentCardIndex advances by 1
- saveBook is never called

**Nested: `when swiping right`**
- currentCardIndex advances by 1
- saveBook is called with isBookmarked = false
- prefetchBookDetail is called for the swiped book key

**Nested: `when bookmarking`**
- currentCardIndex advances by 1
- saveBook is called with isBookmarked = true
- prefetchBookDetail is called for the swiped book key

**Nested: `when swipe count reaches a multiple of 10`**
- TriggerProfileUpdate side effect is emitted

**Nested: `when fewer than 5 cards remain after a swipe`**
- isReplenishing becomes true while fetching
- new cards are appended to the deck
- isReplenishing returns to false after appending completes

**Nested: `when ExpandCard intent is received`**
- NavigateToDetail side effect is emitted with the correct bookKey

**Nested: `when Retry intent is received`**
- state resets to loading
- initialization reruns

### `ReadingListViewModelTest`

**Nested: `state`**
- liked books from repository appear in likedBooks list
- bookmarked books from repository appear in bookmarkedBooks list
- initial state has isLoading = true

**Nested: `when SelectBook is received`**
- NavigateToDetail side effect emitted with correct bookKey

**Nested: `when RemoveBook is received`**
- repository.removeBook called with correct bookKey

### `BookDetailViewModelTest`

**Nested: `initial state`**
- isLoading is true, no content, isSaved is false

**Nested: `when book detail loads successfully`**
- isLoading becomes false
- book detail fields populated in state

**Nested: `when book detail load fails`**
- isLoading becomes false
- error state shown

**Nested: `when book is already saved`**
- isSaved is true after observeSavedStatus completes

**Nested: `when RemoveFromList is received`**
- repository.removeBook is called with the correct bookKey
- isSaved becomes false

**Nested: `when NavigateBack is received`**
- NavigateBack side effect emitted

**Nested: `when OpenOnOpenLibrary is received with a loaded book`**
- OpenUrl side effect emitted with the book's openLibraryUrl

### `TasteProfileViewModelTest`

**Nested: `when a profile exists`**
- state shows AI summary text
- liked genres and avoided genres shown
- swipe stats populated

**Nested: `when no profile exists yet`**
- state shows empty/placeholder profile

---

## Layer 4 — Repositories

**Module:** `:core:data`
**Test source:** `core/data/src/test/`

Mock dependencies are per-class — each repository only injects what it needs:
- `BookRepositoryImplTest`: MockK mocks for `BookDao`, `SwipeEventDao`, `OpenLibraryApiService`
- `SwipeRepositoryImplTest`: MockK mocks for `SwipeEventDao`, `SavedBookDao`, `AiBriefCacheDao`

### `BookRepositoryImplTest`

**Nested: `getSwipeQueue`**
- emits cached books immediately before any network call
- fetches from network when local cache has fewer than 10 books
- re-emits with fresh data after network fetch

**Nested: `prefetchBookDetail`**
- skips network if description is already cached locally
- updates description in DB when network succeeds

**Nested: `fetchNextPage`**
- advances the page counter per genre on each call
- deduplicates books that appear across multiple genre pages

**Nested: `getSeenBookKeys`**
- returns swipeEventDao keys as a Set

### `SwipeRepositoryImplTest`

**Nested: `recordSwipe`**
- inserts a SwipeEventEntity with the correct direction name
- observable swipe count increments

**Nested: `saveBook`**
- inserts into saved books with isBookmarked = false for right-swipe
- inserts into saved books with isBookmarked = true for bookmark

**Nested: `getLikedBooks`**
- only RIGHT-swiped, non-bookmarked books appear

**Nested: `getBookmarkedBooks`**
- only bookmarked books appear

**Nested: `isBookSaved`**
- returns true when book key exists in saved books
- returns false when book key is absent

---

## Layer 5 — AI Use Cases

**Module:** `:core:ai`
**Test source:** `core/ai/src/test/`
**Dependencies:** MockK mock for `AnthropicApiService`; real `Moshi` instance (no Android runtime needed).

### `GenerateBriefUseCaseTest`

**Nested: `when API returns a valid response`**
- returns trimmed non-blank string

**Nested: `when API returns blank content`**
- returns null

**Nested: `when API call throws an exception`**
- returns null (never propagates the exception)

**Nested: `when API call times out`**
- returns null (never propagates the exception)

### `SummarizeProfileUseCaseTest`

**Nested: `when swipe history is empty`**
- returns null without making any API call

**Nested: `when API returns valid JSON`**
- returns a TasteProfile with correct likedGenres and aiSummary

**Nested: `when API returns JSON wrapped in markdown fences`**
- fences are stripped, profile parsed correctly

**Nested: `when API returns malformed JSON`**
- returns null

**Nested: `when API call fails`**
- returns null (existing profile must not be overwritten by caller)

### `PickWildcardUseCaseTest`

**Nested: `when candidates list is empty`**
- returns null without making any API call

**Nested: `when API picks a valid index within candidates`**
- returns WildcardResult with the matching book and reason

**Nested: `when API picks an out-of-range index`**
- returns WildcardResult with a random candidate as fallback (not null)

**Nested: `when API call fails or times out`**
- returns WildcardResult with a random candidate as fallback (not null)

---

## File Layout

```
core/domain/src/test/java/com/pageturner/core/domain/
  util/ResultExtensionsTest.kt

core/data/src/test/java/com/pageturner/core/data/
  mapper/BookMapperTest.kt
  mapper/SwipeEventMapperTest.kt
  repository/BookRepositoryImplTest.kt
  repository/SwipeRepositoryImplTest.kt

core/ai/src/test/java/com/pageturner/core/ai/
  usecase/ExtractJsonTest.kt
  usecase/GenerateBriefUseCaseTest.kt
  usecase/SummarizeProfileUseCaseTest.kt
  usecase/PickWildcardUseCaseTest.kt

feature/onboarding/src/test/java/com/pageturner/feature/onboarding/
  OnboardingViewModelTest.kt

feature/swipedeck/src/test/java/com/pageturner/feature/swipedeck/
  SwipeDeckViewModelTest.kt

feature/readinglist/src/test/java/com/pageturner/feature/readinglist/
  ReadingListViewModelTest.kt

feature/bookdetail/src/test/java/com/pageturner/feature/bookdetail/
  BookDetailViewModelTest.kt

feature/tasteprofile/src/test/java/com/pageturner/feature/tasteprofile/
  TasteProfileViewModelTest.kt
```

---

## Build Config Requirements

All feature modules and `:core:ai` already have the full test dependency block (JUnit 5, MockK, Turbine, coroutines-test, `:core:testing`). One action needed:

**`:core:data` is missing `turbine`** — `BookRepositoryImpl.getSwipeQueue` returns a `Flow`, so Turbine is required for the repository tests. Add to `core/data/build.gradle.kts`:

```kotlin
testImplementation(libs.turbine)
```
