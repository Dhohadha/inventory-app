package com.packready.shopkeeper.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class UserProfile(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val role: String? = "SHOPKEEPER",
    val isGoogleAuth: Boolean = false
)

data class StoreRegistrationInfo(
    val isRegistered: Boolean = false,
    val storeId: String? = null,
    val storeName: String? = null,
    val address: String? = null,
    val city: String? = null,
    val pincode: String? = null,
    val imageUrl: String? = null,
    val contactEmail: String = "dhohadhanaryana@gmail.com"
)

@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("packready_session", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(getAccessToken() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isGuestMode = MutableStateFlow(prefs.getBoolean(KEY_IS_GUEST, false))
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    private val _currentUser = MutableStateFlow(loadUserProfile())
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _storeInfo = MutableStateFlow(loadStoreInfo())
    val storeInfo: StateFlow<StoreRegistrationInfo> = _storeInfo.asStateFlow()

    private fun loadStoreInfo(): StoreRegistrationInfo {
        return StoreRegistrationInfo(
            isRegistered = prefs.getBoolean(KEY_STORE_IS_REGISTERED, false),
            storeId = prefs.getString(KEY_ACTIVE_STORE_ID, null),
            storeName = prefs.getString(KEY_STORE_NAME, null),
            address = prefs.getString(KEY_STORE_ADDRESS, null),
            city = prefs.getString(KEY_STORE_CITY, null),
            pincode = prefs.getString(KEY_STORE_PINCODE, null),
            imageUrl = prefs.getString(KEY_STORE_IMAGE_URL, null),
            contactEmail = "dhohadhanaryana@gmail.com"
        )
    }

    private fun loadUserProfile(): UserProfile? {
        val email = prefs.getString(KEY_USER_EMAIL, null)
        val name = prefs.getString(KEY_USER_NAME, null)
        if (email == null && name == null && getAccessToken() == null) return null
        return UserProfile(
            id = prefs.getString(KEY_USER_ID, null),
            name = name,
            email = email,
            avatarUrl = prefs.getString(KEY_USER_AVATAR, null),
            role = prefs.getString(KEY_USER_ROLE, "SHOPKEEPER"),
            isGoogleAuth = prefs.getBoolean(KEY_IS_GOOGLE_AUTH, false)
        )
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putBoolean(KEY_IS_GUEST, false)
            .apply()
        _isGuestMode.value = false
        _isLoggedIn.value = true
    }

    fun saveUserProfile(
        userId: String? = null,
        name: String? = null,
        email: String? = null,
        avatarUrl: String? = null,
        role: String? = "SHOPKEEPER",
        isGoogleAuth: Boolean = true
    ) {
        val editor = prefs.edit()
        userId?.let { editor.putString(KEY_USER_ID, it) }
        name?.let { editor.putString(KEY_USER_NAME, it) }
        email?.let { editor.putString(KEY_USER_EMAIL, it) }
        avatarUrl?.let { editor.putString(KEY_USER_AVATAR, it) }
        role?.let { editor.putString(KEY_USER_ROLE, it) }
        editor.putBoolean(KEY_IS_GOOGLE_AUTH, isGoogleAuth)
        editor.apply()

        _currentUser.value = UserProfile(
            id = userId ?: prefs.getString(KEY_USER_ID, null),
            name = name ?: prefs.getString(KEY_USER_NAME, null),
            email = email ?: prefs.getString(KEY_USER_EMAIL, null),
            avatarUrl = avatarUrl ?: prefs.getString(KEY_USER_AVATAR, null),
            role = role ?: prefs.getString(KEY_USER_ROLE, "SHOPKEEPER"),
            isGoogleAuth = isGoogleAuth
        )
    }

    fun setGuestMode(isGuest: Boolean = true) {
        prefs.edit().putBoolean(KEY_IS_GUEST, isGuest).apply()
        _isGuestMode.value = isGuest
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun saveActiveStoreId(storeId: String) {
        prefs.edit().putString(KEY_ACTIVE_STORE_ID, storeId).apply()
    }

    fun getActiveStoreId(): String? = prefs.getString(KEY_ACTIVE_STORE_ID, "default-store")

    fun saveStoreInfo(info: StoreRegistrationInfo) {
        prefs.edit()
            .putBoolean(KEY_STORE_IS_REGISTERED, info.isRegistered)
            .putString(KEY_ACTIVE_STORE_ID, info.storeId)
            .putString(KEY_STORE_NAME, info.storeName)
            .putString(KEY_STORE_ADDRESS, info.address)
            .putString(KEY_STORE_CITY, info.city)
            .putString(KEY_STORE_PINCODE, info.pincode)
            .putString(KEY_STORE_IMAGE_URL, info.imageUrl)
            .apply()
        _storeInfo.value = info
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
        _isGuestMode.value = false
        _currentUser.value = null
        _storeInfo.value = StoreRegistrationInfo()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ACTIVE_STORE_ID = "active_store_id"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_IS_GOOGLE_AUTH = "is_google_auth"
        private const val KEY_STORE_IS_REGISTERED = "store_is_registered"
        private const val KEY_STORE_NAME = "store_name"
        private const val KEY_STORE_ADDRESS = "store_address"
        private const val KEY_STORE_CITY = "store_city"
        private const val KEY_STORE_PINCODE = "store_pincode"
        private const val KEY_STORE_IMAGE_URL = "store_image_url"
    }
}
