package com.example.carrentaluser.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.carrentaluser.ChangePasswordActivity;
import com.example.carrentaluser.EditProfileActivity;
import com.example.carrentaluser.ImageViewActivity;
import com.example.carrentaluser.LoginActivity;
import com.example.carrentaluser.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    
    private ImageView profileImageView;
    private CardView profileImageContainer;
    private TextView greetingTextView, emailTextView, ageTextView, nameTextView;
    private Button editProfileBtn, changePasswordBtn, logoutBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String userName = "";
    private String userEmail = "";
    private String userAge = "";
    private String userAddress = "";
    private String userImageUrl = null;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Initialize views
        initViews(view);
        
        // Setup listeners
        setupListeners();
        
        // Load user profile data
        loadUserProfile();
    }
    
    private void initViews(View view) {
        profileImageView = view.findViewById(R.id.profileImageView);
        profileImageContainer = view.findViewById(R.id.profileImageContainer);
        greetingTextView = view.findViewById(R.id.greetingTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        ageTextView = view.findViewById(R.id.ageTextView);
        nameTextView = view.findViewById(R.id.nameTextView);
        editProfileBtn = view.findViewById(R.id.editProfileBtn);
        changePasswordBtn = view.findViewById(R.id.changePasswordBtn);
        logoutBtn = view.findViewById(R.id.logoutBtn);
    }
    
    private void setupListeners() {
        // Set click listener for profile image
        profileImageContainer.setOnClickListener(v -> {
            // Navigate to ImageViewActivity
            Intent intent = new Intent(getActivity(), ImageViewActivity.class);
            intent.putExtra("image_url", userImageUrl);
            startActivity(intent);
        });

        // Set click listener for edit profile button
        editProfileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });
        
        // Set click listener for change password button
        changePasswordBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        // Set click listener for logout button
        logoutBtn.setOnClickListener(v -> {
            // Sign out from Firebase
            if (mAuth != null) {
                mAuth.signOut();
            }
            
            // Navigate to login
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Get user data from Firebase
            db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Extract user data
                        userName = documentSnapshot.getString("full_name");
                        userEmail = documentSnapshot.getString("email");
                        userAge = documentSnapshot.getString("age");
                        userAddress = documentSnapshot.getString("address");
                        userImageUrl = documentSnapshot.getString("profile_image_url");
                        
                        Log.d(TAG, "User data loaded successfully");
                        Log.d(TAG, "Name: " + userName);
                        Log.d(TAG, "Email: " + userEmail);
                        Log.d(TAG, "Age: " + userAge);
                        Log.d(TAG, "Address: " + userAddress);
                        Log.d(TAG, "Image URL: " + userImageUrl);
                        
                        // Update UI with retrieved data
                        updateUI();
                    } else {
                        Log.w(TAG, "User document does not exist");
                        Toast.makeText(getContext(), "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    Toast.makeText(getContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } else {
            Log.w(TAG, "No user is currently logged in");
            // Not logged in, redirect to login
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        }
    }
    
    private void updateUI() {
        // Update UI with user data
        greetingTextView.setText("Hello, " + userName + "!");
        emailTextView.setText(userEmail);
        ageTextView.setText("Age: " + userAge);
        nameTextView.setText(userAddress);
        
        // Load profile image with enhanced error handling
        if (userImageUrl != null && !userImageUrl.isEmpty()) {
            Log.d(TAG, "Loading profile image from URL: " + userImageUrl);
            
            try {
                RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL);
                
                Glide.with(requireContext())
                    .load(userImageUrl)
                    .apply(requestOptions)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, 
                                                   Object model, 
                                                   com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                   boolean isFirstResource) {
                            Log.e(TAG, "Image load failed", e);
                            Toast.makeText(getContext(), "Failed to load profile image", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, 
                                                     Object model, 
                                                     com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                     com.bumptech.glide.load.DataSource dataSource, 
                                                     boolean isFirstResource) {
                            Log.d(TAG, "Image loaded successfully");
                            return false;
                        }
                    })
                    .into(profileImageView);
            } catch (Exception e) {
                Log.e(TAG, "Exception during image loading", e);
                profileImageView.setImageResource(R.drawable.ic_profile);
            }
        } else {
            Log.w(TAG, "No profile image URL available");
            // Use default image
            profileImageView.setImageResource(R.drawable.ic_profile);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload user profile when returning to the fragment (e.g., after editing profile)
        loadUserProfile();
    }
}
