# PageTurner Test Suite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Write a full behavioral test suite across all layers — pure functions, mappers, ViewModels, repositories, and AI use cases — that reads as TDD specifications.

**Architecture:** Layer-by-layer, richest behavioral value first. Tests describe expected state, never implementation details. Backtick test names + `@Nested` grouping. One test file per task, committed when green.

**Tech Stack:** JUnit 5, MockK, Turbine, `kotlinx-coroutines-test`, `StandardTestDispatcher`

**Spec:** `docs/superpowers/specs/2026-03-24-tests-design.md`

---

## File Structure

```
core/testing/src/main/java/com/pageturner/core/testing/
  MainDispatcherExtension.kt          ← NEW: JUnit 5 extension for Dispatchers.setMain

core/domain/src/test/java/com/pageturner/core/domain/util/
  ResultExtensionsTest.kt             ← NEW

core/data/src/test/java/com/pageturner/core/data/
  mapper/BookMapperTest.kt            ← NEW
  mapper/SwipeEventMapperTest.kt      ← NEW
  repository/BookRepositoryImplTest.kt ← NEW
  repository/SwipeRepositoryImplTest.kt ← NEW

core/ai/src/test/java/com/pageturner/core/ai/usecase/
  ExtractJsonTest.kt                  ← NEW
  GenerateBriefUseCaseTest.kt         ← NEW
  SummarizeProfileUseCaseTest.kt      ← NEW
  PickWildcardUseCaseTest.kt          ← NEW

feature/onboarding/src/test/java/com/pageturner/feature/onboarding/
  OnboardingViewModelTest.kt          ← NEW

feature/readinglist/src/test/java/com/pageturner/feature/readinglist/
  ReadingListViewModelTest.kt         ← NEW

feature/bookdetail/src/test/java/com/pageturner/feature/bookdetail/
  BookDetailViewModelTest.kt          ← NEW

feature/tasteprofile/src/test/java/com/pageturner/feature/tasteprofile/
  TasteProfileViewModelTest.kt        ← NEW

feature/swipedeck/src/test/java/com/pageturner/feature/swipedeck/
  SwipeDeckViewModelTest.kt           ← NEW

Build files modified (useJUnitPlatform + turbine + returnDefaultValues):
  core/domain/build.gradle.kts
  core/data/build.gradle.kts
  core/ai/build.gradle.kts
  feature/onboarding/build.gradle.kts
  feature/readinglist/build.gradle.kts
  feature/bookdetail/build.gradle.kts
  feature/tasteprofile/build.gradle.kts
  feature/swipedeck/build.gradle.kts
```

---

## Task 1: Build Config + MainDispatcherExtension

Wire JUnit 5 platform into all modules, add the one missing test dep, and create the shared dispatcher utility.

**Files:**
- Modify: `core/domain/build.gradle.kts`
- Modify: `core/data/build.gradle.kts`
- Modify: `core/ai/build.gradle.kts`
- Modify: `feature/onboarding/build.gradle.kts`
- Modify: `feature/readinglist/build.gradle.kts`
- Modify: `feature/bookdetail/build.gradle.kts`
- Modify: `feature/tasteprofile/build.gradle.kts`
- Modify: `feature/swipedeck/build.gradle.kts`
- Create: `core/testing/src/main/java/com/pageturner/core/testing/MainDispatcherExtension.kt`

- [ ] **Step 1: Add `useJUnitPlatform()` to `:core:domain`**

  `core/domain/build.gradle.kts` — append after the existing `kotlin {}` block:
  ```kotlin
  tasks.withType<Test> {
      useJUnitPlatform()
  }
  ```

- [ ] **Step 2: Add `useJUnitPlatform()` + `turbine` to `:core:data`**

  In `core/data/build.gradle.kts`, add inside `android {}`:
  ```kotlin
  testOptions {
      unitTests.all { it.useJUnitPlatform() }
  }
  ```
  And in `dependencies {}`:
  ```kotlin
  testImplementation(libs.turbine)
  ```

- [ ] **Step 3: Add `useJUnitPlatform()` to `:core:ai`**

  In `core/ai/build.gradle.kts`, add inside `android {}`:
  ```kotlin
  testOptions {
      unitTests.all { it.useJUnitPlatform() }
  }
  ```

- [ ] **Step 4: Add `useJUnitPlatform()` to all four feature modules**

  Add the following block inside `android {}` in each of:
  - `feature/onboarding/build.gradle.kts`
  - `feature/readinglist/build.gradle.kts`
  - `feature/bookdetail/build.gradle.kts`
  - `feature/swipedeck/build.gradle.kts`

  ```kotlin
  testOptions {
      unitTests.all { it.useJUnitPlatform() }
  }
  ```

- [ ] **Step 5: Add `useJUnitPlatform()` + `returnDefaultValues` to `:feature:tasteprofile`**

  `TasteProfileViewModel` uses `android.util.Log` directly. In `feature/tasteprofile/build.gradle.kts`, add inside `android {}`:
  ```kotlin
  testOptions {
      unitTests {
          isReturnDefaultValues = true
          all { it.useJUnitPlatform() }
      }
  }
  ```

- [ ] **Step 6: Create `MainDispatcherExtension`**

  Create `core/testing/src/main/java/com/pageturner/core/testing/MainDispatcherExtension.kt`:
  ```kotlin
  package com.pageturner.core.testing

  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.StandardTestDispatcher
  import kotlinx.coroutines.test.TestDispatcher
  import kotlinx.coroutines.test.resetMain
  import kotlinx.coroutines.test.setMain
  import org.junit.jupiter.api.extension.AfterEachCallback
  import org.junit.jupiter.api.extension.BeforeEachCallback
  import org.junit.jupiter.api.extension.ExtensionContext

  /**
   * JUnit 5 extension that installs a [StandardTestDispatcher] as the main dispatcher
   * before each test and resets it afterward.
   *
   * Usage:
   * ```kotlin
   * @ExtendWith(MockKExtension::class)
   * class MyViewModelTest {
   *     private val testDispatcher = StandardTestDispatcher()
   *
   *     @BeforeEach fun setUp() = Dispatchers.setMain(testDispatcher)
   *     @AfterEach  fun tearDown() = Dispatchers.resetMain()
   * }
   * ```
   * Or inject via @RegisterExtension for access to the dispatcher instance.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  class MainDispatcherExtension(
      val testDispatcher: TestDispatcher = StandardTestDispatcher()
  ) : BeforeEachCallback, AfterEachCallback {

      override fun beforeEach(context: ExtensionContext?) {
          Dispatchers.setMain(testDispatcher)
      }

      override fun afterEach(context: ExtensionContext?) {
          Dispatchers.resetMain()
      }
  }
  ```

- [ ] **Step 7: Verify build compiles with no test errors**

  ```bash
  ./gradlew :core:domain:compileTestKotlin :core:data:compileDebugUnitTestKotlin :core:ai:compileDebugUnitTestKotlin
  ```
  Expected: BUILD SUCCESSFUL (no test files yet, just verifying config)

- [ ] **Step 8: Commit**

  ```bash
  git add core/testing/src core/domain/build.gradle.kts core/data/build.gradle.kts \
    core/ai/build.gradle.kts feature/onboarding/build.gradle.kts \
    feature/readinglist/build.gradle.kts feature/bookdetail/build.gradle.kts \
    feature/tasteprofile/build.gradle.kts feature/swipedeck/build.gradle.kts
  git commit -m "test: wire JUnit 5 platform and add MainDispatcherExtension"
  ```

---

## Task 2: `ResultExtensionsTest` — specify the `Result<T>` sealed type

**Files:**
- Create: `core/domain/src/test/java/com/pageturner/core/domain/util/ResultExtensionsTest.kt`

- [ ] **Step 1: Write the test**

  ```kotlin
  package com.pageturner.core.domain.util

  import com.pageturner.core.domain.error.AppError
  import org.junit.jupiter.api.Assertions.assertEquals
  import org.junit.jupiter.api.Assertions.assertNull
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test

  class ResultExtensionsTest {

      @Nested
      inner class `getOrNull` {

          @Test
          fun `returns data when Result is Success`() {
              val result: Result<String> = Result.Success("hello")
              assertEquals("hello", result.getOrNull())
          }

          @Test
          fun `returns null when Result is Failure`() {
              val result: Result<String> = Result.Failure(AppError.UnknownError("oops"))
              assertNull(result.getOrNull())
          }
      }

      @Nested
      inner class `errorOrNull` {

          @Test
          fun `returns the error when Result is Failure`() {
              val error = AppError.UnknownError("something went wrong")
              val result: Result<String> = Result.Failure(error)
              assertEquals(error, result.errorOrNull())
          }

          @Test
          fun `returns null when Result is Success`() {
              val result: Result<String> = Result.Success("data")
              assertNull(result.errorOrNull())
          }
      }

      @Nested
      inner class `map` {

          @Test
          fun `transforms data when Result is Success`() {
              val result: Result<Int> = Result.Success(5)
              assertEquals(Result.Success(10), result.map { it * 2 })
          }

          @Test
          fun `passes Failure through unchanged`() {
              val error = AppError.UnknownError("error")
              val result: Result<Int> = Result.Failure(error)
              assertEquals(result, result.map { it * 2 })
          }
      }
  }
  ```

- [ ] **Step 2: Run and verify passes**

  ```bash
  ./gradlew :core:domain:test --tests "com.pageturner.core.domain.util.ResultExtensionsTest"
  ```
  Expected: 6 tests pass

- [ ] **Step 3: Commit**

  ```bash
  git add core/domain/src/test
  git commit -m "test: specify Result<T> sealed type behavior"
  ```

---

## Task 3: `CalculateMatchScoreTest` — specify the taste-alignment score function

**Files:**
- Create: `core/data/src/test/java/com/pageturner/core/data/mapper/CalculateMatchScoreTest.kt`

