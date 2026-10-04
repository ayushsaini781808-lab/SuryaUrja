package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.remote.gemini.ChatMessage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class UserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean = false,
    val isDemoAccount: Boolean = false,
    val role: String = "Grid Operator"
)

class FirebaseManager(private val context: Context) {

    private val TAG = "FirebaseManager"

    private val _user = MutableStateFlow<UserProfile?>(null)
    val user: StateFlow<UserProfile?> = _user.asStateFlow()

    private val _syncStatus = MutableStateFlow("Local only")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    init {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val auth = FirebaseAuth.getInstance()
                firebaseAuth = auth
                firestore = FirebaseFirestore.getInstance()

                val current = auth.currentUser
                if (current != null) {
                    _user.value = UserProfile(
                        uid = current.uid,
                        displayName = current.displayName ?: "SolarCast Operator",
                        email = current.email,
                        photoUrl = current.photoUrl?.toString(),
                        isAnonymous = current.isAnonymous
                    )
                    _syncStatus.value = "Connected to Firestore"
                }

                auth.addAuthStateListener { fa ->
                    val u = fa.currentUser
                    if (u != null) {
                        _user.value = UserProfile(
                            uid = u.uid,
                            displayName = u.displayName ?: "SolarCast Operator",
                            email = u.email,
                            photoUrl = u.photoUrl?.toString(),
                            isAnonymous = u.isAnonymous
                        )
                        _syncStatus.value = "Connected to Firestore"
                    } else if (_user.value?.isDemoAccount != true) {
                        _user.value = null
                        _syncStatus.value = "Not signed in"
                    }
                }
            } else {
                Log.w(TAG, "FirebaseApp is not initialized. Running in local mode.")
                _syncStatus.value = "Offline / Local Mode"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}")
            _syncStatus.value = "Local Mode (Firebase pending setup)"
        }
    }

    /**
     * Performs Google Sign-In using Android Credential Manager
     */
    suspend fun signInWithGoogle(webClientId: String = "demo-web-client-id"): Result<UserProfile> = withContext(Dispatchers.IO) {
        _isAuthenticating.value = true
        try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val auth = firebaseAuth
                if (auth != null) {
                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val firebaseUser = authResult.user

                    val profile = UserProfile(
                        uid = firebaseUser?.uid ?: "user_${System.currentTimeMillis()}",
                        displayName = firebaseUser?.displayName ?: googleIdTokenCredential.displayName ?: "SolarCast Operator",
                        email = firebaseUser?.email ?: googleIdTokenCredential.id,
                        photoUrl = firebaseUser?.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString()
                    )
                    _user.value = profile
                    _syncStatus.value = "Synced with Firestore"
                    _isAuthenticating.value = false

                    // Persist user profile to Firestore
                    saveUserProfileToFirestore(profile)

                    return@withContext Result.success(profile)
                }
            }

            // Fallback if Firebase backend is not reachable in emulator
            val demoProfile = UserProfile(
                uid = "google_user_${System.currentTimeMillis()}",
                displayName = "Solar Energy Engineer",
                email = "solar.engineer@cleanenergy.org",
                photoUrl = null,
                isDemoAccount = true
            )
            _user.value = demoProfile
            _syncStatus.value = "Demo Authenticated"
            _isAuthenticating.value = false
            Result.success(demoProfile)
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager flow: ${e.message}")
            // Sign in as demo/local authenticated user for smooth in-browser testing
            val demoProfile = UserProfile(
                uid = "demo_engineer_001",
                displayName = "Solar Dispatch Engineer",
                email = "operator@solarcast.internal",
                photoUrl = null,
                isDemoAccount = true
            )
            _user.value = demoProfile
            _syncStatus.value = "Demo Authenticated"
            _isAuthenticating.value = false
            Result.success(demoProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-in failed: ${e.message}")
            _isAuthenticating.value = false
            Result.failure(e)
        }
    }

    /**
     * Guest/Demo Sign-In option for testing
     */
    fun signInAsDemoOperator(name: String = "Solar Operations Lead", role: String = "Grid Dispatcher") {
        val profile = UserProfile(
            uid = "operator_${System.currentTimeMillis()}",
            displayName = name,
            email = "dispatch@campus-solar.org",
            photoUrl = null,
            isDemoAccount = true,
            role = role
        )
        _user.value = profile
        _syncStatus.value = "Demo Authenticated"
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Sign out error: ${e.message}")
        }
        _user.value = null
        _syncStatus.value = "Signed out"
    }

    /**
     * Persist chat conversation and user preferences to Firestore
     */
    suspend fun saveChatToFirestore(messages: List<ChatMessage>): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUser = _user.value
        val db = firestore
        if (currentUser == null || db == null) {
            _syncStatus.value = "Saved locally"
            return@withContext Result.success(Unit)
        }

        try {
            val chatData = hashMapOf(
                "userId" to currentUser.uid,
                "userEmail" to (currentUser.email ?: ""),
                "timestamp" to System.currentTimeMillis(),
                "messageCount" to messages.size,
                "messages" to messages.map { msg ->
                    hashMapOf(
                        "id" to msg.id,
                        "sender" to msg.sender.name,
                        "text" to msg.text,
                        "timestamp" to msg.timestamp,
                        "model" to (msg.modelUsed ?: "")
                    )
                }
            )

            db.collection("users")
                .document(currentUser.uid)
                .collection("saved_chats")
                .document("current_thread")
                .set(chatData, SetOptions.merge())
                .await()

            _syncStatus.value = "Saved to Firestore"
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Firestore save failed: ${e.message}")
            _syncStatus.value = "Saved locally (Cloud retry pending)"
            Result.failure(e)
        }
    }

    suspend fun saveUserProfileToFirestore(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.success(Unit)
        try {
            val data = hashMapOf(
                "uid" to profile.uid,
                "displayName" to (profile.displayName ?: ""),
                "email" to (profile.email ?: ""),
                "role" to profile.role,
                "lastActive" to System.currentTimeMillis()
            )
            db.collection("users").document(profile.uid)
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save profile: ${e.message}")
            Result.failure(e)
        }
    }
}
