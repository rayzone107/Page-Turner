package com.pageturner.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerType

private enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    DISCOVER(
        route = AppRoute.SWIPE_DECK,
        label = "Discover",
        icon = Icons.Default.MenuBook,
        selectedIcon = Icons.Default.MenuBook,
    ),
    MY_TASTE(
        route = AppRoute.TASTE_PROFILE,
        label = "My Taste",
        icon = Icons.Default.Person,
        selectedIcon = Icons.Default.Person,
    ),
    READING_LIST(
        route = AppRoute.READING_LIST,
        label = "Reading List",
        icon = Icons.Default.BookmarkBorder,
        selectedIcon = Icons.Default.Bookmark,
    ),
}

@Composable
internal fun PageTurnerBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = PageTurnerColors.Surface,
    ) {
        BottomNavItem.entries.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(text = item.label, style = PageTurnerType.Label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PageTurnerColors.Accent,
                    selectedTextColor = PageTurnerColors.Accent,
                    unselectedIconColor = PageTurnerColors.OnSurfaceMuted,
                    unselectedTextColor = PageTurnerColors.OnSurfaceMuted,
                    indicatorColor = PageTurnerColors.Accent.copy(alpha = 0.15f),
                ),
            )
        }
    }
}

