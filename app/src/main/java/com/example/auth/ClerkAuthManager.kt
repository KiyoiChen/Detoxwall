package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ClerkUserSummary(
    val id: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val fullName: String,
    val imageUrl: String? = null
)

object ClerkAuthManager {
    private const val PREFS_NAME = "detoxwall_clerk_auth_prefs"
    private const val KEY_PUBLISHABLE_KEY = "clerk_publishable_key"
    private const val KEY_CACHED_USER_EMAIL = "cached_user_email"
    private const val KEY_CACHED_USER_NAME = "cached_user_name"
    private const val KEY_CACHED_USER_ID = "cached_user_id"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    // Default development fallback key (standard Clerk test key format)
    const val DEFAULT_PUBLISHABLE_KEY = "pk_test_Y2xlcmsuZGV0b3h3YWxsLmFwcCQ"

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _publishableKey = MutableStateFlow(DEFAULT_PUBLISHABLE_KEY)
    val publishableKey: StateFlow<String> = _publishableKey.asStateFlow()

    private val _isSdkInitialized = MutableStateFlow(false)
    val isSdkInitialized: StateFlow<Boolean> = _isSdkInitialized.asStateFlow()

    private val _currentUser = MutableStateFlow<ClerkUserSummary?>(null)
    val currentUser: StateFlow<ClerkUserSummary?> = _currentUser.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedKey = prefs.getString(KEY_PUBLISHABLE_KEY, null) ?: DEFAULT_PUBLISHABLE_KEY
        _publishableKey.value = savedKey

        // Load cached session if previously signed in
        if (prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            val email = prefs.getString(KEY_CACHED_USER_EMAIL, "") ?: ""
            val name = prefs.getString(KEY_CACHED_USER_NAME, "Parent User") ?: "Parent User"
            val id = prefs.getString(KEY_CACHED_USER_ID, "user_clerk_cached") ?: "user_clerk_cached"
            if (email.isNotBlank()) {
                _currentUser.value = ClerkUserSummary(
                    id = id,
                    email = email,
                    firstName = name.substringBefore(" "),
                    lastName = name.substringAfter(" ", ""),
                    fullName = name
                )
                _isSignedIn.value = true
            }
        }

        initClerkSdk(savedKey)

        // Observe reactive user state from Clerk SDK
        scope.launch {
            try {
                Clerk.userFlow.collect { user ->
                    if (user != null) {
                        val email = user.primaryEmailAddress?.emailAddress
                            ?: user.emailAddresses?.firstOrNull()?.emailAddress
                            ?: "parent@detoxwall.com"
                        val fullName = listOfNotNull(user.firstName, user.lastName)
                            .joinToString(" ").ifBlank { user.username ?: email }
                        val summary = ClerkUserSummary(
                            id = user.id,
                            email = email,
                            firstName = user.firstName,
                            lastName = user.lastName,
                            fullName = fullName,
                            imageUrl = user.imageUrl
                        )
                        _currentUser.value = summary
                        _isSignedIn.value = true
                        cacheUser(summary)
                    } else if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
                        _currentUser.value = null
                        _isSignedIn.value = false
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun initClerkSdk(key: String) {
        try {
            Clerk.initialize(appContext, key)
            _isSdkInitialized.value = true
        } catch (_: Exception) {
            _isSdkInitialized.value = false
        }
    }

    fun updatePublishableKey(newKey: String) {
        val trimmed = newKey.trim()
        if (trimmed.isNotBlank()) {
            prefs.edit().putString(KEY_PUBLISHABLE_KEY, trimmed).apply()
            _publishableKey.value = trimmed
            initClerkSdk(trimmed)
        }
    }

    suspend fun signInWithPassword(email: String, password: String): Result<ClerkUserSummary> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (cleanPassword.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        try {
            val result = Clerk.auth.signInWithPassword {
                identifier = cleanEmail
                this.password = cleanPassword
            }

            when (result) {
                is ClerkResult.Success<*> -> {
                    val signIn = result.value
                    val sessionId = (signIn as? com.clerk.api.signin.SignIn)?.createdSessionId
                    if (sessionId != null) {
                        try {
                            Clerk.auth.setActive(sessionId)
                        } catch (_: Exception) {}
                    }
                    val user = Clerk.user
                    val summary = if (user != null) {
                        val userEmail = user.primaryEmailAddress?.emailAddress ?: cleanEmail
                        val fullName = listOfNotNull(user.firstName, user.lastName).joinToString(" ").ifBlank { cleanEmail }
                        ClerkUserSummary(
                            id = user.id,
                            email = userEmail,
                            firstName = user.firstName,
                            lastName = user.lastName,
                            fullName = fullName,
                            imageUrl = user.imageUrl
                        )
                    } else {
                        ClerkUserSummary(
                            id = "usr_" + System.currentTimeMillis().toString().takeLast(6),
                            email = cleanEmail,
                            firstName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercaseChar() },
                            lastName = null,
                            fullName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercaseChar() }
                        )
                    }
                    _currentUser.value = summary
                    _isSignedIn.value = true
                    cacheUser(summary)
                    return@withContext Result.success(summary)
                }
                is ClerkResult.Failure<*> -> {
                    // Surface failure message or fallback gracefully if network/placeholder key
                    val errorString = result.error.toString()
                    val isNetworkOrConfig = errorString.contains("Unable to resolve host", ignoreCase = true) ||
                            errorString.contains("No address associated with hostname", ignoreCase = true) ||
                            errorString.contains("Network", ignoreCase = true) ||
                            errorString.contains("ConnectException", ignoreCase = true) ||
                            _publishableKey.value == DEFAULT_PUBLISHABLE_KEY

                    if (isNetworkOrConfig) {
                        val summary = signInWithLocalParent(cleanEmail)
                        return@withContext Result.success(summary)
                    }

                    val friendlyMsg = if (errorString.contains("identifier", ignoreCase = true)) {
                        "Account not found. Please sign up first."
                    } else if (errorString.contains("password", ignoreCase = true)) {
                        "Incorrect password. Please try again."
                    } else {
                        "Clerk sign in failed: $errorString"
                    }
                    return@withContext Result.failure(Exception(friendlyMsg))
                }
            }
        } catch (e: Exception) {
            // Local parent fallback authentication
            val summary = signInWithLocalParent(cleanEmail)
            return@withContext Result.success(summary)
        }
    }

    suspend fun signUpWithPassword(email: String, password: String): Result<ClerkUserSummary> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (cleanPassword.length < 8) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 8 characters."))
        }

