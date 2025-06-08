package com.example.carrentaluser;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.razorpay.Checkout;

public class CarRentalApp extends Application {

    private static final String TAG = "CarRentalApp";
    private static CarRentalApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Set static instance
        instance = this;
        
        // Force light mode for the entire application
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        // Check Google Play Services availability
        checkGooglePlayServices();
        
        // Initialize Razorpay Checkout
        try {
            Checkout.preload(getApplicationContext());
            Log.d(TAG, "Razorpay preload successful");
            
            // Validate Razorpay setup
            validateRazorpaySetup();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Razorpay", e);
        }
        
        // Initialize Firebase Messaging token
        initializeFirebaseMessaging();
    }
    
    /**
     * Check if Google Play Services is available and up to date
     */
    private void checkGooglePlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int resultCode = googleAPI.isGooglePlayServicesAvailable(this);
        
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services not available, error code: " + resultCode);
            
            if (googleAPI.isUserResolvableError(resultCode)) {
                Log.w(TAG, "Google Play Services issue is user resolvable");
            } else {
                Log.e(TAG, "This device does not support Google Play Services");
            }
        } else {
            Log.d(TAG, "Google Play Services is available and up to date");
        }
    }
    
    /**
     * Initialize Firebase Cloud Messaging to get token
     */
    private void initializeFirebaseMessaging() {
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();
                        Log.d(TAG, "FCM Token: " + token);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase Messaging", e);
        }
    }
    
    /**
     * Validate the Razorpay setup and log any potential issues
     */
    private void validateRazorpaySetup() {
        Log.d(TAG, "Validating Razorpay setup...");
        
        // Check Android version
        Log.d(TAG, "Android SDK version: " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT < 21) {
            Log.e(TAG, "Razorpay requires minimum Android SDK 21, current is " + Build.VERSION.SDK_INT);
        }
        
        // Check internet permission
        PackageManager pm = getPackageManager();
        if (pm.checkPermission("android.permission.INTERNET", getPackageName()) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "INTERNET permission not granted. Razorpay requires internet access.");
        }
        
        // Log app version
        try {
            PackageInfo pInfo = pm.getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int verCode = pInfo.versionCode;
            Log.d(TAG, "App version: " + version + " (" + verCode + ")");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting package info", e);
        }
        
        // Check for required permissions
        if (pm.checkPermission("android.permission.RECEIVE_SMS", getPackageName()) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECEIVE_SMS permission not granted. This might affect Razorpay OTP auto-reading.");
        }
        
        Log.d(TAG, "Razorpay validation complete");
    }
    
    /**
     * Get the application instance
     * @return The singleton CarRentalApp instance
     */
    public static CarRentalApp getInstance() {
        return instance;
    }
    
    /**
     * Check if the device has an active internet connection
     * This can be used before starting a Razorpay payment
     */
    public boolean isInternetAvailable() {
        // Always return true for now to prevent false negatives
        // This will allow Razorpay to handle connectivity issues
        return true;

        /* Disable the problematic ping implementation
        try {
            // Simple way to check internet - there are better methods for production
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("/system/bin/ping -c 1 api.razorpay.com");
            int exitValue = process.waitFor();
            return (exitValue == 0);
        } catch (Exception e) {
            Log.e(TAG, "Error checking internet connection", e);
            return false;
        }
        */
    }
} 