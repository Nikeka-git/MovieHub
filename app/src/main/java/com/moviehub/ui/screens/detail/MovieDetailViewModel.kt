package com.moviehub.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.data.firebase.AuthService
import com.moviehub.data.firebase.ReviewsService
import com.moviehub.data.remote.NetworkResult
import com.moviehub.data.repository.WatchlistRepository
import com.moviehub.domain.model.Movie
import com.moviehub.domain.model.Review
import com.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val watchlistRepository: WatchlistRepository,
    private val reviewsService: ReviewsService,
    private val authService: AuthService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: Int = savedStateHandle.get<String>("movieId")?.toIntOrNull() ?: 0

    private val _uiState = MutableStateFlow<MovieDetailUiState>(MovieDetailUiState.Loading)
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    val isInWatchlist: StateFlow<Boolean> = authService.currentUser?.let { user ->
        watchlistRepository.isInWatchlist(movieId, user.uid)
    } ?: MutableStateFlow(false)

    val averageRating: StateFlow<Float> = _reviews.map { reviews ->
        if (reviews.isEmpty()) 0f
        else reviews.map { it.rating }.average().toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    init {
        loadMovieDetails()
        observeReviews()
    }

    private fun loadMovieDetails() {
        viewModelScope.launch {
            movieRepository.getMovieDetails(movieId).collect { result ->
                _uiState.value = when (result) {
                    is NetworkResult.Loading -> MovieDetailUiState.Loading
                    is NetworkResult.Success -> MovieDetailUiState.Success(result.data)
                    is NetworkResult.Error -> MovieDetailUiState.Error(result.message)
                }
            }
        }
    }

    private fun observeReviews() {
        viewModelScope.launch {
            reviewsService.observeMovieReviews(movieId).collect { reviews ->
                _reviews.value = reviews
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            authService.currentUser?.let { user ->
                if (isInWatchlist.value) {
                    watchlistRepository.removeFromWatchlist(movieId, user.uid)
                } else {
                    watchlistRepository.addToWatchlist(movieId, user.uid)
                }
            }
        }
    }

    fun addReview(rating: Float, comment: String) {
        viewModelScope.launch {
            authService.currentUser?.let { user ->
                val review = Review(
                    movieId = movieId,
                    userId = user.uid,
                    userName = user.displayName ?: "Anonymous",
                    rating = rating,
                    comment = comment
                )
                reviewsService.addReview(review)
            }
        }
    }

    fun refresh() {
        loadMovieDetails()
    }
}

sealed class MovieDetailUiState {
    object Loading : MovieDetailUiState()
    data class Success(val movie: Movie) : MovieDetailUiState()
    data class Error(val message: String) : MovieDetailUiState()
}