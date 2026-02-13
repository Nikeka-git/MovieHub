    package com.moviehub.data.repository
    
    import com.google.firebase.database.FirebaseDatabase
    import com.moviehub.data.local.dao.MovieDao
    import com.moviehub.data.local.dao.WatchlistDao
    import com.moviehub.data.local.entity.WatchlistEntity
    import com.moviehub.domain.model.Movie
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.combine
    import kotlinx.coroutines.flow.map
    import kotlinx.coroutines.tasks.await
    import javax.inject.Inject
    import javax.inject.Singleton
    
    @Singleton
    class WatchlistRepository @Inject constructor(
        private val watchlistDao: WatchlistDao,
        private val movieDao: MovieDao,
        private val database: FirebaseDatabase
    ) {
        private val watchlistRef = database.getReference("watchlist")
    
        fun getWatchlistMovies(userId: String): Flow<List<Movie>> {
            return watchlistDao.getWatchlistForUser(userId)
                .combine(movieDao.getRecentMovies(100)) { watchlist, movies ->
                    val watchlistMovieIds = watchlist.map { it.movieId }.toSet()
                    movies.filter { it.id in watchlistMovieIds }
                        .map { it.toDomain(isInWatchlist = true) }
                }
        }
    
        fun isInWatchlist(movieId: Int, userId: String): Flow<Boolean> {
            return watchlistDao.observeIsInWatchlist(movieId, userId)
        }
    
        suspend fun addToWatchlist(movieId: Int, userId: String): Result<Unit> {
            return try {
                val item = WatchlistEntity(
                    movieId = movieId,
                    userId = userId,
                    addedAt = System.currentTimeMillis()
                )
    
                // Add to local database
                watchlistDao.addToWatchlist(item)
    
                // Sync to Firebase
                watchlistRef.child(userId).child(movieId.toString()).setValue(
                    mapOf(
                        "movieId" to movieId,
                        "userId" to userId,
                        "addedAt" to item.addedAt
                    )
                ).await()
    
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
        suspend fun removeFromWatchlist(movieId: Int, userId: String): Result<Unit> {
            return try {
                // Remove from local database
                watchlistDao.removeFromWatchlist(movieId, userId)
    
                // Remove from Firebase
                watchlistRef.child(userId).child(movieId.toString()).removeValue().await()
    
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
        suspend fun syncWatchlist(userId: String) {
            try {
                // Fetch from Firebase
                val snapshot = watchlistRef.child(userId).get().await()
                val firebaseItems = mutableListOf<WatchlistEntity>()
    
                snapshot.children.forEach { child ->
                    val movieId = child.key?.toIntOrNull() ?: return@forEach
                    val addedAt = child.child("addedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                    firebaseItems.add(WatchlistEntity(
                        id = 0,
                        movieId = movieId,
                        userId = userId,
                        addedAt = addedAt
                    ))            }
    
                // Get local items
                val localItems = mutableListOf<WatchlistEntity>()
                watchlistDao.getWatchlistForUser(userId).collect { items ->
                    localItems.addAll(items)
                }
    
                // Merge strategy: union of both lists
                val allItems = (firebaseItems + localItems).distinctBy { it.movieId }
    
                // Update both local and remote
                allItems.forEach { item ->
                    watchlistDao.addToWatchlist(item)
                    watchlistRef.child(userId).child(item.movieId.toString()).setValue(
                        mapOf(
                            "movieId" to item.movieId,
                            "userId" to userId,
                            "addedAt" to item.addedAt
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    
        fun getWatchlistCount(userId: String): Flow<Int> {
            return watchlistDao.getWatchlistCount(userId)
        }
    }