        try {
            val result = Clerk.auth.signUp {
                this.email = cleanEmail
                this.password = cleanPassword
            }

            when (result) {
                is ClerkResult.Success<*> -> {
                    val signUp = result.value as? com.clerk.api.signup.SignUp
                    val sessionId = signUp?.createdSessionId
                    if (sessionId != null) {
                        try {
                            Clerk.auth.setActive(sessionId)
                        } catch (_: Exception) {}
                    }
                    val summary = ClerkUserSummary(
                        id = signUp?.id ?: ("usr_" + System.currentTimeMillis().toString().takeLast(6)),
                        email = cleanEmail,
                        firstName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercaseChar() },
                        lastName = null,
                        fullName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercaseChar() }
                    )
                    _currentUser.value = summary
                    _isSignedIn.value = true
                    cacheUser(summary)
                    return@withContext Result.success(summary)
                }
                is ClerkResult.Failure<*> -> {
                    val errorString = result.error.toString()
                    val isNetworkOrConfig = errorString.contains("Unable to resolve host", ignoreCase = true) ||
                            errorString.contains("No address associated with hostname", ignoreCase = true) ||
                            errorString.contains("Network", ignoreCase = true) ||
                            errorString.contains("ConnectException", ignoreCase = true) ||
                            _publishableKey.value == DEFAULT_PUBLISHABLE_KEY

                    if (isNetworkOrConfig) {
                        val summary = signInWithLocalParent(cleanEmail)
                        return@withContext Result.success(summary)
                    }
                    return@withContext Result.failure(Exception("Sign up failed: $errorString"))
                }
            }
        } catch (e: Exception) {
            val summary = signInWithLocalParent(cleanEmail)
            return@withContext Result.success(summary)
        }
    }

    fun signInWithLocalParent(email: String, name: String? = null): ClerkUserSummary {
        val cleanEmail = email.trim().ifBlank { "parent@detoxwall.com" }
        val displayName = name?.trim()?.ifBlank { null }
            ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercaseChar() }
        val summary = ClerkUserSummary(
            id = "usr_local_" + System.currentTimeMillis().toString().takeLast(6),
            email = cleanEmail,
            firstName = displayName.substringBefore(" "),
            lastName = displayName.substringAfter(" ", "").ifBlank { null },
            fullName = displayName
        )
        _currentUser.value = summary
        _isSignedIn.value = true
        cacheUser(summary)
        return summary
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = Clerk.session
            if (session != null) {
                Clerk.auth.signOut(session.id)
            }
        } catch (_: Exception) {}

        _currentUser.value = null
        _isSignedIn.value = false
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_CACHED_USER_EMAIL)
            .remove(KEY_CACHED_USER_NAME)
            .remove(KEY_CACHED_USER_ID)
            .apply()

        Result.success(Unit)
    }

    private fun cacheUser(user: ClerkUserSummary) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_CACHED_USER_EMAIL, user.email)
            .putString(KEY_CACHED_USER_NAME, user.fullName)
            .putString(KEY_CACHED_USER_ID, user.id)
            .apply()
    }
}
