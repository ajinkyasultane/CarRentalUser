package com.example.carrentaluser.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.carrentaluser.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.SphericalUtil;

import java.text.DecimalFormat;

public class TrackingFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "TrackingFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float DEFAULT_ZOOM = 15f;
    private static final double AVG_SPEED_KMH = 40.0; // Estimated average car speed in km/h

    // UI elements
    private MapView mapView;
    private GoogleMap googleMap;
    private TextView distanceValueTextView;
    private TextView timeValueTextView;
    private FloatingActionButton myLocationFab;

    // Location variables
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LatLng userLocation;
    private LatLng carLocation;

    // State variables
    private boolean mapReady = false;
    private boolean locationPermissionGranted = false;
    private boolean firstLocationUpdate = true;

    public TrackingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tracking, container, false);
        
        // Initialize UI elements
        try {
            mapView = rootView.findViewById(R.id.map_view);
            distanceValueTextView = rootView.findViewById(R.id.distance_value);
            timeValueTextView = rootView.findViewById(R.id.time_value);
            myLocationFab = rootView.findViewById(R.id.fab_my_location);
            
            // Initialize location client
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
            
            // Setup "My Location" button click
            myLocationFab.setOnClickListener(v -> {
                if (userLocation != null && googleMap != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));
                } else {
                    Toast.makeText(requireContext(), "Location not available yet", Toast.LENGTH_SHORT).show();
                }
            });
            
            // Check location permission
            checkLocationPermission();
            
            // Initialize map
            initializeMap(savedInstanceState);
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error initializing map view", Toast.LENGTH_SHORT).show();
        }
        
        return rootView;
    }
    
    private void initializeMap(Bundle savedInstanceState) {
        try {
            if (mapView != null) {
                mapView.onCreate(savedInstanceState);
                mapView.getMapAsync(this);
            } else {
                Log.e(TAG, "MapView is null!");
                Toast.makeText(requireContext(), "Error: MapView is null", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing map: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to initialize map", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        try {
            Log.d(TAG, "onMapReady called");
            googleMap = map;
            mapReady = true;
            
            // Configure map UI
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false); // Using our own FAB
            
            // Set up mock car location (would be real in production)
            setupCarLocation();
            
            // Enable my location if permission is granted
            if (locationPermissionGranted) {
                enableMyLocation();
            }
            
            // Set initial camera position to car location
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, DEFAULT_ZOOM));
            
            // Start location updates if we have permission
            if (locationPermissionGranted) {
                startLocationUpdates();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onMapReady: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error setting up map", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupCarLocation() {
        // In a real app, you would get this from Firebase or your backend
        carLocation = new LatLng(19.0760, 72.8777); // Mumbai location
        
        if (googleMap != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(carLocation)
                    .title("Your Car")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
        }
    }
    
    private void enableMyLocation() {
        if (googleMap == null) {
            return;
        }
        
        try {
            if (ActivityCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enabling my location: " + e.getMessage(), e);
        }
    }
    
    private void startLocationUpdates() {
        try {
            // Define location request
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(10000)         // 10 seconds
                    .setFastestInterval(5000)   // 5 seconds
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            
            // Define location callback
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    try {
                        if (locationResult.getLastLocation() != null) {
                            Location location = locationResult.getLastLocation();
                            userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            
                            // First location update, zoom to user
                            if (firstLocationUpdate && googleMap != null) {
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));
                                firstLocationUpdate = false;
                            }
                            
                            // Update distance and ETA
                            calculateDistanceAndTime();
                            
                            // Draw route between user and car
                            drawRouteLine();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in location callback: " + e.getMessage(), e);
                    }
                }
            };
            
            // Request location updates if permission is granted
            if (ActivityCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                Log.d(TAG, "Location updates requested");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates: " + e.getMessage(), e);
        }
    }

    private void calculateDistanceAndTime() {
        if (userLocation != null && carLocation != null && distanceValueTextView != null && timeValueTextView != null) {
            try {
                // Calculate distance
                double distanceInMeters = SphericalUtil.computeDistanceBetween(userLocation, carLocation);
                double distanceInKm = distanceInMeters / 1000;
                
                // Format distance
                DecimalFormat df = new DecimalFormat("0.0");
                String formattedDistance = df.format(distanceInKm);
                
                // Calculate ETA (time = distance / speed)
                double timeInHours = distanceInKm / AVG_SPEED_KMH;
                int timeInMinutes = (int) Math.ceil(timeInHours * 60);
                
                // Update UI
                distanceValueTextView.setText(formattedDistance + " km");
                timeValueTextView.setText(timeInMinutes + " min");
                
                Log.d(TAG, "Distance updated: " + formattedDistance + " km, ETA: " + timeInMinutes + " min");
            } catch (Exception e) {
                Log.e(TAG, "Error calculating distance/time: " + e.getMessage(), e);
            }
        }
    }
    
    private void drawRouteLine() {
        if (userLocation == null || carLocation == null || googleMap == null) {
            return;
        }
        
        try {
            // Clear previous polylines and markers
            googleMap.clear();
            
            // Re-add car marker
            googleMap.addMarker(new MarkerOptions()
                    .position(carLocation)
                    .title("Your Car")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
            
            // Add user marker (optional since we have the blue dot)
            googleMap.addMarker(new MarkerOptions()
                    .position(userLocation)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            
            // Draw polyline connecting the points
            googleMap.addPolyline(new PolylineOptions()
                    .add(userLocation, carLocation)
                    .width(5)
                    .color(Color.parseColor("#4A00E0"))
                    .geodesic(true));
            
            // Create bounds to show both points
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(userLocation);
            builder.include(carLocation);
            final LatLngBounds bounds = builder.build();
            
            // Use Handler and post to ensure the map is laid out before animating
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    int padding = 100;
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                } catch (Exception e) {
                    Log.e(TAG, "Error animating camera: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error drawing route: " + e.getMessage(), e);
        }
    }
    
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            requestLocationPermission();
        }
    }
    
    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(requireContext(), "Location permission is needed for tracking", Toast.LENGTH_LONG).show();
        }
        
        requestPermissions(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted");
                locationPermissionGranted = true;
                
                if (mapReady) {
                    enableMyLocation();
                    startLocationUpdates();
                }
            } else {
                Log.d(TAG, "Location permission denied");
                Toast.makeText(requireContext(), "Location permission is required for tracking", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // MapView lifecycle methods - CRITICAL for preventing memory leaks
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach called");
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
        Log.d(TAG, "onStart called");
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        
        if (locationPermissionGranted && locationCallback != null && fusedLocationClient != null) {
            startLocationUpdates();
        }
        
        Log.d(TAG, "onResume called");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
        
        // Stop location updates
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates removed in onPause");
        }
        
        Log.d(TAG, "onPause called");
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
        Log.d(TAG, "onStop called");
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Stop location updates if not already done
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        
        Log.d(TAG, "onDestroyView called");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
        Log.d(TAG, "onDestroy called");
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach called");
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
        Log.d(TAG, "onLowMemory called");
    }
    
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
        Log.d(TAG, "onSaveInstanceState called");
    }
}

