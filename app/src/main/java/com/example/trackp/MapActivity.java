package com.example.trackp;

import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.overlay.Marker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MapActivity extends AppCompatActivity implements LocationListener {

    private MapView mapView;
    private MapController mapController;
    private GeoPoint yaoundeCenter = new GeoPoint(3.848, 11.502);
    private Marker marker;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Initialize the MapView
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mapView.setMultiTouchControls(true);
        mapController = (MapController) mapView.getController();
        mapController.setZoom(17.);  // Initial zoom level

        // Set the bounding box for Yaoundé
        mapController.setCenter(yaoundeCenter);

        // Initialize OSMDroid Configuration
        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", Context.MODE_PRIVATE));

        // Initialize the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Request location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Add a marker overlay for the user's current location
        GeoPoint userLocation = new GeoPoint(3.848, 11.502); // Replace latitude and longitude with actual values
        marker = new Marker(mapView);
        marker.setPosition(userLocation);
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.marker_icon);
        marker.setIcon(drawable);  // Set the modified drawable as the marker icon

        mapView.getOverlays().add(marker);
    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        // Update the marker's position with the new location
        GeoPoint userLocation = new GeoPoint(latitude, longitude);
        marker.setPosition(userLocation);
        mapView.invalidate(); // Refresh the map view
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}
