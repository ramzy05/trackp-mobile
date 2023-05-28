package com.example.trackp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapActivity extends AppCompatActivity implements LocationListener {

    private MapView mapView;
    private MapController mapController;
    private LocationManager locationManager;
    private Marker marker;
    private Toast toast;
    private Location previousLocation;
    private int locationUpdateInterval = 5000; // Interval initial de 5s
    private String authToken;
    double centerLatCurrent;
    double centerLngCurrent;
    double radiusCurrent;
    private static final String API_URL = "http://192.168.43.195/trackpapi/public/api/";
    private static final int INITIAL_UPDATE_INTERVAL = 5000;
    private Polygon circle;


    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean circleDrawn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        authToken = readAuthTokenFromSharedPreferences();

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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdateInterval, 0, this);

        // Add a marker overlay for the user's /current location
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


        GeoPoint circleCenter = marker.getPosition();
        /*circle = createCircle(circleCenter, 50);
        mapView.getOverlays().add(circle);*/


        mapView.getOverlays().add(marker);

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
        //toast.makeText(MapActivity.this, "Distance: " + distance + " meters", Toast.LENGTH_SHORT).show();

        // Store the current location as the new previous location
        previousLocation.setLatitude(location.getLatitude());
        previousLocation.setLongitude(location.getLongitude());

        if(!Objects.equals(authToken, "")){
            sendLocationToServer(location);
        }

    }

    private void sendLocationToServer(Location location) {
        new SendLocationTask().execute(location);
    }

    private class SendLocationTask extends AsyncTask<Location, Void, String> {
        @Override
        protected String doInBackground(Location... locations) {
            Location location = locations[0];
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            Log.d("MapActivity", "Sending location to server: lat=" + lat + ", lng=" + lng);

            // 2. Construct the API request with the required data
            RequestBody formBody = new FormBody.Builder()
                    .add("lat", Double.toString(lat))
                    .add("lng", Double.toString(lng))
                    .build();
            Request request = new Request.Builder()
                    .url(API_URL + "update-location")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .post(formBody)
                    .build();

            // 3. Send the API request using the PATCH method
            OkHttpClient client = new OkHttpClient();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d("MapActivity", "Response: " + responseData);
                    return responseData;
                } else {
                    Log.e("MapActivity", "Request failed with code: " + response.code());
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("MapActivity", "Request failed: " + e.getMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(String responseData) {
            if (responseData != null) {
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    String msgResponse = jsonObject.getString("message");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MapActivity.this, msgResponse, Toast.LENGTH_SHORT).show();

                        }
                    });
                    // 4. Parse the response data and handle accordingly
                    if (jsonObject.has("collection")) {
                        JSONObject collectionObject = jsonObject.getJSONObject("collection");

                        // Extract the required data from the "collection" object
                        double centerLat = collectionObject.getDouble("center_lat");
                        double centerLng = collectionObject.getDouble("center_lng");
                        double radius = collectionObject.getDouble("radius");
                        int frequency = collectionObject.getInt("frequency")*1000;



                        if(locationUpdateInterval != frequency){
                            updateLocationUpdateInterval(frequency);
                        }
                        // Update UI with circle and update interval
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!circleDrawn) {
                                    mapView.getOverlays().remove(marker);
                                    // Créez et ajoutez le cercle
                                    GeoPoint circleCenter = new GeoPoint(centerLat, centerLng);
                                    circle = createCircle(circleCenter, radius);
                                    circleDrawn = true;
                                    mapView.getOverlays().add(circle);
                                    mapView.getOverlays().add(marker);

                                }
                            }
                        });
                    }else{
                        // Supprimez le cercle s'il est déjà dessiné
                        if (circleDrawn) {
                            mapView.getOverlays().remove(circle);
                            circleDrawn = false;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void updateLocationUpdateInterval(int interval) {
        locationUpdateInterval = interval;
        // Mettez à jour l'intervalle de mise à jour en utilisant locationManager
        locationManager.removeUpdates(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdateInterval, 0, this);
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
    }

    // Helper method to check if GPS is enabled
    private boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private String readAuthTokenFromSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getString("authToken", "");
    }

    private Polygon createCircle(GeoPoint center, double radius) {
        ArrayList<GeoPoint> points = new ArrayList<>();

        if (radius != 0.0) {
            double scaledRadius = (radius * Math.pow(10, -5));
            //int numPoints = 1000000;  // Nombre de points pour représenter le cercle
            int numPoints = 1000;

            for (int i = 0; i <= numPoints; i++) {
                double angle = (2 * Math.PI * i) / numPoints;
                double x = center.getLongitude() + (scaledRadius * Math.cos(angle));
                double y = center.getLatitude() + (scaledRadius * Math.sin(angle));

                points.add(new GeoPoint(y, x));
            }
        }


        Polygon circle = new Polygon();
        circle.setPoints(points);
        circle.setFillColor(Color.argb(30, 0,0,255));
        circle.setStrokeColor(Color.argb(100, 0,0,255));
        circle.setStrokeWidth(5);

        return circle;
    }
}
