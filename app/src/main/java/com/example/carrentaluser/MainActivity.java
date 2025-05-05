package com.example.carrentaluser;




import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.carrentaluser.R; // ✅ Required


import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;




import com.example.carrentaluser.fragments.BookingFragment;
import com.example.carrentaluser.fragments.HomeFragment;
import com.example.carrentaluser.fragments.ProfileFragment;
import com.example.carrentaluser.fragments.TrackingFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Links to activity_main.xml

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Load HomeFragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // Handle navigation item selection
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            switch (item.getItemId()) {
                case R.id.nav_home:
                    selectedFragment = new HomeFragment();
                    break;
                case R.id.nav_booking:
                    selectedFragment = new BookingFragment();
                    break;
                case R.id.nav_tracking:
                    selectedFragment = new TrackingFragment();
                    break;
                case R.id.nav_profile:
                    selectedFragment = new ProfileFragment();
                    break;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });
    }
}
