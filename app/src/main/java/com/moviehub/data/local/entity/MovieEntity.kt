package com.moviehub.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.moviehub.domain.model.Cast
import com.moviehub.domain.model.Genre
import com.moviehub.domain.model.Movie

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val genres: List<Genre>,
    val runtime: Int?,
    val cast: List<Cast>,
    val lastUpdated: Long
) {
    fun toDomain(isInWatchlist: Boolean = false): Movie {
        return Movie(
            id = id,
            title = title,
            overview = overview,
            posterPath = posterPath,
            backdropPath = backdropPath,
            releaseDate = releaseDate,
            voteAverage = voteAverage,
            voteCount = voteCount,
            genres = genres,
            runtime = runtime,
            cast = cast,
            isInWatchlist = isInWatchlist,
            lastUpdated = lastUpdated
        )
    }
}

fun Movie.toEntity(): MovieEntity {
    return MovieEntity(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        voteCount = voteCount,
        genres = genres,
        runtime = runtime,
        cast = cast,
        lastUpdated = lastUpdated
    )
}