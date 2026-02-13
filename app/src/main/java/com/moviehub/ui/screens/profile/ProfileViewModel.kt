package com.moviehub.ui.screens.profile

import androidx.lifecycle.ViewModel
import com.moviehub.data.firebase.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    val authService: AuthService
) : ViewModel()