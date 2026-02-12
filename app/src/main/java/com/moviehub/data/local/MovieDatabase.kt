package com.moviehub.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.moviehub.data.local.dao.WatchlistDao
import com.moviehub.data.local.dao.MovieDao
import com.moviehub.data.local.entity.MovieEntity
import com.moviehub.data.local.entity.WatchlistEntity

@Database(
    entities = [MovieEntity::class, WatchlistEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun watchlistDao(): WatchlistDao
}