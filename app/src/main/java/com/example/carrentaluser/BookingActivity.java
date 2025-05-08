package com.example.carrentaluser;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class BookingActivity extends AppCompatActivity {

    private ImageView carImageView;
    private TextView carNameTextView, startDateTextView, endDateTextView, statusTextView, pickupLocationTextView, totalAmountTextView;
    private Button cancelBookingButton;

    private FirebaseFirestore db;
    private String bookingId;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        carImageView = findViewById(R.id.carImageView);
        carNameTextView = findViewById(R.id.carNameTextView);
        startDateTextView = findViewById(R.id.startDateTextView);
        endDateTextView = findViewById(R.id.endDateTextView);
        statusTextView = findViewById(R.id.statusTextView);
        pickupLocationTextView = findViewById(R.id.pickupLocationTextView);
        totalAmountTextView = findViewById(R.id.totalAmountTextView);
        cancelBookingButton = findViewById(R.id.cancelBookingButton);

        // Get booking ID from Intent
        bookingId = getIntent().getStringExtra("bookingId");

        // Load booking details from Firebase
        loadBookingDetails();

        // Handle cancel booking button click
        cancelBookingButton.setOnClickListener(v -> cancelBooking());
    }

    private void loadBookingDetails() {
        db.collection("bookings").document(bookingId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String carName = documentSnapshot.getString("carName");
                        String carImage = documentSnapshot.getString("carImage");
                        String startDate = documentSnapshot.getString("startDate");
                        String endDate = documentSnapshot.getString("endDate");
                        String pickupLocation = documentSnapshot.getString("pickupLocation");
                        String status = documentSnapshot.getString("status");
                        double totalAmount = documentSnapshot.getDouble("totalAmount");

                        // Set values to UI components
                        carNameTextView.setText(carName);
                        startDateTextView.setText(startDate);
                        endDateTextView.setText(endDate);
                        pickupLocationTextView.setText(pickupLocation);
                        statusTextView.setText(status);
                        totalAmountTextView.setText("Total Amount: $" + totalAmount);

                        // Load car image using Glide
                        Glide.with(this).load(carImage).into(carImageView);
                    } else {
                        Toast.makeText(BookingActivity.this, "Booking not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(BookingActivity.this, "Failed to load booking", Toast.LENGTH_SHORT).show());
    }

    private void cancelBooking() {
        db.collection("bookings").document(bookingId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(BookingActivity.this, "Booking canceled", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(BookingActivity.this, "Failed to cancel booking", Toast.LENGTH_SHORT).show());
    }
}
