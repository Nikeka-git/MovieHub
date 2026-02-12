package com.moviehub.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.moviehub.domain.model.*

data class MovieListResponse(
    val page: Int,
    val results: List<MovieDto>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class MovieDto(
    val id: Int,
    val title: String,
    val overview: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int,
    @SerializedName("genre_ids") val genreIds: List<Int>?
) {
    fun toDomain(genres: List<Genre> = emptyList()): Movie {
        return Movie(
            id = id,
            title = title,
            overview = overview,
            posterPath = posterPath,
            backdropPath = backdropPath,
            releaseDate = releaseDate ?: "",
            voteAverage = voteAverage,
            voteCount = voteCount,
            genres = genres
        )
    }
}

data class MovieDetailDto(
    val id: Int,
    val title: String,
    val overview: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int,
    val genres: List<GenreDto>,
    val runtime: Int?
) {
    fun toDomain(cast: List<Cast> = emptyList()): Movie {
        return Movie(
            id = id,
            title = title,
            overview = overview,
            posterPath = posterPath,
            backdropPath = backdropPath,
            releaseDate = releaseDate ?: "",
            voteAverage = voteAverage,
            voteCount = voteCount,
            genres = genres.map { it.toDomain() },
            runtime = runtime,
            cast = cast
        )
    }
}

data class GenreDto(
    val id: Int,
    val name: String
) {
    fun toDomain() = Genre(id, name)
}

data class GenreListResponse(
    val genres: List<GenreDto>
)

data class CreditsResponse(
    val cast: List<CastDto>
)

data class CastDto(
    val id: Int,
    val name: String,
    val character: String,
    @SerializedName("profile_path") val profilePath: String?
) {
    fun toDomain() = Cast(id, name, character, profilePath)
}