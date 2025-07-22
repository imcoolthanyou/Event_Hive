package gautam.projects.event_hive.Presntation.ViewModel

import androidx.lifecycle.ViewModel
import gautam.projects.event_hive.Data.model.MapTargetState
import gautam.projects.event_hive.Data.model.SingleEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint

// A data class to hold the desired state for the map camera.
// It can either be a single point to center on, or a list of events to frame.


class SharedViewModel : ViewModel() {

    private val _initialMapTarget = MutableStateFlow<MapTargetState?>(null)
    val initialMapTarget = _initialMapTarget.asStateFlow()

    /**
     * Sets the desired initial state for the MapScreen.
     * @param target A single GeoPoint to center on.
     * @param zoomToFitAll A list of events to zoom and fit into the view.
     */
    fun setInitialMapTarget(target: GeoPoint?, zoomToFitAll: List<SingleEvent> = emptyList()) {
        _initialMapTarget.value = MapTargetState(
            singleTarget = target,
            zoomToFitEvents = zoomToFitAll
        )
    }

    /**
     * Consumes the map target event so it doesn't trigger again on recomposition.
     */
    fun clearInitialMapTarget() {
        _initialMapTarget.value = null
    }
}
