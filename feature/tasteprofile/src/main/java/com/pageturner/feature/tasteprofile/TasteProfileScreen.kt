package com.pageturner.feature.tasteprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pageturner.core.ui.components.AiBriefText
import com.pageturner.core.ui.components.ErrorState
import com.pageturner.core.ui.components.GenreChip
import com.pageturner.core.ui.components.LoadingIndicator
import com.pageturner.core.ui.components.PageTurnerTopBar
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerSpacing
import com.pageturner.core.ui.theme.PageTurnerType

@Composable
fun TasteProfileScreen(
    viewModel: TasteProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is TasteProfileSideEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { PageTurnerTopBar(title = "My Taste") },
        containerColor = PageTurnerColors.Background,
    ) { innerPadding ->
        when {
            state.isLoading -> {
                LoadingIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            state.error != null -> {
                ErrorState(
                    message = state.error!!.message,
                    onRetry = { viewModel.handleIntent(TasteProfileIntent.Refresh) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            state.swipeStats.totalSwiped < 10 -> {
                InsufficientDataState(
                    swipeCount = state.swipeStats.totalSwiped,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            state.profile != null -> {
                ProfileContent(
                    profile = state.profile!!,
                    stats = state.swipeStats,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            else -> {
                // totalSwiped >= 10 but AI hasn't produced a profile yet (still building or failed).
                BuildingProfileState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile content
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileContent(
    profile: TasteProfileUiModel,
    stats: SwipeStats,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(PageTurnerSpacing.md),
    ) {
        // AI summary section
        SectionHeader(text = "My taste profile")
        Spacer(Modifier.height(PageTurnerSpacing.sm))
        AiBriefText(
            brief = profile.aiSummary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(PageTurnerSpacing.lg))

        // What you love
        if (profile.likedGenres.isNotEmpty()) {
            SectionHeader(text = "What you love")
            Spacer(Modifier.height(PageTurnerSpacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(PageTurnerSpacing.xs)) {
                profile.likedGenres.forEach { genre -> GenreChip(label = genre) }
            }
            Spacer(Modifier.height(PageTurnerSpacing.lg))
        }

        // You tend to skip
        if (profile.avoidedGenres.isNotEmpty()) {
            SectionHeader(text = "You tend to skip")
            Spacer(Modifier.height(PageTurnerSpacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(PageTurnerSpacing.xs)) {
                profile.avoidedGenres.forEach { genre -> AvoidedGenreChip(label = genre) }
            }
            Spacer(Modifier.height(PageTurnerSpacing.lg))
        }

        // Stats grid
        SectionHeader(text = "Reading stats")
        Spacer(Modifier.height(PageTurnerSpacing.sm))
        StatsGrid(stats = stats)

        Spacer(Modifier.height(PageTurnerSpacing.lg))

        // Preferred length
        if (profile.preferredLength.isNotBlank() && profile.preferredLength != "any") {
            SectionHeader(text = "Preferred length")
            Spacer(Modifier.height(PageTurnerSpacing.xs))
            Text(
                text = profile.preferredLength.replaceFirstChar { it.uppercase() },
                style = PageTurnerType.Body,
                color = PageTurnerColors.OnSurface,
            )
            Spacer(Modifier.height(PageTurnerSpacing.lg))
        }

        // Footnote
        Text(
            text = "Profile last updated after swipe #${profile.lastUpdatedSwipeCount}",
            style = PageTurnerType.BodySmall,
            color = PageTurnerColors.OnSurfaceMuted,
        )

        Spacer(Modifier.height(PageTurnerSpacing.xl))
    }
}

@Composable
private fun StatsGrid(
    stats: SwipeStats,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(PageTurnerSpacing.sm),
    ) {
        StatCard(label = "Swiped", value = stats.totalSwiped.toString(), modifier = Modifier.weight(1f).fillMaxHeight())
        StatCard(label = "Saved", value = stats.totalSaved.toString(), modifier = Modifier.weight(1f).fillMaxHeight())
        StatCard(label = "Wildcards\nkept", value = stats.wildcardKept.toString(), modifier = Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PageTurnerColors.SurfaceVariant)
            .padding(PageTurnerSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value,
            style = PageTurnerType.DetailTitle,
            color = PageTurnerColors.Accent,
        )
        Spacer(Modifier.height(PageTurnerSpacing.xs))
        Text(
            text = label,
            style = PageTurnerType.Label,
            color = PageTurnerColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile building state (>= 10 swipes but AI hasn't returned a profile yet)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BuildingProfileState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(PageTurnerSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Building your profile…",
            style = PageTurnerType.CardTitle,
            color = PageTurnerColors.OnBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(PageTurnerSpacing.sm))
        Text(
            text = "Claude is analysing your swipe history. This can take a few seconds after you swipe.",
            style = PageTurnerType.Body,
            color = PageTurnerColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Insufficient data state (< 10 swipes)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InsufficientDataState(
    swipeCount: Int,
    modifier: Modifier = Modifier,
) {
    val remaining = 10 - swipeCount
    Column(
        modifier = modifier.padding(PageTurnerSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Keep swiping!",
            style = PageTurnerType.CardTitle,
            color = PageTurnerColors.OnBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(PageTurnerSpacing.sm))
        Text(
            text = "Swipe $remaining more book${if (remaining != 1) "s" else ""} and Claude will build your taste profile.",
            style = PageTurnerType.Body,
            color = PageTurnerColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(PageTurnerSpacing.lg))
        Text(
            text = "${swipeCount}/10",
            style = PageTurnerType.DetailTitle,
            color = PageTurnerColors.Accent,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Local components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = PageTurnerType.Label,
        color = PageTurnerColors.OnSurfaceMuted,
        modifier = modifier,
    )
}

@Composable
private fun AvoidedGenreChip(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label.uppercase(),
        style = PageTurnerType.Chip,
        color = PageTurnerColors.Error,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(PageTurnerColors.Error.copy(alpha = 0.12f))
            .padding(horizontal = PageTurnerSpacing.sm, vertical = PageTurnerSpacing.xs),
    )
}
