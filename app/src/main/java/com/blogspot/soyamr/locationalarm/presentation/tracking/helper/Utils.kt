package com.blogspot.soyamr.locationalarm.presentation.tracking.helper

import android.content.Context
import android.location.Location
import android.media.Ringtone
import androidx.core.content.edit
import com.blogspot.soyamr.locationalarm.R

/**
 * Returns the `location` object as a human readable string.
 */
fun Location?.toText(): String {
    return "left " + this?.distanceTo(globalDestination)?.toInt().toString() + " meters"
        ?: "Unknown location"
}

val globalDestination = Location("")
var globalArrived = false

/**
 * Provides access to SharedPreferences for location to Activities and Services.
 */
internal object SharedPreferenceUtil {

    const val KEY_FOREGROUND_ENABLED = "tracking_foreground_location"

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The [Context].
     */
    fun getLocationTrackingPref(context: Context): Boolean =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )
            .getBoolean(KEY_FOREGROUND_ENABLED, false)

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    fun saveLocationTrackingPref(context: Context, requestingLocationUpdates: Boolean) =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        ).edit {
            putBoolean(KEY_FOREGROUND_ENABLED, requestingLocationUpdates)
        }
}
