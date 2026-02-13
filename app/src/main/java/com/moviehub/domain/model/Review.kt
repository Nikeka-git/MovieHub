package com.moviehub.domain.model

data class Review(
    val id: String = "",
    val movieId: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "movieId" to movieId,
        "userId" to userId,
        "userName" to userName,
        "rating" to rating,
        "comment" to comment,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}