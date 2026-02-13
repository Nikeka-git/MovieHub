package com.moviehub.ui.screens.detail

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moviehub.BuildConfig
import com.moviehub.domain.model.Cast
import com.moviehub.domain.model.Movie
import com.moviehub.domain.model.Review
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow

private const val TAG = "MovieDetailScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    onBackClick: () -> Unit,
    onLoginRequired: () -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    Log.d(TAG, "MovieDetailScreen composed")

    val uiState by viewModel.uiState.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val isInWatchlist by viewModel.isInWatchlist.collectAsState()
    val averageRating by viewModel.averageRating.collectAsState()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val watchlistError by viewModel.watchlistError.collectAsState()
    val reviewSubmitState by viewModel.reviewSubmitState.collectAsState()

    var showReviewDialog by remember { mutableStateOf(false) }
    var showGuestDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Показываем ошибку watchlist через тот же snackbar
    LaunchedEffect(watchlistError) {
        watchlistError?.let { error ->
            snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
            viewModel.clearWatchlistError()
        }
    }

    LaunchedEffect(reviewSubmitState) {
        Log.d(TAG, "reviewSubmitState changed: $reviewSubmitState")
    }

    LaunchedEffect(reviewSubmitState) {
        when (val state = reviewSubmitState) {
            is ReviewSubmitState.Success -> {
                Log.d(TAG, "Showing success snackbar: ${state.message}")
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Short
                )
            }
            is ReviewSubmitState.Error -> {
                Log.e(TAG, "Showing error snackbar: ${state.message}")
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Movie Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    Log.d(TAG, "FAB clicked - isUserLoggedIn: $isUserLoggedIn")
                    if (isUserLoggedIn) {
                        showReviewDialog = true
                    } else {
                        showGuestDialog = true
                    }
                }
            ) {
                Icon(Icons.Default.RateReview, contentDescription = "Add Review")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        when (val state = uiState) {
            is MovieDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is MovieDetailUiState.Success -> {
                MovieDetailContent(
                    movie = state.movie,
                    reviews = reviews,
                    isInWatchlist = isInWatchlist,
                    averageRating = averageRating,
                    onToggleWatchlist = { viewModel.toggleWatchlist() },
                    modifier = Modifier.padding(padding)
                )
            }
            is MovieDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${state.message}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }

    // Диалог для гостей — предлагаем войти в аккаунт
    if (showGuestDialog) {
        AlertDialog(
            onDismissRequest = { showGuestDialog = false },
            title = { Text("Sign in required") },
            text = {
                Text("You need to be signed in to leave a review. Would you like to go to the login screen?")
            },
            confirmButton = {
                Button(onClick = {
                    showGuestDialog = false
                    onLoginRequired()
                }) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(showReviewDialog) {
        Log.d(TAG, "showReviewDialog changed: $showReviewDialog")
    }

    if (showReviewDialog) {
        Log.d(TAG, "Showing ReviewDialog - isSubmitting: ${reviewSubmitState is ReviewSubmitState.Loading}")
        ReviewDialog(
            isSubmitting = reviewSubmitState is ReviewSubmitState.Loading,
            onDismiss = {
                Log.d(TAG, "ReviewDialog dismissed")
                showReviewDialog = false
                viewModel.resetReviewSubmitState()
            },
            onSubmit = { rating, comment ->
                Log.d(TAG, "🔵 ReviewDialog onSubmit called - rating: $rating, comment: '$comment'")
                viewModel.addReview(rating, comment)
            }
        )
    }

    LaunchedEffect(reviewSubmitState) {
        if (reviewSubmitState is ReviewSubmitState.Success) {
            Log.d(TAG, "Success state detected - closing dialog")
            showReviewDialog = false
        }
    }
}

@Composable
fun MovieDetailContent(
    movie: Movie,
    reviews: List<Review>,
    isInWatchlist: Boolean,
    averageRating: Float,
    onToggleWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            AsyncImage(
                model = "${BuildConfig.TMDB_IMAGE_BASE_URL}${movie.backdropPath ?: movie.posterPath}",
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentScale = ContentScale.Crop
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", movie.voteAverage),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = " (${movie.voteCount} votes)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onToggleWatchlist) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Toggle Watchlist",
                            tint = if (isInWatchlist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    movie.genres.forEach { genre ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(genre.name) }
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = movie.overview,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (movie.cast.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Cast",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(movie.cast) { cast ->
                        CastCard(cast)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reviews (${reviews.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (reviews.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", averageRating),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(reviews) { review ->
            ReviewCard(review = review)
        }

        if (reviews.isEmpty()) {
            item {
                Text(
                    text = "No reviews yet. Be the first to review!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun CastCard(cast: Cast) {
    Column(
        modifier = Modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = "${BuildConfig.TMDB_IMAGE_BASE_URL}${cast.profilePath}",
            contentDescription = cast.name,
            modifier = Modifier
                .size(100.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = cast.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = cast.character,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ReviewCard(review: Review) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.userName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", review.rating),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ReviewDialog(
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (Float, String) -> Unit
) {
    Log.d(TAG, "ReviewDialog composed - isSubmitting: $isSubmitting")

    var rating by remember { mutableStateOf(3f) }
    var comment by remember { mutableStateOf("") }

    val isButtonEnabled = comment.isNotBlank() && !isSubmitting

    AlertDialog(
        onDismissRequest = {
            if (!isSubmitting) onDismiss()
        },
        title = { Text("Write a Review") },
        text = {
            Column {
                Text("Rating: ${String.format("%.1f", rating)}")
                Slider(
                    value = rating,
                    onValueChange = { rating = it },
                    valueRange = 0.5f..5f,
                    steps = 8,
                    enabled = !isSubmitting
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Your review") },
                    placeholder = { Text("Write your thoughts about this movie...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isSubmitting
                )

                if (isSubmitting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submitting review...")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(rating, comment) },
                enabled = isButtonEnabled
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() },
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}