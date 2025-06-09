package com.example.carrentaluser.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentaluser.R;
import com.example.carrentaluser.models.BankAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class BankAccountAdapter extends RecyclerView.Adapter<BankAccountAdapter.BankAccountViewHolder> {

    public interface OnAccountSelectedListener {
        void onAccountSelected(String accountId);
    }

    private final Context context;
    private final List<BankAccount> bankAccounts;
    private final OnAccountSelectedListener listener;
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    public BankAccountAdapter(Context context, List<BankAccount> bankAccounts, OnAccountSelectedListener listener) {
        this.context = context;
        this.bankAccounts = bankAccounts;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public BankAccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bank_account, parent, false);
        return new BankAccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BankAccountViewHolder holder, int position) {
        BankAccount account = bankAccounts.get(position);
        
        // Set bank name
        holder.tvBankName.setText(account.getBankName());
        
        // Set account holder name
        holder.tvAccountHolder.setText(account.getAccountHolderName());
        
        // Set masked account number (show only last 4 digits)
        String accountNumber = account.getAccountNumber();
        String maskedNumber = "XXXX XXXX " + accountNumber.substring(Math.max(0, accountNumber.length() - 4));
        holder.tvAccountNumber.setText(maskedNumber);
        
        // Set IFSC code
        holder.tvIfscCode.setText("IFSC: " + account.getIfscCode());
        
        // Show primary badge if this is the primary account
        holder.tvPrimaryIndicator.setVisibility(account.isPrimary() ? View.VISIBLE : View.GONE);
        
        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccountSelected(account.getId());
            }
        });
        
        // Set options menu click listeners
        holder.btnEdit.setOnClickListener(v -> {
            // Handle edit action
            Toast.makeText(context, "Edit account feature coming soon", Toast.LENGTH_SHORT).show();
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            // Handle delete action
            deleteAccount(account);
        });
        
        holder.btnSetPrimary.setOnClickListener(v -> {
            // Handle set as primary action
            setPrimaryAccount(account);
        });
        
        // Hide set primary button if already primary
        holder.btnSetPrimary.setVisibility(account.isPrimary() ? View.GONE : View.VISIBLE);
    }
    
    private void deleteAccount(BankAccount account) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(context, "You must be logged in to delete an account", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        
        // Prevent deletion of primary account
        if (account.isPrimary()) {
            Toast.makeText(context, "Cannot delete primary account. Set another account as primary first.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Delete the account from Firestore
        db.collection("users").document(userId).collection("bank_accounts")
                .document(account.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                    
                    // Remove from local list and update UI
                    int position = bankAccounts.indexOf(account);
                    if (position != -1) {
                        bankAccounts.remove(position);
                        notifyItemRemoved(position);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void setPrimaryAccount(BankAccount account) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(context, "You must be logged in to set a primary account", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        
        // First, find the current primary account and update it
        for (BankAccount existingAccount : bankAccounts) {
            if (existingAccount.isPrimary()) {
                // Update the current primary account to not be primary
                db.collection("users").document(userId).collection("bank_accounts")
                        .document(existingAccount.getId())
                        .update("isPrimary", false);
                
                // Update the local object
                existingAccount.setPrimary(false);
            }
        }
        
        // Set the new account as primary
        db.collection("users").document(userId).collection("bank_accounts")
                .document(account.getId())
                .update("isPrimary", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Primary account updated", Toast.LENGTH_SHORT).show();
                    
                    // Update the local object
                    account.setPrimary(true);
                    
                    // Refresh the entire list to ensure correct order
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update primary account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return bankAccounts.size();
    }

    static class BankAccountViewHolder extends RecyclerView.ViewHolder {
        TextView tvBankName;
        TextView tvAccountHolder;
        TextView tvAccountNumber;
        TextView tvIfscCode;
        TextView tvPrimaryIndicator;
        TextView tvAccountType;
        TextView tvLastUpdated;
        ImageView btnEdit;
        ImageView btnDelete;
        ImageView btnSetPrimary;

        public BankAccountViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBankName = itemView.findViewById(R.id.tvBankName);
            tvAccountHolder = itemView.findViewById(R.id.tvAccountHolder);
            tvAccountNumber = itemView.findViewById(R.id.tvAccountNumber);
            tvIfscCode = itemView.findViewById(R.id.tvIfscCode);
            tvPrimaryIndicator = itemView.findViewById(R.id.tvPrimaryIndicator);
            tvAccountType = itemView.findViewById(R.id.tvAccountType);
            tvLastUpdated = itemView.findViewById(R.id.tvLastUpdated);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnSetPrimary = itemView.findViewById(R.id.btnSetPrimary);
        }
    }
} 