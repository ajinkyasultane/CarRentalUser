package com.example.carrentaluser.models;

public class BookingModel {

    private String bookingId;
    private String userId;
    private String carId;
    private String carName;
    private String startDate;
    private String endDate;
    private String pickupLocation;
    private int totalAmount;
    private String status;

    // Empty constructor for Firebase
    public BookingModel() {}

    // Constructor
    public BookingModel(String bookingId, String userId, String carId, String carName,
                        String startDate, String endDate, String pickupLocation,
                        int totalAmount, String status) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.carId = carId;
        this.carName = carName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pickupLocation = pickupLocation;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    // Getter and Setter for bookingId
    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    // Getters for other fields
    public String getUserId() { return userId; }
    public String getCarId() { return carId; }
    public String getCarName() { return carName; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getPickupLocation() { return pickupLocation; }
    public int getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
}
