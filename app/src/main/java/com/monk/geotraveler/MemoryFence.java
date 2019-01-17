package com.monk.geotraveler;

import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.maps.model.LatLng;

public class MemoryFence {

    private final String TAG = "MemoryFence";

    private Geofence mGeofence;
    private String id, description;
    private double latitude, longitude;

    public MemoryFence(){}

    public MemoryFence(String id, String description, double latitude, double longitude){
        this.id = id;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        mGeofence = new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(latitude, longitude, 50)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(1000)
                .build();
        Log.d(TAG, "MemoryFence: ");
    }

    public MemoryFence(String id, String description, LatLng point){
        this.id = id;
        this.description = description;
        latitude = point.latitude;
        longitude = point.longitude;
        mGeofence = new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(point.latitude, point.longitude, 50)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(1000)
                .build();
        Log.d(TAG, "MemoryFence: ");
    }

    public Geofence getGeofence(){
        return mGeofence;
    }

    public String getDescription(){
        return description;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getId() {
        return id;
    }
}
