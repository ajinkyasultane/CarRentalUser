package com.example.carrentaluser;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentaluser.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    
    // Splash screen duration - 3 seconds
    private static final int SPLASH_DURATION = 3000;
    
    // UI elements
    private ImageView carImageView;
    private TextView appNameText;
    private TextView taglineText;
    
    // Session manager
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_splash);
        
        // Initialize session manager
        sessionManager = SessionManager.getInstance(this);

        // Initialize UI elements
        carImageView = findViewById(R.id.car_image);
        appNameText = findViewById(R.id.app_name_text);
        taglineText = findViewById(R.id.tagline_text);
        
        // Set initial visibility
        carImageView.setTranslationX(-1000f); // Start off-screen
        
        // Start animations with a slight delay
        new Handler().postDelayed(this::startAnimations, 200);
        
        // Navigate to login after splash duration
        new Handler().postDelayed(this::navigateToLogin, SPLASH_DURATION);
    }
    
    private void startAnimations() {
        // Car drives in from left to center
        carImageView.animate()
            .translationX(0f)
            .setDuration(1500) // 1.5 seconds to drive in
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .withEndAction(() -> {
                // Add a slight bounce when car stops
                carImageView.animate()
                    .translationY(-30f)
                    .setDuration(120)
                    .withEndAction(() -> 
                        carImageView.animate()
                            .translationY(-20f)
                            .setDuration(80)
                            .start())
                    .start();
            })
            .start();
        
        // Fade in app name and tagline
        appNameText.setAlpha(0f);
        taglineText.setAlpha(0f);
        
        appNameText.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(700) // Start after car begins moving
            .start();
        
        taglineText.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(1000) // Start after app name begins fading in
            .start();
    }
    
    private void navigateToLogin() {
        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "User is already logged in, navigating to MainActivity");
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            Log.d(TAG, "No active session found, navigating to LoginActivity");
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
        }
        
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
} 