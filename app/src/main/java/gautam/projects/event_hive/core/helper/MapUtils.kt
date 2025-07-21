package gautam.projects.event_hive.core.helper

import android.content.Context
import android.location.Address
import android.location.Geocoder
import org.osmdroid.util.GeoPoint // ✅ Correct import for osmdroid
import java.io.IOException

/**
 * Converts a string address into geographic coordinates (latitude and longitude).
 *
 * @param context The application context.
 * @param address The string representation of the address to geocode.
 * @return A GeoPoint object containing the latitude and longitude, or null if the address could not be found.
 */
fun getCoordinatesFromAddress(context: Context, address: String): GeoPoint? {

    val geocoder = Geocoder(context)

    return try {
        // Note: getFromLocationName can be slow and should ideally be called from a background thread.
        // Our LocationAutocompleteTextField already does this within a coroutine.
        val addresses: List<Address>? = geocoder.getFromLocationName(address, 1)

        if (addresses.isNullOrEmpty()) {
            null // Address not found
        } else {
            val location = addresses[0]
            // ✅ Create a GeoPoint object, which is what osmdroid uses
            GeoPoint(location.latitude, location.longitude)
        }
    } catch (e: IOException) {
        // Handle network errors or other I/O problems when geocoding
        e.printStackTrace()
        null
    }
}
