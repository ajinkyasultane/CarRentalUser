package com.example.carrentaluser;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        imageCar = findViewById(R.id.image_car);
        tvCarName = findViewById(R.id.tv_car_name);
        tvCarPrice = findViewById(R.id.tv_car_price);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        etStartDate = findViewById(R.id.et_start_date);
        etEndDate = findViewById(R.id.et_end_date);
        etPickupLocation = findViewById(R.id.et_pickup_location);
        btnSubmit = findViewById(R.id.btn_submit_booking);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        carName = getIntent().getStringExtra("car_name");
        carImageUrl = getIntent().getStringExtra("car_image");
        carPrice = getIntent().getIntExtra("car_price", 0);

        tvCarName.setText(carName);
        tvCarPrice.setText("₹" + carPrice);
        Glide.with(this).load(carImageUrl).into(imageCar);

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnSubmit.setOnClickListener(v -> submitBooking());
    }

    private void showDatePicker(TextInputEditText target) {
        final Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    target.setText(sdf.format(calendar.getTime()));
                    calculateTotalPrice();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void calculateTotalPrice() {
        String start = etStartDate.getText().toString();
        String end = etEndDate.getText().toString();

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
        String pickup = etPickupLocation.getText().toString();
        String userId = mAuth.getCurrentUser().getUid();
        String userEmail = mAuth.getCurrentUser().getEmail();

        if (startDate.isEmpty() || endDate.isEmpty() || pickup.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);
            int days = (int) ((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
            int totalPrice = days * carPrice;

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

            db.collection("bookings")
                    .add(booking)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Booking submitted!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Invalid dates", Toast.LENGTH_SHORT).show();
        }
    }
}
