package test.location.com.locationtest;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements GooglePlayService.LocationUpdateListener, View.OnClickListener {

    @Bind(R.id.currentLocationBtn)
    Button currentLocationBtn;

    @Bind(R.id.currentLocationTxt)
    TextView currentLocationTxt;

    protected GooglePlayService playService;
    protected Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        currentLocationBtn.setOnClickListener(this);
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSEnableDialog();
        }
    }

    protected void startLocationTracking() {
        stopLocationTracking();
        playService = new GooglePlayService(this);
        playService.setLocationUpdateListener(this);
    }

    protected void showGPSEnableDialog() {
        stopLocationTracking();
        playService = new GooglePlayService(this);
        playService.checkLocationSettings();
    }

    protected void stopLocationTracking() {
        if (playService != null) {
            playService.stopClientConnection();
            playService = null;
        }
    }

    @Override
    public void onLocationUpdate(Location location) {
        currentLocation = location;
        stopLocationTracking();
        Logger.logError("GPS", "Lat : " + currentLocation.getLatitude() + " Long : " + currentLocation.getLongitude() + " Accuracy : " + currentLocation.getAccuracy());
        currentLocationTxt.setText("Lat : " + currentLocation.getLatitude() + " Long : " + currentLocation.getLongitude() + " Accuracy : " + currentLocation.getAccuracy());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.currentLocationBtn:
                startLocationTracking();
                break;
        }
    }
}
