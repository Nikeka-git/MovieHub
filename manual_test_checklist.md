# Manual Test Checklist - MovieHub

## Authentication & User Management
- [ ] User can register with email and password
- [ ] User can login with valid credentials
- [ ] Login fails with invalid credentials and shows error message
- [ ] User session persists after app restart
- [ ] User can logout successfully
- [ ] Password reset email is sent successfully

## Movie Discovery
- [ ] Movies load on app launch
- [ ] Pagination works - more movies load on scroll
- [ ] Pull-to-refresh updates movie list
- [ ] Movie cards display poster, title, rating correctly
- [ ] Clicking movie card navigates to details screen

## Search & Filtering
- [ ] Search bar accepts text input
- [ ] Search is debounced (doesn't trigger on every keystroke)
- [ ] Search results update after 500ms pause
- [ ] Genre filters work correctly
- [ ] Multiple genres can be selected
- [ ] Sort options (popularity, rating, date) work
- [ ] Clear filters button resets all filters
- [ ] Empty search shows appropriate message

## Movie Details
- [ ] Movie details load completely
- [ ] Backdrop/poster images display correctly
- [ ] Cast list shows with images
- [ ] Similar movies section appears
- [ ] Back button returns to previous screen

## Watchlist (Offline-First)
- [ ] User can add movie to watchlist
- [ ] Bookmark icon toggles correctly
- [ ] Watchlist count updates in profile
- [ ] Watchlist persists offline
- [ ] Watchlist syncs when back online
- [ ] User can remove from watchlist

## Reviews (Firebase Realtime)
- [ ] Reviews load in real-time
- [ ] User can add a new review
- [ ] Review appears immediately for all users
- [ ] Rating slider works (0.5 to 5.0)
- [ ] Comment validation works (min 10 chars)
- [ ] Average rating calculates correctly
- [ ] User can see their own reviews in profile

## Offline Functionality
- [ ] App works without internet for cached content
- [ ] Offline indicator shown when no connection
- [ ] Cached movies are viewable offline
- [ ] Search works on cached data
- [ ] Watchlist accessible offline
- [ ] Data syncs when connection restored

## Error Handling
- [ ] Network timeout shows error with retry button
- [ ] Invalid API response shows user-friendly error
- [ ] Empty states show helpful messages
- [ ] Image loading failures show placeholder
- [ ] Firebase errors handled gracefully

## UI/UX
- [ ] App follows Material Design 3 guidelines
- [ ] Dark mode works correctly
- [ ] Animations are smooth
- [ ] Loading states show progress indicators
- [ ] Transitions between screens are fluid
- [ ] No UI freezing during operations

## Performance
- [ ] App launches in under 3 seconds
- [ ] Scrolling is smooth (60 FPS)
- [ ] Images load progressively
- [ ] No memory leaks during navigation
- [ ] Background tasks don't block UI