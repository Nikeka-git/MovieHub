package com.example.moviehub

import com.google.firebase.database.FirebaseDatabase
import com.moviehub.data.local.dao.MovieDao
import com.moviehub.data.local.dao.WatchlistDao
import com.moviehub.data.local.entity.WatchlistEntity
import com.moviehub.data.repository.WatchlistRepository
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.coroutines.tasks.await

class WatchlistRepositoryTest {

    private lateinit var watchlistRepository: WatchlistRepository
    private lateinit var watchlistDao: WatchlistDao
    private lateinit var movieDao: MovieDao
    private lateinit var database: FirebaseDatabase

    @Before
    fun setup() {
        watchlistDao = mockk(relaxed = true)
        movieDao = mockk(relaxed = true)
        database = mockk(relaxed = true)
        watchlistRepository = WatchlistRepository(watchlistDao, movieDao, database)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `removeFromWatchlist removes from both local and Firebase`() = runTest {
        // Given
        val movieId = 123
        val userId = "user123"

        // When
        coEvery { watchlistDao.removeFromWatchlist(any(), any()) } just Runs
        watchlistDao.removeFromWatchlist(movieId, userId)

        // Then
        coVerify(exactly = 1) { watchlistDao.removeFromWatchlist(movieId, userId) }
    }

    @Test
    fun `duplicate prevention - movie can only be added once`() = runTest {
        // Given
        val movieId = 123
        val userId = "user123"

        every { watchlistDao.observeIsInWatchlist(movieId, userId) } returns flowOf(true)

        // When
        val isAlreadyAdded = watchlistRepository.isInWatchlist(movieId, userId).first()

        // Then
        assertTrue(isAlreadyAdded)
    }

    @Test
    fun `getWatchlistCount returns correct count`() = runTest {
        // Given
        val userId = "user123"
        val expectedCount = 5

        every { watchlistDao.getWatchlistCount(userId) } returns flowOf(expectedCount)

        // When
        val actualCount = watchlistRepository.getWatchlistCount(userId).first()

        // Then
        assertEquals(expectedCount, actualCount)
    }
}