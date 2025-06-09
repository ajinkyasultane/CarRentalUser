package com.example.carrentaluser.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentaluser.R;
import com.example.carrentaluser.models.BankAccount;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BankAccountManagementAdapter extends RecyclerView.Adapter<BankAccountManagementAdapter.BankAccountViewHolder> {

    private final Context context;
    private final List<BankAccount> bankAccounts;
    private final OnAccountEditClickListener editListener;
    private final OnAccountDeleteListener deleteListener;
    private final OnSetPrimaryListener setPrimaryListener;

    // Interface for edit click events
    public interface OnAccountEditClickListener {
        void onAccountEditClick(String accountId);
    }

    // Interface for delete click events (not used in single account mode)
    public interface OnAccountDeleteListener {
        void onAccountDelete(BankAccount account);
    }

    // Interface for setting primary account (not used in single account mode)
    public interface OnSetPrimaryListener {
        void onSetPrimary(BankAccount account);
    }

    public BankAccountManagementAdapter(Context context, List<BankAccount> bankAccounts,
                                        OnAccountEditClickListener editListener,
                                        OnAccountDeleteListener deleteListener,
                                        OnSetPrimaryListener setPrimaryListener) {
        this.context = context;
        this.bankAccounts = bankAccounts;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
        this.setPrimaryListener = setPrimaryListener;
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
        
        // Set account details
        holder.tvBankName.setText(account.getBankName());
        holder.tvAccountNumber.setText(account.getMaskedAccountNumber());
        holder.tvAccountType.setText(account.getAccountType());
        holder.tvAccountHolder.setText(account.getAccountHolderName());
        
        // Always show primary badge in single account mode
        holder.tvPrimaryIndicator.setVisibility(View.VISIBLE);
        
        // Set IFSC code
        holder.tvIfscCode.setText("IFSC: " + account.getIfscCode());
        
        // Format and set the last updated date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        if (account.getLastUpdated() != null) {
            holder.tvLastUpdated.setText("Last updated: " + dateFormat.format(account.getLastUpdated().toDate()));
        } else {
            holder.tvLastUpdated.setText("");
        }
        
        // Set click listeners
        holder.btnEdit.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onAccountEditClick(account.getId());
            }
        });
        
        // Hide delete and set primary buttons in single account mode
        holder.btnDelete.setVisibility(View.GONE);
        holder.btnSetPrimary.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return bankAccounts.size();
    }

    static class BankAccountViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvBankName;
        TextView tvAccountNumber;
        TextView tvAccountType;
        TextView tvAccountHolder;
        TextView tvIfscCode;
        TextView tvLastUpdated;
        TextView tvPrimaryIndicator;
        ImageButton btnEdit;
        ImageButton btnDelete;
        ImageButton btnSetPrimary;

        public BankAccountViewHolder(@NonNull View itemView) {
            super(itemView);
            
            cardView = itemView.findViewById(R.id.cardView);
            tvBankName = itemView.findViewById(R.id.tvBankName);
            tvAccountNumber = itemView.findViewById(R.id.tvAccountNumber);
            tvAccountType = itemView.findViewById(R.id.tvAccountType);
            tvAccountHolder = itemView.findViewById(R.id.tvAccountHolder);
            tvIfscCode = itemView.findViewById(R.id.tvIfscCode);
            tvLastUpdated = itemView.findViewById(R.id.tvLastUpdated);
            tvPrimaryIndicator = itemView.findViewById(R.id.tvPrimaryIndicator);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnSetPrimary = itemView.findViewById(R.id.btnSetPrimary);
        }
    }
} 