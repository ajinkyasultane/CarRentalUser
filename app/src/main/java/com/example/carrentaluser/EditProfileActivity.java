package com.example.carrentaluser;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;

    private ImageView profileImageView;
    private EditText fullNameEditText, emailEditText, ageEditText, addressEditText, mobileEditText;
    private TextView titleTextView, uploadPhotoText;
    private Button saveBtn;
    private Uri imageUri;
    private boolean isImageSelected = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private String userId;
    private boolean isFromBooking = false;
    private String existingImageUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Set up action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Profile");
        }

        // Check if coming from booking
        isFromBooking = getIntent().getBooleanExtra("from_booking", false);

        // Initialize views
        profileImageView = findViewById(R.id.profileImageView);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        ageEditText = findViewById(R.id.ageEditText);
        addressEditText = findViewById(R.id.addressEditText);
        mobileEditText = findViewById(R.id.mobileEditText);
        saveBtn = findViewById(R.id.saveBtn);
        titleTextView = findViewById(R.id.titleTextView);
        uploadPhotoText = findViewById(R.id.uploadPhotoText);

        // Update title if coming from booking
        if (isFromBooking) {
            titleTextView.setText("Complete Your Profile");
            saveBtn.setText("Save and Continue to Booking");
        }

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            emailEditText.setText(user.getEmail()); // Pre-fill user email
            
            // Load existing profile data if available
            loadExistingProfileData();
        }

        profileImageView.setOnClickListener(v -> openImagePicker());
        uploadPhotoText.setOnClickListener(v -> openImagePicker());

        saveBtn.setOnClickListener(v -> {
            if (validateInputs()) {
                if (imageUri != null || isImageSelected) {
                    uploadImageAndSaveData();
                } else if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                    // User already has a profile image and didn't change it
                    saveUserData(existingImageUrl);
                } else {
                    // No image selected and no existing image
                    Toast.makeText(this, "Please select a profile image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void loadExistingProfileData() {
        db.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String fullName = documentSnapshot.getString("full_name");
                    String age = documentSnapshot.getString("age");
                    String address = documentSnapshot.getString("address");
                    String profileImageUrl = documentSnapshot.getString("profile_image_url");
                    String mobile = documentSnapshot.getString("mobile_number");
                    
                    if (fullName != null) fullNameEditText.setText(fullName);
                    if (age != null) ageEditText.setText(age);
                    if (address != null) addressEditText.setText(address);
                    if (mobile != null) mobileEditText.setText(mobile);
                    
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        existingImageUrl = profileImageUrl;
                        isImageSelected = true;
                        
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .into(profileImageView);
                        
                        // Update text to show image is already selected
                        uploadPhotoText.setText("Tap to change profile picture");
                    }
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
            );
    }
    
    private boolean validateInputs() {
        String fullName = fullNameEditText.getText().toString().trim();
        String age = ageEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String mobile = mobileEditText.getText().toString().trim();
        
        boolean isValid = true;
        
        if (fullName.isEmpty()) {
            fullNameEditText.setError("Required");
            isValid = false;
        }
        
        if (age.isEmpty()) {
            ageEditText.setError("Required");
            isValid = false;
        }
        
        if (address.isEmpty()) {
            addressEditText.setError("Required");
            isValid = false;
        }

        if (mobile.isEmpty()) {
            mobileEditText.setError("Required");
            isValid = false;
        } else if (mobile.length() < 10 || mobile.length() > 15) {
            mobileEditText.setError("Please enter a valid mobile number (10-15 digits)");
            isValid = false;
        } else if (!mobile.matches("[0-9+\\-\\s]+")) {
            mobileEditText.setError("Mobile number should contain only digits, +, - or spaces");
            isValid = false;
        }
        
        // Check image selection
        if (!isImageSelected && (existingImageUrl == null || existingImageUrl.isEmpty())) {
            Toast.makeText(this, "Please select a profile image", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        
        return isValid;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
            isImageSelected = true;
            uploadPhotoText.setText("Profile picture selected - Tap to change");
        }
    }

    private void uploadImageAndSaveData() {
        // If user kept existing image
        if (imageUri == null && existingImageUrl != null && !existingImageUrl.isEmpty()) {
            saveUserData(existingImageUrl);
            return;
        }
        
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        StorageReference profileImageRef = storageRef.child("profile_images/" + userId + ".jpg");

        profileImageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            progressDialog.dismiss();
                            saveUserData(uri.toString());
                        }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditProfileActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    Log.e("UploadError", "Upload failed: ", e);
                });
    }

    private void saveUserData(String imageUrl) {
        String fullName = fullNameEditText.getText().toString().trim();
        String age = ageEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String mobile = mobileEditText.getText().toString().trim();

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("full_name", fullName);
        userMap.put("email", email);
        userMap.put("age", age);
        userMap.put("address", address);
        userMap.put("mobile_number", mobile);
        if (imageUrl != null) userMap.put("profile_image_url", imageUrl);

        db.collection("users").document(userId).set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    
                    if (isFromBooking) {
                        // Return to BookingActivity
                        finish();
                    } else {
                        // Go back to the activity that hosts ProfileFragment
                        Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
                        intent.putExtra("navigateTo", "profile");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish(); // close EditProfileActivity
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    Log.e("FirestoreError", "Error updating: ", e);
                });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_profile_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_exit) {
            showExitConfirmationDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Exit Application")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes", (dialog, which) -> {
                finishAffinity(); // Close all activities and exit app
            })
            .setNegativeButton("No", null)
            .show();
    }
    
    @Override
    public void onBackPressed() {
        // Check if there are unsaved changes
        if (!fullNameEditText.getText().toString().isEmpty() || 
            !ageEditText.getText().toString().isEmpty() || 
            !addressEditText.getText().toString().isEmpty() ||
            !mobileEditText.getText().toString().isEmpty() ||
            imageUri != null) {
            
            new AlertDialog.Builder(this)
                .setTitle("Discard Changes")
                .setMessage("You have unsaved changes. Are you sure you want to discard them?")
                .setPositiveButton("Discard", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            super.onBackPressed();
        }
    }
}
