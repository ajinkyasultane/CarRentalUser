package com.example.carrentaluser;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.carrentaluser.models.CarModel;

public class CarDetailsActivity extends AppCompatActivity {

    ImageView imageCar;
    TextView carName, carPrice, carDescription;
    Button bookNowButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);

        imageCar = findViewById(R.id.image_car);
        carName = findViewById(R.id.text_car_name);
        carPrice = findViewById(R.id.text_car_price);
        carDescription = findViewById(R.id.text_car_description);
        bookNowButton = findViewById(R.id.button_book_now);

        // Get carId from intent
        String carId = getIntent().getStringExtra("carId");

        // In a real app, fetch full car info from Firestore using carId.
        // For now, dummy static data:
        CarModel car = new CarModel(carId, "Swift Dzire", 1200, "https://example.com/swift.jpg");

        carName.setText(car.getName());
        carPrice.setText("₹" + car.getPricePerDay() + " / day");
        carDescription.setText("Comfortable, fuel-efficient sedan ideal for city travel.");

        Glide.with(this).load(car.getImageUrl()).into(imageCar);

        bookNowButton.setOnClickListener(v -> {
            Intent intent = new Intent(CarDetailsActivity.this, BookingActivity.class);
            intent.putExtra("carId", car.getId());
            startActivity(intent);
        });
    }
}
