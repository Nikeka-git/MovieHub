# MovieHub Architecture Documentation

## Overview

MovieHub follows **Clean Architecture** principles with **MVVM pattern** and **Repository pattern** to ensure separation of concerns, testability, and maintainability.

## Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                    │
│                    (UI + ViewModels)                     │
├─────────────────────────────────────────────────────────┤
│  Jetpack Compose Screens  │      ViewModels (Hilt)     │
│  - MoviesScreen           │      - MoviesViewModel      │
│  - MovieDetailScreen      │      - MovieDetailViewModel │
│  - SearchScreen           │      - SearchViewModel      │
│  - WatchlistScreen        │      - WatchlistViewModel   │
│  - LoginScreen            │      - AuthViewModel        │
└──────────────┬────────────┴──────────────┬──────────────┘
               │                           │
               │  StateFlow/Events         │
               │                           │
┌──────────────▼───────────────────────────▼──────────────┐
│                     DOMAIN LAYER                         │
│                   (Business Logic)                       │
├─────────────────────────────────────────────────────────┤
│  Domain Models:                                          │
│  - Movie, Review, User, WatchlistItem                   │
│                                                          │
│  Use Cases / Business Rules:                            │
│  - Movie recommendation filtering                       │
│  - Review validation (1-5 stars, min 10 chars)         │
│  - Offline-first data strategy                         │
└──────────────┬──────────────────────────┬───────────────┘
               │                          │
               │  Repository Interface    │
               │                          │
┌──────────────▼──────────────────────────▼───────────────┐
│                      DATA LAYER                          │
│                  (Repositories + Sources)                │
├─────────────────────────────────────────────────────────┤
│  Repositories (Implementations):                         │
│  - MoviesRepository                                      │
│  - WatchlistRepository                                   │
│                                                          │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Local     │  │    Remote    │  │   Firebase   │  │
│  │   Source    │  │    Source    │  │   Source     │  │
│  ├─────────────┤  ├──────────────┤  ├──────────────┤  │
│  │  Room DB    │  │  TMDB API    │  │    Auth      │  │
│  │  - MovieDao │  │  - Retrofit  │  │  - RealtimeDB│  │
│  │  - Entities │  │  - DTOs      │  │  - Reviews   │  │
│  └─────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Data Flow

### Reading Data (Offline-First)

```
User Action
    │
    ▼
[ViewModel] ─────requests data────▶ [Repository]
    │                                     │
    │                              ┌──────▼──────┐
    │                              │ Check Cache │
    │                              └──────┬──────┘
    │                                     │
    │                        ┌────────────┴────────────┐
    │                        │                         │
    │                   [Has Cache]              [No Cache / Stale]
    │                        │                         │
    │                        │                         ▼
    │                        │                  [Fetch from API]
    │                        │                         │
    │                        │                         ▼
    │                        │                  [Save to Cache]
    │                        │                         │
    │                        └────────────┬────────────┘
    │                                     │
    │◀──────────emits StateFlow───────────┘
    │
    ▼
[UI Updates]
```

### Writing Data (Create/Update/Delete)

```
User Action (Add Review)
    │
    ▼
[ViewModel] ──────sends command─────▶ [Repository]
    │                                      │
    │                                      ▼
    │                              [Firebase Realtime DB]
    │                                      │
    │                                      ▼
    │                              [Real-time listener]
    │                                      │
    │◀─────────emits update────────────────┘
    │
    ▼
[UI Updates Automatically]
```

## Key Components

### 1. Presentation Layer

**ViewModels** (One per screen)
- Hold UI state using `StateFlow`
- Handle user events
- Coordinate between repositories
- Survive configuration changes
- Injected via Hilt

**Compose Screens**
- Pure UI functions
- Observe ViewModel state
- Emit user events
- No business logic

### 2. Repository Pattern

**MoviesRepository**
```kotlin
interface MoviesRepository {
    suspend fun getPopularMovies(page: Int): Flow<Result<List<Movie>>>
    suspend fun searchMovies(query: String, page: Int): Flow<Result<List<Movie>>>
    suspend fun getMovieById(id: Int): Flow<Result<Movie>>
}
```

**Implementation Strategy:**
- Try local cache first
- If cache miss/stale, fetch from API
- Save API response to cache
- Return cached data
- Handle errors gracefully

### 3. Data Sources

**Local Source (Room)**
```kotlin
@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE id = :id")
    fun getMovieById(id: Int): Flow<MovieEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)
    
    @Query("DELETE FROM movies WHERE lastUpdated < :threshold")
    suspend fun deleteStaleMovies(threshold: Long)
}
```

**Remote Source (Retrofit)**
```kotlin
interface TmdbApiService {
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int
    ): MovieResponse
}
```

**Firebase Source**
```kotlin
class ReviewsService {
    fun observeMovieReviews(movieId: Int): Flow<List<Review>>
    suspend fun addReview(review: Review): Result<Unit>
    suspend fun updateReview(review: Review): Result<Unit>
    suspend fun deleteReview(movieId: Int, reviewId: String): Result<Unit>
}
```

## Dependency Injection (Hilt)

### Module Structure

