package com.moviehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                    val currentUser by authService.observeAuthState().collectAsState(initial = null)

                    val startDestination = if (currentUser != null) {
                        Screen.Movies.route
                    } else {
                        Screen.Login.route
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