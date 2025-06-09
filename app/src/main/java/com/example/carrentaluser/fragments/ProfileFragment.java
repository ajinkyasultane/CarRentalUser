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
import com.example.carrentaluser.WalletActivity;
import com.example.carrentaluser.BankAccountManagementActivity;
import com.example.carrentaluser.utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    
    private ImageView profileImageView;
    private CardView profileImageContainer;
    private TextView greetingTextView, emailTextView, ageTextView, nameTextView, mobileTextView;
    private Button editProfileBtn, changePasswordBtn, logoutBtn, myWalletBtn, manageBankAccountsBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    private String userName = "";
    private String userEmail = "";
    private String userAge = "";
    private String userAddress = "";
    private String userMobile = "";
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
        sessionManager = SessionManager.getInstance(requireContext());
        
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
        mobileTextView = view.findViewById(R.id.mobileTextView);
        editProfileBtn = view.findViewById(R.id.editProfileBtn);
        changePasswordBtn = view.findViewById(R.id.changePasswordBtn);
        logoutBtn = view.findViewById(R.id.logoutBtn);
        myWalletBtn = view.findViewById(R.id.myWalletBtn);
        manageBankAccountsBtn = view.findViewById(R.id.manageBankAccountsBtn);
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
            // Show confirmation dialog
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Clear session in SessionManager
                    sessionManager.logout();
                    
                    // Sign out from Firebase
                    if (mAuth != null) {
                        mAuth.signOut();
                    }
                    
                    // Navigate to login
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("No", null)
                .show();
        });
        
        // Set click listener for my wallet button
        myWalletBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), WalletActivity.class);
            startActivity(intent);
        });

        // Set click listener for manage bank accounts button
        manageBankAccountsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), BankAccountManagementActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        // Check if fragment is still attached
        if (!isAdded()) {
            Log.d(TAG, "Fragment not attached, skipping profile load");
            return;
        }
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Get user data from Firebase
            db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Check if fragment is still attached before updating UI
                    if (!isAdded()) {
                        Log.d(TAG, "Fragment not attached, skipping profile update");
                        return;
                    }
                    
                    if (documentSnapshot.exists()) {
                        // Extract user data
                        userName = documentSnapshot.getString("full_name");
                        userEmail = documentSnapshot.getString("email");
                        userAge = documentSnapshot.getString("age");
                        userAddress = documentSnapshot.getString("address");
                        userMobile = documentSnapshot.getString("mobile_number");
                        userImageUrl = documentSnapshot.getString("profile_image_url");
                        
                        Log.d(TAG, "User data loaded successfully");
                        Log.d(TAG, "Name: " + userName);
                        Log.d(TAG, "Email: " + userEmail);
                        Log.d(TAG, "Age: " + userAge);
                        Log.d(TAG, "Address: " + userAddress);
                        Log.d(TAG, "Mobile: " + userMobile);
                        Log.d(TAG, "Image URL: " + userImageUrl);
                        
                        // Update UI with retrieved data
                        updateUI();
                    } else {
                        Log.w(TAG, "User document does not exist");
                        if (isAdded()) {
                            Toast.makeText(getContext(), "User profile not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Check if fragment is still attached before showing Toast
                    if (!isAdded()) {
                        Log.d(TAG, "Fragment not attached, skipping error handling");
                        return;
                    }
                    
                    Log.e(TAG, "Error loading user data", e);
                    Toast.makeText(getContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } else {
            Log.w(TAG, "No user is currently logged in");
            // Not logged in, redirect to login
            if (isAdded() && getActivity() != null) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            }
        }
    }
    
    private void updateUI() {
        // Check if fragment is still attached
        if (!isAdded()) {
            Log.d(TAG, "Fragment not attached, skipping UI update");
            return;
        }
        
        try {
            // Update UI with user data
            greetingTextView.setText("Hello, " + userName + "!");
            emailTextView.setText(userEmail);
            ageTextView.setText("Age: " + userAge);
            nameTextView.setText(userAddress);
            
            // Set mobile number if available
            if (userMobile != null && !userMobile.isEmpty()) {
                mobileTextView.setText("Mobile: " + userMobile);
                mobileTextView.setVisibility(View.VISIBLE);
            } else {
                mobileTextView.setVisibility(View.GONE);
            }
            
            // Load profile image with enhanced error handling
            if (userImageUrl != null && !userImageUrl.isEmpty()) {
                Log.d(TAG, "Loading profile image from URL: " + userImageUrl);
                
                try {
                    RequestOptions requestOptions = new RequestOptions()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .diskCacheStrategy(DiskCacheStrategy.ALL);
                    
                    // Check again if still attached before loading image
                    if (isAdded()) {
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
                                    if (isAdded()) {
                                        Toast.makeText(getContext(), "Failed to load profile image", Toast.LENGTH_SHORT).show();
                                    }
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
                    } else {
                        Log.d(TAG, "Fragment detached during image loading");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception during image loading", e);
                    profileImageView.setImageResource(R.drawable.ic_profile);
                }
            } else {
                Log.w(TAG, "No profile image URL available");
                // Use default image
                profileImageView.setImageResource(R.drawable.ic_profile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload user profile when returning to the fragment (e.g., after editing profile)
        loadUserProfile();
    }
}
