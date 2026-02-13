package com.moviehub.ui.screens.favorites

import androidx.compose.runtime.Composable
import com.moviehub.ui.screens.watchlist.WatchlistScreen

@Composable
fun FavoritesScreen(onMovieClick: (Int) -> Unit) {
    WatchlistScreen(onMovieClick)
}
