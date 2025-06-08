package com.example.carrentaluser.fragments;

import android.graphics.Color;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.carrentaluser.R;
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
    private static final float DEFAULT_ZOOM = 15f;
    private static final double AVG_SPEED_KMH = 40.0;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

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

    // Booking info
    private List<DocumentSnapshot> activeBookings;
    private DocumentSnapshot selectedBooking;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Location variables - now just stored locations
    private LatLng pickupLocation;
    private LatLng branchLocation;
    private Marker pickupMarker;
    private Marker branchMarker;

    public TrackingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tracking, container, false);
        
        initializeViews(rootView);
        initializeFirebase();
        
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
            if (pickupLocation != null && branchLocation != null && googleMap != null) {
                // Zoom to show the entire route
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(pickupLocation);
                builder.include(branchLocation);
                LatLngBounds bounds = builder.build();
                
                int padding = 150;
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
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

        // Query all bookings for this user
        db.collection("bookings")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                // Check if fragment is still attached to avoid IllegalStateException
                if (!isAdded()) {
                    Log.d(TAG, "Fragment not attached, skipping UI update");
                    return;
                }
                
                activeBookings = new ArrayList<>();
                List<String> bookingTitles = new ArrayList<>();

                Log.d(TAG, "Processing " + queryDocumentSnapshots.size() + " bookings");
                
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    String status = document.getString("status");
                    String startDateStr = document.getString("start_date");
                    
                    // Log each booking for debugging
                    Log.d(TAG, "Booking found - Status: " + status + 
                          ", Date: " + startDateStr + 
                          ", Car: " + document.getString("car_name"));
                    
                    // Include bookings that are confirmed or in progress
                    if (status != null && 
                        (status.equalsIgnoreCase("Confirmed") || 
                         status.equalsIgnoreCase("confirm") ||
                         status.equalsIgnoreCase("In Progress"))) {
                        
                        try {
                            if (startDateStr != null) {
                                Date startDate = sdf.parse(startDateStr);
                                // Include all confirmed bookings, including past ones
                                // We'll just add them all to the list and let the user select
                                activeBookings.add(document);
                                String carName = document.getString("car_name");
                                String pickupLocation = document.getString("pickup_location");
                                String branchName = document.getString("branch_name");
                                
                                // Include branch name in the booking listing if available
                                String displayText;
                                if (branchName != null && !branchName.isEmpty()) {
                                    displayText = String.format("%s - %s (%s)", 
                                        carName != null ? carName : "Unknown Car",
                                        startDateStr,
                                        branchName);
                                } else {
                                    displayText = String.format("%s - %s (%s)", 
                                        carName != null ? carName : "Unknown Car",
                                        startDateStr,
                                        pickupLocation != null ? pickupLocation : "Unknown Location");
                                }
                                
                                bookingTitles.add(displayText);
                                Log.d(TAG, "Added booking to active list: " + displayText);
                            }
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing date: " + e.getMessage());
                        }
                    }
                }

                if (activeBookings.isEmpty()) {
                    showEmptyState("No active bookings found");
                    Log.d(TAG, "No active bookings after filtering");
                } else {
                    Log.d(TAG, "Setting up spinner with " + activeBookings.size() + " bookings");
                    setupBookingSpinner(bookingTitles);
                }
            })
            .addOnFailureListener(e -> {
                // Check if fragment is still attached to avoid IllegalStateException
                if (!isAdded()) {
                    Log.d(TAG, "Fragment not attached, skipping error UI update");
                    return;
                }
                Log.e(TAG, "Error loading bookings: " + e.getMessage());
                showEmptyState("Error loading bookings: " + e.getMessage());
            });
    }

    private void showEmptyState(String message) {
        // Check if fragment is still attached to avoid IllegalStateException
        if (!isAdded()) {
            Log.d(TAG, "Fragment not attached, skipping empty state UI update");
            return;
        }
        
        bookingSpinner.setVisibility(View.GONE);
        emptyBookingsText.setText(message);
        emptyBookingsText.setVisibility(View.VISIBLE);
        
        // Log the empty state for debugging
        Log.d(TAG, "Empty state shown: " + message);
        
        // Hide the car location if we don't have bookings
        branchLocation = null;
        if (googleMap != null) {
            googleMap.clear();
        }
    }

    private void setupBookingSpinner(List<String> bookingTitles) {
        // Check if fragment is still attached to avoid IllegalStateException
        if (!isAdded()) {
            Log.d(TAG, "Fragment not attached, skipping spinner setup");
            return;
        }
        
        try {
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
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error setting up spinner: " + e.getMessage());
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

        // First get the branch location
        Double branchLat = null;
        Double branchLng = null;
        
        // Try branch location coordinates
        if (selectedBooking.contains("branch_latitude") && selectedBooking.contains("branch_longitude")) {
            branchLat = selectedBooking.getDouble("branch_latitude");
            branchLng = selectedBooking.getDouble("branch_longitude");
            Log.d(TAG, "Found branch coordinates: " + branchLat + ", " + branchLng);
        } 
        // If no branch coordinates found, check if there's a branch reference
        else if (selectedBooking.contains("branch_id")) {
            String branchId = selectedBooking.getString("branch_id");
            if (branchId != null && !branchId.isEmpty()) {
                // Fetch the branch document to get its location
                fetchBranchLocation(branchId);
                return; // Exit early, the fetchBranchLocation will continue the process
            }
        }
        
        // Then get the pickup location  
        Double pickupLat = null;
        Double pickupLng = null;
        
        // Try pickup location coordinates
        if (selectedBooking.contains("pickup_latitude") && selectedBooking.contains("pickup_longitude")) {
            pickupLat = selectedBooking.getDouble("pickup_latitude");
            pickupLng = selectedBooking.getDouble("pickup_longitude");
            Log.d(TAG, "Found pickup coordinates: " + pickupLat + ", " + pickupLng);
        }
        
        // If branch location is still not available, use fallback
        if (branchLat == null || branchLng == null || branchLat == 0 || branchLng == 0) {
            Log.d(TAG, "No branch coordinates found, using fallback location");
            branchLat = 20.5937;
            branchLng = 78.9629; // Center of India as fallback
            
            Toast.makeText(requireContext(), 
                "Branch location data unavailable for this booking", 
                Toast.LENGTH_SHORT).show();
        }
        
        // If pickup location is not available, use fallback or branch location
        if (pickupLat == null || pickupLng == null || pickupLat == 0 || pickupLng == 0) {
            Log.d(TAG, "No pickup coordinates found, using fallback location");
            
            // Try to get pickup location from address using geocoding
            String pickupAddress = selectedBooking.getString("pickup_location");
            if (pickupAddress != null && !pickupAddress.isEmpty()) {
                geocodePickupAddress(pickupAddress, new LatLng(branchLat, branchLng));
                return; // Exit early, continue after geocoding completes
            } else {
                // If no pickup address either, use location near branch
                // Use a slight offset from branch location for demonstration
                pickupLat = branchLat + 0.02;
                pickupLng = branchLng + 0.02;
                Toast.makeText(requireContext(), 
                    "Pickup location data unavailable for this booking", 
                    Toast.LENGTH_SHORT).show();
            }
        }
        
        branchLocation = new LatLng(branchLat, branchLng);
        pickupLocation = new LatLng(pickupLat, pickupLng);
        
        String carName = selectedBooking.getString("car_name");
        String startDate = selectedBooking.getString("start_date");
        String pickupLocationName = selectedBooking.getString("pickup_location");
        String branchName = selectedBooking.getString("branch_name");

        // Log the location data
        Log.d(TAG, "Branch location: " + branchLat + ", " + branchLng);
        Log.d(TAG, "Pickup location: " + pickupLat + ", " + pickupLng);
        
        // Update map with both markers
        updateMapWithLocations(carName, startDate, pickupLocationName, branchName);
    }
    
    private void geocodePickupAddress(String address, LatLng nearbyLocation) {
        // Check if fragment is still attached before starting
        if (!isAdded()) {
            Log.d(TAG, "Fragment not attached, skipping geocoding");
            return;
        }
        
        Thread geocodeThread = new Thread(() -> {
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(requireContext(), java.util.Locale.getDefault());
                List<android.location.Address> addresses = geocoder.getFromLocationName(address, 1);
                
                // Use Handler to run on UI thread and check if fragment is still attached
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    // Check again if fragment is still attached
                    if (!isAdded()) {
                        Log.d(TAG, "Fragment not attached, skipping UI update after geocoding");
                        return;
                    }
                    
                    try {
                        if (addresses != null && !addresses.isEmpty()) {
                            android.location.Address location = addresses.get(0);
                            Double pickupLat = location.getLatitude();
                            Double pickupLng = location.getLongitude();
                            
                            pickupLocation = new LatLng(pickupLat, pickupLng);
                            Log.d(TAG, "Geocoded pickup location: " + pickupLat + ", " + pickupLng);
                            
                            // Continue with the map update
                            String carName = selectedBooking.getString("car_name");
                            String startDate = selectedBooking.getString("start_date");
                            String pickupLocationName = selectedBooking.getString("pickup_location");
                            String branchName = selectedBooking.getString("branch_name");
                            
                            updateMapWithLocations(carName, startDate, pickupLocationName, branchName);
                        } else {
                            // Use location near branch as fallback
                            Double pickupLat = nearbyLocation.latitude + 0.02;
                            Double pickupLng = nearbyLocation.longitude + 0.02;
                            pickupLocation = new LatLng(pickupLat, pickupLng);
                            
                            if (isAdded()) {
                                Toast.makeText(requireContext(), 
                                    "Could not find coordinates for pickup address", 
                                    Toast.LENGTH_SHORT).show();
                            }
                                
                            // Continue with the map update
                            String carName = selectedBooking.getString("car_name");
                            String startDate = selectedBooking.getString("start_date");
                            String pickupLocationName = selectedBooking.getString("pickup_location");
                            String branchName = selectedBooking.getString("branch_name");
                            
                            updateMapWithLocations(carName, startDate, pickupLocationName, branchName);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing geocoding results: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error geocoding address: " + e.getMessage());
                
                // Use Handler to run on UI thread and check if fragment is still attached
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    // Check again if fragment is still attached
                    if (!isAdded()) {
                        Log.d(TAG, "Fragment not attached, skipping error UI update after geocoding");
                        return;
                    }
                    
                    try {
                        // Use location near branch as fallback
                        Double pickupLat = nearbyLocation.latitude + 0.02;
                        Double pickupLng = nearbyLocation.longitude + 0.02;
                        pickupLocation = new LatLng(pickupLat, pickupLng);
                        
                        Toast.makeText(requireContext(), 
                            "Error finding coordinates for pickup address", 
                            Toast.LENGTH_SHORT).show();
                            
                        // Continue with the map update
                        String carName = selectedBooking.getString("car_name");
                        String startDate = selectedBooking.getString("start_date");
                        String pickupLocationName = selectedBooking.getString("pickup_location");
                        String branchName = selectedBooking.getString("branch_name");
                        
                        updateMapWithLocations(carName, startDate, pickupLocationName, branchName);
                    } catch (Exception innerException) {
                        Log.e(TAG, "Error processing geocoding error: " + innerException.getMessage());
                    }
                });
            }
        });
        
        geocodeThread.start();
    }
    
    private void updateMapWithLocations(String carName, String startDate, String pickupLocationName, String branchName) {
        // Check if fragment is still attached
        if (!isAdded()) {
            Log.d(TAG, "Fragment not attached, skipping map update");
            return;
        }
        
        if (googleMap == null || branchLocation == null || pickupLocation == null) {
            Log.d(TAG, "Cannot update map: missing map or locations");
            return;
        }
        
        try {
            // Clear previous markers
            googleMap.clear();
            
            // Add branch marker
            String branchSnippet = "Branch Location";
            if (startDate != null) {
                branchSnippet += "\nDate: " + startDate;
            }
            
            // Add pickup marker - note: variable should be pickupMarker
            pickupMarker = googleMap.addMarker(new MarkerOptions()
                .position(pickupLocation)
                .title("Pickup Location")
                .snippet(pickupLocationName != null ? pickupLocationName : "Selected Pickup")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                
            // Add branch marker - note: variable should be branchMarker
            branchMarker = googleMap.addMarker(new MarkerOptions()
                .position(branchLocation)
                .title(branchName != null ? branchName : "Branch Location")
                .snippet(branchSnippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                
            if (pickupMarker != null) {
                pickupMarker.showInfoWindow();
            }
            
            // Draw route between pickup and branch
            googleMap.addPolyline(new PolylineOptions()
                .add(pickupLocation, branchLocation)
                .width(8)
                .color(Color.rgb(0, 102, 255)));
                
            // Calculate and update distance and time
            calculateDistanceAndTime();
            
            // Zoom to show both points
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(pickupLocation);
            builder.include(branchLocation);
            LatLngBounds bounds = builder.build();
            
            int padding = 150;
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            
            // Update car info text if it exists
            if (carInfoTextView != null) {
                String infoText = carName != null ? carName : "Selected Car";
                if (startDate != null) {
                    infoText += " - " + startDate;
                }
                carInfoTextView.setText(infoText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating map: " + e.getMessage());
            if (isAdded()) {
                Toast.makeText(requireContext(), 
                    "Error displaying route on map", 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    // New method to fetch branch location data
    private void fetchBranchLocation(String branchId) {
        // Check if fragment is still attached before starting
        if (!isAdded()) {
            Log.d(TAG, "Fragment not attached, skipping branch location fetch");
            return;
        }
        
        Log.d(TAG, "Fetching branch location for ID: " + branchId);
        
        db.collection("branches")
            .document(branchId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                // Check if fragment is still attached
                if (!isAdded()) {
                    Log.d(TAG, "Fragment not attached, skipping branch location UI update");
                    return;
                }
                
                if (documentSnapshot.exists()) {
                    Double branchLat = documentSnapshot.getDouble("latitude");
                    Double branchLng = documentSnapshot.getDouble("longitude");
                    String branchName = documentSnapshot.getString("name");
                    
                    Log.d(TAG, "Branch found: " + branchName + " at " + branchLat + ", " + branchLng);
                    
                    if (branchLat != null && branchLng != null && branchLat != 0 && branchLng != 0) {
                        // Update branch location
                        branchLocation = new LatLng(branchLat, branchLng);
                        
                        // Now try to get pickup location
                        Double pickupLat = null;
                        Double pickupLng = null;
                        
                        if (selectedBooking.contains("pickup_latitude") && selectedBooking.contains("pickup_longitude")) {
                            pickupLat = selectedBooking.getDouble("pickup_latitude");
                            pickupLng = selectedBooking.getDouble("pickup_longitude");
                        }
                        
                        if (pickupLat != null && pickupLng != null && pickupLat != 0 && pickupLng != 0) {
                            pickupLocation = new LatLng(pickupLat, pickupLng);
                        } else {
                            // Try geocoding the pickup address
                            String pickupAddress = selectedBooking.getString("pickup_location");
                            if (pickupAddress != null && !pickupAddress.isEmpty()) {
                                geocodePickupAddress(pickupAddress, new LatLng(branchLat, branchLng));
                                return; // Exit and continue after geocoding
                            } else {
                                // Use location near branch as fallback
                                pickupLocation = new LatLng(branchLat + 0.02, branchLng + 0.02);
                            }
                        }
                        
                        // Get other booking details
                        String carName = selectedBooking.getString("car_name");
                        String startDate = selectedBooking.getString("start_date");
                        String pickupLocationName = selectedBooking.getString("pickup_location");
                        
                        // Update the map
                        updateMapWithLocations(carName, startDate, pickupLocationName, branchName);
                    } else {
                        Log.e(TAG, "Branch document doesn't contain valid coordinates");
                        if (isAdded()) {
                            Toast.makeText(requireContext(), 
                                "Branch location data is missing", 
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Log.e(TAG, "Branch document not found");
                    if (isAdded()) {
                        Toast.makeText(requireContext(), 
                            "Branch information not found", 
                            Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .addOnFailureListener(e -> {
                // Check if fragment is still attached
                if (!isAdded()) {
                    Log.d(TAG, "Fragment not attached, skipping branch location error UI update");
                    return;
                }
                
                Log.e(TAG, "Error fetching branch: " + e.getMessage());
                Toast.makeText(requireContext(), 
                    "Error loading branch location", 
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void calculateDistanceAndTime() {
        // Check if fragment is still attached
        if (!isAdded()) {
            Log.d(TAG, "Fragment not attached, skipping distance calculation");
            return;
        }
        
        if (pickupLocation == null || branchLocation == null) {
            Log.d(TAG, "Cannot calculate distance: missing locations");
            return;
        }
        
        if (distanceValueTextView == null || timeValueTextView == null) {
            Log.d(TAG, "Cannot update distance UI: TextViews are null");
            return;
        }
        
        try {
            double distanceInMeters = SphericalUtil.computeDistanceBetween(pickupLocation, branchLocation);
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
            
            Log.d(TAG, "Distance between pickup and branch: " + formattedDistance + " km, ETA: " + formattedTime);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating distance/time: " + e.getMessage());
        }
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
        
        // Rename the FAB to make it clearer
        myLocationFab.setImageResource(android.R.drawable.ic_menu_mylocation);
        myLocationFab.setContentDescription("Show Route");
        
        // If we already have a selected booking, update the map
        if (selectedBooking != null) {
            updateMapForSelectedBooking();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        
        // Refresh bookings when fragment resumes
        loadActiveBookings();
    }

    @Override
    public void onPause() {
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

