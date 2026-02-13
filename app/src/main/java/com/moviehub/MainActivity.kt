package com.moviehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.moviehub.data.firebase.AuthService
import com.moviehub.ui.navigation.MovieHubNavigation
import com.moviehub.ui.navigation.Screen
import com.moviehub.ui.theme.MovieHubTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authService: AuthService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MovieHubTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val startDestination = remember {
                        if (authService.isUserLoggedIn()) {
                            Screen.Movies.route
                        } else {
                            Screen.Login.route
                        }
                    }

                    LaunchedEffect(Unit) {
                        authService.observeAuthState().collect { user ->
                            if (user == null && 
                                navController.currentDestination?.route != Screen.Login.route) {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }

                    MovieHubNavigation(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
