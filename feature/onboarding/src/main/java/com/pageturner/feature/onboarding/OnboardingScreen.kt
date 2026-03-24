package com.pageturner.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pageturner.core.domain.model.Genre
import com.pageturner.core.domain.model.ReadingLength
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerSpacing
import com.pageturner.core.ui.theme.PageTurnerType
import kotlinx.coroutines.launch

/**
 * Onboarding screen — shown exactly once on first launch.
 *
 * Collects the cold-start seed: genre preferences (multi-select) and preferred
 * book length (single-select). No back button. No skip.
 *
 * @param onNavigateToSwipeDeck called when the user confirms their preferences.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onNavigateToSwipeDeck: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Collect side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                OnboardingSideEffect.NavigateToSwipeDeck -> onNavigateToSwipeDeck()
            }
        }
    }

    // Entrance animation for the wordmark
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkScale = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) {
        launch {
            wordmarkAlpha.animateTo(1f, animationSpec = tween(durationMillis = 600))
        }
        launch {
            wordmarkScale.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Scaffold(
        containerColor = PageTurnerColors.Background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PageTurnerSpacing.lg, vertical = PageTurnerSpacing.md)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick  = { viewModel.onIntent(OnboardingIntent.Confirm) },
                    enabled  = uiState.canProceed && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = PageTurnerColors.Accent,
                        contentColor           = PageTurnerColors.OnAccent,
                        disabledContainerColor = PageTurnerColors.Accent.copy(alpha = 0.3f),
                        disabledContentColor   = PageTurnerColors.OnAccent.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text     = if (uiState.isLoading) "Setting up…" else "Start discovering",
                        style    = PageTurnerType.Body,
                        modifier = Modifier.padding(vertical = PageTurnerSpacing.xs)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = PageTurnerSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(PageTurnerSpacing.xl))

            // Animated wordmark
            Text(
                text     = "PageTurner",
                style    = PageTurnerType.AppTitle.copy(fontSize = androidx.compose.ui.unit.sp(32f)),
                color    = PageTurnerColors.OnBackground,
                modifier = Modifier
                    .alpha(wordmarkAlpha.value)
                    .scale(wordmarkScale.value)
            )

            Spacer(modifier = Modifier.height(PageTurnerSpacing.sm))

            Text(
                text      = "Your next great read is waiting.",
                style     = PageTurnerType.Body,
                color     = PageTurnerColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier  = Modifier.alpha(wordmarkAlpha.value)
            )

            Spacer(modifier = Modifier.height(PageTurnerSpacing.xl))

            // ── Genre section ────────────────────────────────────────────
            SectionLabel(text = "What genres interest you?")
            Spacer(modifier = Modifier.height(PageTurnerSpacing.md))

            FlowRow(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PageTurnerSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PageTurnerSpacing.sm)
            ) {
                uiState.genres.forEach { genre ->
                    val isSelected = genre in uiState.selectedGenres
                    SelectableChip(
                        label      = genre.displayName,
                        isSelected = isSelected,
                        onClick    = { viewModel.onIntent(OnboardingIntent.ToggleGenre(genre)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(PageTurnerSpacing.xl))

            // ── Length section ───────────────────────────────────────────
            SectionLabel(text = "How long do you like your books?")
            Spacer(modifier = Modifier.height(PageTurnerSpacing.md))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PageTurnerSpacing.sm)
            ) {
                ReadingLength.entries.forEach { length ->
                    val isSelected = length == uiState.selectedLength
                    SelectableChip(
                        label      = length.displayName,
                        isSelected = isSelected,
                        onClick    = { viewModel.onIntent(OnboardingIntent.SelectLength(length)) },
                        modifier   = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(PageTurnerSpacing.xl))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text,
        style    = PageTurnerType.Label,
        color    = PageTurnerColors.OnSurfaceMuted,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SelectableChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor     = if (isSelected) PageTurnerColors.Accent else PageTurnerColors.SurfaceVariant
    val textColor   = if (isSelected) PageTurnerColors.OnAccent else PageTurnerColors.OnSurfaceMuted
    val borderColor = if (isSelected) PageTurnerColors.Accent else PageTurnerColors.CardBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = PageTurnerSpacing.md, vertical = PageTurnerSpacing.sm)
            .semantics {
                role             = Role.Checkbox
                selected         = isSelected
                stateDescription = if (isSelected) "Selected" else "Not selected"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text      = label,
            style     = PageTurnerType.Chip,
            color     = textColor,
            textAlign = TextAlign.Center
        )
    }
}