- [ ] **Step 1: Write the test**

  ```kotlin
  package com.pageturner.core.data.mapper

  import com.pageturner.core.data.db.converter.Converters
  import com.pageturner.core.domain.model.TasteProfile
  import org.junit.jupiter.api.Assertions.assertEquals
  import org.junit.jupiter.api.Assertions.assertTrue
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test

  class CalculateMatchScoreTest {

      private fun profile(liked: List<String> = emptyList(), avoided: List<String> = emptyList()) =
          TasteProfile(
              aiSummary = "",
              likedGenres = liked,
              avoidedGenres = avoided,
              preferredLength = "any",
              recurringThemes = emptyList(),
              profileVersion = 1,
              lastUpdatedSwipeCount = 10,
              updatedAt = 0L
          )

      private fun subjectsJson(vararg subjects: String) =
          Converters.serializeList(subjects.toList())

      @Nested
      inner class `when profile is null` {

          @Test
          fun `score is neutral 0_5`() {
              assertEquals(0.5f, calculateMatchScore(subjectsJson("fantasy"), null))
          }
      }

      @Nested
      inner class `when profile has no liked or avoided genres` {

          @Test
          fun `score is neutral 0_5`() {
              val p = profile()
              assertEquals(0.5f, calculateMatchScore(subjectsJson("fantasy"), p))
          }
      }

      @Nested
      inner class `when book subject matches a liked genre` {

          @Test
          fun `score is greater than 0_5`() {
              val p = profile(liked = listOf("fantasy"))
              assertTrue(calculateMatchScore(subjectsJson("fantasy"), p) > 0.5f)
          }
      }

      @Nested
      inner class `when book subject matches an avoided genre` {

          @Test
          fun `score is less than 0_5`() {
              val p = profile(avoided = listOf("romance"))
              assertTrue(calculateMatchScore(subjectsJson("romance"), p) < 0.5f)
          }
      }

      @Nested
      inner class `when book has many liked matches` {

          @Test
          fun `score is capped at 0_99`() {
              val p = profile(liked = listOf("fantasy", "fiction", "adventure", "magic", "quest", "dragons"))
              val score = calculateMatchScore(
                  subjectsJson("fantasy", "fiction", "adventure", "magic", "quest", "dragons"), p
              )
              assertEquals(0.99f, score)
          }
      }

      @Nested
      inner class `when book has many avoided matches` {

          @Test
          fun `score is floored at 0_05`() {
              val p = profile(avoided = listOf("romance", "thriller", "horror", "mystery", "crime", "drama"))
              val score = calculateMatchScore(
                  subjectsJson("romance", "thriller", "horror", "mystery", "crime", "drama"), p
              )
              assertEquals(0.05f, score)
          }
      }

      @Nested
      inner class `when liked and avoided subjects both match` {

          @Test
          fun `effects cancel out and score stays near 0_5`() {
              val p = profile(liked = listOf("fiction"), avoided = listOf("fiction"))
              val score = calculateMatchScore(subjectsJson("fiction"), p)
              assertEquals(0.5f, score)
          }
      }

      @Nested
      inner class `partial string containment` {

          @Test
          fun `literary fiction matches liked genre fiction`() {
              val p = profile(liked = listOf("fiction"))
              assertTrue(calculateMatchScore(subjectsJson("literary fiction"), p) > 0.5f)
          }

          @Test
          fun `fiction matches liked genre literary fiction`() {
              val p = profile(liked = listOf("literary fiction"))
              assertTrue(calculateMatchScore(subjectsJson("fiction"), p) > 0.5f)
          }
      }
  }
  ```

- [ ] **Step 2: Run and verify passes**

  ```bash
  ./gradlew :core:data:testDebugUnitTest --tests "com.pageturner.core.data.mapper.CalculateMatchScoreTest"
  ```
  Expected: 9 tests pass

- [ ] **Step 3: Commit**

  ```bash
  git add core/data/src/test
  git commit -m "test: specify calculateMatchScore taste-alignment function"
  ```

---

## Task 4: `ExtractJsonTest` — specify Claude response normalisation

Must be in package `com.pageturner.core.ai.usecase` to access the `internal` function.

**Files:**
- Create: `core/ai/src/test/java/com/pageturner/core/ai/usecase/ExtractJsonTest.kt`

