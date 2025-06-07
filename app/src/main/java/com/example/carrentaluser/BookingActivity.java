package com.example.carrentaluser;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    private ImageView imageCar;
    private TextView tvCarName, tvCarPrice, tvTotalPrice;
    private TextInputEditText etStartDate, etEndDate, etPickupLocation;
    private TextInputLayout pickupLayout, branchesDropdownLayout, startDateLayout, endDateLayout;
    private AutoCompleteTextView branchesDropdown;
    private RadioGroup driverOptionGroup;
    private RadioButton radioWithDriver, radioWithoutDriver;
    private Button btnSubmit, btnTrackLocation, btnPayment50Percent;

    private String carName, carImageUrl;
    private int carPrice;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private Calendar minDate;
    private Date selectedStartDate;
    private boolean isProfileComplete = false;
    
    // Store location details
    private double selectedLatitude = 0;
    private double selectedLongitude = 0;
    
    // Store branch details
    private List<Map<String, Object>> branchList = new ArrayList<>();
    private String selectedBranchId = "";
    private double selectedBranchLatitude = 0;
    private double selectedBranchLongitude = 0;
    private static final double MAX_DISTANCE_KM = 50.0; // Maximum distance in kilometers
    
    // Activity result launcher for map selection
    private ActivityResultLauncher<Intent> mapSelectionLauncher;

    // Add flag to track if 50% payment is done
    private boolean isAdvancePaymentDone = false;
    // Add payment method tracking
    private String paymentMethod = "";
    private String paymentId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // Initialize views
        initializeViews();
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get car details from intent
        getCarDetails();

        // Check user profile completion
        checkProfileCompletion();
        
        // Fetch branches from Firebase
        fetchBranches();

        // Set up date pickers
        setupDatePickers();
        
        // Set up location picker
        setupLocationPicker();
        
        // Set up driver option radio buttons
        setupDriverOptions();
        
        // Setup 50% payment button
        setupPayment50PercentButton();
        
        // Apply initial visibility rules based on selected driver option
        if (radioWithDriver.isChecked()) {
            // With Driver flow initially - show all fields
            startDateLayout.setVisibility(View.VISIBLE);
            endDateLayout.setVisibility(View.VISIBLE);
            pickupLayout.setVisibility(View.VISIBLE);
            btnTrackLocation.setVisibility(View.VISIBLE);
            tvTotalPrice.setVisibility(View.VISIBLE);
            
            // Show 50% payment button and hide submit initially
            btnPayment50Percent.setVisibility(View.VISIBLE);
            btnSubmit.setVisibility(View.GONE);
            
            Toast.makeText(BookingActivity.this, 
                "With driver service requires 50% advance payment and is only available within 50km of our branches", 
                Toast.LENGTH_LONG).show();
        } else {
            // Without Driver flow initially - simplified experience
            startDateLayout.setVisibility(View.VISIBLE);  // Show start date
            endDateLayout.setVisibility(View.VISIBLE);    // Show end date
            pickupLayout.setVisibility(View.GONE);     // Hide pickup location field
            btnTrackLocation.setVisibility(View.GONE);
            tvTotalPrice.setVisibility(View.VISIBLE);
            
            // Hide 50% payment button and show submit button
            btnPayment50Percent.setVisibility(View.GONE);
            btnSubmit.setVisibility(View.VISIBLE);
            
            Toast.makeText(BookingActivity.this, 
                "Please meet at our nearest branch for pickup", 
                Toast.LENGTH_LONG).show();
        }
        
        // Set up track location button
        setupTrackLocationButton();

        // Set up submit button
        btnSubmit.setOnClickListener(v -> {
            if (isProfileComplete) {
                submitBooking();
            } else {
                showProfileIncompleteDialog();
            }
        });
        
        // Initialize map selection result launcher
        mapSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    double latitude = data.getDoubleExtra("latitude", 0);
                    double longitude = data.getDoubleExtra("longitude", 0);
                    String locationName = data.getStringExtra("location_name");
                    
                    // Only update coordinates if they're valid
                    if (latitude != 0 && longitude != 0) {
                        selectedLatitude = latitude;
                        selectedLongitude = longitude;
                        
                        Log.d("BookingActivity", "Location selected: " + latitude + ", " + longitude);
                        
                        if (locationName != null && !locationName.isEmpty()) {
                            etPickupLocation.setText(locationName);
                            
                            // Check distance from selected branch if "With Driver" is selected
                            if (radioWithDriver.isChecked() && !selectedBranchId.isEmpty()) {
                                if (selectedBranchLatitude != 0 && selectedBranchLongitude != 0) {
                                    checkDistanceFromBranch();
                                } else {
                                    Log.e("BookingActivity", "Branch coordinates not available for distance check");
                                }
                            }
                        }
                    } else {
                        Log.e("BookingActivity", "Invalid location coordinates received");
                    }
                }
            }
        );
        
        // DEVELOPMENT ONLY: Uncomment the line below to add sample branch data with proper location coordinates
        // addSampleBranchData();
    }

    private void initializeViews() {
        imageCar = findViewById(R.id.image_car);
        tvCarName = findViewById(R.id.tv_car_name);
        tvCarPrice = findViewById(R.id.tv_car_price);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        etStartDate = findViewById(R.id.et_start_date);
        etEndDate = findViewById(R.id.et_end_date);
        etPickupLocation = findViewById(R.id.et_pickup_location);
        pickupLayout = findViewById(R.id.pickup_location_layout);
        branchesDropdownLayout = findViewById(R.id.branches_dropdown_layout);
        branchesDropdown = findViewById(R.id.branches_dropdown);
        driverOptionGroup = findViewById(R.id.driver_option_group);
        radioWithDriver = findViewById(R.id.radio_with_driver);
        radioWithoutDriver = findViewById(R.id.radio_without_driver);
        btnSubmit = findViewById(R.id.btn_submit_booking);
        btnTrackLocation = findViewById(R.id.btn_track_location);
        btnPayment50Percent = findViewById(R.id.btn_payment_50_percent);
        startDateLayout = findViewById(R.id.start_date_layout);
        endDateLayout = findViewById(R.id.end_date_layout);

        // Set minimum date to today
        minDate = Calendar.getInstance();
        minDate.set(Calendar.HOUR_OF_DAY, 0);
        minDate.set(Calendar.MINUTE, 0);
        minDate.set(Calendar.SECOND, 0);
        minDate.set(Calendar.MILLISECOND, 0);
    }

    private void getCarDetails() {
        carName = getIntent().getStringExtra("car_name");
        carImageUrl = getIntent().getStringExtra("car_image");
        carPrice = getIntent().getIntExtra("car_price", 0);

        tvCarName.setText("Car Name: "+carName);
        tvCarPrice.setText("₹" + carPrice+" Per Day");
        Glide.with(this)
            .load(carImageUrl)
            .centerCrop()
            .into(imageCar);
    }
    
    private void fetchBranches() {
        branchesDropdown.setEnabled(false);
        
        db.collection("branches")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                branchList.clear();
                List<String> branchNames = new ArrayList<>();
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        String branchName = document.getString("name");
                        if (branchName != null) {
                            Map<String, Object> branchData = new HashMap<>();
                            branchData.put("id", document.getId());
                            branchData.put("name", branchName);
                            
                            // Get location data if available
                            GeoPoint location = document.getGeoPoint("location");
                            if (location != null) {
                                Log.d("BookingActivity", "Branch " + branchName + " location: " + 
                                    location.getLatitude() + ", " + location.getLongitude());
                                branchData.put("latitude", location.getLatitude());
                                branchData.put("longitude", location.getLongitude());
                            } else {
                                // Try to get latitude and longitude as separate fields
                                Object latObj = document.get("latitude");
                                Object lngObj = document.get("longitude");
                                
                                if (latObj != null && lngObj != null) {
                                    double lat = 0;
                                    double lng = 0;
                                    
                                    // Handle different possible types
                                    if (latObj instanceof Double) {
                                        lat = (Double) latObj;
                                    } else if (latObj instanceof Long) {
                                        lat = ((Long) latObj).doubleValue();
                                    } else if (latObj instanceof String) {
                                        lat = Double.parseDouble((String) latObj);
                                    }
                                    
                                    if (lngObj instanceof Double) {
                                        lng = (Double) lngObj;
                                    } else if (lngObj instanceof Long) {
                                        lng = ((Long) lngObj).doubleValue();
                                    } else if (lngObj instanceof String) {
                                        lng = Double.parseDouble((String) lngObj);
                                    }
                                    
                                    if (lat != 0 && lng != 0) {
                                        Log.d("BookingActivity", "Branch " + branchName + 
                                            " location from separate fields: " + lat + ", " + lng);
                                        branchData.put("latitude", lat);
                                        branchData.put("longitude", lng);
                                    } else {
                                        Log.e("BookingActivity", "Branch " + branchName + 
                                            " has invalid coordinates: " + latObj + ", " + lngObj);
                                    }
                                } else {
                                    Log.e("BookingActivity", "Branch " + branchName + " has no location data");
                                }
                            }
                            
                            branchData.put("address", document.getString("address"));
                            branchList.add(branchData);
                            branchNames.add(branchName);
                        }
                    } catch (Exception e) {
                        Log.e("BookingActivity", "Error processing branch data: " + e.getMessage());
                    }
                }
                
                if (branchNames.isEmpty()) {
                    Toast.makeText(BookingActivity.this, 
                        "No branches found. Please contact support.", 
                        Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Set up the dropdown adapter
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    BookingActivity.this, 
                    android.R.layout.simple_dropdown_item_1line, 
                    branchNames
                );
                branchesDropdown.setAdapter(adapter);
                branchesDropdown.setEnabled(true);
                
                // Set item click listener
                branchesDropdown.setOnItemClickListener((parent, view, position, id) -> {
                    if (position < branchList.size()) {
                        Map<String, Object> selectedBranch = branchList.get(position);
                        selectedBranchId = (String) selectedBranch.get("id");
                        
                        Log.d("BookingActivity", "Selected branch ID: " + selectedBranchId);
                        
                        Object latObj = selectedBranch.get("latitude");
                        Object lngObj = selectedBranch.get("longitude");
                        
                        if (latObj != null && lngObj != null) {
                            try {
                                if (latObj instanceof Double) {
                                    selectedBranchLatitude = (Double) latObj;
                                } else if (latObj instanceof Long) {
                                    selectedBranchLatitude = ((Long) latObj).doubleValue();
                                } else if (latObj instanceof String) {
                                    selectedBranchLatitude = Double.parseDouble((String) latObj);
                                }
                                
                                if (lngObj instanceof Double) {
                                    selectedBranchLongitude = (Double) lngObj;
                                } else if (lngObj instanceof Long) {
                                    selectedBranchLongitude = ((Long) lngObj).doubleValue();
                                } else if (lngObj instanceof String) {
                                    selectedBranchLongitude = Double.parseDouble((String) lngObj);
                                }
                                
                                Log.d("BookingActivity", "Branch coordinates: " + selectedBranchLatitude + 
                                      ", " + selectedBranchLongitude);
                                
                                // If in "Without Driver" mode, show the track location button
                                if (radioWithoutDriver.isChecked()) {
                                    btnTrackLocation.setVisibility(View.VISIBLE);
                                }
                                
                                // If location is already selected and "With Driver" is selected, check distance
                                if (radioWithDriver.isChecked() && selectedLatitude != 0 && selectedLongitude != 0) {
                                    checkDistanceFromBranch();
                                }
                            } catch (Exception e) {
                                Log.e("BookingActivity", "Error parsing branch coordinates: " + e.getMessage());
                                Toast.makeText(BookingActivity.this, 
                                    "Error reading branch location data. Please try another branch.", 
                                    Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e("BookingActivity", "Branch location coordinates not found!");
                            Toast.makeText(BookingActivity.this, 
                                "This branch doesn't have location data. Please select another branch or contact support.", 
                                Toast.LENGTH_LONG).show();
                        }
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.e("BookingActivity", "Failed to load branches: " + e.getMessage());
                Toast.makeText(BookingActivity.this, 
                    "Failed to load branches: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void checkProfileCompletion() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        
        // Show loading state
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Checking profile...");
        
        db.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Submit Booking");
                
                if (documentSnapshot.exists()) {
                    // Check if required fields exist
                    String fullName = documentSnapshot.getString("full_name");
                    String age = documentSnapshot.getString("age");
                    String address = documentSnapshot.getString("address");
                    
                    isProfileComplete = fullName != null && !fullName.isEmpty() 
                        && age != null && !age.isEmpty() 
                        && address != null && !address.isEmpty();
                    
                    if (!isProfileComplete) {
                        showProfileIncompleteDialog();
                    }
                } else {
                    isProfileComplete = false;
                    showProfileIncompleteDialog();
                }
            })
            .addOnFailureListener(e -> {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Submit Booking");
                Toast.makeText(this, "Failed to check profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void showProfileIncompleteDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Complete Your Profile")
            .setMessage("You need to complete your profile before booking a car. Would you like to complete your profile now?")
            .setPositiveButton("Yes", (dialog, which) -> {
                Intent intent = new Intent(BookingActivity.this, EditProfileActivity.class);
                intent.putExtra("from_booking", true);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
            .setCancelable(false)
            .show();
    }
    
    private void setupDriverOptions() {
        driverOptionGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_with_driver) {
                // With Driver option selected
                startDateLayout.setVisibility(View.VISIBLE);
                endDateLayout.setVisibility(View.VISIBLE);
                pickupLayout.setVisibility(View.VISIBLE);
                btnTrackLocation.setVisibility(View.VISIBLE);
                tvTotalPrice.setVisibility(View.VISIBLE);
                
                // Show 50% payment button
                btnPayment50Percent.setVisibility(View.VISIBLE);
                
                // Only show submit button if payment is done
                btnSubmit.setVisibility(isAdvancePaymentDone ? View.VISIBLE : View.GONE);
                
                // Reset payment status when switching to with driver
                if (!isAdvancePaymentDone) {
                    btnPayment50Percent.setText("Make 50% Advance Payment");
                }
                
                Toast.makeText(BookingActivity.this, 
                    "With driver service requires 50% advance payment and is only available within 50km of our branches", 
                    Toast.LENGTH_LONG).show();
            } else {
                // Without Driver option selected
                startDateLayout.setVisibility(View.VISIBLE); // Keep these visible for without driver too
                endDateLayout.setVisibility(View.VISIBLE);
                pickupLayout.setVisibility(View.GONE);
                btnTrackLocation.setVisibility(View.GONE);
                tvTotalPrice.setVisibility(View.VISIBLE);
                
                // Hide 50% payment button for without driver option
                btnPayment50Percent.setVisibility(View.GONE);
                
                // Always show submit button for without driver
                btnSubmit.setVisibility(View.VISIBLE);
                
                Toast.makeText(BookingActivity.this, 
                    "Please meet at our nearest branch for pickup", 
                    Toast.LENGTH_LONG).show();
            }
            
            // Always update the branch dropdown visibility (existing logic)
            branchesDropdownLayout.setVisibility(View.VISIBLE);
            
            // Trigger total price calculation whenever driver option changes
            calculateTotalPrice();
        });
    }
    
    private void setupTrackLocationButton() {
        btnTrackLocation.setOnClickListener(v -> {
            if (selectedBranchId.isEmpty() || selectedBranchLatitude == 0 || selectedBranchLongitude == 0) {
                Toast.makeText(BookingActivity.this, 
                    "Please select a branch first", 
                    Toast.LENGTH_SHORT).show();
                branchesDropdown.requestFocus();
                branchesDropdown.showDropDown();
                return;
            }
            
            Log.d("BookingActivity", "Track location button clicked. Branch ID: " + selectedBranchId);
            Log.d("BookingActivity", "Branch coordinates: " + selectedBranchLatitude + ", " + selectedBranchLongitude);
            
            // Launch map activity in track branch mode
            Intent intent = new Intent(BookingActivity.this, MapSelectionActivity.class);
            intent.putExtra("track_branch", true); // This will trigger branch tracking mode
            intent.putExtra("branch_latitude", selectedBranchLatitude);
            intent.putExtra("branch_longitude", selectedBranchLongitude);
            intent.putExtra("branch_name", branchesDropdown.getText().toString());
            
            // Pass additional flags for clarity
            intent.putExtra("show_route", true);
            intent.putExtra("from_no_driver_mode", true);
            
            startActivity(intent);
        });
    }

    private void setupDatePickers() {
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate, true));
        etEndDate.setOnClickListener(v -> {
            if (etStartDate.getText().toString().isEmpty()) {
                Toast.makeText(this, "Please select start date first", Toast.LENGTH_SHORT).show();
                return;
            }
            showDatePicker(etEndDate, false);
        });
    }

    private void showDatePicker(TextInputEditText target, boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        if (isStartDate) {
            calendar = minDate;
        } else if (selectedStartDate != null) {
            calendar.setTime(selectedStartDate);
        }

        Calendar finalCalendar = calendar;
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                finalCalendar.set(year, month, dayOfMonth);
                Date selectedDate = finalCalendar.getTime();
                
                if (isStartDate) {
                    selectedStartDate = selectedDate;
                    etEndDate.setText(""); // Clear end date when start date changes
                } else {
                    // Validate end date is after start date
                    if (selectedDate.before(selectedStartDate)) {
                        Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                
                target.setText(sdf.format(selectedDate));
                    calculateTotalPrice();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date
        if (isStartDate) {
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        } else {
            datePickerDialog.getDatePicker().setMinDate(selectedStartDate.getTime());
        }

        // Set maximum date (e.g., 1 year from now)
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, 1);
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void calculateTotalPrice() {
        String start = etStartDate.getText().toString();
        String end = etEndDate.getText().toString();

        if (start.isEmpty() || end.isEmpty()) {
            return;
        }

        try {
            Date startDate = sdf.parse(start);
            Date endDate = sdf.parse(end);
            if (startDate != null && endDate != null && !endDate.before(startDate)) {
                long diff = endDate.getTime() - startDate.getTime();
                int days = (int) (diff / (1000 * 60 * 60 * 24)) + 1;
                int total = days * carPrice;
                tvTotalPrice.setText("Total Price: ₹" + total);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void setupLocationPicker() {
        // Make the pickup location field non-editable but clickable
        etPickupLocation.setFocusable(false);
        etPickupLocation.setClickable(true);
        
        // Set click listener for the pickup location field
        etPickupLocation.setOnClickListener(v -> {
            Log.d("BookingActivity", "Opening map selection. Current branch ID: " + selectedBranchId);
            
            if (selectedBranchId.isEmpty()) {
                Toast.makeText(BookingActivity.this, 
                    "Please select a branch first", 
                    Toast.LENGTH_SHORT).show();
                branchesDropdown.requestFocus();
                branchesDropdown.showDropDown();
            } else {
                if (selectedBranchLatitude == 0 || selectedBranchLongitude == 0) {
                    Log.e("BookingActivity", "Branch location coordinates are missing!");
                    Toast.makeText(BookingActivity.this, 
                        "Branch location data is missing. Please select a different branch.", 
                        Toast.LENGTH_LONG).show();
                    
                    // Reset branch selection
                    branchesDropdown.setText("");
                    selectedBranchId = "";
                    branchesDropdown.requestFocus();
                    branchesDropdown.showDropDown();
                } else {
                    openMapSelection();
                }
            }
        });
        
        // Add a trailing icon to the TextInputLayout for the pickup location
        pickupLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        pickupLayout.setEndIconDrawable(R.drawable.ic_map);
        pickupLayout.setEndIconContentDescription("Select location on map");
        pickupLayout.setEndIconOnClickListener(v -> {
            Log.d("BookingActivity", "Map icon clicked. Current branch ID: " + selectedBranchId);
            
            if (selectedBranchId.isEmpty()) {
                Toast.makeText(BookingActivity.this, 
                    "Please select a branch first", 
                    Toast.LENGTH_SHORT).show();
                branchesDropdown.requestFocus();
                branchesDropdown.showDropDown();
            } else {
                if (selectedBranchLatitude == 0 || selectedBranchLongitude == 0) {
                    Log.e("BookingActivity", "Branch location coordinates are missing!");
                    Toast.makeText(BookingActivity.this, 
                        "Branch location data is missing. Please select a different branch.", 
                        Toast.LENGTH_LONG).show();
                    
                    // Reset branch selection
                    branchesDropdown.setText("");
                    selectedBranchId = "";
                    branchesDropdown.requestFocus();
                    branchesDropdown.showDropDown();
                } else {
                    openMapSelection();
                }
            }
        });
    }
    
    private void openMapSelection() {
        // Check if branch is selected - only need to verify selectedBranchId is not empty
        if (selectedBranchId.isEmpty()) {
            Toast.makeText(BookingActivity.this, 
                "Please select a branch first", 
                Toast.LENGTH_SHORT).show();
            branchesDropdown.requestFocus();
            branchesDropdown.showDropDown();
            return;
        }
        
        Intent intent = new Intent(BookingActivity.this, MapSelectionActivity.class);
        
        // Pass branch location for 50km restriction if in with-driver mode
        intent.putExtra("with_driver_mode", radioWithDriver.isChecked());
        intent.putExtra("branch_latitude", selectedBranchLatitude);
        intent.putExtra("branch_longitude", selectedBranchLongitude);
        
        // Pass current location if already selected
        if (selectedLatitude != 0 && selectedLongitude != 0) {
            intent.putExtra("current_latitude", selectedLatitude);
            intent.putExtra("current_longitude", selectedLongitude);
        }
        
        mapSelectionLauncher.launch(intent);
    }
    
    private void checkDistanceFromBranch() {
        if (selectedBranchLatitude == 0 || selectedBranchLongitude == 0 || 
            selectedLatitude == 0 || selectedLongitude == 0) {
            return;
        }
        
        // Calculate distance between pickup location and branch
        float[] results = new float[1];
        Location.distanceBetween(
            selectedLatitude, selectedLongitude,
            selectedBranchLatitude, selectedBranchLongitude,
            results
        );
        
        // Convert meters to kilometers
        double distanceKm = results[0] / 1000.0;
        
        // Check if distance exceeds maximum allowed
        if (distanceKm > MAX_DISTANCE_KM) {
            showMaxDistanceExceededDialog();
        }
    }
    
    private void showMaxDistanceExceededDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Location Too Far")
            .setMessage("Your selected pickup location is more than 50km away from the chosen branch. " +
                        "With driver service is only available within 50km of our branches.\n\n" +
                        "Please select a closer location, choose a different branch, or use the 'Without Driver' option.")
            .setPositiveButton("Switch to Without Driver", (dialog, which) -> {
                radioWithoutDriver.setChecked(true);
            })
            .setNegativeButton("Select Different Location", (dialog, which) -> {
                openMapSelection();
            })
            .setNeutralButton("Choose Different Branch", (dialog, which) -> {
                branchesDropdown.setText("");
                selectedBranchId = "";
                branchesDropdown.showDropDown();
            })
            .setCancelable(false)
            .show();
    }

    private void setupPayment50PercentButton() {
        btnPayment50Percent.setOnClickListener(v -> {
            if (!isProfileComplete) {
                showProfileIncompleteDialog();
                return;
            }
            
            // Validate inputs before proceeding to payment
            String startDate = etStartDate.getText().toString();
            String endDate = etEndDate.getText().toString();
            String pickup = etPickupLocation.getText().toString().trim();
            String branch = branchesDropdown.getText().toString().trim();

            // Validate inputs
            if (startDate.isEmpty() || endDate.isEmpty() || branch.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (pickup.isEmpty()) {
                Toast.makeText(this, "Please select a pickup location", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (selectedBranchId.isEmpty()) {
                Toast.makeText(this, "Please select a valid branch", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check distance for 50km limit
            if (selectedBranchLatitude != 0 && selectedBranchLongitude != 0 &&
                selectedLatitude != 0 && selectedLongitude != 0) {
                
                float[] results = new float[1];
                Location.distanceBetween(
                    selectedLatitude, selectedLongitude,
                    selectedBranchLatitude, selectedBranchLongitude,
                    results
                );
                
                double distanceKm = results[0] / 1000.0;
                
                if (distanceKm > MAX_DISTANCE_KM) {
                    showMaxDistanceExceededDialog();
                    return;
                }
            }
            
            try {
                Date start = sdf.parse(startDate);
                Date end = sdf.parse(endDate);
                
                if (start == null || end == null) {
                    Toast.makeText(this, "Invalid dates", Toast.LENGTH_SHORT).show();
                    return;
                }

                int days = (int) ((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
                int totalPrice = days * carPrice;
                // Calculate 50% of the total price
                int advancePayment = totalPrice / 2;
                
                // Open PaymentActivity for 50% payment
                Intent intent = new Intent(BookingActivity.this, PaymentActivity.class);
                intent.putExtra("car_name", carName);
                intent.putExtra("car_image", carImageUrl);
                intent.putExtra("amount", advancePayment);
                intent.putExtra("is_advance_payment", true);
                intent.putExtra("total_price", totalPrice);
                
                startActivityForResult(intent, 100); // Use requestCode 100 for payment
                
            } catch (Exception e) {
                Toast.makeText(this, "Invalid dates", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Payment was successful
            isAdvancePaymentDone = true;
            
            // Get payment details
            if (data != null) {
                paymentId = data.getStringExtra("payment_id");
                paymentMethod = data.getStringExtra("payment_method");
            }
            
            // Update UI
            btnPayment50Percent.setText("50% Advance Payment Completed ✓");
            btnPayment50Percent.setEnabled(false);
            
            // Show the submit button now that payment is done
            btnSubmit.setVisibility(View.VISIBLE);
            
            Toast.makeText(this, "Advance payment successful. You can now confirm your booking.", 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void submitBooking() {
        String startDate = etStartDate.getText().toString();
        String endDate = etEndDate.getText().toString();
        String pickup = radioWithDriver.isChecked() ? etPickupLocation.getText().toString().trim() : "";
        String branch = branchesDropdown.getText().toString().trim();

        // Validate inputs
        if (startDate.isEmpty() || endDate.isEmpty() || branch.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (radioWithDriver.isChecked()) {
            if (pickup.isEmpty()) {
                Toast.makeText(this, "Please select a pickup location", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check if advance payment is done for with driver option
            if (!isAdvancePaymentDone) {
                Toast.makeText(this, "Please make the 50% advance payment first", Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        if (selectedBranchId.isEmpty()) {
            Toast.makeText(this, "Please select a valid branch", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check distance again if "With Driver" is selected to prevent booking beyond 50km
        if (radioWithDriver.isChecked() && 
            selectedBranchLatitude != 0 && selectedBranchLongitude != 0 &&
            selectedLatitude != 0 && selectedLongitude != 0) {
            
            float[] results = new float[1];
            Location.distanceBetween(
                selectedLatitude, selectedLongitude,
                selectedBranchLatitude, selectedBranchLongitude,
                results
            );
            
            double distanceKm = results[0] / 1000.0;
            
            if (distanceKm > MAX_DISTANCE_KM) {
                new AlertDialog.Builder(this)
                    .setTitle("Cannot Book With Driver")
                    .setMessage("Booking with driver is not allowed for locations beyond 50km from our branch. " +
                                "Please select a closer location or use the 'Without Driver' option.")
                    .setPositiveButton("Switch to Without Driver", (dialog, which) -> {
                        radioWithoutDriver.setChecked(true);
                    })
                    .setNegativeButton("Select Different Location", (dialog, which) -> {
                        openMapSelection();
                    })
                    .setCancelable(false)
                    .show();
                return;
            }
        }

        try {
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);
            
            if (start == null || end == null) {
                Toast.makeText(this, "Invalid dates", Toast.LENGTH_SHORT).show();
                return;
            }

            int days = (int) ((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
            int totalPrice = days * carPrice;

            String userId = mAuth.getCurrentUser().getUid();
            String userEmail = mAuth.getCurrentUser().getEmail();

            HashMap<String, Object> booking = new HashMap<>();
            booking.put("car_name", carName);
            booking.put("car_image", carImageUrl);
            booking.put("car_price", carPrice);
            booking.put("start_date", startDate);
            booking.put("end_date", endDate);
            booking.put("branch_id", selectedBranchId);
            booking.put("branch_name", branch);
            booking.put("with_driver", radioWithDriver.isChecked());
            booking.put("total_price", totalPrice);
            booking.put("status", "Pending");
            booking.put("user_id", userId);
            booking.put("user_email", userEmail);
            booking.put("booking_date", sdf.format(new Date()));
            
            // Add payment status if with driver
            if (radioWithDriver.isChecked()) {
                booking.put("advance_payment_done", isAdvancePaymentDone);
                booking.put("advance_payment_amount", totalPrice / 2);
                booking.put("remaining_payment", totalPrice / 2);
                
                // Add payment method details
                if (isAdvancePaymentDone && !paymentMethod.isEmpty()) {
                    booking.put("payment_method", paymentMethod);
                }
                
                // Add payment ID if available
                if (isAdvancePaymentDone && !paymentId.isEmpty()) {
                    booking.put("payment_id", paymentId);
                }
            }
            
            // Add pickup location for "With Driver" option
            if (radioWithDriver.isChecked()) {
                booking.put("pickup_location", pickup);
                
                // Add location coordinates if selected from map
                if (selectedLatitude != 0 && selectedLongitude != 0) {
                    booking.put("pickup_latitude", selectedLatitude);
                    booking.put("pickup_longitude", selectedLongitude);
                }
            }
            
            // Add branch location
            if (selectedBranchLatitude != 0 && selectedBranchLongitude != 0) {
                booking.put("branch_latitude", selectedBranchLatitude);
                booking.put("branch_longitude", selectedBranchLongitude);
            }

            // Show loading state
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Processing...");

            db.collection("bookings")
                    .add(booking)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Booking submitted successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit Booking");
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Invalid dates", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Submit Booking");
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Check again when user returns from EditProfileActivity
        checkProfileCompletion();
    }

    private void addSampleBranchData() {
        // This method is for development purposes only
        FirestoreDataHelper.addSampleBranchData(this);
        
        // Reload branches after a short delay
        new android.os.Handler().postDelayed(() -> {
            fetchBranches();
        }, 2000);
    }
}
