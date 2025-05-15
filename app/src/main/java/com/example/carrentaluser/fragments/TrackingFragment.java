package com.example.carrentaluser.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
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
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.maps.android.SphericalUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private TextView carInfoTextView;

    // Location variables
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LatLng userLocation;
    private LatLng carLocation;
    private Marker currentMarker;

    // Booking info
    private String carName;
    private String bookingDate;

    // State variables
    private boolean mapReady = false;
    private boolean locationPermissionGranted = false;
    private boolean firstLocationUpdate = true;
    private boolean isLoadingBookings = false;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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
            carInfoTextView = rootView.findViewById(R.id.car_info_text);
            
            // Initialize Firebase
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();
            
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
            
            // Load booking data
            loadBookingData();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error initializing map view", Toast.LENGTH_SHORT).show();
        }
        
        return rootView;
    }
    
    private void loadBookingData() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in to track your car", Toast.LENGTH_SHORT).show();
            setupDefaultCarLocation();
            return;
        }
        
        isLoadingBookings = true;
        String userId = mAuth.getCurrentUser().getUid();
        
        // Show loading message
        if (carInfoTextView != null) {
            carInfoTextView.setText("Loading booking information...");
            carInfoTextView.setVisibility(View.VISIBLE);
        }
        
        Log.d(TAG, "Loading bookings for user: " + userId);
        
        // Query all bookings first to check what's available
        db.collection("bookings")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener(allBookingsSnapshot -> {
                // Log all bookings to debug
                Log.d(TAG, "All bookings found: " + allBookingsSnapshot.size());
                for (QueryDocumentSnapshot doc : allBookingsSnapshot) {
                    String status = doc.getString("status");
                    Log.d(TAG, "Booking ID: " + doc.getId() + ", Status: " + status);
                }
                
                // Now query for confirmed bookings - try both "Confirmed" and "confirmed" for case sensitivity
                db.collection("bookings")
                    .whereEqualTo("user_id", userId)
                    .whereIn("status", Arrays.asList("Confirmed", "confirmed", "CONFIRMED"))
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        isLoadingBookings = false;
                        
                        Log.d(TAG, "Confirmed bookings found: " + queryDocumentSnapshots.size());
                        
                        List<QueryDocumentSnapshot> activeBookings = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            // Add to active bookings list
                            activeBookings.add(document);
                            Log.d(TAG, "Found confirmed booking: " + document.getId());
                        }
                        
                        if (!activeBookings.isEmpty()) {
                            // Use the most recent booking
                            QueryDocumentSnapshot latestBooking = activeBookings.get(activeBookings.size() - 1);
                            Log.d(TAG, "Using booking ID: " + latestBooking.getId());
                            processBookingData(latestBooking);
                        } else {
                            Log.d(TAG, "No confirmed bookings found after filtering");
                            
                            // Check if there are any "Pending" bookings
                            db.collection("bookings")
                                .whereEqualTo("user_id", userId)
                                .whereEqualTo("status", "Pending")
                                .get()
                                .addOnSuccessListener(pendingSnapshot -> {
                                    if (!pendingSnapshot.isEmpty()) {
                                        if (carInfoTextView != null) {
                                            carInfoTextView.setText("You have pending bookings waiting for confirmation");
                                        }
                                        Toast.makeText(requireContext(), "You have pending bookings waiting for confirmation", Toast.LENGTH_LONG).show();
                                    } else {
                                        if (carInfoTextView != null) {
                                            carInfoTextView.setText("No confirmed bookings found");
                                        }
                                    }
                                    setupDefaultCarLocation();
                                });
                        }
                    })
                    .addOnFailureListener(e -> {
                        isLoadingBookings = false;
                        Log.e(TAG, "Error loading confirmed bookings: " + e.getMessage());
                        if (carInfoTextView != null) {
                            carInfoTextView.setText("Error loading booking data");
                        }
                        Toast.makeText(requireContext(), "Error loading booking data", Toast.LENGTH_SHORT).show();
                        setupDefaultCarLocation();
                    });
            })
            .addOnFailureListener(e -> {
                isLoadingBookings = false;
                Log.e(TAG, "Error loading all bookings: " + e.getMessage());
                if (carInfoTextView != null) {
                    carInfoTextView.setText("Error loading booking data");
                }
                Toast.makeText(requireContext(), "Error loading booking data", Toast.LENGTH_SHORT).show();
                setupDefaultCarLocation();
            });
    }
    
    private void processBookingData(QueryDocumentSnapshot bookingDocument) {
        try {
            Log.d(TAG, "Processing booking data for document: " + bookingDocument.getId());
            
            // Log all fields in the document to debug
            Map<String, Object> data = bookingDocument.getData();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Log.d(TAG, "Field: " + entry.getKey() + " = " + entry.getValue());
            }
            
            // Get car location from booking
            Double latitude = bookingDocument.getDouble("latitude");
            Double longitude = bookingDocument.getDouble("longitude");
            String pickupLocation = bookingDocument.getString("pickup_location");
            
            Log.d(TAG, "Location data - Lat: " + latitude + ", Long: " + longitude + ", Pickup: " + pickupLocation);
            
            if (latitude != null && longitude != null && latitude != 0 && longitude != 0) {
                Log.d(TAG, "Using coordinates from booking: " + latitude + ", " + longitude);
                carLocation = new LatLng(latitude, longitude);
            } else if (pickupLocation != null && !pickupLocation.isEmpty()) {
                // If we don't have coordinates but have a pickup location address, geocode it
                Log.d(TAG, "No coordinates in booking, geocoding address: " + pickupLocation);
                
                // For demonstration, use a temporary location until geocoding is complete
                carLocation = new LatLng(20.5937, 78.9629); // Center of India as fallback
                
                // Start a geocoding operation in background
                new Thread(() -> {
                    try {
                        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocationName(pickupLocation, 1);
                        
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            LatLng geocodedLocation = new LatLng(address.getLatitude(), address.getLongitude());
                            
                            requireActivity().runOnUiThread(() -> {
                                Log.d(TAG, "Geocoded location: " + geocodedLocation.latitude + ", " + geocodedLocation.longitude);
                                carLocation = geocodedLocation;
                                
                                // Update marker and map if they're ready
                                if (mapReady && googleMap != null) {
                                    updateCarMarkerWithBookingInfo(bookingDocument);
                                    
                                    // If we already have user location, draw the route
                                    if (userLocation != null) {
                                        drawRouteLine();
                                        calculateDistanceAndTime();
                                    }
                                }
                            });
                        } else {
                            Log.d(TAG, "Geocoding failed for address: " + pickupLocation);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error geocoding address: " + e.getMessage());
                    }
                }).start();
            } else {
                // Neither coordinates nor address available
                Log.d(TAG, "No location information in booking, using default");
                setupDefaultCarLocation();
                return;
            }
            
            // Get booking details
            carName = bookingDocument.getString("car_name");
            bookingDate = bookingDocument.getString("start_date");
            String endDate = bookingDocument.getString("end_date");
            String bookingStatus = bookingDocument.getString("status");
            Long totalPrice = bookingDocument.getLong("total_price");
            
            // Create detailed car info text
            StringBuilder carInfoBuilder = new StringBuilder();
            if (carName != null) {
                carInfoBuilder.append(carName);
            }
            
            if (bookingDate != null) {
                carInfoBuilder.append(" (").append(bookingDate);
                if (endDate != null) {
                    carInfoBuilder.append(" - ").append(endDate);
                }
                carInfoBuilder.append(")");
            }
            
            // Update UI with car info
            if (carInfoTextView != null) {
                carInfoTextView.setText(carInfoBuilder.toString());
                carInfoTextView.setVisibility(View.VISIBLE);
            }
            
            // Update map with booking info if it's ready
            if (mapReady && googleMap != null) {
                updateCarMarkerWithBookingInfo(bookingDocument);
                
                // If we already have user location, draw the route
                if (userLocation != null) {
                    drawRouteLine();
                    calculateDistanceAndTime();
                }
                
                // Move camera to car location
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, DEFAULT_ZOOM));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing booking data: " + e.getMessage(), e);
            setupDefaultCarLocation();
        }
    }
    
    private void updateCarMarkerWithBookingInfo(QueryDocumentSnapshot bookingDocument) {
        if (googleMap == null || carLocation == null) {
            Log.d(TAG, "Cannot update car marker - map or location is null");
            return;
        }
        
        try {
            // Clear any existing markers
            googleMap.clear();
            
            // Get booking details for the marker
            String carName = bookingDocument.getString("car_name");
            String bookingDate = bookingDocument.getString("start_date");
            String endDate = bookingDocument.getString("end_date");
            String bookingStatus = bookingDocument.getString("status");
            Long totalPrice = bookingDocument.getLong("total_price");
            String pickupLocation = bookingDocument.getString("pickup_location");
            
            // Create a more detailed snippet for the marker
            StringBuilder snippetBuilder = new StringBuilder();
            snippetBuilder.append("Status: ").append(bookingStatus != null ? bookingStatus : "N/A");
            
            if (totalPrice != null) {
                snippetBuilder.append("\nPrice: ₹").append(totalPrice);
            }
            
            if (pickupLocation != null && !pickupLocation.isEmpty()) {
                snippetBuilder.append("\nPickup: ").append(pickupLocation);
            }
            
            if (bookingDate != null) {
                snippetBuilder.append("\nFrom: ").append(bookingDate);
            }
            
            if (endDate != null) {
                snippetBuilder.append("\nTo: ").append(endDate);
            }
            
            // Add car marker with name and detailed booking info
            currentMarker = googleMap.addMarker(new MarkerOptions()
                    .position(carLocation)
                    .title(carName != null ? carName : "Your Car")
                    .snippet(snippetBuilder.toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
            
            // Show info window with details
            if (currentMarker != null) {
                currentMarker.showInfoWindow();
            }
            
            Log.d(TAG, "Updated car marker with booking info");
        } catch (Exception e) {
            Log.e(TAG, "Error updating car marker: " + e.getMessage(), e);
        }
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
            UiSettings uiSettings = googleMap.getUiSettings();
            uiSettings.setZoomControlsEnabled(true);
            uiSettings.setMyLocationButtonEnabled(false); // Using our own FAB
            uiSettings.setCompassEnabled(true);
            uiSettings.setMapToolbarEnabled(false);
            
            // Set up car location (either from booking or default)
            if (carLocation == null) {
                // This will be called if onMapReady is called before booking data is loaded
                setupDefaultCarLocation();
            } else {
                // If we already have the car location from booking data
                updateCarMarker();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, DEFAULT_ZOOM));
            }
            
            // Enable my location if permission is granted
            if (locationPermissionGranted) {
                enableMyLocation();
            }
            
            // Start location updates if we have permission
            if (locationPermissionGranted) {
                startLocationUpdates();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onMapReady: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error setting up map", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupDefaultCarLocation() {
        // Get device location instead of using default location
        if (fusedLocationClient != null) {
            try {
                if (ActivityCompat.checkSelfPermission(requireContext(), 
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(location -> {
                                if (location != null) {
                                    carLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                } else {
                                    // If device location not available, use a generic location
                                    carLocation = new LatLng(20.5937, 78.9629); // Center of India
                                }
                                
                                carName = "No Active Booking";
                                bookingDate = "N/A";
                                
                                if (carInfoTextView != null) {
                                    carInfoTextView.setText("No active booking found");
                                    carInfoTextView.setVisibility(View.VISIBLE);
                                }
                                
                                if (googleMap != null) {
                                    updateCarMarker();
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, DEFAULT_ZOOM));
                                }
                            })
                            .addOnFailureListener(e -> {
                                // If fetching location fails, use generic location
                                carLocation = new LatLng(20.5937, 78.9629); // Center of India
                                
                                carName = "No Active Booking";
                                bookingDate = "N/A";
                                
                                if (carInfoTextView != null) {
                                    carInfoTextView.setText("No active booking found");
                                    carInfoTextView.setVisibility(View.VISIBLE);
                                }
                                
                                if (googleMap != null) {
                                    updateCarMarker();
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, DEFAULT_ZOOM));
                                }
                            });
                            
                    return; // Return early as we're handling the setup in callbacks
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting device location: " + e.getMessage());
                // Continue to fallback location
            }
        }
        
        // Fallback if location services are not available
        carLocation = new LatLng(20.5937, 78.9629); // Center of India
        carName = "No Active Booking";
        bookingDate = "N/A";
        
        if (carInfoTextView != null) {
            carInfoTextView.setText("No active booking found");
            carInfoTextView.setVisibility(View.VISIBLE);
        }
        
        if (googleMap != null) {
            updateCarMarker();
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, DEFAULT_ZOOM));
        }
    }
    
    private void updateCarMarker() {
        if (googleMap == null || carLocation == null) {
            return;
        }
        
        // Clear previous markers
        googleMap.clear();
        
        // Add car marker with name and date
        String snippet = bookingDate != null ? "Booked for: " + bookingDate : "";
        googleMap.addMarker(new MarkerOptions()
                .position(carLocation)
                .title(carName != null ? carName : "Your Car")
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
        
        // If user location exists, draw the route
        if (userLocation != null) {
            drawRouteLine();
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
            
            // Create a detailed snippet with booking info
            StringBuilder snippetBuilder = new StringBuilder();
            
            if (bookingDate != null) {
                snippetBuilder.append("Booked for: ").append(bookingDate);
            }
            
            // Re-add car marker with name and date
            MarkerOptions carMarkerOptions = new MarkerOptions()
                    .position(carLocation)
                    .title(carName != null ? carName : "Your Car")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
            
            if (snippetBuilder.length() > 0) {
                carMarkerOptions.snippet(snippetBuilder.toString());
            }
            
            currentMarker = googleMap.addMarker(carMarkerOptions);
            
            // Show the info window
            if (currentMarker != null) {
                currentMarker.showInfoWindow();
            }
            
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

