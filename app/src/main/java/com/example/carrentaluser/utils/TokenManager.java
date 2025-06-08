package com.example.carrentaluser.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.carrentaluser.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.firestore.FirebaseFirestore;

public class TokenManager extends FirebaseMessagingService {

    private static final String TAG = "TokenManager";
    private static final String CHANNEL_ID = "car_rental_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        uploadToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());
        
        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String message = remoteMessage.getNotification().getBody();
            
            Log.d(TAG, "Message Notification Title: " + title);
            Log.d(TAG, "Message Notification Body: " + message);
            
            sendNotification(title, message);
        }
        
        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            
            // Handle data payload if needed
        }
    }
    
    private void sendNotification(String title, String messageBody) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Create the notification channel (required for Android Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Car Rental Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        
        // Build the notification
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.user_car_logo1)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true);
        
        // Show the notification
        notificationManager.notify(0, notificationBuilder.build());
    }

    public static void uploadToken() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Fetching FCM token failed", task.getException());
                            return;
                        }

                        String token = task.getResult();
                        uploadToken(token);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error getting FCM token", e);
        }
    }
    
    private static void uploadToken(String token) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        
        try {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcm_token", token)
                    .addOnSuccessListener(unused -> Log.d(TAG, "FCM token uploaded successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to upload FCM token", e));
        } catch (Exception e) {
            Log.e(TAG, "Error uploading token to Firestore", e);
        }
    }
}
