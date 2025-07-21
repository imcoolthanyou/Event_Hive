package gautam.projects.event_hive.Data.Repository

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import gautam.projects.event_hive.Data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class ProfileRepository {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    init {
        // When the repository is created, start listening for profile updates
        // if a user is logged in.
        auth.currentUser?.uid?.let {
            listenForProfileUpdates(it)
        }
    }

    private fun listenForProfileUpdates(userId: String) {
        // Get a reference to the specific user's document in the "users" collection
        val userDocRef = db.collection("users").document(userId)

        userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("ProfileRepository", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                _userProfile.value = snapshot.toObject(UserProfile::class.java)
            } else {
                // If the user document doesn't exist, create it
                createNewUserProfile()
            }
        }
    }

    private fun createNewUserProfile() {
        val firebaseUser = auth.currentUser ?: return
        val newUserProfile = UserProfile(
            uid = firebaseUser.uid,
            name = firebaseUser.displayName,
            email = firebaseUser.email,
            profilePictureUrl = firebaseUser.photoUrl?.toString()
            // Notification settings will use their default values (5km, true, true)
        )
        // Save the new profile to Firestore
        db.collection("users").document(firebaseUser.uid).set(newUserProfile)
    }

    suspend fun updateDiscoveryRadius(radius: Int) {
        val userId = auth.currentUser?.uid ?: return
        try {
            db.collection("users").document(userId)
                .update("discoveryRadius", radius).await()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error updating radius", e)
        }
    }

    suspend fun updateNotificationPreference(field: String, isEnabled: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        try {
            db.collection("users").document(userId)
                .update(field, isEnabled).await()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error updating preference", e)
        }
    }
}
