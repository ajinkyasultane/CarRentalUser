package com.example.carrentaluser;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.location.Address;
import android.location.Geocoder;

public class BookingActivity extends AppCompatActivity {

    private ImageView imageCar;
    private TextView tvCarName, tvCarPrice, tvTotalPrice;
    private TextInputEditText etStartDate, etEndDate, etPickupLocation;
    private TextInputLayout pickupLayout;
    private Button btnSubmit;

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
    
    // Activity result launcher for map selection
    private ActivityResultLauncher<Intent> mapSelectionLauncher;

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

        // Set up date pickers
        setupDatePickers();
        
        // Set up location picker
        setupLocationPicker();

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
                    selectedLatitude = data.getDoubleExtra("latitude", 0);
                    selectedLongitude = data.getDoubleExtra("longitude", 0);
                    String locationName = data.getStringExtra("location_name");
                    
                    if (locationName != null && !locationName.isEmpty()) {
                        etPickupLocation.setText(locationName);
                    }
                }
            }
        );
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
        btnSubmit = findViewById(R.id.btn_submit_booking);

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

        tvCarName.setText(carName);
        tvCarPrice.setText("₹" + carPrice);
        Glide.with(this)
            .load(carImageUrl)
            .centerCrop()
            .into(imageCar);
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
        etPickupLocation.setOnClickListener(v -> openMapSelection());
        
        // Add a trailing icon to the TextInputLayout for the pickup location
        pickupLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        pickupLayout.setEndIconDrawable(R.drawable.ic_map);
        pickupLayout.setEndIconContentDescription("Select location on map");
        pickupLayout.setEndIconOnClickListener(v -> openMapSelection());
    }
    
    private void openMapSelection() {
        Intent intent = new Intent(BookingActivity.this, MapSelectionActivity.class);
        // Pass current location if already selected
        if (selectedLatitude != 0 && selectedLongitude != 0) {
            intent.putExtra("current_latitude", selectedLatitude);
            intent.putExtra("current_longitude", selectedLongitude);
        }
        mapSelectionLauncher.launch(intent);
    }

    private void submitBooking() {
        String startDate = etStartDate.getText().toString();
        String endDate = etEndDate.getText().toString();
        String pickup = etPickupLocation.getText().toString().trim();

        // Validate inputs
        if (startDate.isEmpty() || endDate.isEmpty() || pickup.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
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
            booking.put("pickup_location", pickup);
            booking.put("total_price", totalPrice);
            
            // For testing purposes, set status to Confirmed directly
            booking.put("status", "Confirmed");
            
            booking.put("user_id", userId);
            booking.put("user_email", userEmail);
            booking.put("booking_date", sdf.format(new Date()));
            
            // Add location coordinates if selected from map
            if (selectedLatitude != 0 && selectedLongitude != 0) {
                booking.put("latitude", selectedLatitude);
                booking.put("longitude", selectedLongitude);
                Log.d("BookingActivity", "Adding coordinates to booking: " + selectedLatitude + ", " + selectedLongitude);
            } else {
                Log.d("BookingActivity", "No coordinates selected, using geocoding for: " + pickup);
                // For testing, we'll start a geocoding operation to get coordinates
                startGeocodingAddress(pickup, booking);
                return; // Early return as we'll continue in the callback
            }

            // Show loading state
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Processing...");

            // Submit the booking to Firebase
            submitBookingToFirebase(booking);

        } catch (Exception e) {
            Log.e("BookingActivity", "Error in submitBooking: " + e.getMessage(), e);
            Toast.makeText(this, "Invalid dates", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Submit Booking");
        }
    }
    
    private void startGeocodingAddress(String address, HashMap<String, Object> booking) {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Processing location...");
        
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(address, 1);
                
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address location = addresses.get(0);
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        
                        Log.d("BookingActivity", "Geocoded address to: " + latitude + ", " + longitude);
                        
                        // Add the coordinates to the booking
                        booking.put("latitude", latitude);
                        booking.put("longitude", longitude);
                        
                        // Submit the booking to Firebase
                        submitBookingToFirebase(booking);
                    } else {
                        Log.e("BookingActivity", "Could not geocode address: " + address);
                        Toast.makeText(this, "Could not find location. Please try again or select on map.", Toast.LENGTH_LONG).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit Booking");
                    }
                });
            } catch (Exception e) {
                Log.e("BookingActivity", "Error geocoding address: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error finding location. Please try again.", Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Booking");
                });
            }
        }).start();
    }
    
    private void submitBookingToFirebase(HashMap<String, Object> booking) {
        btnSubmit.setText("Submitting booking...");
        
        db.collection("bookings")
                .add(booking)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Booking submitted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("BookingActivity", "Error adding booking: " + e.getMessage());
                    Toast.makeText(this, "Failed to book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Booking");
                });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Check again when user returns from EditProfileActivity
        checkProfileCompletion();
    }
}
