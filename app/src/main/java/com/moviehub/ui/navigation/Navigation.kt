package com.moviehub.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.moviehub.ui.screens.detail.MovieDetailScreen
import com.moviehub.ui.screens.favorites.FavoritesScreen
import com.moviehub.ui.screens.login.LoginScreen
import com.moviehub.ui.screens.movies.MoviesScreen
import com.moviehub.ui.screens.profile.ProfileScreen
import com.moviehub.ui.screens.search.SearchScreen
import com.moviehub.ui.screens.signup.SignUpScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Movies : Screen("movies")
    object MovieDetail : Screen("movie/{movieId}") {
        fun createRoute(movieId: Int) = "movie/$movieId"
    }
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object Profile : Screen("profile")
}

private val bottomBarScreens = listOf(
    Screen.Movies.route,
    Screen.Favorites.route,
    Screen.Profile.route
)

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun MovieHubNavigation(
    navController: NavHostController,
    startDestination: String
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem("Movies", Icons.Default.Home, Screen.Movies.route),
        BottomNavItem("Favorites", Icons.Default.Favorite, Screen.Favorites.route),
        BottomNavItem("Profile", Icons.Default.Person, Screen.Profile.route),
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarScreens) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Movies.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToSignUp = {
                        navController.navigate(Screen.SignUp.route)
                    },
                    onLoginSuccess = {
                        navController.navigate(Screen.Movies.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onContinueAsGuest = {
                        navController.navigate(Screen.Movies.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    },
                    onSignUpSuccess = {
                        navController.navigate(Screen.Movies.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Movies.route) {
                MoviesScreen(
                    onMovieClick = { movieId ->
                        navController.navigate(Screen.MovieDetail.createRoute(movieId))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    }
                )
            }

            composable(
                route = Screen.MovieDetail.route,
                arguments = listOf(navArgument("movieId") { type = NavType.IntType })
            ) {
                MovieDetailScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onBackClick = { navController.popBackStack() },
                    onMovieClick = { movieId ->
                        navController.navigate(Screen.MovieDetail.createRoute(movieId))
                    }
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onMovieClick = { movieId ->
                        navController.navigate(Screen.MovieDetail.createRoute(movieId))
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}