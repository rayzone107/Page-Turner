package com.pageturner

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pageturner.core.ui.theme.PageTurnerColors
import com.pageturner.core.ui.theme.PageTurnerType
import com.pageturner.feature.bookdetail.BookDetailScreen
import com.pageturner.feature.onboarding.OnboardingScreen
import com.pageturner.feature.readinglist.ReadingListScreen
import com.pageturner.feature.swipedeck.SwipeDeckScreen
import com.pageturner.feature.tasteprofile.TasteProfileScreen

// ─────────────────────────────────────────────────────────────────────────────
// Route constants
// ─────────────────────────────────────────────────────────────────────────────

object AppRoute {
    const val ONBOARDING = "onboarding"
    const val SWIPE_DECK = "swipedeck"
    const val TASTE_PROFILE = "tasteprofile"
    const val READING_LIST = "readinglist"
    const val BOOK_DETAIL = "bookdetail/{bookKey}"

    /** URL-encodes [bookKey] to safely handle Open Library keys that contain '/'. */
    fun bookDetail(bookKey: String) = "bookdetail/${Uri.encode(bookKey)}"
}

private val BOTTOM_NAV_ROUTES = setOf(
    AppRoute.SWIPE_DECK,
    AppRoute.TASTE_PROFILE,
    AppRoute.READING_LIST,
)

// ─────────────────────────────────────────────────────────────────────────────
// Nav graph
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PageTurnerNavGraph(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current

    if (startDestination == null) {
        SplashScreen()
        return
    }

    SharedTransitionLayout {
        Scaffold(
            containerColor = PageTurnerColors.Background,
            bottomBar = {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                if (currentRoute in BOTTOM_NAV_ROUTES) {
                    PageTurnerBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination!!,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(AppRoute.ONBOARDING) {
                    OnboardingScreen(
                        onNavigateToSwipeDeck = {
                            navController.navigate(AppRoute.SWIPE_DECK) {
                                popUpTo(AppRoute.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }

                composable(AppRoute.SWIPE_DECK) {
                    SwipeDeckScreen(
                        onNavigateToDetail = { bookKey ->
                            navController.navigate(AppRoute.bookDetail(bookKey))
                        }
                    )
                }

                composable(AppRoute.TASTE_PROFILE) {
                    TasteProfileScreen()
                }

                composable(AppRoute.READING_LIST) {
                    ReadingListScreen(
                        onNavigateToDetail = { bookKey ->
                            navController.navigate(AppRoute.bookDetail(bookKey))
                        }
                    )
                }

                composable(
                    route = AppRoute.BOOK_DETAIL,
                    arguments = listOf(navArgument("bookKey") { type = NavType.StringType }),
                ) {
                    BookDetailScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenUrl = { url ->
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            )
                        },
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom navigation bar
// ─────────────────────────────────────────────────────────────────────────────

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
private fun PageTurnerBottomBar(
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

// ─────────────────────────────────────────────────────────────────────────────
// Splash screen (shown while onboarding state loads from Room)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageTurnerColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.pageturner_logo),
                contentDescription = "Page Turner logo",
                modifier = Modifier.size(140.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Page Turner",
                style = PageTurnerType.AppTitle,
                color = PageTurnerColors.Accent,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your next great read is waiting.",
                style = PageTurnerType.Body,
                color = PageTurnerColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
