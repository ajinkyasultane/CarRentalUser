package com.example.carrentaluser.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carrentaluser.BookingActivity;
import com.example.carrentaluser.R;
import com.example.carrentaluser.models.Car;
import com.example.carrentaluser.models.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> implements Filterable {

    private Context context;
    private List<Car> carList;
    private List<Car> carListFull; // full copy for filtering
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public CarAdapter(Context context, List<Car> carList) {
        this.context = context;
        this.carList = carList;
        this.carListFull = new ArrayList<>(carList);
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_car, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = carList.get(position);

        holder.name.setText("Car Name :- "+car.getName());
        holder.brand.setText("Brand Name :- "+car.getBrand());
        holder.price.setText("Price  ₹" + car.getPrice());
        holder.available.setText("Available: " + car.getAvailablequant());

        Glide.with(context)
                .load(car.getImageUrl())
                .into(holder.image);

        holder.bookButton.setOnClickListener(v -> {
            // Check if user is authenticated
            if (auth.getCurrentUser() == null) {
                Toast.makeText(context, "Please login to book a car", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check if user account is active
            String userId = auth.getCurrentUser().getUid();
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        
                        if (user != null && !user.isActive()) {
                            // User account is blocked
                            showBlockedUserDialog();
                        } else {
                            // User account is active, proceed with booking
                            Intent intent = new Intent(context, BookingActivity.class);
                            intent.putExtra("car_name", car.getName());
                            intent.putExtra("car_price", car.getPrice());
                            intent.putExtra("car_image", car.getImageUrl());
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        }
                    } else {
                        // Assume new users are active by default
                        Intent intent = new Intent(context, BookingActivity.class);
                        intent.putExtra("car_name", car.getName());
                        intent.putExtra("car_price", car.getPrice());
                        intent.putExtra("car_image", car.getImageUrl());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to check user status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });
    }
    
    private void showBlockedUserDialog() {
        new AlertDialog.Builder(context)
            .setTitle("Account Blocked")
            .setMessage("Your account has been blocked by the admin. Please contact support for assistance.")
            .setPositiveButton("OK", null)
            .setCancelable(false)
            .show();
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    @Override
    public Filter getFilter() {
        return carFilter;
    }

    private final Filter carFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Car> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(carListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (Car car : carListFull) {
                    if (car.getName().toLowerCase().contains(filterPattern)
                            || car.getBrand().toLowerCase().contains(filterPattern)) {
                        filteredList.add(car);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            carList.clear();
            carList.addAll((List<Car>) results.values);
            notifyDataSetChanged();
        }
    };

    public static class CarViewHolder extends RecyclerView.ViewHolder {
        TextView name, brand, price, available;
        ImageView image;
        Button bookButton;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.car_name);
            brand = itemView.findViewById(R.id.car_brand);
            price = itemView.findViewById(R.id.car_price);
            available = itemView.findViewById(R.id.car_available);
            image = itemView.findViewById(R.id.car_image);
            bookButton = itemView.findViewById(R.id.btn_book);
        }
    }
}
