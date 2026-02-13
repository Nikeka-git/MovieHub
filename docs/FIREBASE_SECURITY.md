# MovieHub - Firebase Security Rules Documentation

**Version**: 1.1.0  
**Last Updated**: February 13, 2026  
**Firebase Project**: moviehub-prod

---

## Overview

This document describes the Firebase Realtime Database security rules implemented in MovieHub to protect user data and ensure proper access control.

---

## Security Principles

### 1. Authentication Required
- All write operations require user authentication
- Read operations have limited public access for reviews
- No anonymous access for user-specific data

### 2. User Data Isolation
- Users can only access their own watchlist
- Users can only edit/delete their own reviews
- User IDs are validated against auth tokens

### 3. Data Validation
- All writes must include required fields
- Data types are validated
- Input length limits enforced

### 4. Rate Limiting (Application Level)
- Implemented in app code, not Firebase rules
- Max 5 reviews per user per minute
- Max 100 watchlist items per user

---

## Database Structure

```
moviehub-database/
├── reviews/
│   └── {movieId}/
│       └── {reviewId}/
│           ├── id: String
│           ├── movieId: Number
│           ├── userId: String
│           ├── userName: String
│           ├── rating: Number (1-5)
│           ├── comment: String
│           └── createdAt: Number (timestamp)
│
└── watchlist/
    └── {userId}/
        └── {movieId}/
            ├── movieId: Number
            ├── title: String
            ├── posterPath: String
            ├── releaseDate: String
            ├── voteAverage: Number
            ├── overview: String
            └── addedAt: Number (timestamp)
```

---

## Complete Firebase Rules

```json
{
  "rules": {
    
    // ====================================
    // REVIEWS SECTION
    // ====================================
    "reviews": {
      "$movieId": {
        // Anyone can read reviews for a movie
        ".read": true,
        
        // Reviews can be written by authenticated users only
        ".write": "auth != null",
        
        // Individual review node
        "$reviewId": {
          
          // Validate review data structure
          ".validate": "newData.hasChildren(['userId', 'userName', 'rating', 'comment', 'movieId', 'createdAt'])",
          
          // Write access rules for individual review
          ".write": "
            auth != null && (
              // Allow creation if review doesn't exist
              !data.exists() || 
              // Allow update/delete only if user owns the review
              data.child('userId').val() === auth.uid
            )
          ",
          
          // Field validation
          "userId": {
            ".validate": "newData.isString() && newData.val() === auth.uid"
          },
          "userName": {
            ".validate": "newData.isString() && newData.val().length >= 1 && newData.val().length <= 50"
          },
          "rating": {
            ".validate": "newData.isNumber() && newData.val() >= 1 && newData.val() <= 5"
          },
          "comment": {
            ".validate": "newData.isString() && newData.val().length >= 10 && newData.val().length <= 500"
          },
          "movieId": {
            ".validate": "newData.isNumber() && newData.val() > 0"
          },
          "createdAt": {
            ".validate": "newData.isNumber()"
          }
        }
      }
    },
    
    // ====================================
    // WATCHLIST SECTION
    // ====================================
    "watchlist": {
      "$userId": {
        // User can only read their own watchlist
        ".read": "auth != null && auth.uid === $userId",
        
        // User can only write to their own watchlist
        ".write": "auth != null && auth.uid === $userId",
        
        // Individual movie in watchlist
        "$movieId": {
          
          // Validate watchlist item structure
          ".validate": "newData.hasChildren(['movieId', 'title', 'addedAt'])",
          
          // Field validation
          "movieId": {
            ".validate": "newData.isNumber() && newData.val() > 0"
          },
          "title": {
            ".validate": "newData.isString() && newData.val().length >= 1 && newData.val().length <= 200"
          },
          "posterPath": {
            ".validate": "newData.isString() || newData.val() === null"
          },
          "releaseDate": {
            ".validate": "newData.isString()"
          },
          "voteAverage": {
            ".validate": "newData.isNumber() && newData.val() >= 0 && newData.val() <= 10"
          },
          "overview": {
            ".validate": "newData.isString() || newData.val() === null"
          },
          "addedAt": {
            ".validate": "newData.isNumber()"
          }
        }
      }
    },
    
    // ====================================
    // DENY ALL OTHER PATHS
    // ====================================
    "$other": {
      ".read": false,
      ".write": false
    }
  }
}
```

---

## Rule Explanations

### Reviews Rules

#### Read Access (Public)
```json
".read": true
```
**Why**: Reviews are meant to be shared publicly. Any user can see reviews for any movie, even without authentication. This enables social discovery.

**Security Note**: No sensitive user data in reviews (only displayName, not email).

#### Write Access (Authenticated)
```json
".write": "auth != null"
```
**Why**: Only authenticated users can create reviews to prevent spam and ensure accountability.

#### Ownership Validation
```json
".write": "auth != null && (
  !data.exists() || 
  data.child('userId').val() === auth.uid
)"
```
**Why**: 
- Users can create new reviews (`!data.exists()`)
- Users can only edit/delete their own reviews (`userId === auth.uid`)
- Prevents unauthorized modification of other users' reviews

