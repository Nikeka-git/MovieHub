package com.moviehub.ui.screens.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.data.remote.NetworkResult
import com.moviehub.domain.model.Movie
import com.moviehub.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MoviesUiState>(MoviesUiState.Loading)
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPage = 1
    private var canLoadMore = true

    init {
        loadMovies()
    }

    private fun loadMovies() {
        viewModelScope.launch {
            movieRepository.getPopularMovies(currentPage).collect { result ->
                _uiState.value = when (result) {
                    is NetworkResult.Loading -> MoviesUiState.Loading
                    is NetworkResult.Success -> {
                        if (result.data.isEmpty()) {
                            MoviesUiState.Empty
                        } else {
                            MoviesUiState.Success(result.data)
                        }
                    }
                    is NetworkResult.Error -> MoviesUiState.Error(result.message)
                }
            }
        }
    }

    fun loadMoreMovies() {
        if (!canLoadMore || _isLoadingMore.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            currentPage++

            movieRepository.getPopularMovies(currentPage).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val currentMovies = (_uiState.value as? MoviesUiState.Success)?.movies ?: emptyList()
                        _uiState.value = MoviesUiState.Success(currentMovies + result.data)
                        canLoadMore = result.data.isNotEmpty()
                    }
                    is NetworkResult.Error -> {
                        canLoadMore = false
                    }
                    else -> {}
                }
                _isLoadingMore.value = false
            }
        }
    }

    fun refresh() {
        currentPage = 1
        canLoadMore = true
        loadMovies()
    }
}

sealed class MoviesUiState {
    object Loading : MoviesUiState()
    data class Success(val movies: List<Movie>) : MoviesUiState()
    data class Error(val message: String) : MoviesUiState()
    object Empty : MoviesUiState()
}
