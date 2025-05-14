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
    public BookingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.booking_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingAdapter.ViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        holder.carName.setText(booking.getCar_name());
        holder.bookingDates.setText("From: " + booking.getStart_date() + " To: " + booking.getEnd_date());
        holder.location.setText("Pickup: " + booking.getPickup_location());
        holder.status.setText("Status: " + booking.getStatus());
        holder.totalPrice.setText("Total: ₹" + booking.getTotal_price());

        Glide.with(holder.carImage.getContext()).load(booking.getCar_image()).into(holder.carImage);

        if (booking.getStatus().equalsIgnoreCase("Pending")) {
            holder.cancelBtn.setVisibility(View.VISIBLE);
            holder.cancelBtn.setOnClickListener(view -> {
                FirebaseFirestore.getInstance()
                        .collection("bookings")
                        .whereEqualTo("user_id", FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .whereEqualTo("start_date", booking.getStart_date())
                        .whereEqualTo("car_name", booking.getCar_name())
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                                snapshot.getReference().delete()
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(holder.itemView.getContext(), "Booking cancelled", Toast.LENGTH_SHORT).show();
                                            bookingList.remove(holder.getAdapterPosition());
                                            notifyItemRemoved(holder.getAdapterPosition());
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(holder.itemView.getContext(), "Cancel failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(holder.itemView.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        } else {
            holder.cancelBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }
}
