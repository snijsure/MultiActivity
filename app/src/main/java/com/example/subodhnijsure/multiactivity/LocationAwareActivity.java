package com.example.subodhnijsure.multiactivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.canelmas.let.AskPermission;
import com.canelmas.let.DeniedPermission;
import com.canelmas.let.Let;
import com.canelmas.let.RuntimePermissionListener;
import com.canelmas.let.RuntimePermissionRequest;
/**
 * Created by subodhnijsure on 12/7/17.
 */



public abstract class LocationAwareActivity extends AppCompatActivity {
    public static final long INITIAL_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            INITIAL_UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    public static final long UPDATE_INTERVAL_AFTER_LOCK_IN_MILLISECONDS = 60000;
    public static final long FAST_UPDATE_INTERVAL_AFTER_LOCK_IN_MILLISECONDS =
            UPDATE_INTERVAL_AFTER_LOCK_IN_MILLISECONDS / 2;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final static int DISPLACEMENT_IN_METERS = 10;
    public static String TAG = LocationAwareActivity.class.getSimpleName();
    protected LocationRequest fineLocationRequest;
    protected Location currentLocation;
    // See https://android-developers.googleblog.com/2017/06/reduce-friction-with-new-location-apis.html
    FusedLocationProviderClient locationProviderClient;
    LocationCallback locationCallback;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createLocationProvider();
    }

    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        locationProviderClient = null;
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        stopLocationUpdates();
        super.onStop();
    }

    protected synchronized void createLocationProvider() {
        if (hasLocationPermission()) {
            Log.i(TAG, "createLocationProvider have location permission building client");
            locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    onLocationChanged(locationResult);
                }

                @Override
                public void onLocationAvailability(LocationAvailability locationAvailability) {
                    super.onLocationAvailability(locationAvailability);
                }
            };
        } else {
            Log.i(TAG, "createLocationProvider don't have location permission");
        }
    }

    private void createLocationRequest() {
    /*
      Request location update every second minutes or user moves 10 meters (32 ft).
     */
        Log.d(TAG, "createLocationRequest ");
        fineLocationRequest = new LocationRequest();
        fineLocationRequest.setInterval(INITIAL_UPDATE_INTERVAL_IN_MILLISECONDS);
        fineLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        fineLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fineLocationRequest.setSmallestDisplacement(DISPLACEMENT_IN_METERS);
    }

    protected synchronized void startLocationUpdates() {
        if (locationProviderClient == null) {
            Log.d(TAG, "startLocationUpdates - no permission");
            return;
        }

        Log.d(TAG, "startLocationUpdates ");
        try {
            createLocationRequest();
            locationProviderClient
                    .requestLocationUpdates(fineLocationRequest, locationCallback, Looper.myLooper())
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (e instanceof ApiException) {
                                Log.e(TAG, "GoogleServiceActivity " + ((ApiException) e).getMessage());
                            } else {
                                Log.e(TAG, "GoogleServiceActivity " + e.getMessage());
                            }                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                        }
                    });
        } catch (SecurityException exp) {
            Log.d(TAG, " Security exception while startingLocationUpdates " + exp);
        }
    }

    protected void stopLocationUpdates() {
        if (locationProviderClient != null) {
            try {
                final Task<Void> voidTask = locationProviderClient.removeLocationUpdates(locationCallback);
                if (voidTask.isSuccessful()) {
                    Log.d(TAG,"StopLocation updates successful!");
                } else {
                    Log.d(TAG,"StopLocation updates unsuccessful!");
                }
            }
            catch (SecurityException exp) {
                Log.d(TAG, " Security exception while removeLocationUpdates");
            }
        }
    }

    public void onLocationChanged(LocationResult locationResult) {
        Log.d(TAG, "onLocationChanged");
        Log.d(TAG,"Set current location to " + locationResult);
        /* Now that we have our location, we can slowdown rate at which we request updates */
        fineLocationRequest.setInterval(UPDATE_INTERVAL_AFTER_LOCK_IN_MILLISECONDS);
        fineLocationRequest.setFastestInterval(FAST_UPDATE_INTERVAL_AFTER_LOCK_IN_MILLISECONDS);
        setCurrentLocation(locationResult.getLastLocation());
    }

    private void setCurrentLocation(Location location) {
        if (location != null) {
            currentLocation = location;
        }
    }

    @AskPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public boolean hasLocationPermission() {
        int hasPerm = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (hasPerm == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "BaseActivity hasLocationPermission returning true");
            return true;
        }
        ActivityCompat.requestPermissions(this,
                new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                1);
        Log.d(TAG, "BaseActivity hasLocationPermission returning false");
        return false;
    }

}
