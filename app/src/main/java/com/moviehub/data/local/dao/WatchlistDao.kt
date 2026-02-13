package com.moviehub.data.local.dao

import androidx.room.*
import com.moviehub.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist WHERE userId = :userId ORDER BY addedAt DESC")
    fun getWatchlistForUser(userId: String): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE movieId = :movieId AND userId = :userId)")
    fun observeIsInWatchlist(movieId: Int, userId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE movieId = :movieId AND userId = :userId")
    suspend fun removeFromWatchlist(movieId: Int, userId: String)

    @Query("SELECT COUNT(*) FROM watchlist WHERE userId = :userId")
    fun getWatchlistCount(userId: String): Flow<Int>

    @Query("DELETE FROM watchlist WHERE userId = :userId")
    suspend fun deleteUserWatchlist(userId: String)
}
