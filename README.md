# PageTurner

Tinder for books — but the swipes teach it your taste.

Swipe right to save a book, swipe left to pass. Every 10 swipes, AI reads your
history and writes a profile of you as a reader. That profile shapes everything:
the books you see next, how they're described to you, and the occasional intentional
curveball designed to expand your reading.

## Architecture

- **Pattern:** MVI + Clean Architecture, strictly layered
- **Modules:** Multi-module Gradle
- **Stack:** Kotlin, Jetpack Compose, Hilt, Room, Retrofit, Coil, Coroutines + Flow
- **AI:** Anthropic Claude API — brief generation (Haiku), taste profiling and wildcard
  selection (Sonnet). Isolated in :core:ai behind an interface, degrades gracefully on failure.

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
2. Add `ANTHROPIC_API_KEY=sk-ant-...` to `local.properties` (never commit this)
3. Open in Android Studio Meerkat 2025.1 or later
4. Run on emulator or device (min SDK 26)
5. Open Library is public — no other API keys needed

## Testing

```
./gradlew test                       -- full unit test suite
./gradlew :core:ai:test              -- AI UseCase tests (includes failure paths)
./gradlew :feature:swipedeck:test    -- swipe deck ViewModel + logic tests
```

## How this was built

Built with Claude Code using a structured workflow: brainstorm → spec → implementation plan →
incremental execution. Design documents are in `docs/superpowers/`. Agent role definitions
and system prompts are in `docs/agents/` and `AGENTS.md`.
