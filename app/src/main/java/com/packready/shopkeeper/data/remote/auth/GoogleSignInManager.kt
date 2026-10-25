package com.packready.shopkeeper.data.remote.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class GoogleUserAccount(
    val idToken: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val googleId: String
)

@Singleton
class GoogleSignInManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GoogleSignInManager"
        // Web Client ID from google-services.json (Client Type 3)
        const val SERVER_CLIENT_ID = "362156678154-rer0tpe1s3lu3oklst4oism74r0lc5m0.apps.googleusercontent.com"
    }

    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(activityContext: Context): Result<GoogleUserAccount> = withContext(Dispatchers.IO) {
        try {
            // Generate raw nonce for anti-replay security
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(SERVER_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                
                val userAccount = GoogleUserAccount(
                    idToken = googleIdTokenCredential.idToken,
                    email = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName,
                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                    googleId = googleIdTokenCredential.id
                )
                Log.d(TAG, "Successfully authenticated Google account: ${userAccount.email}")
                Result.success(userAccount)
            } else {
                Log.w(TAG, "Unexpected credential type: ${credential.type}")
                Result.failure(IllegalStateException("Unexpected credential type returned from Google Credential Manager"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled Google Sign-In prompt")
            Result.failure(e)
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Failed to parse Google ID Token: ${e.message}")
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-In", e)
            Result.failure(e)
        }
    }
}
