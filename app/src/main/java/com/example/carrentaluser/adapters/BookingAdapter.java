package com.example.carrentaluser.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carrentaluser.R;
import com.example.carrentaluser.models.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;
import java.util.HashMap;
import java.util.Date;
import java.util.Map;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private List<Booking> bookingList;

    public BookingAdapter(List<Booking> bookingList) {
        this.bookingList = bookingList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView carImage;
        TextView carName, dates, branch, status, totalPrice;
        TextView refundStatus;
        LinearLayout advancePaymentSection, refundSection;
        Button cancelBtn;
        ImageView refundIcon;
        
        // Payment related views
        TextView paymentStatus, advanceAmount, remainingAmount, paymentMethod;

        public ViewHolder(View itemView) {
            super(itemView);
            carImage = itemView.findViewById(R.id.booking_image);
            carName = itemView.findViewById(R.id.booking_car_name);
            dates = itemView.findViewById(R.id.booking_dates);
            branch = itemView.findViewById(R.id.booking_location);
            status = itemView.findViewById(R.id.booking_status);
            totalPrice = itemView.findViewById(R.id.booking_total_price);
            
            // Initialize payment related views
            advancePaymentSection = itemView.findViewById(R.id.advance_payment_section);
            paymentStatus = itemView.findViewById(R.id.booking_payment_status);
            advanceAmount = itemView.findViewById(R.id.booking_advance_amount);
            remainingAmount = itemView.findViewById(R.id.booking_remaining_amount);
            paymentMethod = itemView.findViewById(R.id.booking_payment_method);
            
            refundStatus = itemView.findViewById(R.id.booking_refund_status);
            refundSection = itemView.findViewById(R.id.refund_section);
            cancelBtn = itemView.findViewById(R.id.btn_cancel_booking);
            refundIcon = itemView.findViewById(R.id.refund_icon);
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
        holder.dates.setText("From: " + booking.getStart_date() + " To: " + booking.getEnd_date());
        
        // Check if pickup location exists
        String pickup = booking.getPickup_location();
        if (pickup != null && !pickup.isEmpty()) {
            holder.branch.setText("Pickup: " + pickup);
        } else {
            holder.branch.setText("Branch: " + booking.getBranch_name());
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
            
            // Handle refund section visibility for cancelled or rejected bookings
            if ((status.equalsIgnoreCase("Cancelled") || status.equalsIgnoreCase("Rejected")) 
                    && booking.isRefund_processed()) {
                holder.refundSection.setVisibility(View.VISIBLE);
                if (booking.isCredited_to_wallet()) {
                    holder.refundStatus.setText("+ ₹" + booking.getRefund_amount() + " credited to wallet");
                    holder.refundIcon.setImageResource(android.R.drawable.ic_input_add);
                    holder.refundStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    holder.refundStatus.setText("Refund: ₹" + booking.getRefund_amount() + " processed");
                    holder.refundIcon.setImageResource(android.R.drawable.ic_menu_revert);
                    holder.refundStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
                }
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
        // Get the Firestore instance and user ID
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String bookingId = booking.getBooking_id();
        
        // Show processing indicator
        Toast.makeText(holder.itemView.getContext(), 
            "Processing cancellation...", Toast.LENGTH_SHORT).show();
        
        // First, try to get the booking directly by ID if available
        if (bookingId != null && !bookingId.isEmpty()) {
            db.collection("bookings").document(bookingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Found the booking, check its current status
                        String currentStatus = documentSnapshot.getString("status");
                        Boolean refundProcessed = documentSnapshot.getBoolean("refund_processed");
                        
                        if ("Cancelled".equalsIgnoreCase(currentStatus)) {
                            // Already cancelled
                            Toast.makeText(holder.itemView.getContext(), 
                                "Booking already cancelled", Toast.LENGTH_SHORT).show();
                                
                            // Update the UI to reflect cancelled status
                            booking.setStatus("Cancelled");
                            notifyDataSetChanged();
                            return;
                        }
                        
                        // Check if advance payment was made
                        Boolean advancePaymentDone = documentSnapshot.getBoolean("advance_payment_done");
                        String paymentId = documentSnapshot.getString("payment_id");
                        Number advanceAmount = documentSnapshot.getLong("advance_payment_amount");
                        String paymentMethod = documentSnapshot.getString("payment_method");
                        
                        // Update booking status to Cancelled
                        documentSnapshot.getReference().update("status", "Cancelled")
                            .addOnSuccessListener(unused -> {
                                // Update local booking status
                                booking.setStatus("Cancelled");
                                
                                // Process refund if advance payment was made
                                if (advancePaymentDone != null && advancePaymentDone && 
                                    advanceAmount != null && advanceAmount.intValue() > 0 &&
                                    (refundProcessed == null || !refundProcessed)) {
                                    
                                    // Store payment method in the booking object for reference in the refund process
                                    if (paymentMethod != null) {
                                        booking.setPayment_method(paymentMethod);
                                    }
                                    
                                    // Process the refund to wallet
                                    processRefund(holder, booking, paymentId, advanceAmount.intValue());
                                } else if (refundProcessed != null && refundProcessed) {
                                    // Refund was already processed
                                    Toast.makeText(holder.itemView.getContext(), 
                                        "Booking cancelled. Refund was already processed.", 
                                        Toast.LENGTH_SHORT).show();
                                    notifyDataSetChanged();
                                } else {
                                    // No refund needed or no payment data
                                    Toast.makeText(holder.itemView.getContext(), 
                                        "Booking cancelled. No advance payment was made.", 
                                        Toast.LENGTH_SHORT).show();
                                    notifyDataSetChanged();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(holder.itemView.getContext(), 
                                    "Cancel failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                    } else {
                        // Couldn't find booking by ID, try fallback method
                        findAndCancelBookingByDetails(holder, booking);
                    }
                })
                .addOnFailureListener(e -> {
                    // Error, try fallback method
                    Toast.makeText(holder.itemView.getContext(), 
                        "Error accessing booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    findAndCancelBookingByDetails(holder, booking);
                });
        } else {
            // No booking ID, use fallback method
            findAndCancelBookingByDetails(holder, booking);
        }
    }
    
    /**
     * Fallback method to find and cancel a booking by user ID, start date, and car name
     */
    private void findAndCancelBookingByDetails(ViewHolder holder, Booking booking) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Query for matching bookings
        db.collection("bookings")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("start_date", booking.getStart_date())
            .whereEqualTo("car_name", booking.getCar_name())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    Toast.makeText(holder.itemView.getContext(), 
                        "Could not find booking details", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Process the first matching booking
                DocumentSnapshot snapshot = queryDocumentSnapshots.getDocuments().get(0);
                
                // Check if booking is already cancelled and refund processed
                if (snapshot.getString("status") != null && 
                    snapshot.getString("status").equalsIgnoreCase("Cancelled")) {
                    
                    Toast.makeText(holder.itemView.getContext(), 
                        "Booking already cancelled", Toast.LENGTH_SHORT).show();
                    
                    // Update the UI to reflect cancelled status
                    booking.setStatus("Cancelled");
                    notifyDataSetChanged();
                    return;
                }
                
                // Check if advance payment was made
                Boolean advancePaymentDone = snapshot.getBoolean("advance_payment_done");
                String paymentId = snapshot.getString("payment_id");
                Number advanceAmount = snapshot.getLong("advance_payment_amount");
                Boolean refundProcessed = snapshot.getBoolean("refund_processed");
                String paymentMethod = snapshot.getString("payment_method");
                
                // Save the booking ID for future reference
                String docId = snapshot.getId();
                booking.setBooking_id(docId);
                
                // Update booking status to Cancelled
                snapshot.getReference().update("status", "Cancelled")
                    .addOnSuccessListener(unused -> {
                        // Update local booking status
                        booking.setStatus("Cancelled");
                        
                        // Process refund if advance payment was made
                        if (advancePaymentDone != null && advancePaymentDone && 
                            advanceAmount != null && advanceAmount.intValue() > 0 &&
                            (refundProcessed == null || !refundProcessed)) {
                            
                            // Store payment method in the booking object for reference in the refund process
                            if (paymentMethod != null) {
                                booking.setPayment_method(paymentMethod);
                            }
                            
                            // Process the refund to wallet
                            processRefund(holder, booking, paymentId, advanceAmount.intValue());
                        } else if (refundProcessed != null && refundProcessed) {
                            // Refund was already processed
                            Toast.makeText(holder.itemView.getContext(), 
                                "Booking cancelled. Refund was already processed.", 
                                Toast.LENGTH_SHORT).show();
                            notifyDataSetChanged();
                        } else {
                            // No refund needed or no payment data
                            Toast.makeText(holder.itemView.getContext(), 
                                "Booking cancelled. No advance payment was made.", 
                                Toast.LENGTH_SHORT).show();
                            notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(holder.itemView.getContext(), 
                            "Cancel failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
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
        String bookingId = booking.getBooking_id();
        
        // Show processing message
        Toast.makeText(holder.itemView.getContext(), 
            "Processing refund to wallet...", Toast.LENGTH_SHORT).show();
        
        // First check if a refund has already been processed for this booking
        db.collection("bookings").document(bookingId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    Toast.makeText(holder.itemView.getContext(),
                        "Error: Booking not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Boolean refundProcessed = documentSnapshot.getBoolean("refund_processed");
                String paymentMethod = documentSnapshot.getString("payment_method");
                
                // Store payment method in the booking object if it's not already set
                if (paymentMethod != null && (booking.getPayment_method() == null || booking.getPayment_method().isEmpty())) {
                    booking.setPayment_method(paymentMethod);
                }
                
                if (refundProcessed != null && refundProcessed) {
                    // Refund already processed, show message
                    Toast.makeText(holder.itemView.getContext(),
                        "Refund already processed for this booking", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Generate unique refund ID
                String refundId = "refund_" + System.currentTimeMillis();
                
                // Get current wallet balance
                db.collection("users").document(userId).get()
                    .addOnSuccessListener(userDoc -> {
                        if (!userDoc.exists()) {
                            Toast.makeText(holder.itemView.getContext(),
                                "User profile not found", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Get current balance
                        double currentBalance = 0;
                        if (userDoc.contains("wallet_balance")) {
                            Object balanceObj = userDoc.get("wallet_balance");
                            if (balanceObj instanceof Long) {
                                currentBalance = ((Long) balanceObj).doubleValue();
                            } else if (balanceObj instanceof Double) {
                                currentBalance = (Double) balanceObj;
                            } else if (balanceObj instanceof Integer) {
                                currentBalance = ((Integer) balanceObj).doubleValue();
                            }
                        }
                        
                        // Calculate new balance
                        final double newBalance = currentBalance + amount;
                        
                        // Create a batch for atomic operations
                        WriteBatch batch = db.batch();
                        
                        // 1. Update wallet balance
                        batch.update(db.collection("users").document(userId), 
                            "wallet_balance", newBalance);
                        
                        // 2. Create refund record
                        Map<String, Object> refundData = new HashMap<>();
                        refundData.put("original_payment_id", paymentId);
                        refundData.put("refund_id", refundId);
                        refundData.put("user_id", userId);
                        refundData.put("amount", amount);
                        refundData.put("booking_id", bookingId);
                        refundData.put("car_name", booking.getCar_name());
                        refundData.put("refund_date", new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", 
                            java.util.Locale.getDefault()).format(new java.util.Date()));
                        refundData.put("refund_status", "Processed");
                        
                        // Get payment method - use the one from booking object or default to "unknown"
                        String methodToUse = booking.getPayment_method();
                        if (methodToUse == null || methodToUse.isEmpty()) {
                            methodToUse = paymentMethod != null ? paymentMethod : "unknown";
                        }
                        refundData.put("payment_method", methodToUse);
                        
                        batch.set(db.collection("refunds").document(refundId), refundData);
                        
                        // 3. Create wallet transaction
                        String transactionId = "trans_" + System.currentTimeMillis();
                        Map<String, Object> transactionData = new HashMap<>();
                        transactionData.put("userId", userId); // Add userId field to ensure it's properly linked
                        transactionData.put("type", "credit");
                        
                        // Special description based on payment method and status
                        String description;
                        if ("wallet".equalsIgnoreCase(methodToUse)) {
                            description = "Refund to wallet for cancelled booking: " + booking.getCar_name() + " (originally paid from wallet)";
                        } else if (booking.getStatus().equalsIgnoreCase("Rejected")) {
                            description = "Refund for rejected booking: " + booking.getCar_name() + " (credited to wallet)";
                        } else {
                            description = "Refund credited to wallet for booking: " + booking.getCar_name();
                        }
                        transactionData.put("description", description);
                        
                        transactionData.put("amount", amount);
                        transactionData.put("timestamp", new Date());
                        transactionData.put("status", "completed");
                        transactionData.put("related_booking_id", bookingId);
                        transactionData.put("refund_id", refundId);
                        
                        // Add transaction to both the user's transactions subcollection and the main transactions collection
                        batch.set(
                            db.collection("users").document(userId)
                              .collection("transactions").document(transactionId), 
                            transactionData
                        );
                        
                        // Also add to main transactions collection for better tracking
                        batch.set(
                            db.collection("transactions").document(transactionId),
                            transactionData
                        );
                        
                        // 4. Update booking with refund info
                        batch.update(db.collection("bookings").document(bookingId),
                            "refund_processed", true,
                            "refund_id", refundId,
                            "refund_amount", amount,
                            "refund_date", refundData.get("refund_date"),
                            "credited_to_wallet", true
                        );
                        
                        // Store payment method for use in success message
                        final String finalMethodToUse = methodToUse;
                        
                        // Commit all operations as a batch
                        batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                // Double-check that the wallet balance was actually updated
                                db.collection("users").document(userId).get()
                                    .addOnSuccessListener(updatedUserDoc -> {
                                        double updatedBalance = 0;
                                        if (updatedUserDoc.contains("wallet_balance")) {
                                            Object balanceObj = updatedUserDoc.get("wallet_balance");
                                            if (balanceObj instanceof Long) {
                                                updatedBalance = ((Long) balanceObj).doubleValue();
                                            } else if (balanceObj instanceof Double) {
                                                updatedBalance = (Double) balanceObj;
                                            } else if (balanceObj instanceof Integer) {
                                                updatedBalance = ((Integer) balanceObj).doubleValue();
                                            }
                                        }
                                        
                                        // If balance wasn't updated correctly, try again directly
                                        if (Math.abs(updatedBalance - newBalance) > 0.01) {
                                            Log.w("BookingAdapter", "Wallet balance wasn't updated correctly. Trying direct update.");
                                            db.collection("users").document(userId)
                                                .update("wallet_balance", newBalance)
                                                .addOnSuccessListener(v -> {
                                                    Log.d("BookingAdapter", "Wallet balance updated directly: " + newBalance);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("BookingAdapter", "Failed direct wallet update: " + e.getMessage());
                                                });
                                        }
                                    });
                                
                                // Success - show message with payment method info
                                String successMessage;
                                if ("wallet".equalsIgnoreCase(finalMethodToUse)) {
                                    successMessage = "✅ ₹" + amount + " credited to wallet for cancellation of " + booking.getCar_name();
                                } else {
                                    successMessage = "✅ ₹" + amount + " refund credited to wallet for cancellation of " + booking.getCar_name();
                                }
                                
                                Toast.makeText(holder.itemView.getContext(),
                                    successMessage, 
                                    Toast.LENGTH_LONG).show();
                                
                                // Update local object
                                booking.setRefund_processed(true);
                                booking.setRefund_amount(amount);
                                booking.setCredited_to_wallet(true);
                                notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(holder.itemView.getContext(),
                                    "Failed to process refund: " + e.getMessage(), 
                                    Toast.LENGTH_LONG).show();
                                
                                // Log the error for debugging
                                Log.e("BookingAdapter", "Refund batch failed: " + e.getMessage(), e);
                            });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(holder.itemView.getContext(),
                            "Failed to get wallet balance: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                        
                        // Log the error for debugging
                        Log.e("BookingAdapter", "Failed to get wallet balance: " + e.getMessage(), e);
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(holder.itemView.getContext(),
                    "Error checking booking: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
                
                // Log the error for debugging
                Log.e("BookingAdapter", "Error checking booking: " + e.getMessage(), e);
            });
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }
}
