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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.maps.android.SphericalUtil;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrackingFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "TrackingFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float DEFAULT_ZOOM = 15f;
    private static final double AVG_SPEED_KMH = 40.0;

    // UI elements
    private MapView mapView;
    private GoogleMap googleMap;
    private TextView distanceValueTextView;
    private TextView timeValueTextView;
    private FloatingActionButton myLocationFab;
    private TextView carInfoTextView;
    private Spinner bookingSpinner;
    private TextView emptyBookingsText;
    private MaterialButton refreshButton;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Location variables
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LatLng userLocation;
    private LatLng carLocation;
    private Marker currentMarker;

    // Booking info
    private List<DocumentSnapshot> activeBookings;
    private DocumentSnapshot selectedBooking;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public TrackingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tracking, container, false);
        
        initializeViews(rootView);
        initializeFirebase();
        
        if (checkLocationPermission()) {
            initializeLocationServices();
        } else {
            requestLocationPermission();
        }
        
        initializeMap(savedInstanceState);
        loadActiveBookings();
        
        return rootView;
    }

    private void initializeViews(View rootView) {
        mapView = rootView.findViewById(R.id.map_view);
        distanceValueTextView = rootView.findViewById(R.id.distance_value);
        timeValueTextView = rootView.findViewById(R.id.time_value);
        myLocationFab = rootView.findViewById(R.id.fab_my_location);
        carInfoTextView = rootView.findViewById(R.id.car_info_text);
        bookingSpinner = rootView.findViewById(R.id.booking_spinner);
        emptyBookingsText = rootView.findViewById(R.id.empty_bookings_text);
        
        // Add refresh button programmatically next to spinner
        refreshButton = new MaterialButton(requireContext());
        refreshButton.setText("Refresh");
        refreshButton.setIcon(ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_popup_sync));
        refreshButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Refreshing bookings...", Toast.LENGTH_SHORT).show();
            loadActiveBookings();
        });
        
        // Add the refresh button to the layout
        ViewGroup spinnerParent = (ViewGroup) bookingSpinner.getParent();
        if (spinnerParent != null) {
            refreshButton.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            spinnerParent.addView(refreshButton);
        }

        myLocationFab.setOnClickListener(v -> {
            if (userLocation != null && googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));
            }
        });

        bookingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (activeBookings != null && !activeBookings.isEmpty() && position < activeBookings.size()) {
                    selectedBooking = activeBookings.get(position);
                    updateMapForSelectedBooking();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        createLocationCallback();
        startLocationUpdates();
    }

    private void loadActiveBookings() {
        if (mAuth.getCurrentUser() == null) {
            showEmptyState("Please log in to track your bookings");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        
        Log.d(TAG, "Loading bookings for user: " + userId);
        
        // Show loading state
        showEmptyState("Loading bookings...");
        
        // Get current date for comparison
        Date currentDate = Calendar.getInstance().getTime();
        Log.d(TAG, "Current date: " + sdf.format(currentDate));

        // Query all bookings for this user to debug
        db.collection("bookings")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener(allQuerySnapshot -> {
                Log.d(TAG, "Total bookings found: " + allQuerySnapshot.size());
                
                // Log all bookings for debugging
                for (DocumentSnapshot doc : allQuerySnapshot) {
                    String status = doc.getString("status");
                    String startDate = doc.getString("start_date");
                    String carName = doc.getString("car_name");
                    Log.d(TAG, "Booking found - Car: " + carName + ", Status: " + status + ", Date: " + startDate);
                }
                
                // Now query specifically for confirmed bookings
                db.collection("bookings")
                    .whereEqualTo("user_id", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        activeBookings = new ArrayList<>();
                        List<String> bookingTitles = new ArrayList<>();

                        Log.d(TAG, "Processing bookings");
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            String status = document.getString("status");
                            // Check for both "Confirmed" and "confirmed" status (case insensitive)
                            if (status != null && 
                                (status.equalsIgnoreCase("Confirmed") || 
                                 status.equalsIgnoreCase("confirm"))) {
                                
                                try {
                                    String startDateStr = document.getString("start_date");
                                    if (startDateStr != null) {
                                        Date startDate = sdf.parse(startDateStr);
                                        
                                        // For debugging, log all confirmed bookings
                                        Log.d(TAG, "Confirmed booking - Date: " + startDateStr + 
                                              ", Car: " + document.getString("car_name") + 
                                              ", Expired: " + (startDate != null && startDate.before(currentDate)));
                                        
                                        // Include all confirmed bookings regardless of date for now
                                        activeBookings.add(document);
                                        String carName = document.getString("car_name");
                                        String date = startDateStr;
                                        String pickupLocation = document.getString("pickup_location");
                                        bookingTitles.add(String.format("%s - %s (%s)", carName, date, pickupLocation));
                                    }
                                } catch (ParseException e) {
                                    Log.e(TAG, "Error parsing date: " + e.getMessage());
                                }
                            }
                        }

                        if (activeBookings.isEmpty()) {
                            showEmptyState("No confirmed bookings found");
                            Log.d(TAG, "No active bookings after filtering");
                        } else {
                            Log.d(TAG, "Setting up spinner with " + activeBookings.size() + " bookings");
                            setupBookingSpinner(bookingTitles);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading confirmed bookings: " + e.getMessage());
                        showEmptyState("Error loading bookings: " + e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading all bookings: " + e.getMessage());
                showEmptyState("Error loading bookings: " + e.getMessage());
            });
    }

    private void showEmptyState(String message) {
        bookingSpinner.setVisibility(View.GONE);
        emptyBookingsText.setText(message);
        emptyBookingsText.setVisibility(View.VISIBLE);
        
        // Log the empty state for debugging
        Log.d(TAG, "Empty state shown: " + message);
        
        // Hide the car location if we don't have bookings
        carLocation = null;
        if (googleMap != null) {
            googleMap.clear();
        }
    }

    private void setupBookingSpinner(List<String> bookingTitles) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            bookingTitles
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        bookingSpinner.setVisibility(View.VISIBLE);
        emptyBookingsText.setVisibility(View.GONE);
        bookingSpinner.setAdapter(adapter);
        
        // Log successful setup
        Log.d(TAG, "Spinner setup with " + bookingTitles.size() + " items");
        
        // Select the first booking automatically
        if (!bookingTitles.isEmpty()) {
            bookingSpinner.setSelection(0);
        }
    }

    private void updateMapForSelectedBooking() {
        if (selectedBooking == null || googleMap == null) {
            Log.d(TAG, "Cannot update map: selectedBooking or googleMap is null");
            return;
        }

        Log.d(TAG, "Updating map for selected booking: " + 
              selectedBooking.getString("car_name") + " - " + 
              selectedBooking.getString("start_date"));

        // Try to get coordinates directly from booking
        Double latitude = selectedBooking.getDouble("latitude");
        Double longitude = selectedBooking.getDouble("longitude");
        
        if (latitude == null || longitude == null || latitude == 0 || longitude == 0) {
            Log.d(TAG, "Coordinates not found in booking, using fallback location");
            // If no coordinates in booking, use a fallback location
            latitude = 20.5937;
            longitude = 78.9629; // Center of India as fallback
        }
        
        String carName = selectedBooking.getString("car_name");
        String startDate = selectedBooking.getString("start_date");
        String pickupLocation = selectedBooking.getString("pickup_location");

        // Log the location data
        Log.d(TAG, "Car location: " + latitude + ", " + longitude);
        
        carLocation = new LatLng(latitude, longitude);
        
        // Update marker
        if (currentMarker != null) {
            currentMarker.remove();
        }
        
        // Create a detailed snippet
        String snippet = "Pickup: " + (pickupLocation != null ? pickupLocation : "Unknown");
        if (startDate != null) {
            snippet += "\nDate: " + startDate;
        }
        
        currentMarker = googleMap.addMarker(new MarkerOptions()
            .position(carLocation)
            .title(carName != null ? carName : "Selected Car")
            .snippet(snippet)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

        if (currentMarker != null) {
            currentMarker.showInfoWindow();
        }

        // Update camera to show both user and car
        if (userLocation != null) {
            showRouteOnMap();
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, DEFAULT_ZOOM));
        }
    }

    private void showRouteOnMap() {
        if (userLocation == null || carLocation == null) {
            Log.d(TAG, "Cannot show route: userLocation or carLocation is null");
            return;
        }

        // Clear previous polylines
        googleMap.clear();
        
        // Re-add the car marker
        String carName = selectedBooking.getString("car_name");
        String startDate = selectedBooking.getString("start_date");
        String pickupLocation = selectedBooking.getString("pickup_location");
        
        // Create a detailed snippet
        String snippet = "Pickup: " + (pickupLocation != null ? pickupLocation : "Unknown");
        if (startDate != null) {
            snippet += "\nDate: " + startDate;
        }
        
        currentMarker = googleMap.addMarker(new MarkerOptions()
            .position(carLocation)
            .title(carName != null ? carName : "Selected Car")
            .snippet(snippet)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

        if (currentMarker != null) {
            currentMarker.showInfoWindow();
        }

        // Draw route line
        googleMap.addPolyline(new PolylineOptions()
            .add(userLocation, carLocation)
            .width(5)
            .color(Color.BLUE));

        // Update distance and time
        calculateDistanceAndTime();

        // Show both points
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(userLocation);
        builder.include(carLocation);
        LatLngBounds bounds = builder.build();

        int padding = 100;
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    private void calculateDistanceAndTime() {
        if (userLocation != null && carLocation != null) {
            double distanceInMeters = SphericalUtil.computeDistanceBetween(userLocation, carLocation);
            double distanceInKm = distanceInMeters / 1000;
            
            DecimalFormat df = new DecimalFormat("0.0");
            String formattedDistance = df.format(distanceInKm);
            distanceValueTextView.setText(formattedDistance + " km");
            
            // Calculate ETA based on average speed
            double timeInHours = distanceInKm / AVG_SPEED_KMH;
            int hours = (int) timeInHours;
            int minutes = (int) Math.round((timeInHours - hours) * 60);
            
            // Format as H.MM hr
            String formattedTime;
            if (hours > 0) {
                formattedTime = hours + "." + String.format("%02d", minutes) + " hr";
            } else {
                formattedTime = "0." + String.format("%02d", minutes) + " hr";
            }
            
            timeValueTextView.setText(formattedTime);
            
            Log.d(TAG, "Distance: " + formattedDistance + " km, ETA: " + formattedTime);
        }
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    Location location = locationResult.getLastLocation();
                    userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    Log.d(TAG, "User location updated: " + userLocation.latitude + ", " + userLocation.longitude);
                    
                    if (carLocation != null) {
                        showRouteOnMap();
                    }
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (!checkLocationPermission()) return;

        LocationRequest locationRequest = LocationRequest.create()
            .setInterval(10000)
            .setFastestInterval(5000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback, Looper.getMainLooper());
            Log.d(TAG, "Location updates started");
        } catch (SecurityException e) {
            Log.e(TAG, "Error starting location updates: " + e.getMessage());
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
            LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void initializeMap(Bundle savedInstanceState) {
        if (mapView != null) {
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        Log.d(TAG, "Map is ready");
        
        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMyLocationButtonEnabled(false);
        uiSettings.setCompassEnabled(true);
        
        if (checkLocationPermission()) {
            googleMap.setMyLocationEnabled(true);
            startLocationUpdates();
        }
        
        // If we already have a selected booking, update the map
        if (selectedBooking != null) {
            updateMapForSelectedBooking();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeLocationServices();
                if (googleMap != null) {
                    if (ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        googleMap.setMyLocationEnabled(true);
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (checkLocationPermission()) {
            startLocationUpdates();
        }
        
        // Refresh bookings when fragment resumes
        loadActiveBookings();
    }

    @Override
    public void onPause() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}

