package com.moviehub.data.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.moviehub.domain.model.Review
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewsService @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val reviewsRef = database.getReference("reviews")

    // Realtime updates for movie reviews
    fun observeMovieReviews(movieId: Int): Flow<List<Review>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reviews = mutableListOf<Review>()
                snapshot.children.forEach { reviewSnapshot ->
                    reviewSnapshot.getValue(Review::class.java)?.let { review ->
                        reviews.add(review)
                    }
                }
                trySend(reviews.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        reviewsRef.child(movieId.toString()).addValueEventListener(listener)
        awaitClose { reviewsRef.child(movieId.toString()).removeEventListener(listener) }
    }

    suspend fun addReview(review: Review): Result<Unit> {
        return try {
            val reviewId = review.id.ifEmpty {
                reviewsRef.child(review.movieId.toString()).push().key ?: return Result.failure(
                    Exception("Failed to generate review ID")
                )
            }

            val reviewWithId = review.copy(id = reviewId)
            reviewsRef.child(review.movieId.toString())
                .child(reviewId)
                .setValue(reviewWithId.toMap())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReview(review: Review): Result<Unit> {
        return try {
            reviewsRef.child(review.movieId.toString())
                .child(review.id)
                .updateChildren(
                    mapOf(
                        "rating" to review.rating,
                        "comment" to review.comment,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReview(movieId: Int, reviewId: String): Result<Unit> {
        return try {
            reviewsRef.child(movieId.toString())
                .child(reviewId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserReviewForMovie(movieId: Int, userId: String): Review? {
        return try {
            val snapshot = reviewsRef.child(movieId.toString())
                .orderByChild("userId")
                .equalTo(userId)
                .get()
                .await()

            snapshot.children.firstOrNull()?.getValue(Review::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun observeUserReviews(userId: String): Flow<List<Review>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reviews = mutableListOf<Review>()
                snapshot.children.forEach { movieSnapshot ->
                    movieSnapshot.children.forEach { reviewSnapshot ->
                        reviewSnapshot.getValue(Review::class.java)?.let { review ->
                            if (review.userId == userId) {
                                reviews.add(review)
                            }
                        }
                    }
                }
                trySend(reviews.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        reviewsRef.addValueEventListener(listener)
        awaitClose { reviewsRef.removeEventListener(listener) }
    }
}