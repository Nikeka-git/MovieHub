package com.moviehub.data.firebase

import android.util.Log
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
    companion object {
        private const val TAG = "ReviewsService"
    }
    
    private val reviewsRef = database.getReference("reviews")

    init {
        Log.d(TAG, "ReviewsService initialized")
        Log.d(TAG, "Database reference: ${reviewsRef.toString()}")
    }

    // Realtime updates for movie reviews
    fun observeMovieReviews(movieId: Int): Flow<List<Review>> = callbackFlow {
        Log.d(TAG, "observeMovieReviews called for movieId: $movieId")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reviews = mutableListOf<Review>()
                Log.d(TAG, "onDataChange - snapshot exists: ${snapshot.exists()}, children count: ${snapshot.childrenCount}")
                
                snapshot.children.forEach { reviewSnapshot ->
                    reviewSnapshot.getValue(Review::class.java)?.let { review ->
                        reviews.add(review)
                        Log.d(TAG, "Loaded review: ${review.id} by ${review.userName}")
                    }
                }
                
                Log.d(TAG, "Total reviews loaded: ${reviews.size}")
                trySend(reviews.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeMovieReviews cancelled: ${error.message}", error.toException())
                close(error.toException())
            }
        }

        reviewsRef.child(movieId.toString()).addValueEventListener(listener)
        awaitClose { 
            Log.d(TAG, "Removing listener for movieId: $movieId")
            reviewsRef.child(movieId.toString()).removeEventListener(listener) 
        }
    }

    suspend fun addReview(review: Review): Result<Unit> {
        Log.d(TAG, "═══════════════════════════════════════════")
        Log.d(TAG, "addReview called")
        Log.d(TAG, "Review details:")
        Log.d(TAG, "  - movieId: ${review.movieId}")
        Log.d(TAG, "  - userId: ${review.userId}")
        Log.d(TAG, "  - userName: ${review.userName}")
        Log.d(TAG, "  - rating: ${review.rating}")
        Log.d(TAG, "  - comment: ${review.comment}")
        Log.d(TAG, "  - comment length: ${review.comment.length}")
        Log.d(TAG, "═══════════════════════════════════════════")
        
        return try {
            // Generate review ID if not provided
            val reviewId = review.id.ifEmpty {
                Log.d(TAG, "Review ID is empty, generating new one...")
                val path = reviewsRef.child(review.movieId.toString())
                Log.d(TAG, "Database path: ${path.toString()}")
                
                val key = path.push().key
                Log.d(TAG, "Generated key: $key")
                
                if (key == null) {
                    Log.e(TAG, "Failed to generate review ID - key is null")
                    return Result.failure(Exception("Failed to generate review ID"))
                }
                key
            }

            Log.d(TAG, "Using review ID: $reviewId")
            
            // Create review with ID
            val reviewWithId = review.copy(id = reviewId)
            Log.d(TAG, "Review with ID created: $reviewWithId")
            
            // Convert to map
            val reviewMap = reviewWithId.toMap()
            Log.d(TAG, "Review map created: $reviewMap")
            
            // Construct full database path
            val fullPath = "reviews/${review.movieId}/$reviewId"
            Log.d(TAG, "Full database path: $fullPath")
            
            // Save to Firebase
            Log.d(TAG, "Saving to Firebase...")
            val dbRef = reviewsRef.child(review.movieId.toString()).child(reviewId)
            Log.d(TAG, "Database reference path: ${dbRef.toString()}")
            
            dbRef.setValue(reviewMap).await()
            
            Log.d(TAG, "Review saved successfully!")
            Log.d(TAG, "═══════════════════════════════════════════")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "═══════════════════════════════════════════")
            Log.e(TAG, "Exception in addReview", e)
            Log.e(TAG, "Exception type: ${e.javaClass.name}")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            Log.e(TAG, "═══════════════════════════════════════════")
            
            Result.failure(e)
        }
    }

    suspend fun updateReview(review: Review): Result<Unit> {
        Log.d(TAG, "updateReview called for reviewId: ${review.id}")
        
        return try {
            val updates = mapOf(
                "rating" to review.rating,
                "comment" to review.comment,
                "updatedAt" to System.currentTimeMillis()
            )
            
            Log.d(TAG, "Update data: $updates")
            
            reviewsRef.child(review.movieId.toString())
                .child(review.id)
                .updateChildren(updates)
                .await()
            
            Log.d(TAG, "Review updated successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating review", e)
            Result.failure(e)
        }
    }

    suspend fun deleteReview(movieId: Int, reviewId: String): Result<Unit> {
        Log.d(TAG, "deleteReview called - movieId: $movieId, reviewId: $reviewId")
        
        return try {
            reviewsRef.child(movieId.toString())
                .child(reviewId)
                .removeValue()
                .await()
            
            Log.d(TAG, "Review deleted successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting review", e)
            Result.failure(e)
        }
    }

    suspend fun getUserReviewForMovie(movieId: Int, userId: String): Review? {
        Log.d(TAG, "getUserReviewForMovie - movieId: $movieId, userId: $userId")
        
        return try {
            val snapshot = reviewsRef.child(movieId.toString())
                .orderByChild("userId")
                .equalTo(userId)
                .get()
                .await()

            val review = snapshot.children.firstOrNull()?.getValue(Review::class.java)
            Log.d(TAG, "Found user review: ${review != null}")
            review
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user review", e)
            null
        }
    }

    fun observeUserReviews(userId: String): Flow<List<Review>> = callbackFlow {
        Log.d(TAG, "observeUserReviews called for userId: $userId")
        
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
                
                Log.d(TAG, "Loaded ${reviews.size} reviews for user")
                trySend(reviews.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeUserReviews cancelled: ${error.message}", error.toException())
                close(error.toException())
            }
        }

        reviewsRef.addValueEventListener(listener)
        awaitClose { 
            Log.d(TAG, "Removing user reviews listener")
            reviewsRef.removeEventListener(listener) 
        }
    }
}
