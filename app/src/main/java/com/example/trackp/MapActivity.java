package com.example.trackp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.text.DecimalFormat;

public class MapActivity extends AppCompatActivity implements LocationListener {

    private MapView mapView;
    private MapController mapController;
    private LocationManager locationManager;
    private Marker marker;
    private Handler handler;
    private Context context;
    private Toast toast;
    private Location previousLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Initialize the MapView
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mapView.setMultiTouchControls(true);

        RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(mapView);
        rotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(rotationGestureOverlay);

        mapController = (MapController) mapView.getController();
        mapController.setZoom(17.);  // Initial zoom level

        // Initialize OSMDroid Configuration
        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", Context.MODE_PRIVATE));

        // Request location permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Check if GPS is enabled
            if (!isGPSEnabled()) {
                // Request GPS activation
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("GPS Activation Required");
                builder.setMessage("Please enable GPS to use this app.");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Open location settings
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                });
                builder.setCancelable(false);
                builder.show();
            }
        }

        // Set up location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);

        // Add a marker overlay for the user's current location
        marker = new Marker(mapView);
        marker.setTitle("Your location");
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.marker_icon);
        marker.setIcon(drawable);  // Set the modified drawable as the marker icon

        // Set the initial position of the marker and center as the current location of the user
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation != null) {
                double lat = lastLocation.getLatitude();
                double lng = lastLocation.getLongitude();
                GeoPoint userLocation = new GeoPoint(lat, lng);
                marker.setPosition(userLocation);
                mapController.animateTo(userLocation);
            }
        }

        mapView.getOverlays().add(marker);

        // Set up handler to update location every second
        handler = new Handler();
        context = this;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Get the latest location
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    return;
                }

                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                System.out.println(lastLocation);
                if (lastLocation != null) {
                    onLocationChanged(lastLocation);
                }

                // Schedule the next update
                handler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    @Override
    public void onLocationChanged(Location location) {
        // Check if previousLocation is null
        if (previousLocation == null) {
            previousLocation = new Location("");
            previousLocation.setLatitude(location.getLatitude());
            previousLocation.setLongitude(location.getLongitude());
        }

        // Update marker and center of the map with user's current location
        double lat = location.getLatitude();
        double lng = location.getLongitude();

        DecimalFormat decimalFormat = new DecimalFormat("#.#####");
        lat = Double.parseDouble(decimalFormat.format(lat));
        lng = Double.parseDouble(decimalFormat.format(lng));

        GeoPoint userLocation = new GeoPoint(lat, lng);
        mapView.invalidate();
        marker.setPosition(userLocation);
        mapController.animateTo(userLocation);

        // Calculate the distance between the previous and current location
        float distance = previousLocation.distanceTo(location);

        // Annuler le toast précédent s'il existe
        if (toast != null) {
            toast.cancel();
        }

        // Display the distance in the toast
        toast.makeText(MapActivity.this, "Distance: " + distance + " meters", Toast.LENGTH_SHORT).show();

        // Store the current location as the new previous location
        previousLocation.setLatitude(location.getLatitude());
        previousLocation.setLongitude(location.getLongitude());
    }



    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop receiving location updates and remove the handler when the activity is destroyed
        locationManager.removeUpdates(this);
        handler.removeCallbacksAndMessages(null);
    }

    // Helper method to check if GPS is enabled
    private boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
