package com.moviehub.domain.model

data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val watchlistCount: Int = 0,
    val reviewsCount: Int = 0
)