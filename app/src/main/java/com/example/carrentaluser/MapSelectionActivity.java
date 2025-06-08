package com.example.carrentaluser;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapSelectionActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapSelectionActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float DEFAULT_ZOOM = 15f;
    private static final String DIRECTIONS_API_KEY = "YOUR_DIRECTIONS_API_KEY"; // Replace with your API key
    private static final double MAX_DISTANCE_KM = 50.0; // Maximum distance in kilometers

    private GoogleMap googleMap;
    private EditText searchEditText;
    private Button selectLocationButton;
    private ProgressBar progressBar;
    private FusedLocationProviderClient fusedLocationClient;
    private FloatingActionButton myLocationFab;

    private LatLng selectedLocation;
    private String selectedLocationName;
    private Marker currentMarker;
    private Marker branchMarker;
    private Polyline routePolyline;
    private Circle rangeCircle;

    // Default location (will be updated with current location)
    private LatLng DEFAULT_LOCATION = new LatLng(19.8762, 75.3433);
    private boolean useDefaultLocation = false;
    private boolean isTrackingBranch = false;
    private boolean isWithDriverMode = false;
    private LatLng branchLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_selection);

        // Add this logging to verify we're getting proper branch tracking mode
        Log.d(TAG, "MapSelectionActivity onCreate - track_branch: " + getIntent().getBooleanExtra("track_branch", false));
        
        // Check if we're tracking a branch location
        if (getIntent().getBooleanExtra("track_branch", false)) {
            isTrackingBranch = true;
            double branchLat = getIntent().getDoubleExtra("branch_latitude", 0);
            double branchLng = getIntent().getDoubleExtra("branch_longitude", 0);
            
            Log.d(TAG, "Branch location from intent: " + branchLat + ", " + branchLng);
            
            if (branchLat != 0 && branchLng != 0) {
                branchLocation = new LatLng(branchLat, branchLng);
                Log.d(TAG, "Track branch mode: Branch location set to " + branchLat + ", " + branchLng);
            } else {
                Log.e(TAG, "Track branch mode: Invalid branch coordinates received");
                Toast.makeText(this, "Branch location data is missing", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            // Check if we're in with_driver mode and getting branch location for 50km check
            isWithDriverMode = getIntent().getBooleanExtra("with_driver_mode", false);
            if (isWithDriverMode) {
                double branchLat = getIntent().getDoubleExtra("branch_latitude", 0);
                double branchLng = getIntent().getDoubleExtra("branch_longitude", 0);
                
                Log.d(TAG, "With driver mode: Received branch coordinates: " + branchLat + ", " + branchLng);
                
                if (branchLat != 0 && branchLng != 0) {
                    branchLocation = new LatLng(branchLat, branchLng);
                    Log.d(TAG, "With driver mode: Branch location set successfully");
                } else {
                    Log.e(TAG, "With driver mode: Invalid branch coordinates received");
                    Toast.makeText(this, "Branch location data is missing. Please go back and try again.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
        }

        // Check if a current location was passed in
        if (getIntent().hasExtra("current_latitude") && getIntent().hasExtra("current_longitude")) {
            double latitude = getIntent().getDoubleExtra("current_latitude", 0);
            double longitude = getIntent().getDoubleExtra("current_longitude", 0);
            if (latitude != 0 && longitude != 0) {
                DEFAULT_LOCATION = new LatLng(latitude, longitude);
                useDefaultLocation = true;
            }
        }

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (isTrackingBranch) {
                getSupportActionBar().setTitle("Branch Location");
            } else if (isWithDriverMode) {
                getSupportActionBar().setTitle("Select Pickup Location (Within 50km)");
            } else {
                getSupportActionBar().setTitle("Select Location");
            }
        }

        searchEditText = findViewById(R.id.search_edit_text);
        selectLocationButton = findViewById(R.id.select_location_button);
        progressBar = findViewById(R.id.progress_bar);
        myLocationFab = findViewById(R.id.fab_my_location);
        
        // Hide select button if we're tracking a branch
        if (isTrackingBranch) {
            selectLocationButton.setVisibility(View.GONE);
            searchEditText.setEnabled(false);
        } else if (isWithDriverMode) {
            // In with_driver mode, select button is initially disabled until valid location is selected
            selectLocationButton.setEnabled(false);
            selectLocationButton.setText("Select Location (Within 50km)");
        }

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No automatic search while typing
            }
        });
        
        // Set up search on IME action (search button on keyboard)
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchLocation(query);
                }
                return true;
            }
            return false;
        });

        // Setup location selection
        selectLocationButton.setOnClickListener(v -> {
            if (selectedLocation != null) {
                if (isWithDriverMode && branchLocation != null) {
                    double distanceKm = calculateDistance(selectedLocation, branchLocation);
                    if (distanceKm > MAX_DISTANCE_KM) {
                        showLocationTooFarDialog();
                        return;
                    }
                }
                
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedLocation.latitude);
                resultIntent.putExtra("longitude", selectedLocation.longitude);
                resultIntent.putExtra("location_name", selectedLocationName);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup my location button
        myLocationFab.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                getDeviceLocation();
            } else {
                requestLocationPermission();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Configure map settings
        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMyLocationButtonEnabled(false); // Using our own FAB
        uiSettings.setCompassEnabled(true);
        uiSettings.setMapToolbarEnabled(false);

        if (isTrackingBranch && branchLocation != null) {
            Log.d(TAG, "onMapReady - Branch tracking mode active");
            // Add marker for the branch location
            branchMarker = googleMap.addMarker(new MarkerOptions()
                .position(branchLocation)
                .title("Branch Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            
            // Get current location to draw directions
            if (checkLocationPermission()) {
                getDeviceLocationForDirections();
            } else {
                requestLocationPermission();
                // Show branch location even without permissions
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(branchLocation, DEFAULT_ZOOM));
            }
        } else if (isWithDriverMode && branchLocation != null) {
            // Add marker for the branch location
            branchMarker = googleMap.addMarker(new MarkerOptions()
                .position(branchLocation)
                .title("Branch Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            
            // Draw 50km radius circle around branch
            drawRangeCircle();
            
            // Move camera to show the branch with appropriate zoom to see the 50km radius
            float zoomLevel = calculateZoomLevel(MAX_DISTANCE_KM);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(branchLocation, zoomLevel));
            
            // Normal location selection mode with distance check
            if (!useDefaultLocation) {
                if (checkLocationPermission()) {
                    getDeviceLocation();
                } else {
                    requestLocationPermission();
                }
            } else {
                // Use the location passed in from BookingActivity
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
                getAddressFromLocation(DEFAULT_LOCATION);
            }
            
            // Setup map click listener for location selection with distance validation
            googleMap.setOnMapClickListener(latLng -> {
                updateSelectedLocation(latLng, null);
                getAddressFromLocation(latLng);
                
                // Check if within 50km
                double distanceKm = calculateDistance(latLng, branchLocation);
                if (distanceKm > MAX_DISTANCE_KM) {
                    selectLocationButton.setEnabled(false);
                    showLocationTooFarDialog();
                } else {
                    selectLocationButton.setEnabled(true);
                }
            });
        } else {
            // Normal location selection mode
            if (!useDefaultLocation) {
                if (checkLocationPermission()) {
                    getDeviceLocation();
                } else {
                    // If permissions not granted, move to default location initially
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
                    updateSelectedLocation(DEFAULT_LOCATION, "Selected Location");
                }
            } else {
                // Use the location passed in from BookingActivity
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
                getAddressFromLocation(DEFAULT_LOCATION);
            }

            // Setup map click listener for location selection
            googleMap.setOnMapClickListener(latLng -> {
                // Only allow selecting location if not in tracking mode
                if (!isTrackingBranch) {
                    updateSelectedLocation(latLng, null);
                    getAddressFromLocation(latLng);
                }
            });
        }

        // Check for location permission
        if (checkLocationPermission()) {
            googleMap.setMyLocationEnabled(true);
        } else {
            requestLocationPermission();
        }
    }

    private float calculateZoomLevel(double radiusInKm) {
        // This is an approximation - zoom level 10 is roughly a 100km view
        // So we calculate relatively from that
        double radiusRatio = 100.0 / radiusInKm;
        float zoomLevel = (float) (10 + Math.log(radiusRatio) / Math.log(2));
        return Math.max(9, Math.min(zoomLevel, 14)); // Limit between 9 and 14 for reasonable view
    }
    
    private void drawRangeCircle() {
        if (rangeCircle != null) {
            rangeCircle.remove();
        }
        
        rangeCircle = googleMap.addCircle(new CircleOptions()
            .center(branchLocation)
            .radius(MAX_DISTANCE_KM * 1000) // Convert km to meters
            .strokeWidth(2)
            .strokeColor(Color.BLUE)
            .fillColor(Color.argb(50, 0, 0, 255)));
    }
    
    private double calculateDistance(LatLng location1, LatLng location2) {
        float[] results = new float[1];
        Location.distanceBetween(
            location1.latitude, location1.longitude,
            location2.latitude, location2.longitude,
            results
        );
        
        // Convert meters to kilometers
        return results[0] / 1000.0;
    }
    
    private void showLocationTooFarDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Location Too Far")
            .setMessage("This location is more than 50km away from the branch.\n\n" +
                      "With driver service is only available within 50km of our branches. " +
                      "Please select a location within the blue circle.")
            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
            .setCancelable(false)
            .show();
    }

    private void getDeviceLocationForDirections() {
        try {
            if (checkLocationPermission()) {
                progressBar.setVisibility(View.VISIBLE);
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            progressBar.setVisibility(View.GONE);
                            if (location != null) {
                                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                
                                Log.d(TAG, "Current location: " + currentLocation.latitude + ", " + currentLocation.longitude);
                                Log.d(TAG, "Branch location: " + branchLocation.latitude + ", " + branchLocation.longitude);
                                
                                // Add marker for current location
                                if (currentMarker != null) {
                                    currentMarker.remove();
                                }
                                
                                currentMarker = googleMap.addMarker(new MarkerOptions()
                                    .position(currentLocation)
                                    .title("Your Location"));
                                
                                // Calculate distance between user and branch
                                double distanceInKm = calculateDistance(currentLocation, branchLocation);
                                
                                // Show distance toast message
                                String branchName = getIntent().getStringExtra("branch_name");
                                if (branchName == null || branchName.isEmpty()) {
                                    branchName = "Branch";
                                }
                                Toast.makeText(this, 
                                    "Distance to " + branchName + ": " + 
                                    String.format("%.1f", distanceInKm) + " km", 
                                    Toast.LENGTH_LONG).show();
                                
                                // Update branch marker title with distance info
                                if (branchMarker != null) {
                                    branchMarker.setTitle(branchName + " (Distance: " + String.format("%.1f", distanceInKm) + " km)");
                                    branchMarker.showInfoWindow(); // Show the info window with distance
                                }
                                
                                // Show both markers on screen
                                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                                boundsBuilder.include(currentLocation);
                                boundsBuilder.include(branchLocation);
                                LatLngBounds bounds = boundsBuilder.build();
                                
                                // Add padding to ensure markers aren't cut off
                                int padding = 150; // pixels
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                                
                                // Always get directions between current location and branch
                                getDirections(currentLocation, branchLocation);
                                
                                // Fallback: Draw direct line if API request might fail
                                drawDirectLine(currentLocation, branchLocation);
                            } else {
                                // If we can't get location, just show the branch
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(branchLocation, DEFAULT_ZOOM));
                                Toast.makeText(this, 
                                    "Could not get your current location. Please check your GPS settings.", 
                                    Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Log.e(TAG, "Error getting location: " + e.getMessage());
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(branchLocation, DEFAULT_ZOOM));
                            Toast.makeText(this, "Failed to get your location. Please check your GPS settings.", Toast.LENGTH_SHORT).show();
                        });
            }
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Log.e(TAG, "Error in getDeviceLocationForDirections: " + e.getMessage());
        }
    }
    
    private void getDirections(LatLng origin, LatLng destination) {
        progressBar.setVisibility(View.VISIBLE);
        
        try {
            // Build an enhanced directions request with more parameters for better routing
            String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=" + origin.latitude + "," + origin.longitude +
                    "&destination=" + destination.latitude + "," + destination.longitude +
                    "&mode=driving" +  // Get driving directions
                    "&units=metric" +  // Use metric units
                    "&alternatives=false" + // Get only the best route
                    "&key=" + DIRECTIONS_API_KEY;
            
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
                    
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            
            Log.d(TAG, "Requesting directions from: " + origin.latitude + "," + origin.longitude + 
                    " to: " + destination.latitude + "," + destination.longitude);
                    
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Directions API request failed: " + e.getMessage());
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MapSelectionActivity.this, "Failed to get driving directions", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseData = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseData);
                            
                            Log.d(TAG, "Directions API response status: " + jsonResponse.getString("status"));
                            
                            if (jsonResponse.has("routes") && jsonResponse.getJSONArray("routes").length() > 0) {
                                JSONObject route = jsonResponse.getJSONArray("routes").getJSONObject(0);
                                
                                // Get the encoded polyline representing the route
                                String polyline = route.getJSONObject("overview_polyline").getString("points");
                                List<LatLng> decodedPath = PolyUtil.decode(polyline);
                                
                                // Get the route distance and duration if available
                                String distance = "";
                                String duration = "";
                                if (route.has("legs") && route.getJSONArray("legs").length() > 0) {
                                    JSONObject leg = route.getJSONArray("legs").getJSONObject(0);
                                    distance = leg.getJSONObject("distance").getString("text");
                                    duration = leg.getJSONObject("duration").getString("text");
                                }
                                
                                final String finalDistance = distance;
                                final String finalDuration = duration;
                                
                                runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    drawRoute(decodedPath);
                                    
                                    // Display route info in a toast if available
                                    if (!finalDistance.isEmpty() && !finalDuration.isEmpty()) {
                                        Toast.makeText(MapSelectionActivity.this, 
                                            "Driving distance: " + finalDistance + "\nEstimated time: " + finalDuration, 
                                            Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(MapSelectionActivity.this, "No driving route found", Toast.LENGTH_SHORT).show();
                                });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing directions response: " + e.getMessage());
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(MapSelectionActivity.this, "Error processing directions", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        Log.e(TAG, "Directions API request failed with code: " + response.code());
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(MapSelectionActivity.this, "Failed to get directions", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up directions request: " + e.getMessage());
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error getting directions", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void drawRoute(List<LatLng> path) {
        // Clear any existing route
        if (routePolyline != null) {
            routePolyline.remove();
        }
        
        // Draw the new route with enhanced visibility
        if (path != null && path.size() > 0) {
            PolylineOptions options = new PolylineOptions()
                    .addAll(path)
                    .color(Color.rgb(0, 102, 255))  // Bright blue color
                    .width(12)                       // Thicker line
                    .geodesic(true)                  // Follow the curvature of the Earth
                    .clickable(false)                // Not clickable
                    .zIndex(1);                      // Draw on top of other map elements
            
            routePolyline = googleMap.addPolyline(options);
            
            // Add route highlight effect (outer glow)
            PolylineOptions highlightOptions = new PolylineOptions()
                    .addAll(path)
                    .color(Color.argb(50, 0, 102, 255))  // Semi-transparent blue
                    .width(20)                           // Wider than the main route
                    .geodesic(true)
                    .zIndex(0);                          // Draw under the main route
            
            googleMap.addPolyline(highlightOptions);
            
            // Log that route was drawn
            Log.d(TAG, "Route drawn with " + path.size() + " points");
        } else {
            Log.e(TAG, "Failed to draw route - path was null or empty");
        }
    }

    private void searchLocation(String query) {
        progressBar.setVisibility(View.VISIBLE);
        
        Thread searchThread = new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(query, 5);
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        LatLng location = new LatLng(address.getLatitude(), address.getLongitude());
                        
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM));
                        updateSelectedLocation(location, address.getAddressLine(0));
                        
                        // Check distance if in with_driver mode
                        if (isWithDriverMode && branchLocation != null) {
                            double distanceKm = calculateDistance(location, branchLocation);
                            if (distanceKm > MAX_DISTANCE_KM) {
                                selectLocationButton.setEnabled(false);
                                showLocationTooFarDialog();
                            } else {
                                selectLocationButton.setEnabled(true);
                            }
                        }
                    } else {
                        Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Error searching for location: " + e.getMessage());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error searching location", Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        searchThread.start();
    }

    private void getAddressFromLocation(LatLng latLng) {
        progressBar.setVisibility(View.VISIBLE);
        
        Thread geocodeThread = new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String addressLine = address.getAddressLine(0);
                        updateSelectedLocation(latLng, addressLine);
                    } else {
                        updateSelectedLocation(latLng, "Unknown Location");
                    }
                    
                    // Check distance if in with_driver mode
                    if (isWithDriverMode && branchLocation != null) {
                        double distanceKm = calculateDistance(latLng, branchLocation);
                        if (distanceKm > MAX_DISTANCE_KM) {
                            selectLocationButton.setEnabled(false);
                            showLocationTooFarDialog();
                        } else {
                            selectLocationButton.setEnabled(true);
                        }
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Error getting address: " + e.getMessage());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateSelectedLocation(latLng, "Unknown Location");
                });
            }
        });
        
        geocodeThread.start();
    }

    private void updateSelectedLocation(LatLng location, String name) {
        selectedLocation = location;
        selectedLocationName = name;
        
        // Update UI
        if (name != null) {
            searchEditText.setText(name);
        }
        
        // Update marker
        if (currentMarker != null) {
            currentMarker.remove();
        }
        
        currentMarker = googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title(name != null ? name : "Selected Location"));
        
        // Enable select button if not in with_driver mode
        // In with_driver mode, it's handled by distance check
        if (!isWithDriverMode) {
            selectLocationButton.setEnabled(true);
        }
    }

    private void getDeviceLocation() {
        try {
            if (checkLocationPermission()) {
                progressBar.setVisibility(View.VISIBLE);
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            progressBar.setVisibility(View.GONE);
                            if (location != null) {
                                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
                                getAddressFromLocation(currentLocation);
                            } else {
                                Toast.makeText(this, "Could not get current location, please wait or select manually", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Log.e(TAG, "Error getting location: " + e.getMessage());
                            Toast.makeText(this, "Failed to get location, please select manually", Toast.LENGTH_SHORT).show();
                        });
            }
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Log.e(TAG, "Error in getDeviceLocation: " + e.getMessage());
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (googleMap != null) {
                    if (ActivityCompat.checkSelfPermission(this, 
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        googleMap.setMyLocationEnabled(true);
                        
                        if (isTrackingBranch && branchLocation != null) {
                            getDeviceLocationForDirections();
                        } else {
                            getDeviceLocation();
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Add a fallback method to draw a direct line between points if the directions API fails
    private void drawDirectLine(LatLng start, LatLng end) {
        Log.d(TAG, "Drawing direct line as fallback between: " + start.latitude + "," + start.longitude +
              " and " + end.latitude + "," + end.longitude);
        
        PolylineOptions options = new PolylineOptions()
                .add(start)
                .add(end)
                .color(Color.RED)  // Use red to distinguish from the proper route
                .width(6)
                .geodesic(true);
        
        googleMap.addPolyline(options);
    }
} 