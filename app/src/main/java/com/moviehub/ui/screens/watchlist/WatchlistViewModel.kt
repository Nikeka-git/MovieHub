package com.moviehub.ui.screens.watchlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.data.firebase.AuthService
import com.moviehub.data.repository.WatchlistRepository
import com.moviehub.domain.model.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val authService: AuthService
) : ViewModel() {

    companion object {
        private const val TAG = "WatchlistViewModel"
    }

    val uiState: StateFlow<WatchlistUiState> = authService
        .observeAuthState()
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(WatchlistUiState.NotLoggedIn)
            } else {
                watchlistRepository.getWatchlistMovies(user.uid)
                    .map { movies ->
                        if (movies.isEmpty()) WatchlistUiState.Empty
                        else WatchlistUiState.Success(movies)
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WatchlistUiState.Loading
        )

    fun removeFromWatchlist(movieId: Int) {
        viewModelScope.launch {
            val user = authService.currentUser ?: return@launch
            try {
                watchlistRepository.removeFromWatchlist(movieId, user.uid)
                Log.d(TAG, "Removed movie $movieId from watchlist")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from watchlist", e)
            }
        }
    }
}

sealed class WatchlistUiState {
    object Loading : WatchlistUiState()
    object NotLoggedIn : WatchlistUiState()
    object Empty : WatchlistUiState()
    data class Success(val movies: List<Movie>) : WatchlistUiState()
}