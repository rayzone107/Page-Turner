package com.pageturner.core.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerType

/**
 * PageTurner top app bar.
 *
 * [onBackClick] is null on root screens (swipe deck, taste profile, reading list)
 * and non-null on leaf screens (book detail). When non-null, a back arrow is shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageTurnerTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = PageTurnerType.AppTitle,
                color = PageTurnerColors.OnBackground
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = PageTurnerColors.OnBackground
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PageTurnerColors.Background,
            titleContentColor = PageTurnerColors.OnBackground
        ),
        modifier = modifier
    )
}
