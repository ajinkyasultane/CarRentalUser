package com.example.carrentaluser;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;

public class BookCarActivity extends AppCompatActivity {

    private ImageView carImageView;
    private EditText startDateEditText, endDateEditText, pickupLocationEditText;
    private Button confirmBookingButton;

    private String carId, carName, carImage;
    private double pricePerDay;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_car);

        // Initialize Firestore and FirebaseAuth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        // Get car data from Intent
        carId = getIntent().getStringExtra("carId");
        carName = getIntent().getStringExtra("carName");
        carImage = getIntent().getStringExtra("carImage");
        pricePerDay = getIntent().getDoubleExtra("pricePerDay", 0.0);

        // Initialize Views
        carImageView = findViewById(R.id.carImageView);
        startDateEditText = findViewById(R.id.startDateEditText);
        endDateEditText = findViewById(R.id.endDateEditText);
        pickupLocationEditText = findViewById(R.id.pickupLocationEditText);
        confirmBookingButton = findViewById(R.id.confirmBookingButton);

        // Load car image using Glide
        Glide.with(this).load(carImage).into(carImageView);

        // Handle confirm booking button click
        confirmBookingButton.setOnClickListener(v -> confirmBooking());
    }

    private void confirmBooking() {
        String startDate = startDateEditText.getText().toString();
        String endDate = endDateEditText.getText().toString();
        String pickupLocation = pickupLocationEditText.getText().toString();

        // Check if fields are empty
        if (startDate.isEmpty() || endDate.isEmpty() || pickupLocation.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate total price based on the start and end dates
        long startDateMillis = Long.parseLong(startDate);
        long endDateMillis = Long.parseLong(endDate);
        long duration = (endDateMillis - startDateMillis) / (1000 * 60 * 60 * 24); // in days
        double totalAmount = pricePerDay * duration;

        // Create a booking object and save it to Firestore
        HashMap<String, Object> bookingMap = new HashMap<>();
        bookingMap.put("carId", carId);
        bookingMap.put("carName", carName);
        bookingMap.put("carImage", carImage);
        bookingMap.put("startDate", startDate);
        bookingMap.put("endDate", endDate);
        bookingMap.put("pickupLocation", pickupLocation);
        bookingMap.put("totalAmount", totalAmount);
        bookingMap.put("status", "Pending");
        bookingMap.put("userId", userId);

        // Save booking to Firestore under the user's bookings collection
        db.collection("bookings").add(bookingMap)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(BookCarActivity.this, "Booking submitted successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity after booking
                })
                .addOnFailureListener(e -> Toast.makeText(BookCarActivity.this, "Booking failed", Toast.LENGTH_SHORT).show());
    }
}
