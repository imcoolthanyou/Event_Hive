package gautam.projects.event_hive.core.helper

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import gautam.projects.event_hive.Presntation.ViewModel.NotificationViewModel

class NotificationViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}