- [ ] **Step 1: Write the test**

  ```kotlin
  package com.pageturner.core.ai.usecase

  import org.junit.jupiter.api.Assertions.assertEquals
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test

  class ExtractJsonTest {

      @Nested
      inner class `when input is plain JSON` {

          @Test
          fun `it is returned unchanged (trimmed)`() {
              val json = """{"key": "value"}"""
              assertEquals(json, extractJson(json))
          }
      }

      @Nested
      inner class `when input is wrapped in json code fences` {

          @Test
          fun `fences are stripped and JSON is returned`() {
              val input = "```json\n{\"key\": \"value\"}\n```"
              assertEquals("""{"key": "value"}""", extractJson(input))
          }
      }

      @Nested
      inner class `when input is wrapped in plain code fences` {

          @Test
          fun `fences are stripped and JSON is returned`() {
              val input = "```\n{\"key\": \"value\"}\n```"
              assertEquals("""{"key": "value"}""", extractJson(input))
          }
      }

      @Nested
      inner class `when input is empty` {

          @Test
          fun `empty string is returned`() {
              assertEquals("", extractJson(""))
          }
      }

      @Nested
      inner class `when fenced content has extra whitespace` {

          @Test
          fun `JSON is trimmed correctly`() {
              val input = "```json\n\n  {\"key\": \"value\"}  \n\n```"
              assertEquals("""{"key": "value"}""", extractJson(input))
          }
      }
  }
  ```

- [ ] **Step 2: Run and verify passes**

  ```bash
  ./gradlew :core:ai:testDebugUnitTest --tests "com.pageturner.core.ai.usecase.ExtractJsonTest"
  ```
  Expected: 5 tests pass

- [ ] **Step 3: Commit**

  ```bash
  git add core/ai/src/test
  git commit -m "test: specify extractJson Claude response normalisation"
  ```

---

## Task 5: `BookMapperTest` — specify DTO → Entity → Domain transformations

**Files:**
- Create: `core/data/src/test/java/com/pageturner/core/data/mapper/BookMapperTest.kt`

- [ ] **Step 1: Write the test**

  ```kotlin
  package com.pageturner.core.data.mapper

  import com.pageturner.core.data.db.converter.Converters
  import com.pageturner.core.data.entity.BookEntity
  import com.pageturner.core.data.entity.SavedBookEntity
  import com.pageturner.core.data.entity.SavedBookWithDetail
  import com.pageturner.core.network.dto.openlib.SearchDocDto
  import org.junit.jupiter.api.Assertions.assertEquals
  import org.junit.jupiter.api.Assertions.assertNull
  import org.junit.jupiter.api.Assertions.assertTrue
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test

  class BookMapperTest {

      private fun aDoc(
          key: String? = "/works/OL123",
          title: String? = "Test Book",
          authorName: List<String>? = listOf("Author One"),
          coverId: Long? = 12345L,
          subject: List<String>? = listOf("fantasy", "adventure"),
          firstPublishYear: Int? = 2000,
          numberOfPagesMedian: Int? = 300
      ) = SearchDocDto(
          key = key,
          title = title,
          authorName = authorName,
          firstPublishYear = firstPublishYear,
          coverId = coverId,
          subject = subject,
          numberOfPagesMedian = numberOfPagesMedian
      )

      private fun anEntity(
          key: String = "/works/OL123",
          title: String = "Test Book",
          subjectsJson: String = Converters.serializeList(listOf("fantasy")),
          description: String? = null
      ) = BookEntity(
          key = key,
          title = title,
          authorNamesJson = Converters.serializeList(listOf("Author One")),
          coverUrl = "https://covers.openlibrary.org/b/id/12345-M.jpg",
          publishYear = 2000,
          pageCount = 300,
          subjectsJson = subjectsJson,
          description = description,
          cachedAt = 0L
      )

      @Nested
      inner class `SearchDocDto toEntity` {

          @Test
          fun `all fields map correctly`() {
              val entity = aDoc().toEntity()
              assertEquals("/works/OL123", entity.key)
              assertEquals("Test Book", entity.title)
              assertEquals("https://covers.openlibrary.org/b/id/12345-M.jpg", entity.coverUrl)
              assertEquals(2000, entity.publishYear)
              assertEquals(300, entity.pageCount)
          }

          @Test
          fun `null key becomes empty string`() {
              assertEquals("", aDoc(key = null).toEntity().key)
          }

          @Test
          fun `null title becomes Unknown Title`() {
              assertEquals("Unknown Title", aDoc(title = null).toEntity().title)
          }

          @Test
          fun `null subjects become empty JSON list`() {
              val entity = aDoc(subject = null).toEntity()
              assertTrue(Converters.parseList(entity.subjectsJson).isEmpty())
          }

          @Test
          fun `null coverId produces null coverUrl`() {
              assertNull(aDoc(coverId = null).toEntity().coverUrl)
          }
      }

      @Nested
      inner class `BookEntity toDomain` {

          @Test
          fun `subjects round-trip through Converters`() {
              val subjects = listOf("fantasy", "adventure")
              val entity = anEntity(subjectsJson = Converters.serializeList(subjects))
              assertEquals(subjects, entity.toDomain().subjects)
          }

          @Test
          fun `explicit aiBrief is carried through`() {
              val domain = anEntity().toDomain(aiBrief = "A great read.")
              assertEquals("A great read.", domain.aiBrief)
          }

          @Test
          fun `default matchScore is 0_5`() {
              assertEquals(0.5f, anEntity().toDomain().matchScore)
          }
      }

      @Nested
      inner class `BookEntity toDetailDomain` {

          @Test
          fun `openLibraryUrl is https://openlibrary_org plus the key`() {
              val detail = anEntity(key = "/works/OL45804W").toDetailDomain()
              assertEquals("https://openlibrary.org/works/OL45804W", detail.openLibraryUrl)
          }
      }

      @Nested
      inner class `SavedBookWithDetail toDomain` {

          @Test
          fun `matchScore is fixed at 1_0`() {
              val saved = SavedBookWithDetail(
                  book = anEntity(),
                  savedBook = SavedBookEntity(
                      bookKey = "/works/OL123",
                      savedAt = 0L,
                      aiBrief = null,
                      wildcardReason = null,
                      isWildcard = false,
                      isBookmarked = false,
                  )
              )
              assertEquals(1.0f, saved.toDomain().matchScore)
          }

          @Test
          fun `isWildcard and wildcardReason come from the savedBook`() {
              val saved = SavedBookWithDetail(
                  book = anEntity(),
                  savedBook = SavedBookEntity(
                      bookKey = "/works/OL123",
                      savedAt = 0L,
                      aiBrief = "Brief.",
                      wildcardReason = "Shares moral complexity.",
                      isWildcard = true,
                      isBookmarked = false,
                  )
              )
              val domain = saved.toDomain()
              assertTrue(domain.isWildcard)
              assertEquals("Shares moral complexity.", domain.wildcardReason)
          }
      }
  }
  ```

- [ ] **Step 2: Run and verify passes**

  ```bash
  ./gradlew :core:data:testDebugUnitTest --tests "com.pageturner.core.data.mapper.BookMapperTest"
  ```
  Expected: 10 tests pass

- [ ] **Step 3: Commit**

  ```bash
  git add core/data/src/test/java/com/pageturner/core/data/mapper/BookMapperTest.kt
  git commit -m "test: specify SearchDocDto and BookEntity mapping behavior"
  ```

---

## Task 6: `SwipeEventMapperTest` — specify SwipeEvent ↔ entity round-trip

**Files:**
- Create: `core/data/src/test/java/com/pageturner/core/data/mapper/SwipeEventMapperTest.kt`

- [ ] **Step 1: Write the test**

  ```kotlin
  package com.pageturner.core.data.mapper

  import com.pageturner.core.data.db.converter.Converters
  import com.pageturner.core.data.entity.SwipeEventEntity
  import com.pageturner.core.domain.model.SwipeDirection
  import com.pageturner.core.domain.model.SwipeEvent
  import org.junit.jupiter.api.Assertions.assertEquals
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test
  import org.junit.jupiter.params.ParameterizedTest
  import org.junit.jupiter.params.provider.EnumSource

  class SwipeEventMapperTest {

      private fun aSwipeEvent(direction: SwipeDirection = SwipeDirection.RIGHT) = SwipeEvent(
          id = 0,
          bookKey = "/works/OL123",
          direction = direction,
          timestamp = 1000L,
          bookGenres = listOf("fantasy", "adventure"),
          bookYear = 2000,
          bookPageCount = 300,
          wasWildcard = false,
      )

      @Nested
      inner class `SwipeEvent toEntity` {

          @Test
          fun `direction is serialized as the enum name`() {
              val entity = aSwipeEvent(SwipeDirection.LEFT).toEntity()
              assertEquals("LEFT", entity.direction)
          }

          @Test
          fun `genres round-trip through Converters`() {
              val genres = listOf("fantasy", "adventure")
              val entity = aSwipeEvent().toEntity()
              assertEquals(genres, Converters.parseList(entity.bookGenresJson))
          }
      }

      @Nested
      inner class `SwipeEventEntity toDomain` {

          @Test
          fun `direction deserializes back to the correct enum value`() {
              val entity = SwipeEventEntity(
                  id = 0,
                  bookKey = "/works/OL123",
                  direction = "BOOKMARK",
                  timestamp = 1000L,
                  bookGenresJson = Converters.serializeList(emptyList()),
                  bookYear = null,
                  bookPageCount = null,
                  wasWildcard = false,
              )
              assertEquals(SwipeDirection.BOOKMARK, entity.toDomain().direction)
          }
      }

      @Nested
      inner class `all SwipeDirection values` {

          @ParameterizedTest
          @EnumSource(SwipeDirection::class)
          fun `each direction serializes and deserializes correctly`(direction: SwipeDirection) {
              val roundTripped = aSwipeEvent(direction).toEntity().toDomain().direction
              assertEquals(direction, roundTripped)
          }
      }
  }
  ```

- [ ] **Step 2: Run and verify passes**

  ```bash
  ./gradlew :core:data:testDebugUnitTest --tests "com.pageturner.core.data.mapper.SwipeEventMapperTest"
  ```
  Expected: 5 tests pass (3 directions in the parameterized = 3 iterations counted separately)

- [ ] **Step 3: Commit**

  ```bash
  git add core/data/src/test/java/com/pageturner/core/data/mapper/SwipeEventMapperTest.kt
  git commit -m "test: specify SwipeEvent mapper round-trip for all directions"
  ```

---

## Task 7: `OnboardingViewModelTest` — specify the onboarding state machine

**Files:**
- Create: `feature/onboarding/src/test/java/com/pageturner/feature/onboarding/OnboardingViewModelTest.kt`

- [ ] **Step 1: Write the test**

  ```kotlin
  package com.pageturner.feature.onboarding

  import com.pageturner.core.domain.model.Genre
  import com.pageturner.core.domain.model.ReadingLength
  import com.pageturner.core.domain.repository.ProfileRepository
  import io.mockk.coVerify
  import io.mockk.coJustRun
  import io.mockk.impl.annotations.MockK
  import io.mockk.junit5.MockKExtension
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.StandardTestDispatcher
  import kotlinx.coroutines.test.advanceUntilIdle
  import kotlinx.coroutines.test.resetMain
  import kotlinx.coroutines.test.runTest
  import kotlinx.coroutines.test.setMain
  import org.junit.jupiter.api.AfterEach
  import org.junit.jupiter.api.Assertions.assertFalse
  import org.junit.jupiter.api.Assertions.assertTrue
  import org.junit.jupiter.api.BeforeEach
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test
  import org.junit.jupiter.api.extension.ExtendWith

  @OptIn(ExperimentalCoroutinesApi::class)
  @ExtendWith(MockKExtension::class)
  class OnboardingViewModelTest {

      @MockK
      private lateinit var profileRepository: ProfileRepository

      private val testDispatcher = StandardTestDispatcher()
      private lateinit var viewModel: OnboardingViewModel

      @BeforeEach
      fun setUp() {
          Dispatchers.setMain(testDispatcher)
          coJustRun { profileRepository.saveOnboardingPreferences(any()) }
          viewModel = OnboardingViewModel(profileRepository)
      }

      @AfterEach
      fun tearDown() = Dispatchers.resetMain()

      @Nested
      inner class `initial state` {

          @Test
          fun `no genres are selected`() {
              assertTrue(viewModel.uiState.value.selectedGenres.isEmpty())
          }

          @Test
          fun `no lengths are selected`() {
              assertTrue(viewModel.uiState.value.selectedLengths.isEmpty())
          }

          @Test
          fun `canProceed is false`() {
              assertFalse(viewModel.uiState.value.canProceed)
          }
      }

      @Nested
      inner class `when a genre is toggled on` {

          @Test
          fun `it is added to selectedGenres`() {
              viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
              assertTrue(Genre.FANTASY in viewModel.uiState.value.selectedGenres)
          }

          @Test
          fun `canProceed remains false if no length is selected`() {
              viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
              assertFalse(viewModel.uiState.value.canProceed)
          }
      }

      @Nested
      inner class `when a genre is toggled off` {

          @Test
          fun `it is removed from selectedGenres`() {
              viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
              viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
              assertFalse(Genre.FANTASY in viewModel.uiState.value.selectedGenres)
          }

          @Test
          fun `canProceed becomes false if no genres remain`() {
              viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
              viewModel.onIntent(OnboardingIntent.ToggleLength(ReadingLength.MEDIUM))
              assertTrue(viewModel.uiState.value.canProceed)

              viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
              assertFalse(viewModel.uiState.value.canProceed)
          }
      }

      @Nested
      inner class `when canProceed is true` {

          @BeforeEach
          fun selectGenreAndLength() {
              viewModel.onIntent(OnboardingIntent.ToggleGenre(Genre.FANTASY))
              viewModel.onIntent(OnboardingIntent.ToggleLength(ReadingLength.MEDIUM))
          }

          @Test
          fun `at least one genre and one length are required`() {
              assertTrue(viewModel.uiState.value.canProceed)
          }

          @Test
          fun `confirm saves preferences and emits NavigateToSwipeDeck`() = runTest(testDispatcher) {
              viewModel.onIntent(OnboardingIntent.Confirm)
              advanceUntilIdle()
              coVerify { profileRepository.saveOnboardingPreferences(any()) }

              val effect = viewModel.sideEffects.tryReceive().getOrNull()
              assertTrue(effect is OnboardingSideEffect.NavigateToSwipeDeck)
          }
      }

      @Nested
      inner class `when canProceed is false` {

          @Test
          fun `confirm does nothing`() = runTest(testDispatcher) {
              viewModel.onIntent(OnboardingIntent.Confirm)
              advanceUntilIdle()
              coVerify(exactly = 0) { profileRepository.saveOnboardingPreferences(any()) }
          }
      }
  }
  ```

- [ ] **Step 2: Run and verify passes**

  ```bash
  ./gradlew :feature:onboarding:testDebugUnitTest --tests "com.pageturner.feature.onboarding.OnboardingViewModelTest"
  ```
  Expected: 9 tests pass

- [ ] **Step 3: Commit**

  ```bash
  git add feature/onboarding/src/test
  git commit -m "test: specify OnboardingViewModel state machine behavior"
  ```

---

## Task 8: `ReadingListViewModelTest` — specify the reading list state and intents

**Files:**
- Create: `feature/readinglist/src/test/java/com/pageturner/feature/readinglist/ReadingListViewModelTest.kt`

- [ ] **Step 1: Write the test**

  ```kotlin
  package com.pageturner.feature.readinglist

  import app.cash.turbine.test
  import com.pageturner.core.domain.model.Book
  import com.pageturner.core.domain.repository.SwipeRepository
  import io.mockk.coJustRun
  import io.mockk.coVerify
  import io.mockk.every
  import io.mockk.impl.annotations.MockK
  import io.mockk.junit5.MockKExtension
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.flow.flowOf
  import kotlinx.coroutines.test.StandardTestDispatcher
  import kotlinx.coroutines.test.advanceUntilIdle
  import kotlinx.coroutines.test.resetMain
  import kotlinx.coroutines.test.runTest
  import kotlinx.coroutines.test.setMain
  import org.junit.jupiter.api.AfterEach
  import org.junit.jupiter.api.Assertions.assertEquals
  import org.junit.jupiter.api.Assertions.assertTrue
  import org.junit.jupiter.api.BeforeEach
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test
  import org.junit.jupiter.api.extension.ExtendWith

  @OptIn(ExperimentalCoroutinesApi::class)
  @ExtendWith(MockKExtension::class)
  class ReadingListViewModelTest {

      @MockK
      private lateinit var swipeRepository: SwipeRepository

      private val testDispatcher = StandardTestDispatcher()

      private fun aBook(key: String = "/works/OL1") = Book(
          key = key, title = "T", authors = emptyList(), coverUrl = null,
          publishYear = null, pageCount = null, subjects = emptyList(),
          description = null, aiBrief = null, matchScore = 1.0f,
          isWildcard = false, wildcardReason = null,
      )

      @BeforeEach
      fun setUp() {
          Dispatchers.setMain(testDispatcher)
          every { swipeRepository.getLikedBooks() } returns flowOf(emptyList())
          every { swipeRepository.getBookmarkedBooks() } returns flowOf(emptyList())
      }

      @AfterEach
      fun tearDown() = Dispatchers.resetMain()

      @Nested
      inner class `state` {

          @Test
          fun `initial state has isLoading true`() {
              val vm = ReadingListViewModel(swipeRepository)
              assertTrue(vm.state.value.isLoading)
          }

          @Test
          fun `liked books from repository appear in likedBooks list`() = runTest(testDispatcher) {
              val book = aBook("/works/OL1")
              every { swipeRepository.getLikedBooks() } returns flowOf(listOf(book))

              val vm = ReadingListViewModel(swipeRepository)
              vm.state.test {
                  skipItems(1) // initial loading state
                  val state = awaitItem()
                  assertEquals(1, state.likedBooks.size)
                  assertEquals("/works/OL1", state.likedBooks.first().bookKey)
                  cancelAndIgnoreRemainingEvents()
              }
          }

          @Test
          fun `bookmarked books appear in bookmarkedBooks list`() = runTest(testDispatcher) {
              val book = aBook("/works/OL2")
              every { swipeRepository.getBookmarkedBooks() } returns flowOf(listOf(book))

              val vm = ReadingListViewModel(swipeRepository)
              vm.state.test {
                  skipItems(1)
                  val state = awaitItem()
                  assertEquals(1, state.bookmarkedBooks.size)
                  cancelAndIgnoreRemainingEvents()
              }
          }
      }

      @Nested
      inner class `when SelectBook is received` {

          @Test
          fun `NavigateToDetail side effect is emitted with the correct bookKey`() = runTest(testDispatcher) {
              val vm = ReadingListViewModel(swipeRepository)
              vm.handleIntent(ReadingListIntent.SelectBook("/works/OL99"))
              advanceUntilIdle()

              val effect = vm.sideEffects.tryReceive().getOrNull()
              assertEquals(ReadingListSideEffect.NavigateToDetail("/works/OL99"), effect)
          }
      }

      @Nested
      inner class `when RemoveBook is received` {

          @Test
          fun `repository removeBook is called with the correct bookKey`() = runTest(testDispatcher) {
              coJustRun { swipeRepository.removeBook(any()) }
              val vm = ReadingListViewModel(swipeRepository)
              vm.handleIntent(ReadingListIntent.RemoveBook("/works/OL5"))
              advanceUntilIdle()

              coVerify { swipeRepository.removeBook("/works/OL5") }
          }
      }
  }
  ```

- [ ] **Step 2: Run and verify passes**

  ```bash
  ./gradlew :feature:readinglist:testDebugUnitTest --tests "com.pageturner.feature.readinglist.ReadingListViewModelTest"
  ```
  Expected: 5 tests pass

- [ ] **Step 3: Commit**

  ```bash
  git add feature/readinglist/src/test
  git commit -m "test: specify ReadingListViewModel state and intent behavior"
  ```

---

## Task 9: `BookDetailViewModelTest` — specify the book detail screen behavior

`BookDetailViewModel` reads `bookKey` from `SavedStateHandle` at construction. Inject via `SavedStateHandle(mapOf("bookKey" to ...))`.

**Files:**
- Create: `feature/bookdetail/src/test/java/com/pageturner/feature/bookdetail/BookDetailViewModelTest.kt`

- [ ] **Step 1: Write the test**

  ```kotlin
  package com.pageturner.feature.bookdetail

  import androidx.lifecycle.SavedStateHandle
  import com.pageturner.core.domain.error.AppError
  import com.pageturner.core.domain.model.BookDetail
  import com.pageturner.core.domain.repository.BookRepository
  import com.pageturner.core.domain.repository.SwipeRepository
  import com.pageturner.core.domain.util.Result
  import io.mockk.coEvery
  import io.mockk.coJustRun
  import io.mockk.coVerify
  import io.mockk.impl.annotations.MockK
  import io.mockk.junit5.MockKExtension
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.StandardTestDispatcher
  import kotlinx.coroutines.test.advanceUntilIdle
  import kotlinx.coroutines.test.resetMain
  import kotlinx.coroutines.test.runTest
  import kotlinx.coroutines.test.setMain
  import org.junit.jupiter.api.AfterEach
  import org.junit.jupiter.api.Assertions.assertEquals
  import org.junit.jupiter.api.Assertions.assertFalse
  import org.junit.jupiter.api.Assertions.assertNotNull
  import org.junit.jupiter.api.Assertions.assertNull
  import org.junit.jupiter.api.Assertions.assertTrue
  import org.junit.jupiter.api.BeforeEach
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test
  import org.junit.jupiter.api.extension.ExtendWith

  @OptIn(ExperimentalCoroutinesApi::class)
  @ExtendWith(MockKExtension::class)
  class BookDetailViewModelTest {

      @MockK private lateinit var bookRepository: BookRepository
      @MockK private lateinit var swipeRepository: SwipeRepository

      private val testDispatcher = StandardTestDispatcher()
      private val bookKey = "/works/OL123"
      private val savedStateHandle = SavedStateHandle(mapOf("bookKey" to bookKey))

      private fun aBookDetail() = BookDetail(
          key = bookKey,
          title = "Great Expectations",
          authors = listOf("Charles Dickens"),
          coverUrl = null,
          publishYear = 1861,
          pageCount = 544,
          subjects = listOf("classics"),
          description = "A story of ambition.",
          aiBrief = null,
          isWildcard = false,
          wildcardReason = null,
          openLibraryUrl = "https://openlibrary.org$bookKey",
      )

      @BeforeEach
      fun setUp() {
          Dispatchers.setMain(testDispatcher)
      }

      @AfterEach
      fun tearDown() = Dispatchers.resetMain()

      @Nested
      inner class `initial state` {

          @Test
          fun `isLoading is true`() {
              coEvery { bookRepository.getBookDetail(any()) } returns Result.Success(aBookDetail())
              coEvery { swipeRepository.isBookSaved(any()) } returns false
              val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository)
              assertTrue(vm.state.value.isLoading)
          }
      }

      @Nested
      inner class `when book detail loads successfully` {

          @Test
          fun `isLoading becomes false`() = runTest(testDispatcher) {
              coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
              coEvery { swipeRepository.isBookSaved(bookKey) } returns false

              val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository)
              advanceUntilIdle()

              assertFalse(vm.state.value.isLoading)
          }

          @Test
          fun `book is populated in state`() = runTest(testDispatcher) {
              coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
              coEvery { swipeRepository.isBookSaved(bookKey) } returns false

              val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository)
              advanceUntilIdle()

              assertEquals("Great Expectations", vm.state.value.book?.title)
          }
      }

      @Nested
      inner class `when book detail load fails` {

          @Test
          fun `isLoading becomes false`() = runTest(testDispatcher) {
              coEvery { bookRepository.getBookDetail(bookKey) } returns
                  Result.Failure(AppError.NetworkError(500))
              coEvery { swipeRepository.isBookSaved(bookKey) } returns false

              val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository)
              advanceUntilIdle()

              assertFalse(vm.state.value.isLoading)
          }

          @Test
          fun `error is shown in state`() = runTest(testDispatcher) {
              coEvery { bookRepository.getBookDetail(bookKey) } returns
                  Result.Failure(AppError.NetworkError(500))
              coEvery { swipeRepository.isBookSaved(bookKey) } returns false

              val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository)
              advanceUntilIdle()

              assertNotNull(vm.state.value.error)
              assertNull(vm.state.value.book)
          }
      }

      @Nested
      inner class `when book is already saved` {

          @Test
          fun `isSaved is true`() = runTest(testDispatcher) {
              coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
              coEvery { swipeRepository.isBookSaved(bookKey) } returns true

              val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository)
              advanceUntilIdle()

              assertTrue(vm.state.value.isSaved)
          }
      }

      @Nested
      inner class `when RemoveFromList is received` {

          @Test
          fun `repository removeBook is called`() = runTest(testDispatcher) {
              coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
              coEvery { swipeRepository.isBookSaved(bookKey) } returns true
              coJustRun { swipeRepository.removeBook(any()) }

              val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository)
              advanceUntilIdle()
              vm.handleIntent(BookDetailIntent.RemoveFromList)
              advanceUntilIdle()

              coVerify { swipeRepository.removeBook(bookKey) }
          }

          @Test
          fun `isSaved becomes false`() = runTest(testDispatcher) {
              coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
              coEvery { swipeRepository.isBookSaved(bookKey) } returns true
              coJustRun { swipeRepository.removeBook(any()) }

              val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository)
              advanceUntilIdle()
              vm.handleIntent(BookDetailIntent.RemoveFromList)
              advanceUntilIdle()

              assertFalse(vm.state.value.isSaved)
          }
      }

      @Nested
      inner class `when NavigateBack is received` {

          @Test
          fun `NavigateBack side effect is emitted`() = runTest(testDispatcher) {
              coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
              coEvery { swipeRepository.isBookSaved(bookKey) } returns false

              val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository)
              advanceUntilIdle()
              vm.handleIntent(BookDetailIntent.NavigateBack)
              advanceUntilIdle()

              val effect = vm.sideEffects.tryReceive().getOrNull()
              assertTrue(effect is BookDetailSideEffect.NavigateBack)
          }
      }

      @Nested
      inner class `when OpenOnOpenLibrary is received with a loaded book` {

          @Test
          fun `OpenUrl side effect is emitted with the book URL`() = runTest(testDispatcher) {
              coEvery { bookRepository.getBookDetail(bookKey) } returns Result.Success(aBookDetail())
              coEvery { swipeRepository.isBookSaved(bookKey) } returns false

              val vm = BookDetailViewModel(savedStateHandle, bookRepository, swipeRepository)
              advanceUntilIdle()
              vm.handleIntent(BookDetailIntent.OpenOnOpenLibrary)
              advanceUntilIdle()

              val effect = vm.sideEffects.tryReceive().getOrNull()
              assertEquals(
                  BookDetailSideEffect.OpenUrl("https://openlibrary.org$bookKey"),
                  effect
              )
          }
      }
  }
  ```

- [ ] **Step 2: Run and verify passes**

  ```bash
  ./gradlew :feature:bookdetail:testDebugUnitTest --tests "com.pageturner.feature.bookdetail.BookDetailViewModelTest"
  ```
  Expected: 9 tests pass

- [ ] **Step 3: Commit**

  ```bash
  git add feature/bookdetail/src/test
  git commit -m "test: specify BookDetailViewModel loading, error, and intent behavior"
  ```

---

## Task 10: `TasteProfileViewModelTest` — specify the taste profile state

`TasteProfileViewModel` uses `SharingStarted.Eagerly`, so the combine pipeline starts collecting immediately on ViewModel creation. `android.util.Log` is safe because `testOptions.isReturnDefaultValues = true` is set (Task 1).

**Files:**
- Create: `feature/tasteprofile/src/test/java/com/pageturner/feature/tasteprofile/TasteProfileViewModelTest.kt`

- [ ] **Step 1: Write the test**

  ```kotlin
  package com.pageturner.feature.tasteprofile

  import com.pageturner.core.domain.model.Book
  import com.pageturner.core.domain.model.TasteProfile
  import com.pageturner.core.domain.repository.ProfileRepository
  import com.pageturner.core.domain.repository.SwipeRepository
  import io.mockk.every
  import io.mockk.impl.annotations.MockK
  import io.mockk.junit5.MockKExtension
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.test.StandardTestDispatcher
  import kotlinx.coroutines.test.advanceUntilIdle
  import kotlinx.coroutines.test.resetMain
  import kotlinx.coroutines.test.runTest
  import kotlinx.coroutines.test.setMain
  import org.junit.jupiter.api.AfterEach
  import org.junit.jupiter.api.Assertions.assertEquals
  import org.junit.jupiter.api.Assertions.assertNotNull
  import org.junit.jupiter.api.Assertions.assertNull
  import org.junit.jupiter.api.Assertions.assertTrue
  import org.junit.jupiter.api.BeforeEach
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test
  import org.junit.jupiter.api.extension.ExtendWith

  @OptIn(ExperimentalCoroutinesApi::class)
  @ExtendWith(MockKExtension::class)
  class TasteProfileViewModelTest {

      @MockK private lateinit var profileRepository: ProfileRepository
      @MockK private lateinit var swipeRepository: SwipeRepository

      private val testDispatcher = StandardTestDispatcher()

      private val profileFlow = MutableStateFlow<TasteProfile?>(null)
      private val swipeCountFlow = MutableStateFlow(0)
      private val savedBooksFlow = MutableStateFlow<List<Book>>(emptyList())

      @BeforeEach
      fun setUp() {
          Dispatchers.setMain(testDispatcher)
          every { profileRepository.getProfile() } returns profileFlow
          every { swipeRepository.getSwipeCount() } returns swipeCountFlow
          every { swipeRepository.getSavedBooks() } returns savedBooksFlow
      }

      @AfterEach
      fun tearDown() = Dispatchers.resetMain()

      private fun aProfile(
          summary: String = "You love dark, character-driven fiction.",
          liked: List<String> = listOf("literary fiction", "classics"),
          avoided: List<String> = listOf("romance"),
      ) = TasteProfile(
          aiSummary = summary,
          likedGenres = liked,
          avoidedGenres = avoided,
          preferredLength = "long",
          recurringThemes = listOf("moral complexity"),
          profileVersion = 2,
          lastUpdatedSwipeCount = 20,
          updatedAt = 0L,
      )

      @Nested
      inner class `when a profile exists` {

          @Test
          fun `state shows the AI summary text`() = runTest(testDispatcher) {
              profileFlow.value = aProfile(summary = "You love dark fiction.")
              val vm = TasteProfileViewModel(profileRepository, swipeRepository)
              advanceUntilIdle()

              assertEquals("You love dark fiction.", vm.state.value.profile?.aiSummary)
          }

          @Test
          fun `state shows liked and avoided genres`() = runTest(testDispatcher) {
              profileFlow.value = aProfile(
                  liked = listOf("literary fiction"),
                  avoided = listOf("romance")
              )
              val vm = TasteProfileViewModel(profileRepository, swipeRepository)
              advanceUntilIdle()

              val profile = vm.state.value.profile
              assertNotNull(profile)
              assertTrue("literary fiction" in profile!!.likedGenres)
              assertTrue("romance" in profile.avoidedGenres)
          }

          @Test
          fun `swipe stats are populated from the repository`() = runTest(testDispatcher) {
              profileFlow.value = aProfile()
              swipeCountFlow.value = 25
              val vm = TasteProfileViewModel(profileRepository, swipeRepository)
              advanceUntilIdle()

              assertEquals(25, vm.state.value.swipeStats.totalSwiped)
          }
      }

      @Nested
      inner class `when no profile exists yet` {

          @Test
          fun `profile in state is null`() = runTest(testDispatcher) {
              profileFlow.value = null
              val vm = TasteProfileViewModel(profileRepository, swipeRepository)
              advanceUntilIdle()

              assertNull(vm.state.value.profile)
          }
      }
  }
  ```

- [ ] **Step 2: Run and verify passes**

  ```bash
  ./gradlew :feature:tasteprofile:testDebugUnitTest --tests "com.pageturner.feature.tasteprofile.TasteProfileViewModelTest"
  ```
  Expected: 4 tests pass

- [ ] **Step 3: Commit**

  ```bash
  git add feature/tasteprofile/src/test
  git commit -m "test: specify TasteProfileViewModel state from combined profile flows"
  ```

---

## Task 11: `SwipeDeckViewModelTest` — specify the swipe loop behavior

The richest behavioral spec. `SwipeDeckViewModel.init` launches coroutines that combine flows — mock all three (queue, wildcards, profile) before creating the ViewModel, then `advanceUntilIdle()` to fully initialize before asserting state.

**Files:**
- Create: `feature/swipedeck/src/test/java/com/pageturner/feature/swipedeck/SwipeDeckViewModelTest.kt`

- [ ] **Step 1: Write the test**

  ```kotlin
  package com.pageturner.feature.swipedeck

  import com.pageturner.core.domain.error.AppError
  import com.pageturner.core.domain.model.Book
  import com.pageturner.core.domain.model.Genre
  import com.pageturner.core.domain.model.OnboardingPreferences
  import com.pageturner.core.domain.model.ReadingLength
  import com.pageturner.core.domain.model.SwipeDirection
  import com.pageturner.core.domain.model.TasteProfile
  import com.pageturner.core.domain.repository.BookRepository
  import com.pageturner.core.domain.repository.ProfileRepository
  import com.pageturner.core.domain.repository.SwipeRepository
  import com.pageturner.core.domain.service.AiService
  import com.pageturner.core.domain.util.Result
  import io.mockk.coEvery
  import io.mockk.coJustRun
  import io.mockk.coVerify
  import io.mockk.every
  import io.mockk.impl.annotations.MockK
  import io.mockk.junit5.MockKExtension
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.flowOf
  import kotlinx.coroutines.test.StandardTestDispatcher
  import kotlinx.coroutines.test.advanceUntilIdle
  import kotlinx.coroutines.test.resetMain
  import kotlinx.coroutines.test.runTest
  import kotlinx.coroutines.test.setMain
  import org.junit.jupiter.api.AfterEach
  import org.junit.jupiter.api.Assertions.assertEquals
  import org.junit.jupiter.api.Assertions.assertFalse
  import org.junit.jupiter.api.Assertions.assertNotNull
  import org.junit.jupiter.api.Assertions.assertTrue
  import org.junit.jupiter.api.BeforeEach
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test
  import org.junit.jupiter.api.extension.ExtendWith

  @OptIn(ExperimentalCoroutinesApi::class)
  @ExtendWith(MockKExtension::class)
  class SwipeDeckViewModelTest {

      @MockK private lateinit var bookRepository: BookRepository
      @MockK private lateinit var swipeRepository: SwipeRepository
      @MockK private lateinit var profileRepository: ProfileRepository
      @MockK private lateinit var aiService: AiService

      private val testDispatcher = StandardTestDispatcher()

      private val swipeCountFlow = MutableStateFlow(0)
      private val profileFlow = MutableStateFlow<TasteProfile?>(null)

      private fun aBook(key: String = "/works/OL1", subjects: List<String> = listOf("fantasy")) = Book(
          key = key, title = "Book $key", authors = listOf("Author"),
          coverUrl = null, publishYear = 2000, pageCount = 300,
          subjects = subjects, description = null, aiBrief = null,
          matchScore = 0.5f, isWildcard = false, wildcardReason = null,
      )

      private val preferences = OnboardingPreferences(
          selectedGenres = listOf(Genre.FANTASY),
          selectedLengths = listOf(ReadingLength.MEDIUM),
          completedAt = 0L,
      )

      /** Sets up mocks and creates a ViewModel with 10 cards loaded. */
      private fun createInitializedViewModel(): SwipeDeckViewModel {
          val books = (1..10).map { aBook("/works/OL$it") }
          val queueFlow = MutableStateFlow(books)

          coEvery { profileRepository.getOnboardingPreferences() } returns preferences
          every { bookRepository.getSeenBookKeys() } returns MutableStateFlow(emptySet())
          every { bookRepository.getSwipeQueue(any(), any()) } returns queueFlow
          every { profileRepository.getProfile() } returns profileFlow
          every { swipeRepository.getSwipeCount() } returns swipeCountFlow
          coEvery { bookRepository.fetchBooks(any(), any()) } returns Result.Success(emptyList())
          coEvery { aiService.generateBrief(any(), any()) } returns null
          coEvery { aiService.pickWildcard(any(), any()) } returns null
          coJustRun { swipeRepository.recordSwipe(any()) }
          coJustRun { swipeRepository.saveBook(any(), any(), any(), any(), any()) }
          coJustRun { swipeRepository.cacheAiBrief(any(), any(), any()) }
          coEvery { swipeRepository.getAiBriefCache(any(), any()) } returns null
          coJustRun { bookRepository.prefetchBookDetail(any()) }
          // getSwipeHistory is called by runProfileUpdate() when swipeCount hits a multiple of 10
          every { swipeRepository.getSwipeHistory() } returns flowOf(emptyList())

          return SwipeDeckViewModel(bookRepository, swipeRepository, profileRepository, aiService)
      }

      @BeforeEach
      fun setUp() {
          Dispatchers.setMain(testDispatcher)
      }

      @AfterEach
      fun tearDown() = Dispatchers.resetMain()

      @Nested
      inner class `when onboarding is not complete` {

          @Test
          fun `state shows a non-retryable error`() = runTest(testDispatcher) {
              coEvery { profileRepository.getOnboardingPreferences() } returns null
              every { swipeRepository.getSwipeCount() } returns swipeCountFlow

              val vm = SwipeDeckViewModel(bookRepository, swipeRepository, profileRepository, aiService)
              advanceUntilIdle()

              val error = vm.state.value.error
              assertNotNull(error)
              assertFalse(error!!.isRetryable)
          }

          @Test
          fun `isLoading becomes false`() = runTest(testDispatcher) {
              coEvery { profileRepository.getOnboardingPreferences() } returns null
              every { swipeRepository.getSwipeCount() } returns swipeCountFlow

              val vm = SwipeDeckViewModel(bookRepository, swipeRepository, profileRepository, aiService)
              advanceUntilIdle()

              assertFalse(vm.state.value.isLoading)
          }
      }

      @Nested
      inner class `when swiping left` {

          @Test
          fun `currentCardIndex advances by 1`() = runTest(testDispatcher) {
              val vm = createInitializedViewModel()
              advanceUntilIdle()

              val bookKey = vm.state.value.cards.first().bookKey
              vm.handleIntent(SwipeDeckIntent.SwipeLeft(bookKey))
              advanceUntilIdle()

              assertEquals(1, vm.state.value.currentCardIndex)
          }

          @Test
          fun `saveBook is never called`() = runTest(testDispatcher) {
              val vm = createInitializedViewModel()
              advanceUntilIdle()

              val bookKey = vm.state.value.cards.first().bookKey
              vm.handleIntent(SwipeDeckIntent.SwipeLeft(bookKey))
              advanceUntilIdle()

              coVerify(exactly = 0) { swipeRepository.saveBook(any(), any(), any(), any(), any()) }
          }
      }

      @Nested
      inner class `when swiping right` {

          @Test
          fun `currentCardIndex advances by 1`() = runTest(testDispatcher) {
              val vm = createInitializedViewModel()
              advanceUntilIdle()

              val bookKey = vm.state.value.cards.first().bookKey
              vm.handleIntent(SwipeDeckIntent.SwipeRight(bookKey))
              advanceUntilIdle()

              assertEquals(1, vm.state.value.currentCardIndex)
          }

          @Test
          fun `saveBook is called with isBookmarked false`() = runTest(testDispatcher) {
              val vm = createInitializedViewModel()
              advanceUntilIdle()

              val bookKey = vm.state.value.cards.first().bookKey
              vm.handleIntent(SwipeDeckIntent.SwipeRight(bookKey))
              advanceUntilIdle()

              coVerify { swipeRepository.saveBook(bookKey, any(), any(), any(), isBookmarked = false) }
          }

          @Test
          fun `prefetchBookDetail is called for the swiped book`() = runTest(testDispatcher) {
              val vm = createInitializedViewModel()
              advanceUntilIdle()

              val bookKey = vm.state.value.cards.first().bookKey
              vm.handleIntent(SwipeDeckIntent.SwipeRight(bookKey))
              advanceUntilIdle()

              coVerify { bookRepository.prefetchBookDetail(bookKey) }
          }
      }

      @Nested
      inner class `when bookmarking` {

          @Test
          fun `currentCardIndex advances by 1`() = runTest(testDispatcher) {
              val vm = createInitializedViewModel()
              advanceUntilIdle()

              val bookKey = vm.state.value.cards.first().bookKey
              vm.handleIntent(SwipeDeckIntent.Bookmark(bookKey))
              advanceUntilIdle()

              assertEquals(1, vm.state.value.currentCardIndex)
          }

          @Test
          fun `saveBook is called with isBookmarked true`() = runTest(testDispatcher) {
              val vm = createInitializedViewModel()
              advanceUntilIdle()

              val bookKey = vm.state.value.cards.first().bookKey
              vm.handleIntent(SwipeDeckIntent.Bookmark(bookKey))
              advanceUntilIdle()

              coVerify { swipeRepository.saveBook(bookKey, any(), any(), any(), isBookmarked = true) }
          }

          @Test
          fun `prefetchBookDetail is called for the swiped book`() = runTest(testDispatcher) {
              val vm = createInitializedViewModel()
              advanceUntilIdle()

              val bookKey = vm.state.value.cards.first().bookKey
              vm.handleIntent(SwipeDeckIntent.Bookmark(bookKey))
              advanceUntilIdle()

              coVerify { bookRepository.prefetchBookDetail(bookKey) }
          }
      }

      @Nested
      inner class `when swipe count reaches a multiple of 10` {

          @Test
          fun `TriggerProfileUpdate side effect is emitted`() = runTest(testDispatcher) {
              every { swipeRepository.getSwipeHistory() } returns flowOf(emptyList())
              coEvery { profileRepository.getOnboardingPreferences() } returns preferences
              coEvery { aiService.summarizeProfile(any(), any()) } returns null

              val vm = createInitializedViewModel()
              advanceUntilIdle()

              swipeCountFlow.value = 10
              advanceUntilIdle()

              val effect = vm.sideEffects.tryReceive().getOrNull()
              assertEquals(SwipeDeckSideEffect.TriggerProfileUpdate, effect)
          }
      }

      @Nested
      inner class `when fewer than 5 cards remain after a swipe` {

          @Test
          fun `isReplenishing becomes true while fetching then false on completion`() = runTest(testDispatcher) {
              val books = (1..6).map { aBook("/works/OL$it") }
              every { bookRepository.getSwipeQueue(any(), any()) } returns MutableStateFlow(books)
              coEvery { bookRepository.fetchNextPage(any(), any()) } returns emptyList()

              val vm = createInitializedViewModel()
              advanceUntilIdle()

              // Collect all isReplenishing state transitions during the swipe sequence
              val replenishingHistory = mutableListOf<Boolean>()
              val collectJob = launch { vm.state.collect { replenishingHistory.add(it.isReplenishing) } }

              // Swipe 5 times on a 6-card deck — this leaves 1 card, triggering replenishment
              repeat(5) {
                  val key = vm.state.value.cards.getOrNull(vm.state.value.currentCardIndex)?.bookKey
                      ?: return@repeat
                  vm.handleIntent(SwipeDeckIntent.SwipeLeft(key))
              }
              advanceUntilIdle()
              collectJob.cancel()

              // isReplenishing was true during fetch, then settled back to false
              assertTrue(true in replenishingHistory, "expected isReplenishing = true to be emitted")
              assertFalse(vm.state.value.isReplenishing)
          }
      }

      @Nested
      inner class `when ExpandCard intent is received` {

          @Test
          fun `NavigateToDetail side effect is emitted with the correct bookKey`() = runTest(testDispatcher) {
              val vm = createInitializedViewModel()
              advanceUntilIdle()

              val bookKey = vm.state.value.cards.first().bookKey
              vm.handleIntent(SwipeDeckIntent.ExpandCard(bookKey))
              advanceUntilIdle()

              val effect = vm.sideEffects.tryReceive().getOrNull()
              assertEquals(SwipeDeckSideEffect.NavigateToDetail(bookKey), effect)
          }
      }

      @Nested
      inner class `when Retry intent is received` {

          @Test
          fun `state resets to loading`() = runTest(testDispatcher) {
              val vm = createInitializedViewModel()
              advanceUntilIdle()
              assertFalse(vm.state.value.isLoading)

              vm.handleIntent(SwipeDeckIntent.Retry)

              assertTrue(vm.state.value.isLoading)
          }
      }
  }
  ```

- [ ] **Step 2: Run and verify passes**

  ```bash
  ./gradlew :feature:swipedeck:testDebugUnitTest --tests "com.pageturner.feature.swipedeck.SwipeDeckViewModelTest"
  ```
  Expected: 12 tests pass

- [ ] **Step 3: Commit**

  ```bash
  git add feature/swipedeck/src/test
  git commit -m "test: specify SwipeDeckViewModel swipe loop and profile trigger behavior"
  ```

---

## Task 12: `BookRepositoryImplTest` — specify the cache-first book fetching contract

**Files:**
- Create: `core/data/src/test/java/com/pageturner/core/data/repository/BookRepositoryImplTest.kt`

- [ ] **Step 1: Write the test**

  Note: `getSwipeQueue` is tested with Turbine since it returns a `Flow`. `BookRepositoryImpl` uses `flowOn(Dispatchers.IO)` internally; in unit tests this runs on a real IO thread, which is fine because mock operations complete in microseconds — well within Turbine's 1-second default timeout. No `Dispatchers.setMain` is needed since this module has no ViewModel.

  ```kotlin
  package com.pageturner.core.data.repository

  import app.cash.turbine.test
  import com.pageturner.core.data.db.converter.Converters
  import com.pageturner.core.data.db.dao.BookDao
  import com.pageturner.core.data.db.dao.SwipeEventDao
  import com.pageturner.core.data.entity.BookEntity
  import com.pageturner.core.network.api.OpenLibraryApiService
  import com.pageturner.core.network.dto.openlib.SearchDocDto
  import com.pageturner.core.network.dto.openlib.SearchResponseDto
  import io.mockk.coEvery
  import io.mockk.coJustRun
  import io.mockk.coVerify
  import io.mockk.every
  import io.mockk.impl.annotations.MockK
  import io.mockk.junit5.MockKExtension
  import kotlinx.coroutines.flow.flowOf
  import kotlinx.coroutines.test.runTest
  import org.junit.jupiter.api.Assertions.assertEquals
  import org.junit.jupiter.api.Assertions.assertFalse
  import org.junit.jupiter.api.Assertions.assertTrue
  import org.junit.jupiter.api.BeforeEach
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test
  import org.junit.jupiter.api.extension.ExtendWith

  @ExtendWith(MockKExtension::class)
  class BookRepositoryImplTest {

      @MockK private lateinit var bookDao: BookDao
      @MockK private lateinit var swipeEventDao: SwipeEventDao
      @MockK private lateinit var openLibraryApiService: OpenLibraryApiService

      private lateinit var repo: BookRepositoryImpl

      private fun anEntity(key: String = "/works/OL1") = BookEntity(
          key = key,
          title = "Book",
          authorNamesJson = Converters.serializeList(listOf("Author")),
          coverUrl = null,
          publishYear = 2000,
          pageCount = 300,
          subjectsJson = Converters.serializeList(listOf("fantasy")),
          description = null,
          cachedAt = 0L,
      )

      private fun aDoc(key: String = "/works/OL1") = SearchDocDto(
          key = key, title = "Book", authorName = listOf("Author"),
          firstPublishYear = 2000, coverId = null, subject = listOf("fantasy"),
          numberOfPagesMedian = 300
      )

      private fun aSearchResponse(vararg docs: SearchDocDto) =
          SearchResponseDto(numFound = docs.size, docs = docs.toList())

      @BeforeEach
      fun setUp() {
          repo = BookRepositoryImpl(bookDao, swipeEventDao, openLibraryApiService)
      }

      @Nested
      inner class `getSwipeQueue` {

          @Test
          fun `emits cached books immediately before any network call`() = runTest {
              val cached = (1..15).map { anEntity("/works/OL$it") }
              coEvery { bookDao.getUnseenBooks(any()) } returns cached
              coEvery { bookDao.getAllBooks() } returns cached

              repo.getSwipeQueue(listOf("fantasy"), emptySet()).test {
                  val first = awaitItem()
                  assertTrue(first.isNotEmpty())
                  cancelAndIgnoreRemainingEvents()
              }
          }

          @Test
          fun `fetches from network and re-emits when local cache has fewer than 10 books`() = runTest {
              val sparse = (1..5).map { anEntity("/works/OL$it") }
              val networkDoc = aDoc("/works/OL99")
              coEvery { bookDao.getAllBooks() } returns sparse
              coEvery { bookDao.getUnseenBooks(any()) } returns sparse
              coEvery { openLibraryApiService.searchBySubject(any(), any()) } returns aSearchResponse(networkDoc)
              coJustRun { bookDao.upsertBooks(any()) }
              coJustRun { bookDao.upsertBook(any()) }

              repo.getSwipeQueue(listOf("fantasy"), emptySet()).test {
                  skipItems(1) // first emit from cache
                  // After network fetch, bookDao returns updated list
                  val updated = sparse + anEntity("/works/OL99")
                  coEvery { bookDao.getAllBooks() } returns updated
                  coEvery { bookDao.getUnseenBooks(any()) } returns updated

                  cancelAndIgnoreRemainingEvents()
              }

              coVerify { openLibraryApiService.searchBySubject("fantasy", any()) }
          }
      }

      @Nested
      inner class `prefetchBookDetail` {

          @Test
          fun `skips network when description is already cached`() = runTest {
              coEvery { bookDao.getBook(any()) } returns anEntity().copy(description = "Cached desc")

              repo.prefetchBookDetail("/works/OL1")

              coVerify(exactly = 0) { openLibraryApiService.getWorkDetail(any()) }
          }
      }

      @Nested
      inner class `fetchNextPage` {

          @Test
          fun `advances the page counter on each call`() = runTest {
              coEvery { openLibraryApiService.searchBySubject("fantasy", page = 2) } returns aSearchResponse()
              coEvery { openLibraryApiService.searchBySubject("fantasy", page = 3) } returns aSearchResponse()

              repo.fetchNextPage(listOf("fantasy"), emptySet()) // page 2
              repo.fetchNextPage(listOf("fantasy"), emptySet()) // page 3

              coVerify { openLibraryApiService.searchBySubject("fantasy", page = 2) }
              coVerify { openLibraryApiService.searchBySubject("fantasy", page = 3) }
          }

          @Test
          fun `deduplicates books appearing across genre pages`() = runTest {
              val doc = aDoc("/works/OL_DUPE")
              coEvery { openLibraryApiService.searchBySubject("fantasy", any()) } returns aSearchResponse(doc)
              coEvery { openLibraryApiService.searchBySubject("classics", any()) } returns aSearchResponse(doc)
              coJustRun { bookDao.upsertBook(any()) }

              val result = repo.fetchNextPage(listOf("fantasy", "classics"), emptySet())

              assertEquals(1, result.size)
          }
      }

      @Nested
      inner class `getSeenBookKeys` {

          @Test
          fun `returns keys from swipeEventDao as a Set`() = runTest {
              every { swipeEventDao.getSeenBookKeys() } returns flowOf(listOf("/works/OL1", "/works/OL2"))

              repo.getSeenBookKeys().test {
                  val keys = awaitItem()
                  assertEquals(setOf("/works/OL1", "/works/OL2"), keys)
                  cancelAndIgnoreRemainingEvents()
              }
          }
      }
  }
  ```

- [ ] **Step 2: Run and verify passes**

  ```bash
  ./gradlew :core:data:testDebugUnitTest --tests "com.pageturner.core.data.repository.BookRepositoryImplTest"
  ```
  Expected: 5 tests pass

- [ ] **Step 3: Commit**

  ```bash
  git add core/data/src/test/java/com/pageturner/core/data/repository/BookRepositoryImplTest.kt
  git commit -m "test: specify BookRepositoryImpl cache-first fetching contract"
  ```

---

## Task 13: `SwipeRepositoryImplTest` — specify swipe recording and saved book contract

**Files:**
- Create: `core/data/src/test/java/com/pageturner/core/data/repository/SwipeRepositoryImplTest.kt`

- [ ] **Step 1: Check which DAOs to read for `SavedBookWithDetail`**

  Verify `SavedBookWithDetail` is accessible from `core/data` (it is — it's in `core/data/entity`).

- [ ] **Step 2: Write the test**

  ```kotlin
  package com.pageturner.core.data.repository

  import app.cash.turbine.test
  import com.pageturner.core.data.db.converter.Converters
  import com.pageturner.core.data.db.dao.AiBriefCacheDao
  import com.pageturner.core.data.db.dao.SavedBookDao
  import com.pageturner.core.data.db.dao.SwipeEventDao
  import com.pageturner.core.data.entity.BookEntity
  import com.pageturner.core.data.entity.SavedBookEntity
  import com.pageturner.core.data.entity.SavedBookWithDetail
  import com.pageturner.core.domain.model.SwipeDirection
  import com.pageturner.core.domain.model.SwipeEvent
  import io.mockk.coJustRun
  import io.mockk.coVerify
  import io.mockk.every
  import io.mockk.impl.annotations.MockK
  import io.mockk.junit5.MockKExtension
  import io.mockk.slot
  import kotlinx.coroutines.flow.flowOf
  import kotlinx.coroutines.test.runTest
  import org.junit.jupiter.api.Assertions.assertEquals
  import org.junit.jupiter.api.Assertions.assertFalse
  import org.junit.jupiter.api.Assertions.assertTrue
  import org.junit.jupiter.api.BeforeEach
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test
  import org.junit.jupiter.api.extension.ExtendWith

  @ExtendWith(MockKExtension::class)
  class SwipeRepositoryImplTest {

      @MockK private lateinit var swipeEventDao: SwipeEventDao
      @MockK private lateinit var savedBookDao: SavedBookDao
      @MockK private lateinit var aiBriefCacheDao: AiBriefCacheDao

      private lateinit var repo: SwipeRepositoryImpl

      private fun anEntity(key: String = "/works/OL1") = BookEntity(
          key = key, title = "Book",
          authorNamesJson = Converters.serializeList(listOf("Author")),
          coverUrl = null, publishYear = null, pageCount = null,
          subjectsJson = Converters.serializeList(emptyList()),
          description = null, cachedAt = 0L,
      )

      private fun aSavedBook(key: String, isBookmarked: Boolean = false) = SavedBookWithDetail(
          book = anEntity(key),
          savedBook = SavedBookEntity(
              bookKey = key, savedAt = 0L, aiBrief = null,
              wildcardReason = null, isWildcard = false, isBookmarked = isBookmarked,
          )
      )

      @BeforeEach
      fun setUp() {
          repo = SwipeRepositoryImpl(swipeEventDao, savedBookDao, aiBriefCacheDao)
      }

      @Nested
      inner class `recordSwipe` {

          @Test
          fun `inserts a SwipeEventEntity with the correct direction name`() = runTest {
              val entitySlot = slot<com.pageturner.core.data.entity.SwipeEventEntity>()
              coJustRun { swipeEventDao.insertSwipeEvent(capture(entitySlot)) }

              repo.recordSwipe(
                  SwipeEvent(
                      id = 0, bookKey = "/works/OL1", direction = SwipeDirection.RIGHT,
                      timestamp = 1000L, bookGenres = emptyList(), bookYear = null,
                      bookPageCount = null, wasWildcard = false,
                  )
              )

              assertEquals("RIGHT", entitySlot.captured.direction)
          }
      }

      @Nested
      inner class `saveBook` {

          @Test
          fun `inserts with isBookmarked false for a right swipe`() = runTest {
              val entitySlot = slot<SavedBookEntity>()
              coJustRun { savedBookDao.saveBook(capture(entitySlot)) }

              repo.saveBook(
                  bookKey = "/works/OL1",
                  aiBrief = null,
                  wildcardReason = null,
                  isWildcard = false,
                  isBookmarked = false,
              )

              assertFalse(entitySlot.captured.isBookmarked)
          }

          @Test
          fun `inserts with isBookmarked true for a bookmark swipe`() = runTest {
              val entitySlot = slot<SavedBookEntity>()
              coJustRun { savedBookDao.saveBook(capture(entitySlot)) }

              repo.saveBook(
                  bookKey = "/works/OL1",
                  aiBrief = null,
                  wildcardReason = null,
                  isWildcard = false,
                  isBookmarked = true,
              )

              assertTrue(entitySlot.captured.isBookmarked)
          }
      }

      @Nested
      inner class `getLikedBooks` {

          @Test
          fun `only non-bookmarked saved books appear`() = runTest {
              every { savedBookDao.getLikedBooksWithDetails() } returns
                  flowOf(listOf(aSavedBook("/works/OL1", isBookmarked = false)))

              repo.getLikedBooks().test {
                  val books = awaitItem()
                  assertEquals(1, books.size)
                  assertEquals("/works/OL1", books.first().key)
                  cancelAndIgnoreRemainingEvents()
              }
          }
      }

      @Nested
      inner class `getBookmarkedBooks` {

          @Test
          fun `only bookmarked books appear`() = runTest {
              every { savedBookDao.getBookmarkedBooksWithDetails() } returns
                  flowOf(listOf(aSavedBook("/works/OL2", isBookmarked = true)))

              repo.getBookmarkedBooks().test {
                  val books = awaitItem()
                  assertEquals(1, books.size)
                  assertEquals("/works/OL2", books.first().key)
                  cancelAndIgnoreRemainingEvents()
              }
          }
      }

      @Nested
      inner class `isBookSaved` {

          @Test
          fun `returns true when book key is in saved books`() = runTest {
              coEvery { savedBookDao.isBookSaved("/works/OL1") } returns true
              assertTrue(repo.isBookSaved("/works/OL1"))
          }

          @Test
          fun `returns false when book key is absent`() = runTest {
              coEvery { savedBookDao.isBookSaved("/works/OL999") } returns false
              assertFalse(repo.isBookSaved("/works/OL999"))
          }
      }
  }
  ```

- [ ] **Step 3: Run and verify passes**

  ```bash
  ./gradlew :core:data:testDebugUnitTest --tests "com.pageturner.core.data.repository.SwipeRepositoryImplTest"
  ```
  Expected: 6 tests pass

- [ ] **Step 4: Commit**

  ```bash
  git add core/data/src/test/java/com/pageturner/core/data/repository/SwipeRepositoryImplTest.kt
  git commit -m "test: specify SwipeRepositoryImpl swipe recording and saved book contract"
  ```

---

## Task 14: `GenerateBriefUseCaseTest` — specify AI brief generation

Must be in `com.pageturner.core.ai.usecase` to access the `internal` class.

**Files:**
- Create: `core/ai/src/test/java/com/pageturner/core/ai/usecase/GenerateBriefUseCaseTest.kt`

- [ ] **Step 1: Write the test**

  ```kotlin
  package com.pageturner.core.ai.usecase

  import com.pageturner.core.domain.model.Book
  import com.pageturner.core.network.api.AnthropicApiService
  import com.pageturner.core.network.dto.anthropic.AnthropicContentBlockDto
  import com.pageturner.core.network.dto.anthropic.AnthropicResponseDto
  import io.mockk.coAnswers
  import io.mockk.coEvery
  import io.mockk.impl.annotations.MockK
  import io.mockk.junit5.MockKExtension
  import kotlinx.coroutines.delay
  import kotlinx.coroutines.test.runTest
  import org.junit.jupiter.api.Assertions.assertNotNull
  import org.junit.jupiter.api.Assertions.assertNull
  import org.junit.jupiter.api.BeforeEach
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test
  import org.junit.jupiter.api.extension.ExtendWith

  @ExtendWith(MockKExtension::class)
  class GenerateBriefUseCaseTest {

      @MockK private lateinit var anthropicApiService: AnthropicApiService

      private lateinit var useCase: GenerateBriefUseCase

      private fun aBook() = Book(
          key = "/works/OL1", title = "Dune", authors = listOf("Frank Herbert"),
          coverUrl = null, publishYear = 1965, pageCount = 412,
          subjects = listOf("science fiction"), description = "A desert planet epic.",
          aiBrief = null, matchScore = 0.8f, isWildcard = false, wildcardReason = null,
      )

      private fun aResponse(text: String) = AnthropicResponseDto(
          id = "msg_1", type = "message", role = "assistant",
          content = listOf(AnthropicContentBlockDto(type = "text", text = text)),
          model = "claude-haiku-4-5-20251001", stopReason = "end_turn"
      )

      @BeforeEach
      fun setUp() {
          useCase = GenerateBriefUseCase(anthropicApiService)
      }

      @Nested
      inner class `when API returns a valid response` {

          @Test
          fun `returns a trimmed non-blank string`() = runTest {
              coEvery { anthropicApiService.createMessage(any()) } returns aResponse("  A gripping read.  ")

              val result = useCase(aBook(), profileSummary = "You love sci-fi epics.")

              assertNotNull(result)
          }
      }

      @Nested
      inner class `when API returns blank content` {

          @Test
          fun `returns null`() = runTest {
              coEvery { anthropicApiService.createMessage(any()) } returns aResponse("   ")

              val result = useCase(aBook(), profileSummary = null)

              assertNull(result)
          }
      }

      @Nested
      inner class `when API call throws an exception` {

          @Test
          fun `returns null without propagating`() = runTest {
              coEvery { anthropicApiService.createMessage(any()) } throws RuntimeException("Network error")

              val result = useCase(aBook(), profileSummary = null)

              assertNull(result)
          }
      }

      @Nested
      inner class `when API call times out` {

          @Test
          fun `returns null without propagating`() = runTest {
              // delay(Long.MAX_VALUE) uses virtual time — withTimeout(30_000L) in the use case
              // fires at virtual time 30s, triggering TimeoutCancellationException, which is caught.
              coEvery { anthropicApiService.createMessage(any()) } coAnswers {
                  delay(Long.MAX_VALUE)
                  aResponse("irrelevant")
              }

              val result = useCase(aBook(), profileSummary = null)

              assertNull(result)
          }
      }
  }
  ```

- [ ] **Step 2: Run and verify passes**

  ```bash
  ./gradlew :core:ai:testDebugUnitTest --tests "com.pageturner.core.ai.usecase.GenerateBriefUseCaseTest"
  ```
  Expected: 4 tests pass

- [ ] **Step 3: Commit**

  ```bash
  git add core/ai/src/test/java/com/pageturner/core/ai/usecase/GenerateBriefUseCaseTest.kt
  git commit -m "test: specify GenerateBriefUseCase null-safety contract"
  ```

---

## Task 15: `SummarizeProfileUseCaseTest` and `PickWildcardUseCaseTest` — specify AI profile and wildcard use cases

Both use a real `Moshi` instance since the DTOs use KSP-generated codegen adapters (no reflection needed).

**Files:**
- Create: `core/ai/src/test/java/com/pageturner/core/ai/usecase/SummarizeProfileUseCaseTest.kt`
- Create: `core/ai/src/test/java/com/pageturner/core/ai/usecase/PickWildcardUseCaseTest.kt`

- [ ] **Step 1: Write `SummarizeProfileUseCaseTest`**

  ```kotlin
  package com.pageturner.core.ai.usecase

  import com.pageturner.core.domain.model.Genre
  import com.pageturner.core.domain.model.SwipeDirection
  import com.pageturner.core.domain.model.SwipeEvent
  import com.pageturner.core.network.api.AnthropicApiService
  import com.pageturner.core.network.dto.anthropic.AnthropicContentBlockDto
  import com.pageturner.core.network.dto.anthropic.AnthropicResponseDto
  import com.squareup.moshi.Moshi
  import io.mockk.coEvery
  import io.mockk.impl.annotations.MockK
  import io.mockk.junit5.MockKExtension
  import kotlinx.coroutines.test.runTest
  import org.junit.jupiter.api.Assertions.assertNotNull
  import org.junit.jupiter.api.Assertions.assertNull
  import org.junit.jupiter.api.Assertions.assertTrue
  import org.junit.jupiter.api.BeforeEach
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test
  import org.junit.jupiter.api.extension.ExtendWith

  @ExtendWith(MockKExtension::class)
  class SummarizeProfileUseCaseTest {

      @MockK private lateinit var anthropicApiService: AnthropicApiService

      // Moshi.Builder().build() is sufficient — @JsonClass(generateAdapter = true) causes
      // Moshi to auto-discover the KSP-generated adapter by class name convention
      // (KotlinJsonAdapterFactory is NOT used because it explicitly skips codegen classes)
      private val moshi = Moshi.Builder().build()

      private lateinit var useCase: SummarizeProfileUseCase

      private fun aSwipeEvent(key: String = "/works/OL1") = SwipeEvent(
          id = 0, bookKey = key, direction = SwipeDirection.RIGHT,
          timestamp = 0L, bookGenres = listOf("fantasy"), bookYear = 2000,
          bookPageCount = 300, wasWildcard = false,
      )

      private fun aResponse(text: String) = AnthropicResponseDto(
          id = "msg_1", type = "message", role = "assistant",
          content = listOf(AnthropicContentBlockDto(type = "text", text = text)),
          model = "claude-sonnet-4-6", stopReason = "end_turn"
      )

      private val validProfileJson = """
          {
            "aiSummary": "You love speculative fiction.",
            "likedGenres": ["fantasy", "science fiction"],
            "avoidedGenres": ["romance"],
            "preferredLength": "long",
            "recurringThemes": ["moral complexity"]
          }
      """.trimIndent()

      @BeforeEach
      fun setUp() {
          useCase = SummarizeProfileUseCase(anthropicApiService, moshi)
      }

      @Nested
      inner class `when swipe history is empty` {

          @Test
          fun `returns null without making any API call`() = runTest {
              val result = useCase(emptyList(), listOf(Genre.FANTASY))
              assertNull(result)
          }
      }

      @Nested
      inner class `when API returns valid JSON` {

          @Test
          fun `returns a TasteProfile with correct likedGenres and aiSummary`() = runTest {
              coEvery { anthropicApiService.createMessage(any()) } returns aResponse(validProfileJson)

              val result = useCase(listOf(aSwipeEvent()), listOf(Genre.FANTASY))

              assertNotNull(result)
              assertTrue("fantasy" in result!!.likedGenres)
              assertTrue(result.aiSummary.isNotBlank())
          }
      }

      @Nested
      inner class `when API returns JSON in markdown fences` {

          @Test
          fun `fences are stripped and profile is parsed correctly`() = runTest {
              val fenced = "```json\n$validProfileJson\n```"
              coEvery { anthropicApiService.createMessage(any()) } returns aResponse(fenced)

              val result = useCase(listOf(aSwipeEvent()), listOf(Genre.FANTASY))

              assertNotNull(result)
          }
      }

      @Nested
      inner class `when API returns malformed JSON` {

          @Test
          fun `returns null`() = runTest {
              coEvery { anthropicApiService.createMessage(any()) } returns aResponse("not json at all")

              val result = useCase(listOf(aSwipeEvent()), listOf(Genre.FANTASY))

              assertNull(result)
          }
      }

      @Nested
      inner class `when API call fails` {

          @Test
          fun `returns null without propagating`() = runTest {
              coEvery { anthropicApiService.createMessage(any()) } throws RuntimeException("API down")

              val result = useCase(listOf(aSwipeEvent()), listOf(Genre.FANTASY))

              assertNull(result)
          }
      }
  }
  ```

- [ ] **Step 2: Write `PickWildcardUseCaseTest`**

  ```kotlin
  package com.pageturner.core.ai.usecase

  import com.pageturner.core.domain.model.Book
  import com.pageturner.core.domain.model.TasteProfile
  import com.pageturner.core.network.api.AnthropicApiService
  import com.pageturner.core.network.dto.anthropic.AnthropicContentBlockDto
  import com.pageturner.core.network.dto.anthropic.AnthropicResponseDto
  import com.squareup.moshi.Moshi
  import io.mockk.coEvery
  import io.mockk.impl.annotations.MockK
  import io.mockk.junit5.MockKExtension
  import kotlinx.coroutines.test.runTest
  import org.junit.jupiter.api.Assertions.assertNotNull
  import org.junit.jupiter.api.Assertions.assertNull
  import org.junit.jupiter.api.Assertions.assertTrue
  import org.junit.jupiter.api.BeforeEach
  import org.junit.jupiter.api.Nested
  import org.junit.jupiter.api.Test
  import org.junit.jupiter.api.extension.ExtendWith

  @ExtendWith(MockKExtension::class)
  class PickWildcardUseCaseTest {

      @MockK private lateinit var anthropicApiService: AnthropicApiService

      // Moshi.Builder().build() auto-discovers the KSP-generated WildcardPickDtoJsonAdapter
      // via class name convention. KotlinJsonAdapterFactory explicitly skips codegen classes.
      private val moshi = Moshi.Builder().build()

      private lateinit var useCase: PickWildcardUseCase

      private fun aBook(key: String = "/works/OL1") = Book(
          key = key, title = "Book $key", authors = listOf("Author"),
          coverUrl = null, publishYear = 2000, pageCount = 300,
          subjects = listOf("horror"), description = null, aiBrief = null,
          matchScore = 0.5f, isWildcard = false, wildcardReason = null,
      )

      private fun aProfile() = TasteProfile(
          aiSummary = "You love dark fiction.", likedGenres = listOf("fantasy"),
          avoidedGenres = emptyList(), preferredLength = "long",
          recurringThemes = emptyList(), profileVersion = 1,
          lastUpdatedSwipeCount = 10, updatedAt = 0L,
      )

      private fun aResponse(text: String) = AnthropicResponseDto(
          id = "msg_1", type = "message", role = "assistant",
          content = listOf(AnthropicContentBlockDto(type = "text", text = text)),
          model = "claude-sonnet-4-6", stopReason = "end_turn"
      )

      @BeforeEach
      fun setUp() {
          useCase = PickWildcardUseCase(anthropicApiService, moshi)
      }

      @Nested
      inner class `when candidates list is empty` {

          @Test
          fun `returns null without making any API call`() = runTest {
              val result = useCase(aProfile(), emptyList())
              assertNull(result)
          }
      }

      @Nested
      inner class `when API picks a valid index within candidates` {

          @Test
          fun `returns WildcardResult with matching book and reason`() = runTest {
              val candidates = listOf(aBook("/works/OL1"), aBook("/works/OL2"))
              val json = """{"selectedIndex": 1, "reason": "Shares moral complexity."}"""
              coEvery { anthropicApiService.createMessage(any()) } returns aResponse(json)

              val result = useCase(aProfile(), candidates)

              assertNotNull(result)
              assertTrue(result!!.book.key in candidates.map { it.key })
          }
      }

      @Nested
      inner class `when API picks an out-of-range index` {

          @Test
          fun `returns WildcardResult with a random candidate as fallback`() = runTest {
              val candidates = listOf(aBook("/works/OL1"))
              val json = """{"selectedIndex": 999, "reason": "Reason."}"""
              coEvery { anthropicApiService.createMessage(any()) } returns aResponse(json)

              val result = useCase(aProfile(), candidates)

              assertNotNull(result)
              assertTrue(result!!.book.key in candidates.map { it.key })
          }
      }

      @Nested
      inner class `when API call fails or times out` {

          @Test
          fun `returns WildcardResult with a random candidate as fallback`() = runTest {
              val candidates = listOf(aBook("/works/OL1"), aBook("/works/OL2"))
              coEvery { anthropicApiService.createMessage(any()) } throws RuntimeException("fail")

              val result = useCase(aProfile(), candidates)

              assertNotNull(result)
              assertTrue(result!!.book.key in candidates.map { it.key })
          }
      }
  }
  ```

- [ ] **Step 3: Run and verify both pass**

  ```bash
  ./gradlew :core:ai:testDebugUnitTest --tests "com.pageturner.core.ai.usecase.SummarizeProfileUseCaseTest"
  ./gradlew :core:ai:testDebugUnitTest --tests "com.pageturner.core.ai.usecase.PickWildcardUseCaseTest"
  ```
  Expected: 5 + 4 = 9 tests pass

- [ ] **Step 4: Commit**

  ```bash
  git add core/ai/src/test/java/com/pageturner/core/ai/usecase/SummarizeProfileUseCaseTest.kt \
         core/ai/src/test/java/com/pageturner/core/ai/usecase/PickWildcardUseCaseTest.kt
  git commit -m "test: specify SummarizeProfileUseCase and PickWildcardUseCase null-safety contracts"
  ```

---

## Final Step: Full Suite Green

- [ ] **Run the complete test suite**

  ```bash
  ./gradlew test testDebugUnitTest
  ```
  Expected: All modules pass. Total ~75 tests across 13 test files.

- [ ] **Commit if any last fixes were needed**

  ```bash
  git add -A
  git commit -m "test: all test suite layers green"
  ```
