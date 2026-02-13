package com.moviehub.data.local.dao

import androidx.room.*
import com.moviehub.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE id = :movieId")
    suspend fun getMovieById(movieId: Int): MovieEntity?

    @Query("SELECT * FROM movies WHERE id = :movieId")
    fun observeMovieById(movieId: Int): Flow<MovieEntity?>

    @Query("SELECT * FROM movies ORDER BY lastUpdated DESC LIMIT :limit")
    fun getRecentMovies(limit: Int = 20): Flow<List<MovieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Update
    suspend fun updateMovie(movie: MovieEntity)

    @Delete
    suspend fun deleteMovie(movie: MovieEntity)

    @Query("DELETE FROM movies WHERE id = :movieId")
    suspend fun deleteMovieById(movieId: Int)

    @Query("DELETE FROM movies")
    suspend fun deleteAllMovies()

    @Query("DELETE FROM movies WHERE lastUpdated < :timestamp")
    suspend fun deleteOldMovies(timestamp: Long)

    @Query("SELECT * FROM movies WHERE title LIKE '%' || :query || '%' ORDER BY voteAverage DESC")
    fun searchMovies(query: String): Flow<List<MovieEntity>>
}