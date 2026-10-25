package com.packready.shopkeeper.data.repository

import android.content.Context
import android.util.Log
import com.packready.shopkeeper.data.local.UserProfile
import com.packready.shopkeeper.data.local.UserSessionManager
import com.packready.shopkeeper.data.remote.auth.AuthApiService
import com.packready.shopkeeper.data.remote.auth.GoogleAuthRequestDto
import com.packready.shopkeeper.data.remote.auth.GoogleSignInManager
import com.packready.shopkeeper.data.remote.auth.GoogleUserAccount
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionManager: UserSessionManager,
    private val googleSignInManager: GoogleSignInManager
) {
    val isLoggedIn: StateFlow<Boolean> = sessionManager.isLoggedIn
    val isGuestMode: StateFlow<Boolean> = sessionManager.isGuestMode
    val currentUser: StateFlow<UserProfile?> = sessionManager.currentUser
    val storeInfo: StateFlow<com.packready.shopkeeper.data.local.StoreRegistrationInfo> = sessionManager.storeInfo

    suspend fun signInWithGoogle(activityContext: Context): Result<UserProfile> {
        val googleResult = googleSignInManager.signIn(activityContext)
        return googleResult.fold(
            onSuccess = { account ->
                processGoogleAccount(account)
            },
            onFailure = { err ->
                Result.failure(err)
            }
        )
    }

    private suspend fun processGoogleAccount(account: GoogleUserAccount): Result<UserProfile> {
        return try {
            val response = authApiService.loginWithGoogle(GoogleAuthRequestDto(idToken = account.idToken))
            if (response.isSuccessful && response.body() != null) {
                val tokenDto = response.body()!!
                sessionManager.saveTokens(tokenDto.accessToken, tokenDto.refreshToken)

                // Fetch server user info or use account info
                val meResponse = try { authApiService.getMe() } catch (e: Exception) { null }
                val me = meResponse?.body()

                val profile = UserProfile(
                    id = me?.id ?: account.googleId,
                    name = me?.fullName ?: account.displayName ?: "Shopkeeper",
                    email = me?.email ?: account.email,
                    avatarUrl = me?.avatarUrl ?: account.photoUrl,
                    role = me?.role ?: "SHOPKEEPER",
                    isGoogleAuth = true
                )
                sessionManager.saveUserProfile(
                    userId = profile.id,
                    name = profile.name,
                    email = profile.email,
                    avatarUrl = profile.avatarUrl,
                    role = profile.role,
                    isGoogleAuth = true
                )

                // Check Admin Store Registration Status
                checkStoreRegistration(profile.email)

                Result.success(profile)
            } else {
                // If server returns error, still save Google account locally so user can work
                val fallbackProfile = UserProfile(
                    id = account.googleId,
                    name = account.displayName ?: "Shopkeeper",
                    email = account.email,
                    avatarUrl = account.photoUrl,
                    role = "SHOPKEEPER",
                    isGoogleAuth = true
                )
                sessionManager.saveUserProfile(
                    userId = fallbackProfile.id,
                    name = fallbackProfile.name,
                    email = fallbackProfile.email,
                    avatarUrl = fallbackProfile.avatarUrl,
                    role = fallbackProfile.role,
                    isGoogleAuth = true
                )
                sessionManager.saveTokens(account.idToken, "local-refresh-token")
                checkStoreRegistration(fallbackProfile.email)
                Result.success(fallbackProfile)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Server auth network exception, saving local Google credentials", e)
            val fallbackProfile = UserProfile(
                id = account.googleId,
                name = account.displayName ?: "Shopkeeper",
                email = account.email,
                avatarUrl = account.photoUrl,
                role = "SHOPKEEPER",
                isGoogleAuth = true
            )
            sessionManager.saveUserProfile(
                userId = fallbackProfile.id,
                name = fallbackProfile.name,
                email = fallbackProfile.email,
                avatarUrl = fallbackProfile.avatarUrl,
                role = fallbackProfile.role,
                isGoogleAuth = true
            )
            sessionManager.saveTokens(account.idToken, "local-refresh-token")
            checkStoreRegistration(fallbackProfile.email)
            Result.success(fallbackProfile)
        }
    }

    suspend fun checkStoreRegistration(email: String? = currentUser.value?.email) {
        val targetEmail = email ?: currentUser.value?.email ?: return
        try {
            val res = authApiService.checkStoreRegistration(targetEmail)
            val body = res.body()
            if (body != null && body.isRegistered && body.store != null) {
                val st = body.store
                sessionManager.saveStoreInfo(
                    com.packready.shopkeeper.data.local.StoreRegistrationInfo(
                        isRegistered = true,
                        storeId = st.id,
                        storeName = st.name,
                        address = st.address,
                        city = st.city,
                        pincode = st.pincode,
                        imageUrl = st.imageUrl,
                        contactEmail = body.contactEmail
                    )
                )
            } else {
                sessionManager.saveStoreInfo(
                    com.packready.shopkeeper.data.local.StoreRegistrationInfo(
                        isRegistered = false,
                        contactEmail = body?.contactEmail ?: "dhohadhanaryana@gmail.com"
                    )
                )
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Could not verify store registration with server", e)
        }
    }

    fun continueAsGuest() {
        sessionManager.setGuestMode(true)
    }

    fun logout() {
        sessionManager.clearSession()
    }
}
