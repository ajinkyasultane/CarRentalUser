package com.example.carrentaluser.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.firestore.FirebaseFirestore;

public class TokenManager {

    public static void uploadToken() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("TokenManager", "Fetching FCM token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .update("fcm_token", token)
                            .addOnSuccessListener(unused -> Log.d("TokenManager", "FCM token uploaded"))
                            .addOnFailureListener(e -> Log.e("TokenManager", "Failed to upload FCM token", e));
                });
    }
}
