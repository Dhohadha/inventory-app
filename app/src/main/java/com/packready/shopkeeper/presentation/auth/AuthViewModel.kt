package com.packready.shopkeeper.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packready.shopkeeper.data.local.UserProfile
import com.packready.shopkeeper.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUser: UserProfile? = null,
    val storeInfo: com.packready.shopkeeper.data.local.StoreRegistrationInfo = com.packready.shopkeeper.data.local.StoreRegistrationInfo(),
    val isLoggedIn: Boolean = false,
    val isGuestMode: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                _uiState.update { it.copy(isLoggedIn = loggedIn) }
            }
        }
        viewModelScope.launch {
            authRepository.isGuestMode.collect { guest ->
                _uiState.update { it.copy(isGuestMode = guest) }
            }
        }
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
        viewModelScope.launch {
            authRepository.storeInfo.collect { store ->
                _uiState.update { it.copy(storeInfo = store) }
            }
        }
    }

    fun signInWithGoogle(activityContext: Context, onComplete: ((Boolean) -> Unit)? = null) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(activityContext)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Welcome, ${user.name ?: "Shopkeeper"}!",
                            currentUser = user,
                            isLoggedIn = true,
                            isGuestMode = false
                        )
                    }
                    onComplete?.invoke(true)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.localizedMessage ?: "Failed to sign in with Google"
                        )
                    }
                    onComplete?.invoke(false)
                }
            )
        }
    }

    fun continueAsGuest(onComplete: (() -> Unit)? = null) {
        authRepository.continueAsGuest()
        _uiState.update { it.copy(isGuestMode = true) }
        onComplete?.invoke()
    }

    fun logout(onComplete: (() -> Unit)? = null) {
        authRepository.logout()
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                isGuestMode = false,
                currentUser = null,
                errorMessage = null
            )
        }
        onComplete?.invoke()
    }

    fun checkStoreRegistration(onComplete: ((Boolean) -> Unit)? = null) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            authRepository.checkStoreRegistration()
            val isRegistered = authRepository.storeInfo.value.isRegistered
            _uiState.update { it.copy(isLoading = false) }
            onComplete?.invoke(isRegistered)
        }
    }

    fun switchGoogleAccount(activityContext: Context, onComplete: ((Boolean) -> Unit)? = null) {
        logout {
            signInWithGoogle(activityContext, onComplete)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
