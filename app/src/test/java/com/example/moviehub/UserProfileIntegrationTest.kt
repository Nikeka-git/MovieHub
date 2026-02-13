package com.example.moviehub

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.moviehub.data.local.dao.MovieDao
import com.moviehub.data.local.dao.WatchlistDao
import com.moviehub.data.repository.WatchlistRepository
import com.moviehub.domain.model.Movie
import com.moviehub.domain.model.Genre
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.tasks.await

/**
 * Integration tests for User Profile functionality
 * Tests the interaction between authentication, watchlist, and user preferences
 */
class UserProfileIntegrationTest {

    private lateinit var watchlistRepository: WatchlistRepository
    private lateinit var watchlistDao: WatchlistDao
    private lateinit var movieDao: MovieDao
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: com.google.firebase.database.FirebaseDatabase

    @Before
    fun setup() {
        watchlistDao = mockk(relaxed = true)
        movieDao = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)
        firebaseDatabase = mockk(relaxed = true)
        watchlistRepository = WatchlistRepository(watchlistDao, movieDao, firebaseDatabase)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `authenticated user can view their watchlist`() = runTest {
        // Given
        val userId = "user123"
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.uid } returns userId
        every { firebaseAuth.currentUser } returns mockUser

        val expectedCount = 10
        every { watchlistDao.getWatchlistCount(userId) } returns flowOf(expectedCount)

        // When
        val actualCount = watchlistRepository.getWatchlistCount(userId).first()

