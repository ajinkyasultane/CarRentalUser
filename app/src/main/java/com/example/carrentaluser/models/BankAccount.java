package com.example.carrentaluser.models;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.Date;

public class BankAccount implements Serializable {
    private String id;
    private String accountHolderName;
    private String accountNumber;
    private String accountType;
    private String bankName;
    private String ifscCode;
    private boolean isPrimary;
    private boolean isVerified;
    private Timestamp createdAt;
    private Timestamp lastUpdated;
    private String userId;
    private String upiId;

    // Empty constructor needed for Firestore
    public BankAccount() {
    }

    public BankAccount(String accountHolderName, String accountNumber, String accountType,
                       String bankName, String ifscCode, boolean isPrimary, String userId) {
        this.accountHolderName = accountHolderName;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.bankName = bankName;
        this.ifscCode = ifscCode;
        this.isPrimary = isPrimary;
        this.isVerified = false;
        this.userId = userId;
        this.createdAt = Timestamp.now();
        this.lastUpdated = Timestamp.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public void updateLastUpdated() {
        this.lastUpdated = Timestamp.now();
    }

    // Helper method to get masked account number for display
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        
        String lastFourDigits = accountNumber.substring(accountNumber.length() - 4);
        return "XXXX XXXX " + lastFourDigits;
    }
} 