package com.moviehub.domain.model

data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val genres: List<Genre> = emptyList(),
    val runtime: Int? = null,
    val cast: List<Cast> = emptyList(),
    val isInWatchlist: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class Genre(
    val id: Int,
    val name: String
)

data class Cast(
    val id: Int,
    val name: String,
    val character: String,
    val profilePath: String?
)