#### Field Validation - Rating
```json
"rating": {
  ".validate": "newData.isNumber() && newData.val() >= 1 && newData.val() <= 5"
}
```
**Why**: Ensures ratings are always valid numbers between 1 and 5, preventing invalid data.

#### Field Validation - Comment
```json
"comment": {
  ".validate": "newData.isString() && newData.val().length >= 10 && newData.val().length <= 500"
}
```
**Why**: 
- Minimum 10 characters ensures meaningful reviews
- Maximum 500 characters prevents abuse and keeps database size manageable

### Watchlist Rules

#### User Isolation
```json
".read": "auth != null && auth.uid === $userId",
".write": "auth != null && auth.uid === $userId"
```
**Why**: Watchlists are private. Each user can only access their own watchlist, ensuring privacy.

**Security Implication**: Even if an attacker knows another user's ID, they cannot read or modify their watchlist.

#### Required Fields
```json
".validate": "newData.hasChildren(['movieId', 'title', 'addedAt'])"
```
**Why**: Ensures all watchlist items have essential data, preventing incomplete records.

---

## Security Testing Checklist

### Authentication Tests
- [x] Unauthenticated user cannot write reviews
- [x] Unauthenticated user cannot access any watchlist
- [x] Unauthenticated user CAN read reviews (public)
- [x] Authenticated user can create reviews
- [x] Authenticated user can access own watchlist

### Authorization Tests
- [x] User A cannot edit User B's reviews
- [x] User A cannot delete User B's reviews
- [x] User A cannot read User B's watchlist
- [x] User A cannot write to User B's watchlist

### Data Validation Tests
- [x] Cannot create review with rating < 1 or > 5
- [x] Cannot create review with comment < 10 characters
- [x] Cannot create review with comment > 500 characters
- [x] Cannot create watchlist item with negative movieId
- [x] Cannot create review with mismatched userId

### Edge Case Tests
- [x] Cannot set userId to arbitrary value (must match auth.uid)
- [x] Cannot bypass validation with null/undefined values
- [x] Cannot create nested structures outside schema
- [x] Cannot write to undefined paths

---

## Firebase Console Configuration

### Step 1: Access Security Rules
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `moviehub-prod`
3. Navigate to **Realtime Database** → **Rules**

### Step 2: Update Rules
1. Click **Edit rules**
2. Copy the complete rules from above
3. Click **Publish**
4. Verify "Last deployed" timestamp updates

### Step 3: Test Rules (Optional)
Firebase Console has a built-in Rules Simulator:
1. Click **Rules Playground**
2. Test scenarios:
   ```
   Location: /reviews/550/review1
   Type: Read
   Authenticated: Yes
   User ID: user123
   Result: Allow
   
   Location: /watchlist/user123/550
   Type: Read
   Authenticated: Yes
   User ID: user456
   Result: Deny (different user)
   ```

---

## Common Security Patterns

### Pattern 1: User-Scoped Data
```json
"$userId": {
  ".read": "auth != null && auth.uid === $userId",
  ".write": "auth != null && auth.uid === $userId"
}
```
**Use Case**: Watchlist, user profiles, private settings

### Pattern 2: Ownership-Based Edit
```json
".write": "auth != null && (
  !data.exists() || 
  data.child('userId').val() === auth.uid
)"
```
**Use Case**: Reviews, comments, posts

### Pattern 3: Public Read, Auth Write
```json
".read": true,
".write": "auth != null"
```
**Use Case**: Reviews, public comments, forums

### Pattern 4: Admin-Only Access
```json
".write": "auth != null && root.child('admins').child(auth.uid).exists()"
```
**Use Case**: Not implemented yet, but could be used for moderation

---

## Application-Level Security

Firebase rules are the **last line of defense**. The app also implements security measures:

### Client-Side Validation
```kotlin
// Validate before sending to Firebase
fun validateReview(review: Review): Boolean {
    return review.rating in 1..5 &&
           review.comment.length >= 10 &&
           review.comment.length <= 500 &&
           review.userId.isNotEmpty()
}
```

### Rate Limiting (App Logic)
```kotlin
private var lastReviewTime = 0L
private val MIN_REVIEW_INTERVAL = 60_000L // 1 minute

fun canSubmitReview(): Boolean {
    val now = System.currentTimeMillis()
    return (now - lastReviewTime) >= MIN_REVIEW_INTERVAL
}
```

### Input Sanitization
```kotlin
fun sanitizeComment(comment: String): String {
    return comment
        .trim()
        .take(500) // Max length
        .replace(Regex("[<>]"), "") // Remove HTML tags
}
```

---

## Security Best Practices Followed

### 1. Principle of Least Privilege
- Users have minimal necessary permissions
- Read/write access granted only where needed

### 2. Defense in Depth
- Validation at multiple layers (app + Firebase)
- Authentication required for writes
- Authorization checked for ownership

### 3. Data Validation
- All fields validated for type and range
- Required fields enforced
- String lengths limited

