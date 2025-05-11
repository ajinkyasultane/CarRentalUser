package com.example.carrentaluser;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    private ImageView carImage;
    private TextView carName;
    private EditText startDate, endDate, location;
    private Button confirmButton;

    private String name, imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // Initialize views
        carImage = findViewById(R.id.booking_car_image);
        carName = findViewById(R.id.booking_car_name);
        startDate = findViewById(R.id.booking_start_date);
        endDate = findViewById(R.id.booking_end_date);
        location = findViewById(R.id.booking_location);
        confirmButton = findViewById(R.id.btn_confirm_booking);

        // Get data from Intent
        name = getIntent().getStringExtra("car_name");
        imageUrl = getIntent().getStringExtra("car_image");

        // Set values to views
        carName.setText(name);
        Glide.with(this).load(imageUrl).into(carImage);

        confirmButton.setOnClickListener(v -> confirmBooking());
    }

    private void confirmBooking() {
        String sDate = startDate.getText().toString().trim();
        String eDate = endDate.getText().toString().trim();
        String pickupLoc = location.getText().toString().trim();

        if (sDate.isEmpty() || eDate.isEmpty() || pickupLoc.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        String userEmail = user.getEmail();

        Map<String, Object> booking = new HashMap<>();
        booking.put("car_name", name);
        booking.put("car_image", imageUrl);
        booking.put("start_date", sDate);
        booking.put("end_date", eDate);
        booking.put("pickup_location", pickupLoc);
        booking.put("status", "Pending");
        booking.put("user_id", userId);
        booking.put("user_email", userEmail);

        FirebaseFirestore.getInstance()
                .collection("bookings")
                .add(booking)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Booking Confirmed!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Booking Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
