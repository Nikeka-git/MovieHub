# MovieHub - Android Movie Discovery App

**Version:** 1.1.0 (Final Release)  
**Platform:** Android (Kotlin)  
**Min SDK:** 24 (Android 7.0)  
**Target SDK:** 34 (Android 14)

## Overview

MovieHub is a production-ready movie discovery application that allows users to browse popular movies, search for films, manage a personal watchlist, and share reviews in real-time with other users. The app demonstrates modern Android development practices including offline-first architecture, Firebase integration, and clean code principles.

## Key Features

- **Movie Discovery**: Browse trending and popular movies from TMDB
- **Advanced Search**: Debounced search with instant results
- **Pagination**: Infinite scroll for seamless browsing
- **Offline Mode**: Full offline support with local caching
- **Watchlist**: User-specific watchlist with Firebase sync
- **Real-time Reviews**: Share and view movie reviews in real-time
- **Authentication**: Secure Firebase Authentication
- **Material Design 3**: Modern, responsive UI with Jetpack Compose

## Architecture

- **Pattern**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Dependency Injection**: Hilt
- **UI Framework**: Jetpack Compose
- **Concurrency**: Kotlin Coroutines + Flow/StateFlow
- **Navigation**: Jetpack Navigation Compose

See [ARCHITECTURE.md](/docs/ARCHITECTURE.md) for detailed architecture documentation.

## Tech Stack

### Core
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Repository Pattern
- **DI**: Dagger Hilt

### Networking & Data
- **REST API**: Retrofit + OkHttp
- **JSON**: Gson
- **Image Loading**: Coil
- **Local Database**: Room
- **Remote Database**: Firebase Realtime Database

### Firebase Services
- Firebase Authentication
- Firebase Realtime Database

### External API
- **TMDB API**: The Movie Database API v3
- Base URL: `https://api.themoviedb.org/3/`
- Endpoints used:
  - `/movie/popular` - Fetch popular movies
  - `/search/movie` - Search movies by query
  - `/movie/{id}` - Get movie details
  - `/movie/{id}/recommendations` - Get movie recommendations

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or newer
- JDK 17
- Android SDK with API 34
- TMDB API Key
- Firebase Project

### Step 1: Clone Repository
```bash
git clone https://github.com/yourusername/moviehub.git
cd moviehub
```

### Step 2: Configure API Keys
1. Copy `local.properties.example` to `local.properties`
2. Add your TMDB API key:
   ```properties
   TMDB_API_KEY=your_actual_tmdb_api_key
   sdk.dir=/path/to/android/sdk
   ```
3. Get TMDB API key from: https://www.themoviedb.org/settings/api

### Step 3: Configure Firebase
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing one
3. Add Android app with package name: `com.moviehub`
4. Download `google-services.json`
5. Place it in `/app/` directory
6. Enable Authentication (Email/Password)
7. Enable Realtime Database

### Step 4: Firebase Realtime Database Rules
Set the following security rules in Firebase Console:
```json
{
  "rules": {
    "reviews": {
      "$movieId": {
        ".read": true,
        ".write": "auth != null",
        "$reviewId": {
          ".validate": "newData.hasChildren(['userId', 'userName', 'rating', 'comment'])",
          ".write": "auth != null && (
            !data.exists() || 
            data.child('userId').val() === auth.uid
          )"
        }
      }
    },
    "watchlist": {
      "$userId": {
        ".read": "auth != null && auth.uid === $userId",
        ".write": "auth != null && auth.uid === $userId"
      }
    }
  }
}
```

### Step 5: Build & Run
```bash
# Debug build
./gradlew assembleDebug

# Release build (signed)
./gradlew assembleRelease

# Run on connected device
./gradlew installDebug
```

## Testing

### Unit Tests
Run all unit tests (25 tests covering repositories, ViewModels, and business logic):
```bash
./gradlew test
```

### Test Coverage
- Repository layer: 85%+
- ViewModel layer: 80%+
- Business logic: 90%+

See test files in:
- `/app/src/test/java/com/moviehub/data/repository/`
- `/app/src/test/java/com/moviehub/ui/screens/`

## Release Build

### Create Signed APK
```bash
./gradlew assembleRelease
```
Output: `/app/build/outputs/apk/release/app-release.apk`

### Create Signed AAB (for Play Store)
```bash
./gradlew bundleRelease
```
Output: `/app/build/outputs/bundle/release/app-release.aab`

### Version History
- **v1.1.0** (Final) - Production-ready release with hardened quality
- **v1.0.0** (Endterm) - Initial feature-complete version

## Security Notes

- API keys stored in `local.properties` (excluded from Git)
- Firebase security rules enforce user-scoped data
- Input validation on all user inputs
- Signed release builds
- ProGuard enabled for release builds
- For demo purposes, using debug keystore (production should use proper keystore)

Or create your own account through the Sign Up screen.

## Project Structure

```
moviehub/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/moviehub/
│   │   │   │   ├── data/           # Data layer
│   │   │   │   │   ├── local/      # Room database
│   │   │   │   │   ├── remote/     # Retrofit API
│   │   │   │   │   ├── firebase/   # Firebase services
│   │   │   │   │   └── repository/ # Repository implementations
│   │   │   │   ├── domain/         # Domain models
│   │   │   │   ├── ui/             # UI layer (Compose)
│   │   │   │   │   ├── screens/    # Screen composables
│   │   │   │   │   ├── components/ # Reusable components
│   │   │   │   │   └── navigation/ # Navigation setup
│   │   │   │   └── di/             # Hilt modules
│   │   │   └── res/                # Resources
│   │   └── test/                   # Unit tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── docs/                           # Documentation
│   ├── ARCHITECTURE.md
│   ├── RELEASE_NOTES.md
│   ├── QA_LOG.md
│   └── STORE_LISTING.md
└── README.md
```

## Known Issues & Limitations

- Background sync not implemented (manual refresh required)
- Review editing limited to review author only
- Image caching could be more aggressive
- No analytics integration yet

## Performance

- Cold start time: <2s on mid-range devices
- Smooth 60fps scrolling
- Efficient image loading with Coil
- Room database indexed for fast queries
- API response caching reduces network calls


## Author

- Tsybus Nikita & Karimbay Zhandos

## Acknowledgments

- TMDB for providing the movie database API
- Firebase for backend services
- Android Jetpack libraries
- Kotlin Coroutines team
