package com.example.carrentaluser.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for UPI payments.
 * This class will be used for future integration with UPI payment gateways.
 */
public class UpiPaymentHelper {
    
    private static final String TAG = "UpiPaymentHelper";
    
    // Common UPI app package names
    public static final String GOOGLE_PAY_PACKAGE = "com.google.android.apps.nbu.paisa.user";
    public static final String PHONE_PE_PACKAGE = "com.phonepe.app";
    public static final String PAYTM_PACKAGE = "net.one97.paytm";
    public static final String BHIM_UPI_PACKAGE = "in.org.npci.upiapp";
    public static final String AMAZON_PAY_PACKAGE = "in.amazon.mShop.android.shopping";
    
    /**
     * Create a UPI payment intent URI
     * 
     * @param upiId The UPI ID to send payment to
     * @param name Payee name
     * @param description Transaction description/note
     * @param amount Payment amount
     * @return URI for UPI payment
     */
    public static Uri createUpiPaymentUri(String upiId, String name, String description, String amount) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("upi").authority("pay");
        uriBuilder.appendQueryParameter("pa", upiId);  // Payee address (UPI ID)
        uriBuilder.appendQueryParameter("pn", name);   // Payee name
        uriBuilder.appendQueryParameter("tn", description);  // Transaction note
        uriBuilder.appendQueryParameter("am", amount);  // Amount
        uriBuilder.appendQueryParameter("cu", "INR");  // Currency
        // Transaction reference ID (for future use)
        uriBuilder.appendQueryParameter("tr", "TR" + System.currentTimeMillis());
        return uriBuilder.build();
    }
    
    /**
     * Check if UPI payment is possible on the device
     * 
     * @param context Application context
     * @return true if UPI payment is possible, false otherwise
     */
    public static boolean isUpiPaymentPossible(Context context) {
        Uri uri = Uri.parse("upi://pay");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);
        return apps != null && !apps.isEmpty();
    }
    
    /**
     * Get list of available UPI apps on the device
     * 
     * @param context Application context
     * @return List of UPI app package names and display names
     */
    public static List<Map<String, String>> getAvailableUpiApps(Context context) {
        List<Map<String, String>> upiApps = new ArrayList<>();
        
        Uri uri = Uri.parse("upi://pay");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);
        
        for (ResolveInfo app : apps) {
            String packageName = app.activityInfo.packageName;
            String appName = app.loadLabel(packageManager).toString();
            
            Map<String, String> upiApp = new HashMap<>();
            upiApp.put("packageName", packageName);
            upiApp.put("appName", appName);
            
            upiApps.add(upiApp);
            Log.d(TAG, "UPI app: " + appName + " (" + packageName + ")");
        }
        
        return upiApps;
    }
    
    /**
     * Create an intent to launch a specific UPI app for payment
     * 
     * @param upiId UPI ID to send payment to
     * @param name Payee name
     * @param description Transaction description/note
     * @param amount Payment amount
     * @param packageName Package name of the UPI app to launch
     * @return Intent to launch the UPI app
     */
    public static Intent createUpiPaymentIntent(String upiId, String name, String description, 
                                               String amount, String packageName) {
        Uri uri = createUpiPaymentUri(upiId, name, description, amount);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        
        if (packageName != null && !packageName.isEmpty()) {
            intent.setPackage(packageName);
        }
        
        return intent;
    }
    
    /**
     * Verify UPI ID format
     * Basic validation - more complex validation would require a server-side check
     * 
     * @param upiId The UPI ID to validate
     * @return true if format is valid, false otherwise
     */
    public static boolean isValidUpiIdFormat(String upiId) {
        if (upiId == null || upiId.isEmpty()) {
            return false;
        }
        
        // Basic format validation: username@provider
        return upiId.matches("[a-zA-Z0-9.]+@[a-zA-Z0-9]+");
    }
    
    /**
     * Parse the result from a UPI payment
     * Different UPI apps may return different response formats
     * This is a simplified implementation
     * 
     * @param data Intent data returned from UPI app
     * @return Map containing payment status information
     */
    public static Map<String, String> parseUpiPaymentResult(Intent data) {
        Map<String, String> result = new HashMap<>();
        
        if (data == null) {
            result.put("status", "failed");
            result.put("message", "No data returned");
            return result;
        }
        
        String status = data.getStringExtra("Status");
        if (status == null) {
            status = data.getStringExtra("status");
        }
        
        if (status == null) {
            status = "unknown";
        }
        
        result.put("status", status.toLowerCase());
        
        // Get transaction ID if available
        String txnId = data.getStringExtra("txnId");
        if (txnId != null) {
            result.put("transaction_id", txnId);
        }
        
        // Get response code if available
        String responseCode = data.getStringExtra("responseCode");
        if (responseCode != null) {
            result.put("response_code", responseCode);
        }
        
        // Get transaction reference if available
        String txnRef = data.getStringExtra("txnRef");
        if (txnRef != null) {
            result.put("transaction_ref", txnRef);
        }
        
        return result;
    }
} 