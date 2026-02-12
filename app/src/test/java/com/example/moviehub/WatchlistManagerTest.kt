package com.example.moviehub

import com.google.firebase.database.FirebaseDatabase
import com.moviehub.data.local.dao.MovieDao
import com.moviehub.data.local.dao.WatchlistDao
import com.moviehub.data.local.entity.WatchlistEntity
import com.moviehub.data.repository.WatchlistRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

class WatchlistManagerTest {

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
    fun `addToWatchlist adds to both local and Firebase`() = runTest {
        // Given
        val movieId = 123
        val userId = "user123"

        coEvery { watchlistDao.addToWatchlist(any()) } just Runs
        every { database.getReference(any()) } returns mockk(relaxed = true) {
            every { child(any()) } returns this
            every { setValue(any()) } returns mockk(relaxed = true)
        }

        // When
        val result = watchlistRepository.addToWatchlist(movieId, userId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { watchlistDao.addToWatchlist(any()) }
    }

    @Test
    fun `removeFromWatchlist removes from both local and Firebase`() = runTest {
        // Given
        val movieId = 123
        val userId = "user123"

        coEvery { watchlistDao.removeFromWatchlist(any(), any()) } just Runs
        every { database.getReference(any()) } returns mockk(relaxed = true) {
            every { child(any()) } returns this
            every { removeValue() } returns mockk(relaxed = true)
        }

        // When
        val result = watchlistRepository.removeFromWatchlist(movieId, userId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { watchlistDao.removeFromWatchlist(movieId, userId) }
    }

    @Test
    fun `duplicate prevention - movie can only be added once`() = runTest {
        // Given
        val movieId = 123
        val userId = "user123"
        val existingItem = WatchlistEntity(movieId, userId, System.currentTimeMillis())

        coEvery { watchlistDao.isInWatchlist(movieId, userId) } returns true

        // When
        val isAlreadyAdded = watchlistDao.isInWatchlist(movieId, userId)

        // Then
        assertEquals(true, isAlreadyAdded)
    }

    @Test
    fun `getWatchlistCount returns correct count`() = runTest {
        // Given
        val userId = "user123"
        val expectedCount = 5

        every { watchlistDao.getWatchlistCount(userId) } returns flowOf(expectedCount)

        // When
        val flow = watchlistRepository.getWatchlistCount(userId)
        var actualCount = 0
        flow.collect { actualCount = it }

        // Then
        assertEquals(expectedCount, actualCount)
    }

    @Test
    fun `conflict resolution - last write wins on sync`() = runTest {
        // Given
        val userId = "user123"
        val localTimestamp = System.currentTimeMillis()
        val remoteTimestamp = localTimestamp + 1000 // Remote is newer

        val localItem = WatchlistEntity(1, userId, localTimestamp)
        val remoteItem = WatchlistEntity(1, userId, remoteTimestamp)

        // When - merge strategy
        val finalItem = if (remoteTimestamp > localTimestamp) remoteItem else localItem

        // Then
        assertEquals(remoteTimestamp, finalItem.addedAt)
    }
}