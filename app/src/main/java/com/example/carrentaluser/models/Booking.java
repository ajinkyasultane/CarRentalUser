package com.example.carrentaluser.models;

public class Booking {
    private String bookingId;
    private String carId;
    private String carName;
    private String carImageUrl;
    private String startDate;
    private String endDate;
    private String pickupLocation;
    private String status;
    private double totalAmount;
    private String userId;

    public Booking() {
        // Needed for Firebase
    }

    public Booking(String bookingId, String carId, String carName, String carImageUrl,
                   String startDate, String endDate, String pickupLocation,
                   String status, double totalAmount, String userId) {
        this.bookingId = bookingId;
        this.carId = carId;
        this.carName = carName;
        this.carImageUrl = carImageUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pickupLocation = pickupLocation;
        this.status = status;
        this.totalAmount = totalAmount;
        this.userId = userId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getCarId() {
        return carId;
    }

    public String getCarName() {
        return carName;
    }

    public String getCarImageUrl() {
        return carImageUrl;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public String getStatus() {
        return status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getUserId() {
        return userId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }

    public void setCarImageUrl(String carImageUrl) {
        this.carImageUrl = carImageUrl;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
