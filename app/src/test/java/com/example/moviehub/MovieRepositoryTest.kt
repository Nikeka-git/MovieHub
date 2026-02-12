package com.example.moviehub

import com.moviehub.data.local.dao.MovieDao
import com.moviehub.data.local.dao.*
import com.moviehub.data.remote.NetworkResult
import com.moviehub.data.remote.api.TmdbApi
import com.moviehub.data.remote.dto.*
import com.moviehub.data.repository.MovieRepositoryImpl
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.compareTo

class MovieRepositoryTest {

    private lateinit var repository: MovieRepositoryImpl
    private lateinit var api: TmdbApi
    private lateinit var movieDao: MovieDao
    private lateinit var watchlistDao: WatchlistDao

    @Before
    fun setup() {
        api = mockk()
        movieDao = mockk(relaxed = true)
        watchlistDao = mockk(relaxed = true)
        repository = MovieRepositoryImpl(api, movieDao, watchlistDao)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getPopularMovies returns success when API call succeeds`() = runTest {
        // Given
        val mockMovies = listOf(
            MovieDto(
                id = 1,
                title = "Test Movie",
                overview = "Test Overview",
                posterPath = "/test.jpg",
                backdropPath = "/backdrop.jpg",
                releaseDate = "2024-01-01",
                voteAverage = 7.5,
                voteCount = 100,
                genreIds = listOf(1, 2)
            )
        )
        val mockResponse = MovieListResponse(
            page = 1,
            results = mockMovies,
            totalPages = 10,
            totalResults = 100
        )

        coEvery { api.getPopularMovies(any(), any()) } returns Response.success(mockResponse)
        coEvery { api.getGenres(any()) } returns Response.success(
            GenreListResponse(listOf(
                GenreDto(1, "Action"),
                GenreDto(2, "Drama")
            ))
        )
        coEvery { movieDao.getRecentMovies(any()) } returns flowOf(emptyList())

        // When
        val flow = repository.getPopularMovies(1)
        val results = mutableListOf<NetworkResult<*>>()
        flow.collect { results.add(it) }

        // Then
        assertTrue(results.any { it is NetworkResult.Success })
        val successResult = results.filterIsInstance<NetworkResult.Success<List<*>>>().first()
        assertEquals(1, successResult.data.size)

        coVerify { movieDao.insertMovies(any()) }
    }

    @Test
    fun `getPopularMovies returns error when API call fails`() = runTest {
        // Given
        coEvery { api.getPopularMovies(any(), any()) } returns Response.error(
            404,
            mockk(relaxed = true)
        )
        coEvery { movieDao.getRecentMovies(any()) } returns flowOf(emptyList())

        // When
        val flow = repository.getPopularMovies(1)
        val results = mutableListOf<NetworkResult<*>>()
        flow.collect { results.add(it) }

        // Then
        assertTrue(results.any { it is NetworkResult.Error })
    }

    @Test
    fun `offline-first behavior loads cached data first`() = runTest {
        // Given
        val cachedMovies = listOf(
            mockk<MovieEntity>(relaxed = true) {
                every { id } returns 1
                every { title } returns "Cached Movie"
                every { toDomain(any()) } returns mockk(relaxed = true)
            }
        )

        coEvery { movieDao.getRecentMovies(any()) } returns flowOf(cachedMovies)
        coEvery { api.getPopularMovies(any(), any()) } returns Response.success(
            MovieListResponse(1, emptyList(), 1, 0)
        )

        // When
        val flow = repository.getPopularMovies(1)
        val results = mutableListOf<NetworkResult<*>>()
        flow.collect { results.add(it) }

        // Then
        // Should emit cached data before network data
        assertTrue(results.size >= 2)
    }

    @Test
    fun `searchMovies uses local cache when offline`() = runTest {
        // Given
        val query = "test"
        val cachedResults = listOf(
            mockk<MovieEntity>(relaxed = true) {
                every { toDomain(any()) } returns mockk(relaxed = true)
            }
        )

        coEvery { movieDao.searchMovies(query) } returns flowOf(cachedResults)
        coEvery { api.searchMovies(any(), any(), any()) } throws Exception("Network error")

        // When
        val flow = repository.searchMovies(query, 1)
        val results = mutableListOf<NetworkResult<*>>()
        flow.collect { results.add(it) }

        // Then
        assertTrue(results.any { it is NetworkResult.Success })
    }

    @Test
    fun `refreshMovies deletes old cache data`() = runTest {
        // Given
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        coEvery { movieDao.deleteOldMovies(any()) } just Runs

        // When
        repository.refreshMovies()

        // Then
        coVerify { movieDao.deleteOldMovies(match { it compareTo weekAgo + 1000 }) }
    }
}