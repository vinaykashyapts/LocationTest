package test.location.com.locationtest;

import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.LocationSource;

public class GooglePlayService implements LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationSource {

    public interface LocationUpdateListener {
        void onLocationUpdate(Location location);
    }

    private LocationUpdateListener lupl;

    public void setLocationUpdateListener(LocationUpdateListener lupl) {
        this.lupl = lupl;
    }

    private MainActivity mActivity;
    private Context context;
    private LocationRequest mLocationRequest;
    public GoogleApiClient mGoogleApiClient;

    private OnLocationChangedListener mMapLocationChangedListener;

    public Location currentLocation;
    public boolean hasCurrentLocation = false;

    //Request code to use when Play service need to update
    public static final int REQUEST_SERVICE_UPDATE = 1002;

    public static final int REQUEST_CHECK_SETTINGS = 1000;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private static final int REQUEST_INTERVAL = 3000;
    private static final int REQUEST_FASTEST_INTERVAL = 5000;

    public GooglePlayService(MainActivity mActivity) {
        this.mActivity = mActivity;
        init();
    }

    public GooglePlayService(Context ctx) {
        context = ctx;
        init();
    }

    private void init() {
        setManualModeLocationRequest();
        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mActivity != null ? mActivity : context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mMapLocationChangedListener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        mMapLocationChangedListener = null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Logger.logError("GPS", "onConnected...");
        //startPeriodicUpdates();
        try {
            currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(currentLocation != null) {
                Logger.logError("GPS", "current location : " + currentLocation.toString());
            }

        } catch (Exception e) {
            Logger.logError("Exception", e.toString());
        }
//        if (servicesConnected()) {
//
//            if (currentLocation != null) {
//                Logger.logError("GPS", "current location not null : " + currentLocation.toString());
//                if (lupl != null) {
//                    lupl.onLocationUpdate(currentLocation);
//                }
//            } else {
//                Logger.logError("GPS", "current location is null");
//                //checkLocationSettings();
//            }
//        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Logger.logError("GPS", "onLocationChanged...");

        if (lupl != null) {
            lupl.onLocationUpdate(location);
        }

        if (mMapLocationChangedListener != null) {
            mMapLocationChangedListener.onLocationChanged(location);
            currentLocation = location;
            hasCurrentLocation = true;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Logger.logError("GPS", "onConnectionFailed..." + result.getErrorMessage());
        if (result.hasResolution()) {
            if (null != mActivity) {
                try {
                    result.startResolutionForResult(mActivity, REQUEST_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException e) {
                    // There was an error with the resolution intent. Try again.
                    mGoogleApiClient.connect();
                }
            }
        } else {
            if (null != mActivity) {
                // Show dialog using GoogleApiAvailability.getErrorDialog()
                GoogleApiAvailability.getInstance().getErrorDialog(mActivity, result.getErrorCode(), REQUEST_RESOLVE_ERROR).show();
            }
        }
    }

    private boolean servicesConnected() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mActivity != null ? mActivity : context);
        return (ConnectionResult.SUCCESS == resultCode);
    }

    public void startPeriodicUpdates() {
        if (mGoogleApiClient.isConnected())
            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
    }

    public void stopPeriodicUpdates() {
        if (mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    public void stopClientConnection() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            stopPeriodicUpdates();
            mGoogleApiClient.disconnect();
//            mGoogleApiClient = null;
//            lupl = null;
//            mLocationRequest = null;
        }
    }

    public void setManualModeLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(REQUEST_INTERVAL);
        mLocationRequest.setFastestInterval(REQUEST_FASTEST_INTERVAL);
    }

    public void checkLocationSettings() {

        if (mLocationRequest != null && mGoogleApiClient != null) {

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
            builder.setAlwaysShow(true);

            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                                         @Override
                                         public void onResult(LocationSettingsResult result) {
                                             final Status status = result.getStatus();
                                             switch (status.getStatusCode()) {
                                                 case LocationSettingsStatusCodes.SUCCESS:
                                                     // All location settings are satisfied. The client can initialize location requests here.
                                                     break;
                                                 case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                                     if (null != mActivity) {
                                                         // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                                                         try {
                                                             // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                                                             status.startResolutionForResult(mActivity, REQUEST_CHECK_SETTINGS);
                                                         } catch (IntentSender.SendIntentException e) {
                                                             // Ignore the error.
                                                         }
                                                     }
                                                     break;
                                                 case LocationSettingsStatusCodes.SERVICE_VERSION_UPDATE_REQUIRED:
                                                     if (null != mActivity) {
                                                         // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                                                         try {
                                                             // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                                                             status.startResolutionForResult(mActivity, REQUEST_SERVICE_UPDATE);
                                                         } catch (IntentSender.SendIntentException e) {
                                                             // Ignore the error.
                                                         }
                                                     }
                                                     break;
                                                 case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                                     // Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.
                                                     break;
                                             }
                                         }
                                     }
            );
        }
    }
}