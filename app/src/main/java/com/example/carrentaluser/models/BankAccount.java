package com.example.carrentaluser.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

@IgnoreExtraProperties
public class BankAccount {
    @DocumentId
    private String id;
    private String userId;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String accountType; // "Savings" or "Current"
    private String upiId; // Optional UPI ID
    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date lastUpdated;
    private boolean isVerified; // Whether the account is verified
    private boolean isPrimary; // Whether this is the primary account for the user

    // Default constructor required for Firestore
    public BankAccount() {
    }

    public BankAccount(String id, String userId, String accountHolderName, String accountNumber, 
                       String ifscCode, String bankName, String accountType, String upiId,
                       Date createdAt, Date lastUpdated, boolean isVerified, boolean isPrimary) {
        this.id = id;
        this.userId = userId;
        this.accountHolderName = accountHolderName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        this.bankName = bankName;
        this.accountType = accountType;
        this.upiId = upiId;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
        this.isVerified = isVerified;
        this.isPrimary = isPrimary;
    }

    // Getters and setters
    @PropertyName("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("accountHolderName")
    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    @PropertyName("accountNumber")
    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    @PropertyName("ifscCode")
    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    @PropertyName("bankName")
    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    @PropertyName("accountType")
    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    @PropertyName("upiId")
    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    @PropertyName("createdAt")
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("lastUpdated")
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @PropertyName("isVerified")
    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    @PropertyName("isPrimary")
    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    @Override
    public String toString() {
        return "BankAccount{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", accountHolderName='" + accountHolderName + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", ifscCode='" + ifscCode + '\'' +
                ", bankName='" + bankName + '\'' +
                ", accountType='" + accountType + '\'' +
                ", upiId='" + upiId + '\'' +
                ", createdAt=" + createdAt +
                ", lastUpdated=" + lastUpdated +
                ", isVerified=" + isVerified +
                ", isPrimary=" + isPrimary +
                '}';
    }
} 