**AppModule** - Application-level dependencies
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideMovieDatabase(app: Application): MovieDatabase
    
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit
    
    @Provides
    @Singleton
    fun provideTmdbApi(retrofit: Retrofit): TmdbApiService
}
```

**FirebaseModule** - Firebase dependencies
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth
    
    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase
}
```

## Concurrency & Threading

### Coroutines Strategy

**UI Layer**
```kotlin
viewModelScope.launch {
    moviesRepository.getPopularMovies()
        .collect { result ->
            _uiState.value = when (result) {
                is Success -> UiState.Success(result.data)
                is Error -> UiState.Error(result.message)
            }
        }
}
```

**Repository Layer**
```kotlin
override suspend fun getPopularMovies(): Flow<Result<List<Movie>>> = flow {
    // Emit cached data first
    val cached = movieDao.getCachedMovies()
    if (cached.isNotEmpty()) {
        emit(Result.success(cached))
    }
    
    // Fetch fresh data
    try {
        val response = api.getPopularMovies()
        val movies = response.results.map { it.toDomain() }
        
        // Update cache
        movieDao.insertMovies(movies.map { it.toEntity() })
        
        emit(Result.success(movies))
    } catch (e: Exception) {
        if (cached.isEmpty()) {
            emit(Result.failure(e))
        }
    }
}.flowOn(Dispatchers.IO)
```

### StateFlow for UI State

```kotlin
private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// In Compose
val uiState by viewModel.uiState.collectAsState()
```

## Offline-First Strategy

### Cache Policy

1. **On App Start**: Load cached data immediately
2. **Background Fetch**: Refresh from API if online
3. **Stale Check**: Data older than 1 hour is refreshed
4. **No Internet**: Show cached data with indicator
5. **Cache Update**: Always update cache on successful API call

### Sync Strategy

**Movies (Read-Only Cache)**
```
Local DB ◀─── API Response
    │
    └───▶ UI (StateFlow)
```

**Watchlist (User-Scoped Sync)**
```
Firebase Realtime DB ◀──▶ Local Cache
           │
           └───▶ UI (Real-time updates)
```

**Reviews (Real-time Feature)**
```
Firebase Realtime DB ──▶ UI (Live updates)
    ▲
    └─── User writes
```

### Duplicate Prevention

- Room: `@Insert(onConflict = OnConflictStrategy.REPLACE)`
- Firebase: Use push() generated keys for unique IDs
- Watchlist: Primary key = (userId, movieId)

## Error Handling

### Error Types

```kotlin
sealed class ApiError {
    object NetworkError : ApiError()
    object ServerError : ApiError()
    object AuthError : ApiError()
    data class UnknownError(val message: String) : ApiError()
}
```

### Error Handling Flow

```kotlin
try {
    val result = api.getMovies()
    Result.success(result)
} catch (e: IOException) {
    Result.failure(ApiError.NetworkError)
} catch (e: HttpException) {
    when (e.code()) {
        401 -> Result.failure(ApiError.AuthError)
        500 -> Result.failure(ApiError.ServerError)
        else -> Result.failure(ApiError.UnknownError(e.message))
    }
}
```

### Retry Strategy

- **Transient Errors**: Automatic retry with exponential backoff
- **Network Errors**: Manual retry button + use cached data
- **Auth Errors**: Redirect to login
- **Server Errors**: Show error message + retry option

## Testing Strategy

### Unit Tests (25 tests)

**Repository Tests**
- Cache hit/miss scenarios
- API success/failure handling
- Offline behavior
- Data transformation

**ViewModel Tests**
- State transitions
- User action handling
- Error states
- Loading states

**Use Case Tests**
- Business logic validation
- Edge cases
- Data filtering

### Test Tools

- JUnit 4
- MockK for mocking
- Turbine for Flow testing
- Coroutines Test for async testing

## Security

### API Key Management
- Keys stored in `local.properties` (git-ignored)
- Accessed via BuildConfig
- Never hardcoded in source

### Firebase Security Rules
- User-scoped read/write permissions
- Authentication required for writes
- Data validation rules
- See README.md for full rules

### Input Validation
- Email format validation
- Password strength requirements (min 6 chars)
- Review content validation (1-5 stars, min 10 chars)
- SQL injection prevention (Room parameterized queries)

## Performance Optimizations

### Database
- Indexed queries on frequently searched columns
- Efficient pagination with LIMIT/OFFSET
- Cascade delete for related data

### Networking
- Response caching with OkHttp
- Connection pooling
- Timeout configuration (30s read, 10s connect)

### UI
- Lazy loading with LazyColumn
- Image caching with Coil
- State hoisting to prevent recomposition
- Remember stateful computations

### Memory
- WeakReference for large objects
- Clear cache periodically
- Efficient data structures (Lists vs Sets)

## Future Improvements

1. **WorkManager** for background sync
2. **Paging 3** for advanced pagination
3. **DataStore** for preferences
4. **Crashlytics** for crash reporting
5. **Analytics** for user behavior tracking
6. **Push Notifications** for new reviews
7. **Offline write queue** for pending changes
8. **Image preloading** for smoother scrolling

## Conclusion

MovieHub demonstrates production-ready Android architecture with:
- Clean separation of concerns
- Testable components
- Offline-first approach
- Real-time capabilities
- Scalable structure
- Modern Android development practices