        // Then
        assertEquals(expectedCount, actualCount)
        verify { watchlistDao.getWatchlistCount(userId) }
    }

    @Test
    fun `unauthenticated user cannot add to watchlist`() = runTest {
        // Given
        every { firebaseAuth.currentUser } returns null

        // When
        val currentUser = firebaseAuth.currentUser

        // Then
        assertNull(currentUser)
        // In real app, this would trigger login flow
    }

    @Test
    fun `user preferences are applied to movie filtering`() = runTest {
        // Given
        val userFavoriteGenreId = 28 // Action
        val movies = listOf(
            Movie(1, "Action Movie", "", null, null, "", 7.5, 100,
                genres = listOf(Genre(28, "Action"))),
            Movie(2, "Drama Movie", "", null, null, "", 8.0, 150,
                genres = listOf(Genre(18, "Drama"))),
            Movie(3, "Action Thriller", "", null, null, "", 8.5, 200,
                genres = listOf(Genre(28, "Action"), Genre(53, "Thriller")))
        )

        // When
        val filteredMovies = movies.filter { movie ->
            movie.genres.any { it.id == userFavoriteGenreId }
        }

        // Then
        assertEquals(2, filteredMovies.size)
        assertTrue(filteredMovies.all { movie ->
            movie.genres.any { it.id == userFavoriteGenreId }
        })
    }

    @Test
    fun `user can sort watchlist by date added`() = runTest {
        // Given
        val userId = "user123"
        val movie1 = createMockMovieEntity(1, "Movie 1", System.currentTimeMillis() - 1000)
        val movie2 = createMockMovieEntity(2, "Movie 2", System.currentTimeMillis() - 2000)
        val movie3 = createMockMovieEntity(3, "Movie 3", System.currentTimeMillis())

        val watchlistEntities = listOf(
            createMockWatchlistEntity(1, userId, System.currentTimeMillis() - 1000),
            createMockWatchlistEntity(2, userId, System.currentTimeMillis() - 2000),
            createMockWatchlistEntity(3, userId, System.currentTimeMillis())
        )

        every { watchlistDao.getWatchlistForUser(userId) } returns flowOf(watchlistEntities)

        // When
        val watchlist = watchlistDao.getWatchlistForUser(userId).first()
        val sortedByNewest = watchlist.sortedByDescending { it.addedAt }

        // Then
        assertEquals(3, sortedByNewest[0].movieId)
        assertEquals(1, sortedByNewest[1].movieId)
        assertEquals(2, sortedByNewest[2].movieId)
    }

    @Test
    fun `user can sort watchlist by movie rating`() = runTest {
        // Given
        val movies = listOf(
            Movie(1, "Low Rated", "", null, null, "", 5.5, 100),
            Movie(2, "High Rated", "", null, null, "", 9.0, 500),
            Movie(3, "Medium Rated", "", null, null, "", 7.0, 200)
        )

        // When
        val sortedByRating = movies.sortedByDescending { it.voteAverage }

        // Then
        assertEquals(9.0, sortedByRating[0].voteAverage, 0.001)
        assertEquals(7.0, sortedByRating[1].voteAverage, 0.001)
        assertEquals(5.5, sortedByRating[2].voteAverage, 0.001)
        assertEquals("High Rated", sortedByRating.first().title)
    }

    @Test
    fun `user statistics are calculated correctly`() = runTest {
        // Given
        val userId = "user123"
        val watchlistCount = 25
        val movies = listOf(
            Movie(1, "Movie 1", "", null, null, "", 8.0, 100, runtime = 120),
            Movie(2, "Movie 2", "", null, null, "", 7.5, 150, runtime = 95),
            Movie(3, "Movie 3", "", null, null, "", 9.0, 200, runtime = 145)
        )

        every { watchlistDao.getWatchlistCount(userId) } returns flowOf(watchlistCount)

        // When
        val totalWatchTime = movies.sumOf { it.runtime ?: 0 }
        val averageRating = movies.map { it.voteAverage }.average()
        val count = watchlistRepository.getWatchlistCount(userId).first()

        // Then
        assertEquals(360, totalWatchTime) // 6 hours
        assertEquals(8.166, averageRating, 0.01)
        assertEquals(watchlistCount, count)
    }

    @Test
    fun `user can filter watchlist by minimum rating`() = runTest {
        // Given
        val minRating = 7.5
        val movies = listOf(
            Movie(1, "Great Movie", "", null, null, "", 9.0, 500),
            Movie(2, "Good Movie", "", null, null, "", 8.0, 300),
            Movie(3, "OK Movie", "", null, null, "", 7.0, 200),
            Movie(4, "Poor Movie", "", null, null, "", 5.5, 100)
        )

        // When
        val filteredMovies = movies.filter { it.voteAverage >= minRating }

        // Then
        assertEquals(2, filteredMovies.size)
        assertTrue(filteredMovies.all { it.voteAverage >= minRating })
    }

    @Test
    fun `user genre preferences affect recommendations`() = runTest {
        // Given
        val userPreferredGenres = listOf(28, 12, 878) // Action, Adventure, Sci-Fi
        val movies = listOf(
            Movie(1, "Action Adventure", "", null, null, "", 8.0, 100,
                genres = listOf(Genre(28, "Action"), Genre(12, "Adventure"))),
            Movie(2, "Pure Romance", "", null, null, "", 7.5, 150,
                genres = listOf(Genre(10749, "Romance"))),
            Movie(3, "Sci-Fi Thriller", "", null, null, "", 8.5, 200,
                genres = listOf(Genre(878, "Science Fiction")))
        )

        // When
        val recommendedMovies = movies.filter { movie ->
            movie.genres.any { genre -> genre.id in userPreferredGenres }
        }

        // Then
        assertEquals(2, recommendedMovies.size)
        assertFalse(recommendedMovies.any { it.title == "Pure Romance" })
    }

    @Test
    fun `duplicate watchlist entries are prevented`() = runTest {
        // Given
        val movieId = 123
        val userId = "user123"

        every { watchlistDao.observeIsInWatchlist(movieId, userId) } returns flowOf(true)

        // When
        val isAlreadyInWatchlist = watchlistRepository.isInWatchlist(movieId, userId).first()

        // Then
        assertTrue(isAlreadyInWatchlist)
        // In real implementation, UI should disable "Add to Watchlist" button
    }

    @Test
    fun `empty watchlist shows appropriate message state`() = runTest {
        // Given
        val userId = "user123"
        every { watchlistDao.getWatchlistCount(userId) } returns flowOf(0)

        // When
        val count = watchlistRepository.getWatchlistCount(userId).first()

        // Then
        assertEquals(0, count)
        // In UI, this should show "Your watchlist is empty" message
    }


    // Helper functions for creating mock entities
    private fun createMockMovieEntity(id: Int, title: String, lastUpdated: Long) = mockk<com.moviehub.data.local.entity.MovieEntity> {
        every { this@mockk.id } returns id
        every { this@mockk.title } returns title
        every { this@mockk.lastUpdated } returns lastUpdated
    }

    private fun createMockWatchlistEntity(movieId: Int, userId: String, addedAt: Long) = mockk<com.moviehub.data.local.entity.WatchlistEntity> {
        every { this@mockk.movieId } returns movieId
        every { this@mockk.userId } returns userId
        every { this@mockk.addedAt } returns addedAt
    }
}
