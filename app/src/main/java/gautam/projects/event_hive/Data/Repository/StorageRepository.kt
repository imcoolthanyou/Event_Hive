package gautam.projects.event_hive.Data.Repository

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.collections.get
import kotlin.coroutines.resume

class StorageRepository {

    /**
     * Uploads a single image file to Cloudinary using its Uri.
     * This function is a suspend function, making it easy to use within coroutines.
     * It wraps the old callback-style SDK into a modern coroutine.
     *
     * @param uri The Uri of the image file on the user's device.
     * @return The public URL of the uploaded image as a String, or null if the upload fails.
     */
    suspend fun uploadImage(uri: Uri): String? {
        return suspendCancellableCoroutine { continuation ->
            val requestId = MediaManager.get().upload(uri)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("StorageRepository", "Image upload started...")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        // You could use this to show an upload progress bar
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String // Use "secure_url" for https
                        Log.d("StorageRepository", "Upload successful: $url")
                        // Resume the coroutine with the successful URL
                        if (continuation.isActive) {
                            continuation.resume(url)
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("StorageRepository", "Upload error: ${error.description}")
                        // Resume the coroutine with null to indicate failure
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        // Not typically used
                    }
                })
                .dispatch()

            // If the calling coroutine is cancelled, we should cancel the upload request
            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
            }
        }
    }
}
