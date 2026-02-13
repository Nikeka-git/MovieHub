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
import com.moviehub.data.repository.MovieRepository
import com.moviehub.utils.AppLogger
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

    companion object {
        private const val TAG = "MovieDetailViewModel"
    }

    private val movieId: Int = savedStateHandle.get<Int>("movieId") ?: 0

    private val _uiState = MutableStateFlow<MovieDetailUiState>(MovieDetailUiState.Loading)
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    private val _reviewSubmitState = MutableStateFlow<ReviewSubmitState>(ReviewSubmitState.Idle)
    val reviewSubmitState: StateFlow<ReviewSubmitState> = _reviewSubmitState.asStateFlow()

    val reviews: StateFlow<List<Review>> = reviewsService
        .observeMovieReviews(movieId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isInWatchlist: StateFlow<Boolean> = authService.currentUser?.let { user ->
        watchlistRepository.isInWatchlist(movieId, user.uid)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )
    } ?: MutableStateFlow(false)

    val averageRating: StateFlow<Float> = reviews.map { reviewsList ->
        if (reviewsList.isEmpty()) 0f
        else reviewsList.map { it.rating }.average().toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    init {
        loadMovieDetails()
    }

    private fun loadMovieDetails() {
        viewModelScope.launch {
            try {
                AppLogger.d(TAG, "Loading movie details for ID: $movieId")
                movieRepository.getMovieDetails(movieId).collect { result ->
                    _uiState.value = when (result) {
                        is NetworkResult.Loading -> {
                            AppLogger.d(TAG, "Loading movie details...")
                            MovieDetailUiState.Loading
                        }
                        is NetworkResult.Success -> {
                            AppLogger.d(TAG, "Successfully loaded movie: ${result.data.title}")
                            MovieDetailUiState.Success(result.data)
                        }
                        is NetworkResult.Error -> {
                            AppLogger.e(TAG, "Error loading movie: ${result.message}")
                            MovieDetailUiState.Error(result.message)
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Exception loading movie details", e)
                _uiState.value = MovieDetailUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            authService.currentUser?.let { user ->
                try {
                    if (isInWatchlist.value) {
                        watchlistRepository.removeFromWatchlist(movieId, user.uid)
                        AppLogger.d(TAG, "Removed movie $movieId from watchlist")
                    } else {
                        watchlistRepository.addToWatchlist(movieId, user.uid)
                        AppLogger.d(TAG, "Added movie $movieId to watchlist")
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error toggling watchlist", e)
                }
            } ?: run {
                AppLogger.w(TAG, "User not logged in, cannot toggle watchlist")
            }
        }
    }

    fun addReview(rating: Float, comment: String) {
        AppLogger.d(TAG, "addReview called - rating: $rating, comment length: ${comment.length}")

        viewModelScope.launch {
            try {
                _reviewSubmitState.value = ReviewSubmitState.Loading
                AppLogger.d(TAG, "Setting review submit state to Loading")

                val currentUser = authService.currentUser
                if (currentUser == null) {
                    AppLogger.e(TAG, "Cannot add review: User is not logged in")
                    _reviewSubmitState.value = ReviewSubmitState.Error("Please log in to submit a review")
                    return@launch
                }

                AppLogger.d(TAG, "User logged in: ${currentUser.uid}, displayName: ${currentUser.displayName}")

                val review = Review(
                    movieId = movieId,
                    userId = currentUser.uid,
                    userName = currentUser.displayName ?: "Anonymous",
                    rating = rating,
                    comment = comment
                )

                AppLogger.d(TAG, "Created review object: $review")
                AppLogger.d(TAG, "Calling reviewsService.addReview...")

                val result = reviewsService.addReview(review)

                result.fold(
                    onSuccess = {
                        AppLogger.d(TAG, "✅ Review added successfully!")
                        _reviewSubmitState.value = ReviewSubmitState.Success("Review submitted successfully!")

                        kotlinx.coroutines.delay(3000)
                        _reviewSubmitState.value = ReviewSubmitState.Idle
                    },
                    onFailure = { error ->
                        AppLogger.e(TAG, "❌ Failed to add review: ${error.message}", error)
                        _reviewSubmitState.value = ReviewSubmitState.Error(
                            error.message ?: "Failed to submit review"
                        )
                    }
                )
            } catch (e: Exception) {
                AppLogger.e(TAG, "❌ Exception in addReview", e)
                _reviewSubmitState.value = ReviewSubmitState.Error(
                    "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    fun resetReviewSubmitState() {
        _reviewSubmitState.value = ReviewSubmitState.Idle
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

sealed class ReviewSubmitState {
    object Idle : ReviewSubmitState()
    object Loading : ReviewSubmitState()
    data class Success(val message: String) : ReviewSubmitState()
    data class Error(val message: String) : ReviewSubmitState()
}