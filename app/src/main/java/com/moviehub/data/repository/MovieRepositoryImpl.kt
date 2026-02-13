package com.moviehub.data.repository

import com.moviehub.data.local.dao.MovieDao
import com.moviehub.data.local.dao.WatchlistDao
import com.moviehub.data.local.entity.toEntity
import com.moviehub.data.remote.NetworkResult
import com.moviehub.data.remote.api.TmdbApi
import com.moviehub.data.remote.safeApiCall
import com.moviehub.domain.model.Genre
import com.moviehub.domain.model.Movie
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val api: TmdbApi,
    private val movieDao: MovieDao,
    private val watchlistDao: WatchlistDao
) : MovieRepository {

    private var cachedGenres: List<Genre> = emptyList()

    override fun getPopularMovies(page: Int): Flow<NetworkResult<List<Movie>>> = flow {
        emit(NetworkResult.Loading)

        if (page == 1) {
            try {
                val cachedMovies = movieDao.getRecentMovies(20).first()
                if (cachedMovies.isNotEmpty()) {
                    emit(NetworkResult.Success(cachedMovies.map { it.toDomain() }))
                }
            } catch (e: Exception) {
            }
        }

        // Fetch from network
        when (val result = safeApiCall { api.getPopularMovies(page) }) {
            is NetworkResult.Success -> {
                val genres = getGenresInternal()
                val movies = result.data.results.map { dto ->
                    val movieGenres = genres.filter { it.id in (dto.genreIds ?: emptyList()) }
                    dto.toDomain(movieGenres)
                }

                // Cache movies
                if (page == 1) {
                    try {
                        movieDao.insertMovies(movies.map { it.toEntity() })
                    } catch (e: Exception) {

                    }
                }

                emit(NetworkResult.Success(movies))
            }
            is NetworkResult.Error -> emit(NetworkResult.Error(result.message, result.code))
            is NetworkResult.Loading -> {}
        }
    }

    override fun getTrendingMovies(page: Int): Flow<NetworkResult<List<Movie>>> = flow {
        emit(NetworkResult.Loading)
        when (val result = safeApiCall { api.getTrendingMovies(page) }) {
            is NetworkResult.Success -> {
                val genres = getGenresInternal()
                val movies = result.data.results.map { dto ->
                    val movieGenres = genres.filter { it.id in (dto.genreIds ?: emptyList()) }
                    dto.toDomain(movieGenres)
                }
                emit(NetworkResult.Success(movies))
            }
            is NetworkResult.Error -> emit(result)
            is NetworkResult.Loading -> {}
        }
    }

    override fun searchMovies(query: String, page: Int): Flow<NetworkResult<List<Movie>>> = flow {
        emit(NetworkResult.Loading)

        // Fetch from API first
        when (val result = safeApiCall { api.searchMovies(query, page) }) {
            is NetworkResult.Success -> {
                val genres = getGenresInternal()
                val movies = result.data.results.map { dto ->
                    val movieGenres = genres.filter { it.id in (dto.genreIds ?: emptyList()) }
                    dto.toDomain(movieGenres)
                }
                emit(NetworkResult.Success(movies))
            }
            is NetworkResult.Error -> emit(result)
            is NetworkResult.Loading -> {}
        }
    }

    override fun getMovieDetails(movieId: Int): Flow<NetworkResult<Movie>> = flow {
        emit(NetworkResult.Loading)

        try {
            val cached = movieDao.getMovieById(movieId)
            if (cached != null) {
                emit(NetworkResult.Success(cached.toDomain()))
            }
        } catch (e: Exception) {

        }

        // Fetch from network
        when (val detailsResult = safeApiCall { api.getMovieDetails(movieId) }) {
            is NetworkResult.Success -> {
                when (val creditsResult = safeApiCall { api.getMovieCredits(movieId) }) {
                    is NetworkResult.Success -> {
                        val cast = creditsResult.data.cast.take(10).map { it.toDomain() }
                        val movie = detailsResult.data.toDomain(cast)

                        // Cache the movie
                        try {
                            movieDao.insertMovie(movie.toEntity())
                        } catch (e: Exception) {

                        }

                        emit(NetworkResult.Success(movie))
                    }
                    is NetworkResult.Error -> {
                        // Still emit movie without cast if credits fail
                        emit(NetworkResult.Success(detailsResult.data.toDomain()))
                    }
                    is NetworkResult.Loading -> {}
                }
            }
            is NetworkResult.Error -> emit(detailsResult)
            is NetworkResult.Loading -> {}
        }
    }

    override fun getSimilarMovies(movieId: Int): Flow<NetworkResult<List<Movie>>> = flow {
        emit(NetworkResult.Loading)
        when (val result = safeApiCall { api.getSimilarMovies(movieId) }) {
            is NetworkResult.Success -> {
                val genres = getGenresInternal()
                val movies = result.data.results.map { dto ->
                    val movieGenres = genres.filter { it.id in (dto.genreIds ?: emptyList()) }
                    dto.toDomain(movieGenres)
                }
                emit(NetworkResult.Success(movies))
            }
            is NetworkResult.Error -> emit(result)
            is NetworkResult.Loading -> {}
        }
    }

    override fun getGenres(): Flow<NetworkResult<List<Genre>>> = flow {
        emit(NetworkResult.Loading)

        if (cachedGenres.isNotEmpty()) {
            emit(NetworkResult.Success(cachedGenres))
            return@flow
        }

        when (val result = safeApiCall { api.getGenres() }) {
            is NetworkResult.Success -> {
                cachedGenres = result.data.genres.map { it.toDomain() }
                emit(NetworkResult.Success(cachedGenres))
            }
            is NetworkResult.Error -> emit(result)
            is NetworkResult.Loading -> {}
        }
    }

    override fun discoverMovies(
        genreIds: List<Int>?,
        sortBy: String,
        page: Int
    ): Flow<NetworkResult<List<Movie>>> = flow {
        emit(NetworkResult.Loading)
        val genreString = genreIds?.joinToString(",")
        when (val result = safeApiCall { api.discoverMovies(genreString, sortBy, page) }) {
            is NetworkResult.Success -> {
                val genres = getGenresInternal()
                val movies = result.data.results.map { dto ->
                    val movieGenres = genres.filter { it.id in (dto.genreIds ?: emptyList()) }
                    dto.toDomain(movieGenres)
                }
                emit(NetworkResult.Success(movies))
            }
            is NetworkResult.Error -> emit(result)
            is NetworkResult.Loading -> {}
        }
    }

    override suspend fun refreshMovies() {
        // Clean old cache (older than 7 days)
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        try {
            movieDao.deleteOldMovies(weekAgo)
        } catch (e: Exception) {

        }
    }

    override fun observeCachedMovie(movieId: Int): Flow<Movie?> {
        return movieDao.observeMovieById(movieId).map { it?.toDomain() }
    }

    private suspend fun getGenresInternal(): List<Genre> {
        if (cachedGenres.isNotEmpty()) return cachedGenres

        when (val result = safeApiCall { api.getGenres() }) {
            is NetworkResult.Success -> {
                cachedGenres = result.data.genres.map { it.toDomain() }
            }
            else -> {}
        }
        return cachedGenres
    }
}
