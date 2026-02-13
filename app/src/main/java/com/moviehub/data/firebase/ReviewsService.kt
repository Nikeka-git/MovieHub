package com.moviehub.data.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.moviehub.domain.model.Review
import com.moviehub.utils.AppLogger
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
    companion object {
        private const val TAG = "ReviewsService"
    }

    private val reviewsRef = database.getReference("reviews")

    init {
        AppLogger.d(TAG, "ReviewsService initialized")
        AppLogger.d(TAG, "Database reference: ${reviewsRef.toString()}")
    }

    fun observeMovieReviews(movieId: Int): Flow<List<Review>> = callbackFlow {
        AppLogger.d(TAG, "observeMovieReviews called for movieId: $movieId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reviews = mutableListOf<Review>()
                AppLogger.d(TAG, "onDataChange - snapshot exists: ${snapshot.exists()}, children count: ${snapshot.childrenCount}")

                snapshot.children.forEach { reviewSnapshot ->
                    reviewSnapshot.getValue(Review::class.java)?.let { review ->
                        reviews.add(review)
                        AppLogger.d(TAG, "Loaded review: ${review.id} by ${review.userName}")
                    }
                }

                AppLogger.d(TAG, "Total reviews loaded: ${reviews.size}")
                trySend(reviews.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                AppLogger.e(TAG, "observeMovieReviews cancelled: ${error.message}", error.toException())
                close(error.toException())
            }
        }

        reviewsRef.child(movieId.toString()).addValueEventListener(listener)
        awaitClose {
            AppLogger.d(TAG, "Removing listener for movieId: $movieId")
            reviewsRef.child(movieId.toString()).removeEventListener(listener)
        }
    }

    suspend fun addReview(review: Review): Result<Unit> {
        AppLogger.separator(TAG)
        AppLogger.d(TAG, "addReview called")
        AppLogger.d(TAG, "Review details:")
        AppLogger.d(TAG, "  - movieId: ${review.movieId}")
        AppLogger.d(TAG, "  - userId: ${review.userId}")
        AppLogger.d(TAG, "  - userName: ${review.userName}")
        AppLogger.d(TAG, "  - rating: ${review.rating}")
        AppLogger.d(TAG, "  - comment: ${review.comment}")
        AppLogger.d(TAG, "  - comment length: ${review.comment.length}")
        AppLogger.separator(TAG)

        return try {
            val reviewId = review.id.ifEmpty {
                AppLogger.d(TAG, "Review ID is empty, generating new one...")
                val path = reviewsRef.child(review.movieId.toString())
                AppLogger.d(TAG, "Database path: ${path.toString()}")

                val key = path.push().key
                AppLogger.d(TAG, "Generated key: $key")

                if (key == null) {
                    AppLogger.e(TAG, "Failed to generate review ID - key is null")
                    return Result.failure(Exception("Failed to generate review ID"))
                }
                key
            }

            AppLogger.d(TAG, "Using review ID: $reviewId")

            val reviewWithId = review.copy(id = reviewId)
            AppLogger.d(TAG, "Review with ID created: $reviewWithId")

            val reviewMap = reviewWithId.toMap()
            AppLogger.d(TAG, "Review map created: $reviewMap")

            val fullPath = "reviews/${review.movieId}/$reviewId"
            AppLogger.d(TAG, "Full database path: $fullPath")

            AppLogger.d(TAG, "Saving to Firebase...")
            val dbRef = reviewsRef.child(review.movieId.toString()).child(reviewId)
            AppLogger.d(TAG, "Database reference path: ${dbRef.toString()}")

            dbRef.setValue(reviewMap).await()

            AppLogger.d(TAG, "Review saved successfully!")
            AppLogger.separator(TAG)

            Result.success(Unit)

        } catch (e: Exception) {
            AppLogger.separator(TAG)
            AppLogger.e(TAG, "Exception in addReview", e)
            AppLogger.e(TAG, "Exception type: ${e.javaClass.name}")
            AppLogger.e(TAG, "Exception message: ${e.message}")
            AppLogger.separator(TAG)

            Result.failure(e)
        }
    }

    suspend fun updateReview(review: Review): Result<Unit> {
        AppLogger.d(TAG, "updateReview called for reviewId: ${review.id}")

        return try {
            val updates = mapOf(
                "rating" to review.rating,
                "comment" to review.comment,
                "updatedAt" to System.currentTimeMillis()
            )

            AppLogger.d(TAG, "Update data: $updates")

            reviewsRef.child(review.movieId.toString())
                .child(review.id)
                .updateChildren(updates)
                .await()

            AppLogger.d(TAG, "Review updated successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            AppLogger.e(TAG, "Error updating review", e)
            Result.failure(e)
        }
    }

    suspend fun deleteReview(movieId: Int, reviewId: String): Result<Unit> {
        AppLogger.d(TAG, "deleteReview called - movieId: $movieId, reviewId: $reviewId")

        return try {
            reviewsRef.child(movieId.toString())
                .child(reviewId)
                .removeValue()
                .await()

            AppLogger.d(TAG, "Review deleted successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            AppLogger.e(TAG, "Error deleting review", e)
            Result.failure(e)
        }
    }

    suspend fun getUserReviewForMovie(movieId: Int, userId: String): Review? {
        AppLogger.d(TAG, "getUserReviewForMovie - movieId: $movieId, userId: $userId")

        return try {
            val snapshot = reviewsRef.child(movieId.toString())
                .orderByChild("userId")
                .equalTo(userId)
                .get()
                .await()

            val review = snapshot.children.firstOrNull()?.getValue(Review::class.java)
            AppLogger.d(TAG, "Found user review: ${review != null}")
            review

        } catch (e: Exception) {
            AppLogger.e(TAG, "Error getting user review", e)
            null
        }
    }

    fun observeUserReviews(userId: String): Flow<List<Review>> = callbackFlow {
        AppLogger.d(TAG, "observeUserReviews called for userId: $userId")

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

                AppLogger.d(TAG, "Loaded ${reviews.size} reviews for user")
                trySend(reviews.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                AppLogger.e(TAG, "observeUserReviews cancelled: ${error.message}", error.toException())
                close(error.toException())
            }
        }

        reviewsRef.addValueEventListener(listener)
        awaitClose {
            AppLogger.d(TAG, "Removing user reviews listener")
            reviewsRef.removeEventListener(listener)
        }
    }
}