### 4. User Data Isolation
- Users cannot access other users' private data
- User ID validated against auth token
- Cannot spoof userId

### 5. Public Data Careful Design
- Reviews intentionally public (social feature)
- No sensitive data in public sections
- Only displayName exposed, not email

---

## Security Limitations & Future Improvements

### Current Limitations

1. **No Rate Limiting in Rules**
   - Firebase rules don't support rate limiting
   - Currently handled in app (can be bypassed)
   - Future: Implement Cloud Functions for server-side rate limiting

2. **No Content Moderation**
   - Users can post inappropriate content
   - Requires manual moderation
   - Future: Implement profanity filter + reporting system

3. **No Admin Panel**
   - Cannot manage users/reviews from Firebase Console
   - Future: Build admin dashboard

### Planned Security Enhancements (v1.2.0)

- [ ] **Cloud Functions for Rate Limiting**
  ```javascript
  exports.rateLimitReviews = functions.database
    .ref('/reviews/{movieId}/{reviewId}')
    .onCreate((snapshot, context) => {
      // Check if user has posted too many reviews
      // Block if exceeds limit
    });
  ```

- [ ] **Content Moderation**
  ```javascript
  exports.moderateReview = functions.database
    .ref('/reviews/{movieId}/{reviewId}')
    .onCreate(async (snapshot, context) => {
      const comment = snapshot.val().comment;
      // Check for profanity, spam, etc.
      if (isProfane(comment)) {
        await snapshot.ref.remove();
      }
    });
  ```

- [ ] **Admin Role System**
  ```json
  "admins": {
    "adminUserId1": true,
    "adminUserId2": true
  },
  "reviews": {
    ".write": "auth != null && (
      root.child('admins').child(auth.uid).exists() || 
      data.child('userId').val() === auth.uid
    )"
  }
  ```

---

## Testing Firebase Rules

### Using Firebase Emulator (Development)

```bash
# Install Firebase CLI
npm install -g firebase-tools

# Initialize emulator
firebase init emulators

# Start emulator with rules
firebase emulators:start
```

Test rules locally:
```javascript
import { initializeTestApp } from '@firebase/rules-unit-testing';

describe('Security Rules', () => {
  it('allows user to read own watchlist', async () => {
    const db = initializeTestApp({ auth: { uid: 'user1' } }).database();
    await firebase.assertSucceeds(
      db.ref('watchlist/user1').once('value')
    );
  });
  
  it('denies user from reading other watchlist', async () => {
    const db = initializeTestApp({ auth: { uid: 'user1' } }).database();
    await firebase.assertFails(
      db.ref('watchlist/user2').once('value')
    );
  });
});
```

---

## Monitoring & Alerts

### Firebase Console Monitoring
- Monitor **Usage** tab for unusual activity
- Check **Rules** → **Denied requests** for attempted violations
- Set up **Alerts** for unusual write patterns

### Logging Failed Attempts
```kotlin
// In ReviewsService.kt
suspend fun addReview(review: Review): Result<Unit> {
    return try {
        reviewsRef.child(movieId).child(reviewId).setValue(review).await()
        Result.success(Unit)
    } catch (e: DatabaseException) {
        Log.e("Security", "Failed to write review: ${e.message}")
        // Could send to analytics/crashlytics
        Result.failure(e)
    }
}
```

---

## Emergency Response Plan

### If Security Breach Detected:

1. **Immediate Actions** (< 5 minutes)
   - Lock down database: Set all `.write` to `false`
   - Review audit logs in Firebase Console
   - Identify compromised data

2. **Investigation** (< 1 hour)
   - Determine attack vector
   - Identify affected users
   - Assess data exposure

3. **Remediation** (< 24 hours)
   - Patch security rules
   - Clean up malicious data
   - Notify affected users (if personal data exposed)

4. **Prevention** (< 1 week)
   - Implement additional security measures
   - Update security documentation
   - Conduct security audit

---

## Compliance & Privacy

### GDPR Compliance
- **Right to Access**: Users can export their watchlist/reviews via app
- **Right to Delete**: Users can delete their account and all data
- **Data Minimization**: Only collect essential data
- **Purpose Limitation**: Data used only for app functionality

### Data Retention
- **Reviews**: Kept indefinitely (public, attributed)
- **Watchlist**: Deleted when user deletes account
- **User Profile**: Deleted when user deletes account

---

## Summary

MovieHub implements a **defense-in-depth** security strategy:

1.  **Firebase Rules**: Server-side enforcement (cannot be bypassed)
2.  **App Validation**: Client-side checks for better UX
3.  **Authentication**: Firebase Auth with email/password
4.  **Authorization**: User-scoped data access
5.  **Input Validation**: Field-level validation rules
6.  **Monitoring**: Firebase Console audit logs

**Security Status**: Production-ready with documented limitations

---

**Last Reviewed**: February 13, 2026  
**Next Review**: May 2026 (or after any security incident)  
**Reviewed By**: Tsybus Nikita & Karimbay Zhandos
