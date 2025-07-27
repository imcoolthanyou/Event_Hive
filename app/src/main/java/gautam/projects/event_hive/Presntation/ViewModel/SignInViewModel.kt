// SignInViewModel.kt
package gautam.projects.event_hive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignInViewModel : ViewModel() {

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn = _isSigningIn.asStateFlow()

    private val _signInError = MutableStateFlow<String?>(null)
    val signInError = _signInError.asStateFlow()

    fun clearError() {
        _signInError.value = null
    }

    suspend fun signInWithGoogle(credential: AuthCredential): Boolean {
        return try {
            _isSigningIn.value = true
            Firebase.auth.signInWithCredential(credential).await()
            _signInError.value = null // Clear previous errors
            _isSigningIn.value = false
            Firebase.messaging.token.addOnCompleteListener { task ->
                if (task.isSuccessful){
                    val token = task.result
                    val userId = Firebase.auth.currentUser?.uid?:return@addOnCompleteListener
                    Firebase.firestore.collection("tokens").document(userId)
                        .set(mapOf("token" to token))

                }
            }
            true // Success
        } catch (e: Exception) {
            _signInError.value = e.message
            _isSigningIn.value = false
            false // Failure
        }
    }

    // ✅ NEW: Function for Email/Password Sign-In
    suspend fun signInWithEmailPassword(email: String, password: String): Boolean {
        return try {
            _isSigningIn.value = true
            Firebase.auth.signInWithEmailAndPassword(email, password).await()
            _signInError.value = null
            _isSigningIn.value = false
            Firebase.messaging.token.addOnCompleteListener { task ->
                if (task.isSuccessful){
                    val token = task.result
                    val userId = Firebase.auth.currentUser?.uid?:return@addOnCompleteListener
                    Firebase.firestore.collection("tokens").document(userId)
                        .set(mapOf("token" to token))

                }
            }
            true // Success
        } catch (e: Exception) {
            _signInError.value = e.message
            _isSigningIn.value = false
            false // Failure
        }
    }

    // ✅ NEW: Function for Email/Password Sign-Up
    suspend fun createUserWithEmailPassword(email: String, password: String): Boolean {
        return try {
            _isSigningIn.value = true
            Firebase.auth.createUserWithEmailAndPassword(email, password).await()
            _signInError.value = null
            _isSigningIn.value = false
            Firebase.messaging.token.addOnCompleteListener { task ->
                if (task.isSuccessful){
                    val token = task.result
                    val userId = Firebase.auth.currentUser?.uid?:return@addOnCompleteListener
                    Firebase.firestore.collection("tokens").document(userId)
                        .set(mapOf("token" to token))

                }
            }
            true // Success
        } catch (e: Exception) {
            _signInError.value = e.message
            _isSigningIn.value = false
            false // Failure
        }
    }
}