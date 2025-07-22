package gautam.projects.event_hive.Data.model

import org.osmdroid.util.GeoPoint

data class MapTargetState(
    val singleTarget: GeoPoint? = null,
    val zoomToFitEvents: List<SingleEvent> = emptyList()
)
