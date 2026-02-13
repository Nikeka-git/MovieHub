package com.moviehub.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.data.remote.NetworkResult
import com.moviehub.domain.model.Genre
import com.moviehub.domain.model.Movie
import com.moviehub.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenres = MutableStateFlow<Set<Int>>(emptySet())
    val selectedGenres: StateFlow<Set<Int>> = _selectedGenres.asStateFlow()

    private val _sortBy = MutableStateFlow("popularity.desc")
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadGenres()
        setupDebouncedSearch()
    }

    private fun loadGenres() {
        viewModelScope.launch {
            movieRepository.getGenres().collect { result ->
                if (result is NetworkResult.Success) {
                    _genres.value = result.data
                }
            }
        }
    }

    private fun setupDebouncedSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500) // Debounce for 500ms
                .distinctUntilChanged()
                .filter { it.length >= 2 || it.isEmpty() }
                .collect { query ->
                    if (query.isEmpty()) {
                        _uiState.value = SearchUiState.Idle
                    } else {
                        performSearch(query)
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _uiState.value = SearchUiState.Idle
        } else {
            _uiState.value = SearchUiState.Loading
        }
    }

    fun toggleGenre(genreId: Int) {
        _selectedGenres.value = if (genreId in _selectedGenres.value) {
            _selectedGenres.value - genreId
        } else {
            _selectedGenres.value + genreId
        }
        applyFilters()
    }

    fun setSortBy(sortBy: String) {
        _sortBy.value = sortBy
        applyFilters()
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            movieRepository.searchMovies(query, 1).collect { result ->
                _uiState.value = when (result) {
                    is NetworkResult.Loading -> SearchUiState.Loading
                    is NetworkResult.Success -> {
                        if (result.data.isEmpty()) {
                            SearchUiState.Empty(query)
                        } else {
                            SearchUiState.Success(result.data)
                        }
                    }
                    is NetworkResult.Error -> SearchUiState.Error(result.message)
                }
            }
        }
    }

    private fun applyFilters() {
        if (_selectedGenres.value.isEmpty() && _searchQuery.value.isEmpty()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading

            val genreIds = if (_selectedGenres.value.isNotEmpty()) {
                _selectedGenres.value.toList()
            } else null

            movieRepository.discoverMovies(
                genreIds = genreIds,
                sortBy = _sortBy.value,
                page = 1
            ).collect { result ->
                _uiState.value = when (result) {
                    is NetworkResult.Loading -> SearchUiState.Loading
                    is NetworkResult.Success -> {
                        if (result.data.isEmpty()) {
                            SearchUiState.Empty("")
                        } else {
                            SearchUiState.Success(result.data)
                        }
                    }
                    is NetworkResult.Error -> SearchUiState.Error(result.message)
                }
            }
        }
    }

    fun clearFilters() {
        _selectedGenres.value = emptySet()
        _sortBy.value = "popularity.desc"
        _searchQuery.value = ""
        _uiState.value = SearchUiState.Idle
    }
}

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val movies: List<Movie>) : SearchUiState()
    data class Empty(val query: String) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}