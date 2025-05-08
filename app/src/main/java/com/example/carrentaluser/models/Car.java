package com.example.carrentaluser.models;

public class Car {

    private String carId;
    private String carName;
    private String carImage;
    private int pricePerDay;
    private int availableQuantity;

    // Empty constructor for Firestore
    public Car() {
    }

    // Constructor
    public Car(String carId, String carName, String carImage, int pricePerDay, int availableQuantity) {
        this.carId = carId;
        this.carName = carName;
        this.carImage = carImage;
        this.pricePerDay = pricePerDay;
        this.availableQuantity = availableQuantity;
    }

    // Getters and Setters
    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public String getCarName() {
        return carName;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }

    public String getCarImage() {
        return carImage;
    }

    public void setCarImage(String carImage) {
        this.carImage = carImage;
    }

    public int getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(int pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}
