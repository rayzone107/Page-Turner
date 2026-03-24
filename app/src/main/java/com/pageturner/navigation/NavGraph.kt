package com.pageturner.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.pageturner.feature.bookdetail.BookDetailScreen
import com.pageturner.feature.onboarding.OnboardingScreen
import com.pageturner.feature.readinglist.ReadingListScreen
import com.pageturner.feature.swipedeck.SwipeDeckScreen
import com.pageturner.feature.tasteprofile.TasteProfileScreen
import com.pageturner.main.MainViewModel
import com.pageturner.main.SplashScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PageTurnerNavGraph(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

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
