package com.example.carrentaluser;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private ImageView imageCar;
    private TextView tvCarName, tvCarPrice, tvTotalPrice;
    private TextInputEditText etStartDate, etEndDate, etPickupLocation;
    private Button btnSubmit;

    private String carName, carImageUrl;
    private int carPrice;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private Calendar minDate;
    private Date selectedStartDate;
    private boolean isProfileComplete = false;

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

        // Set up submit button
        btnSubmit.setOnClickListener(v -> {
            if (isProfileComplete) {
                submitBooking();
            } else {
                showProfileIncompleteDialog();
            }
        });
    }

    private void initializeViews() {
        imageCar = findViewById(R.id.image_car);
        tvCarName = findViewById(R.id.tv_car_name);
        tvCarPrice = findViewById(R.id.tv_car_price);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        etStartDate = findViewById(R.id.et_start_date);
        etEndDate = findViewById(R.id.et_end_date);
        etPickupLocation = findViewById(R.id.et_pickup_location);
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
            booking.put("status", "Pending");
            booking.put("user_id", userId);
            booking.put("user_email", userEmail);
            booking.put("booking_date", sdf.format(new Date()));

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
}
