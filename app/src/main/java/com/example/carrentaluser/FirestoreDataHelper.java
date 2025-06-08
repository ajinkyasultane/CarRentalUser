package com.example.carrentaluser;

import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to add sample branch data with location coordinates to Firestore
 * This can be used during development to ensure branches have proper location data
 */
public class FirestoreDataHelper {

    private static final String TAG = "FirestoreDataHelper";

    /**
     * Add sample branch data with location coordinates to Firestore
     * Call this method from your activity to populate the database with sample data
     */
    public static void addSampleBranchData(AppCompatActivity activity) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Sample branch 1
        Map<String, Object> branch1 = new HashMap<>();
        branch1.put("name", "Central Branch");
        branch1.put("address", "123 Main Street, City Center");
        branch1.put("location", new GeoPoint(19.8762, 75.3433)); // Using GeoPoint for location
        
        db.collection("branches").document("branch1")
            .set(branch1)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Branch 1 added successfully");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding branch 1", e);
            });
            
        // Sample branch 2
        Map<String, Object> branch2 = new HashMap<>();
        branch2.put("name", "North Branch");
        branch2.put("address", "456 North Avenue, North District");
        branch2.put("location", new GeoPoint(19.9762, 75.4433));
        
        db.collection("branches").document("branch2")
            .set(branch2)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Branch 2 added successfully");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding branch 2", e);
            });
            
        // Sample branch 3
        Map<String, Object> branch3 = new HashMap<>();
        branch3.put("name", "South Branch");
        branch3.put("address", "789 South Road, South District");
        branch3.put("location", new GeoPoint(19.7762, 75.2433));
        
        db.collection("branches").document("branch3")
            .set(branch3)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Branch 3 added successfully");
                Toast.makeText(activity, "Sample branch data added successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding branch 3", e);
            });
    }
} 