package com.example.carrentaluser.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carrentaluser.R;
import com.example.carrentaluser.models.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.HashMap;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private List<Booking> bookingList;

    public BookingAdapter(List<Booking> bookingList) {
        this.bookingList = bookingList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView carImage;
        TextView carName, bookingDates, location, status, totalPrice;
        Button cancelBtn;
        
        // Payment related views
        LinearLayout advancePaymentSection;
        TextView paymentStatus, advanceAmount, remainingAmount, paymentMethod;
        
        // Refund related views
        LinearLayout refundSection;
        TextView refundStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            carImage = itemView.findViewById(R.id.booking_image);
            carName = itemView.findViewById(R.id.booking_car_name);
            bookingDates = itemView.findViewById(R.id.booking_dates);
            location = itemView.findViewById(R.id.booking_location);
            status = itemView.findViewById(R.id.booking_status);
            totalPrice = itemView.findViewById(R.id.booking_total_price);
            cancelBtn = itemView.findViewById(R.id.btn_cancel_booking);
            
            // Initialize payment related views
            advancePaymentSection = itemView.findViewById(R.id.advance_payment_section);
            paymentStatus = itemView.findViewById(R.id.booking_payment_status);
            advanceAmount = itemView.findViewById(R.id.booking_advance_amount);
            remainingAmount = itemView.findViewById(R.id.booking_remaining_amount);
            paymentMethod = itemView.findViewById(R.id.booking_payment_method);
            
            // Initialize refund related views
            refundSection = itemView.findViewById(R.id.refund_section);
            refundStatus = itemView.findViewById(R.id.booking_refund_status);
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

        holder.carName.setText("Car Name: " +booking.getCar_name());
        holder.bookingDates.setText("From: " + booking.getStart_date() + " To: " + booking.getEnd_date());
        
        // Check if pickup location exists
        String pickup = booking.getPickup_location();
        if (pickup != null && !pickup.isEmpty()) {
            holder.location.setText("Pickup: " + pickup);
        } else {
            holder.location.setText("Branch: " + booking.getBranch_name());
        }
        
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
             
        // Handle payment section visibility and data
        boolean isWithDriver = booking.isWith_driver();
        boolean advancePaymentDone = booking.isAdvance_payment_done();
        
        if (isWithDriver && advancePaymentDone) {
            // Show payment section for with-driver bookings with completed advance payment
            holder.advancePaymentSection.setVisibility(View.VISIBLE);
            
            // Set payment status
            holder.paymentStatus.setText("Advance Payment: Completed");
            holder.paymentStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.success));
            
            // Set amounts
            int advanceAmount = booking.getAdvance_payment_amount();
            int remainingAmount = booking.getRemaining_payment();
            
            holder.advanceAmount.setText("Paid: ₹" + advanceAmount);
            holder.remainingAmount.setText("Remaining: ₹" + remainingAmount);
            
            // Set payment method if available
            String paymentMethod = booking.getPayment_method();
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                holder.paymentMethod.setText("Payment Method: " + paymentMethod);
                holder.paymentMethod.setVisibility(View.VISIBLE);
            } else {
                holder.paymentMethod.setVisibility(View.GONE);
            }
            
            // Handle refund section visibility for cancelled bookings
            if (status.equalsIgnoreCase("Cancelled") && booking.isRefund_processed()) {
                holder.refundSection.setVisibility(View.VISIBLE);
                holder.refundStatus.setText("Refund: ₹" + booking.getRefund_amount() + " Processed");
            } else {
                holder.refundSection.setVisibility(View.GONE);
            }
        } else {
            // Hide payment and refund sections
            holder.advancePaymentSection.setVisibility(View.GONE);
            holder.refundSection.setVisibility(View.GONE);
        }

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
                        // Check if advance payment was made
                        boolean advancePaymentDone = booking.isAdvance_payment_done();
                        String paymentId = booking.getPayment_id();
                        int refundAmount = booking.getAdvance_payment_amount();
                        
                        // Update booking status to Cancelled
                        snapshot.getReference().update("status", "Cancelled")
                                .addOnSuccessListener(unused -> {
                                    // Process refund if advance payment was made
                                    if (advancePaymentDone && paymentId != null && !paymentId.isEmpty()) {
                                        processRefund(holder, booking, paymentId, refundAmount);
                                    } else {
                                        // No refund needed, just show cancellation message
                                        Toast.makeText(holder.itemView.getContext(), 
                                            "Booking cancelled", Toast.LENGTH_SHORT).show();
                                    }
                                    
                                    booking.setStatus("Cancelled");
                                    notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(holder.itemView.getContext(), 
                                        "Cancel failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(holder.itemView.getContext(), 
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Process a refund for a cancelled booking
     * @param holder The ViewHolder
     * @param booking The booking to refund
     * @param paymentId The payment ID to refund
     * @param amount The amount to refund
     */
    private void processRefund(ViewHolder holder, Booking booking, String paymentId, int amount) {
        // Create a refund record in Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String refundId = "refund_" + System.currentTimeMillis();
        
        // Create refund data
        HashMap<String, Object> refund = new HashMap<>();
        refund.put("original_payment_id", paymentId);
        refund.put("refund_id", refundId);
        refund.put("user_id", userId);
        refund.put("amount", amount);
        refund.put("booking_id", booking.getBooking_id());
        refund.put("car_name", booking.getCar_name());
        refund.put("refund_date", new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", 
            java.util.Locale.getDefault()).format(new java.util.Date()));
        refund.put("refund_status", "Processed");
        refund.put("payment_method", booking.getPayment_method());
        
        // Store refund record in Firestore
        db.collection("refunds")
            .document(refundId)
            .set(refund)
            .addOnSuccessListener(aVoid -> {
                // Update the booking document to include refund information
                FirebaseFirestore.getInstance()
                    .collection("bookings")
                    .document(booking.getBooking_id())
                    .update(
                        "refund_processed", true,
                        "refund_id", refundId,
                        "refund_amount", amount,
                        "refund_date", refund.get("refund_date")
                    )
                    .addOnSuccessListener(unused -> {
                        // Show success message
                        Toast.makeText(holder.itemView.getContext(), 
                            "Booking cancelled. Refund of ₹" + amount + " has been processed.", 
                            Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(holder.itemView.getContext(), 
                            "Refund processed but failed to update booking: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(holder.itemView.getContext(), 
                    "Booking cancelled but refund failed: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
            });
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }
}
