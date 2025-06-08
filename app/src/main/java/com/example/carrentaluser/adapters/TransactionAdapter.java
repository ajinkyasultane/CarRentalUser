package com.example.carrentaluser.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentaluser.R;
import com.example.carrentaluser.models.Transaction;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final Context context;
    private final List<Transaction> transactionList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
    private boolean showTransactionIds = false; // Toggle to show transaction IDs (for debugging)

    public TransactionAdapter(Context context, List<Transaction> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        if (position >= transactionList.size()) {
            Log.e("TransactionAdapter", "Invalid position: " + position + ", list size: " + transactionList.size());
            return;
        }
        
        Transaction transaction = transactionList.get(position);
        if (transaction == null) {
            Log.e("TransactionAdapter", "Null transaction at position: " + position);
            return;
        }

        try {
            // Set transaction title based on description
            String description = transaction.getDescription();
            holder.titleTextView.setText(description != null ? description : "Transaction");
    
            // Set transaction date
            if (transaction.getTimestamp() != null) {
                holder.dateTextView.setText(dateFormat.format(transaction.getTimestamp()));
            } else {
                holder.dateTextView.setText("Date not available");
            }
    
            // Set transaction status
            String status = transaction.getStatus();
            if (status != null) {
                holder.statusTextView.setVisibility(View.VISIBLE);
                
                if (status.equalsIgnoreCase("completed")) {
                    holder.statusTextView.setText("Completed");
                    holder.statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                } else if (status.equalsIgnoreCase("pending")) {
                    holder.statusTextView.setText("Pending");
                    holder.statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
                } else if (status.equalsIgnoreCase("failed")) {
                    holder.statusTextView.setText("Failed");
                    holder.statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                } else {
                    holder.statusTextView.setText(status);
                    holder.statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                }
            } else {
                holder.statusTextView.setVisibility(View.GONE);
            }
    
            // Set transaction ID (for debugging)
            if (showTransactionIds) {
                holder.idTextView.setVisibility(View.VISIBLE);
                String id = transaction.getId();
                holder.idTextView.setText("ID: " + (id != null ? id : "unknown"));
            } else {
                holder.idTextView.setVisibility(View.GONE);
            }
    
            // Set amount and color based on transaction type
            String amountText;
            int backgroundResId;
            int iconResId;
    
            String type = transaction.getType();
            if (type != null && "credit".equals(type)) {
                amountText = "+ ₹" + String.format(Locale.getDefault(), "%.2f", transaction.getAmount());
                backgroundResId = R.drawable.credit_background;
                iconResId = android.R.drawable.ic_input_add;
            } else {
                amountText = "- ₹" + String.format(Locale.getDefault(), "%.2f", transaction.getAmount());
                backgroundResId = R.drawable.debit_background;
                iconResId = android.R.drawable.ic_menu_send;
            }
    
            holder.amountTextView.setText(amountText);
            
            // Set colors based on transaction type
            if (type != null && "credit".equals(type)) {
                holder.amountTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            } else {
                holder.amountTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            }
            
            holder.iconView.setBackground(ContextCompat.getDrawable(context, backgroundResId));
            holder.iconView.setImageResource(iconResId);
        } catch (Exception e) {
            Log.e("TransactionAdapter", "Error binding transaction: " + e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        if (newTransactions == null) {
            Log.e("TransactionAdapter", "Null transaction list provided");
            return;
        }
        
        Log.d("TransactionAdapter", "Updating transactions: " + newTransactions.size() + " items");
        transactionList.clear();
        transactionList.addAll(newTransactions);
        notifyDataSetChanged();
    }

    public void toggleTransactionIds() {
        showTransactionIds = !showTransactionIds;
        notifyDataSetChanged();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView titleTextView;
        TextView dateTextView;
        TextView statusTextView;
        TextView amountTextView;
        TextView idTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.transactionTypeIcon);
            titleTextView = itemView.findViewById(R.id.transactionTitleTextView);
            dateTextView = itemView.findViewById(R.id.transactionDateTextView);
            statusTextView = itemView.findViewById(R.id.transactionStatusTextView);
            amountTextView = itemView.findViewById(R.id.transactionAmountTextView);
            idTextView = itemView.findViewById(R.id.transactionIdTextView);
        }
    }
} 