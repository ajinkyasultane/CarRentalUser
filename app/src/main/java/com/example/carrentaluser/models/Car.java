package com.example.carrentaluser.models;

public class Car {
    private String carId;
    private String carName;
    private String carImageUrl;
    private int availableQuantity;
    private double pricePerDay;

    public Car() {
        // Firestore requires empty constructor
    }

    public Car(String carId, String carName, String carImageUrl, int availableQuantity, double pricePerDay) {
        this.carId = carId;
        this.carName = carName;
        this.carImageUrl = carImageUrl;
        this.availableQuantity = availableQuantity;
        this.pricePerDay = pricePerDay;
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

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public double getPricePerDay() {
        return pricePerDay;
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

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public void setPricePerDay(double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }
}
