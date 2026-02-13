package com.moviehub.ui.screens.signup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviehub.data.firebase.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    companion object {
        private const val TAG = "SignUpViewModel"
    }

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun signUp(email: String, password: String, displayName: String) {
        Log.d(TAG, "SignUp attempt with email: $email, displayName: $displayName")

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                authService.signUp(email, password, displayName).fold(
                    onSuccess = { user ->
                        Log.d(TAG, "SignUp successful for user: ${user.id}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSignedUp = true,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "SignUp failed: ${error.message}", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Sign up failed"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception during sign up", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class SignUpUiState(
    val isLoading: Boolean = false,
    val isSignedUp: Boolean = false,
    val error: String? = null
)