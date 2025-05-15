package com.example.carrentaluser.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carrentaluser.R;
import com.example.carrentaluser.models.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private List<Booking> bookingList;

    public BookingAdapter(List<Booking> bookingList) {
        this.bookingList = bookingList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView carImage;
        TextView carName, bookingDates, location, status, totalPrice;
        Button cancelBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            carImage = itemView.findViewById(R.id.booking_image);
            carName = itemView.findViewById(R.id.booking_car_name);
            bookingDates = itemView.findViewById(R.id.booking_dates);
            location = itemView.findViewById(R.id.booking_location);
            status = itemView.findViewById(R.id.booking_status);
            totalPrice = itemView.findViewById(R.id.booking_total_price);
            cancelBtn = itemView.findViewById(R.id.btn_cancel_booking);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.booking_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        holder.carName.setText(booking.getCar_name());
        holder.bookingDates.setText("From: " + booking.getStart_date() + " To: " + booking.getEnd_date());
        holder.location.setText("Pickup: " + booking.getPickup_location());
        
        // Set status with color and background based on status value
        String status = booking.getStatus();
        holder.status.setText("Status: " + status);
        
        int textColor;
        int backgroundColor;
        
        if (status.equalsIgnoreCase("Pending")) {
            textColor = holder.itemView.getContext().getResources().getColor(android.R.color.black);
            backgroundColor = holder.itemView.getContext().getResources().getColor(android.R.color.holo_orange_light);
        } else if (status.equalsIgnoreCase("Confirmed")) {
            textColor = holder.itemView.getContext().getResources().getColor(android.R.color.white);
            backgroundColor = holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark);
        } else if (status.equalsIgnoreCase("Cancelled")) {
            textColor = holder.itemView.getContext().getResources().getColor(android.R.color.white);
            backgroundColor = holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark);
        } else {
            textColor = holder.itemView.getContext().getResources().getColor(android.R.color.white);
            backgroundColor = holder.itemView.getContext().getResources().getColor(android.R.color.holo_blue_dark);
        }
        
        holder.status.setTextColor(textColor);
        holder.status.setBackgroundColor(backgroundColor);
        holder.status.setPadding(24, 8, 24, 8);
        
        holder.totalPrice.setText("Total: ₹" + booking.getTotal_price());

        // Load image with placeholder
        Glide.with(holder.carImage.getContext())
             .load(booking.getCar_image())
             .placeholder(R.drawable.ic_profile_placeholder)
             .error(R.drawable.ic_profile_placeholder)
             .centerCrop()
             .into(holder.carImage);

        // Only show cancel button for pending bookings
        if (booking.getStatus().equalsIgnoreCase("Pending")) {
            holder.cancelBtn.setVisibility(View.VISIBLE);
            holder.cancelBtn.setOnClickListener(view -> cancelBooking(holder, booking));
        } else {
            holder.cancelBtn.setVisibility(View.GONE);
        }
    }
    
    private void cancelBooking(ViewHolder holder, Booking booking) {
        FirebaseFirestore.getInstance()
            .collection("bookings")
            .whereEqualTo("user_id", FirebaseAuth.getInstance().getCurrentUser().getUid())
            .whereEqualTo("start_date", booking.getStart_date())
            .whereEqualTo("car_name", booking.getCar_name())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                    snapshot.getReference().update("status", "Cancelled")
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(holder.itemView.getContext(), "Booking cancelled", Toast.LENGTH_SHORT).show();
                                booking.setStatus("Cancelled");
                                notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(holder.itemView.getContext(), "Cancel failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(holder.itemView.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }
}
