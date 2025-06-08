package com.example.carrentaluser.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

@IgnoreExtraProperties
public class Transaction {
    @DocumentId
    private String id;
    private String userId;
    private String type; // "credit" or "debit"
    private String description;
    private double amount;
    @ServerTimestamp
    private Date timestamp;
    private String status; // "completed", "pending", "failed"

    // Default constructor required for Firestore
    public Transaction() {
    }

    public Transaction(String id, String userId, String type, String description, double amount, Date timestamp, String status) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.description = description;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
    }

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

    @PropertyName("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @PropertyName("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @PropertyName("amount")
    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @PropertyName("timestamp")
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @PropertyName("status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                '}';
    }
} 