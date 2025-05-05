package com.example.carrentaluser;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.carrentaluser.models.CarModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    ImageView imageCar;
    TextView carName, carPrice;
    EditText startDate, endDate, pickupLocation;
    Button proceedButton;

    CarModel selectedCar;
    Calendar startCalendar, endCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        imageCar = findViewById(R.id.image_car);
        carName = findViewById(R.id.text_car_name);
        carPrice = findViewById(R.id.text_car_price);
        startDate = findViewById(R.id.edit_start_date);
        endDate = findViewById(R.id.edit_end_date);
        pickupLocation = findViewById(R.id.edit_pickup_location);
        proceedButton = findViewById(R.id.button_proceed_to_payment);

        // Get passed carId (and mock load full car object)
        String carId = getIntent().getStringExtra("carId");
        selectedCar = new CarModel(carId, "Swift Dzire", 1200, "https://example.com/swift.jpg");

        carName.setText(selectedCar.getName());
        carPrice.setText("₹" + selectedCar.getPricePerDay() + " / day");
        Glide.with(this).load(selectedCar.getImageUrl()).into(imageCar);

        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();

        startDate.setOnClickListener(v -> showDatePicker(startDate, startCalendar));
        endDate.setOnClickListener(v -> showDatePicker(endDate, endCalendar));

        proceedButton.setOnClickListener(v -> {
            if (validateInput()) {
                Intent intent = new Intent(BookingActivity.this, PaymentActivity.class);
                intent.putExtra("carId", selectedCar.getId());
                intent.putExtra("startDate", startDate.getText().toString());
                intent.putExtra("endDate", endDate.getText().toString());
                intent.putExtra("pickupLocation", pickupLocation.getText().toString());
                startActivity(intent);
            }
        });
    }

    private void showDatePicker(EditText editText, Calendar calendar) {
        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(year, month, day);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            editText.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private boolean validateInput() {
        if (startDate.getText().toString().isEmpty() ||
                endDate.getText().toString().isEmpty() ||
                pickupLocation.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
