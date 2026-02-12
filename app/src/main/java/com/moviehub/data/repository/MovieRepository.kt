package com.moviehub.data.repository

import com.moviehub.data.remote.NetworkResult
import com.moviehub.domain.model.Genre
import com.moviehub.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getPopularMovies(page: Int): Flow<NetworkResult<List<Movie>>>
    fun getTrendingMovies(page: Int): Flow<NetworkResult<List<Movie>>>
    fun searchMovies(query: String, page: Int): Flow<NetworkResult<List<Movie>>>
    fun getMovieDetails(movieId: Int): Flow<NetworkResult<Movie>>
    fun getSimilarMovies(movieId: Int): Flow<NetworkResult<List<Movie>>>
    fun getGenres(): Flow<NetworkResult<List<Genre>>>
    fun discoverMovies(genreIds: List<Int>?, sortBy: String, page: Int): Flow<NetworkResult<List<Movie>>>

    // Offline-first
    suspend fun refreshMovies()
    fun observeCachedMovie(movieId: Int): Flow<Movie?>
}