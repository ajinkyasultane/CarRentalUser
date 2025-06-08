package com.example.carrentaluser;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;

public class ImageViewActivity extends AppCompatActivity {
    
    private static final String TAG = "ImageViewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Profile Photo");

        // Get the image URL from intent
        String imageUrl = getIntent().getStringExtra("image_url");
        Log.d(TAG, "Image URL received: " + imageUrl);
        
        // Load the image
        PhotoView fullImageView = findViewById(R.id.full_image_view);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Log.d(TAG, "Attempting to load image from URL: " + imageUrl);
                
                RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL);
                
                Glide.with(this)
                    .load(imageUrl)
                    .apply(requestOptions)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, 
                                                  Object model, 
                                                  com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                  boolean isFirstResource) {
                            Log.e(TAG, "Image load failed", e);
                            Toast.makeText(ImageViewActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, 
                                                     Object model, 
                                                     com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                     com.bumptech.glide.load.DataSource dataSource, 
                                                     boolean isFirstResource) {
                            Log.d(TAG, "Full image loaded successfully");
                            return false;
                        }
                    })
                    .into(fullImageView);
            } catch (Exception e) {
                Log.e(TAG, "Exception during image loading", e);
                fullImageView.setImageResource(R.drawable.ic_profile);
                Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // Load default image if url is null
            Log.w(TAG, "No image URL provided");
            fullImageView.setImageResource(R.drawable.ic_profile);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle the back button click
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 