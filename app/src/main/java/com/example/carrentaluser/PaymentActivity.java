package com.example.carrentaluser;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentaluser.models.BookingModel;
import com.example.carrentaluser.models.CarModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity {

    TextView summaryText, totalPriceText;
    Button confirmPaymentButton;

    String carId, startDateStr, endDateStr, pickupLocation;
    CarModel selectedCar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        summaryText = findViewById(R.id.text_summary);
        totalPriceText = findViewById(R.id.text_total_price);
        confirmPaymentButton = findViewById(R.id.button_confirm_payment);

        // Get the data passed from BookingActivity
        carId = getIntent().getStringExtra("carId");
        startDateStr = getIntent().getStringExtra("startDate");
        endDateStr = getIntent().getStringExtra("endDate");
        pickupLocation = getIntent().getStringExtra("pickupLocation");

        // Mock Car data (Replace this with real car data from Firebase or intent)
        selectedCar = new CarModel(carId, "Swift Dzire", 1200, "https://example.com/swift.jpg");

        summaryText.setText(
                "Car: " + selectedCar.getName() + "\n" +
                        "From: " + startDateStr + "\n" +
                        "To: " + endDateStr + "\n" +
                        "Pickup: " + pickupLocation
        );

        // Calculate total cost
        int totalDays = calculateDays(startDateStr, endDateStr);
        int totalAmount = selectedCar.getPricePerDay() * totalDays;
        totalPriceText.setText("Total: ₹" + totalAmount);

        confirmPaymentButton.setOnClickListener(v -> {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            BookingModel booking = new BookingModel(
                    UUID.randomUUID().toString(),
                    userId,
                    selectedCar.getId(),
                    selectedCar.getName(),
                    startDateStr,
                    endDateStr,
                    pickupLocation,
                    totalAmount,
                    "Confirmed"
            );

            // Save booking to Firebase Firestore
            FirebaseFirestore.getInstance()
                    .collection("Bookings")
                    .document(booking.getBookingId())
                    .set(booking)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Booking Confirmed!", Toast.LENGTH_SHORT).show();
                        finish(); // Close activity
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Booking Failed! Please try again.", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // Helper function to calculate the number of days between the start and end date
    private int calculateDays(String startDate, String endDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

            // Parse the string dates to Date objects
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);

            // Get the time in milliseconds
            long diff = end.getTime() - start.getTime();

            // Calculate the difference in days
            return (int) (diff / (1000 * 60 * 60 * 24)) + 1; // +1 to include the end day
        } catch (ParseException e) {
            e.printStackTrace();
            return 1;  // Return 1 day in case of error
        }
    }
}
