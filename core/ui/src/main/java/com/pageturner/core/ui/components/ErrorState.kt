package com.pageturner.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerSpacing
import com.pageturner.core.ui.theme.PageTurnerType

/**
 * Full-screen (or scoped) error state with a retry action.
 * Used when a critical operation fails and the user can recover.
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(PageTurnerSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = PageTurnerType.CardTitle,
            color = PageTurnerColors.OnBackground
        )
        Spacer(modifier = Modifier.height(PageTurnerSpacing.sm))
        Text(
            text = message,
            style = PageTurnerType.Body,
            color = PageTurnerColors.OnSurfaceMuted
        )
        Spacer(modifier = Modifier.height(PageTurnerSpacing.lg))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = PageTurnerColors.Accent,
                contentColor = PageTurnerColors.OnAccent
            )
        ) {
            Text(text = "Try again", style = PageTurnerType.Body)
        }
    }
}
