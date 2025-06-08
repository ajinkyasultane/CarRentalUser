package com.example.carrentaluser.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Manages user session data using SharedPreferences.
 * Handles saving and retrieving login state for one-time login functionality.
 */
public class SessionManager {
    private static final String TAG = "SessionManager";
    
    // Shared preferences file name
    private static final String PREF_NAME = "CarRentalSession";
    
    // Shared preferences keys
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_LAST_LOGIN = "lastLogin";
    
    // Shared preferences
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    
    // Singleton instance
    private static SessionManager instance;
    
    /**
     * Constructor
     */
    private SessionManager(Context context) {
        // Private mode for shared preferences
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Create login session
     */
    public void createLoginSession(String userId, String email) {
        // Store login values
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putLong(KEY_LAST_LOGIN, System.currentTimeMillis());
        
        // Commit changes
        editor.apply();
        
        Log.d(TAG, "User login session created for: " + email);
    }
    
    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    /**
     * Get stored session data
     */
    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }
    
    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }
    
    public long getLastLoginTime() {
        return pref.getLong(KEY_LAST_LOGIN, 0);
    }
    
    /**
     * Clear session details
     */
    public void logout() {
        // Clear all data from shared preferences
        editor.clear();
        editor.apply();
        
        Log.d(TAG, "User logged out, session cleared");
    }
    
    /**
     * Update session with latest info
     */
    public void updateSessionInfo(String userId, String email) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putLong(KEY_LAST_LOGIN, System.currentTimeMillis());
        editor.apply();
        
        Log.d(TAG, "Session info updated for: " + email);
    }
} 