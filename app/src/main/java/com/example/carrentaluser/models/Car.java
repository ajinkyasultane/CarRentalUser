package com.example.carrentaluser.models;

public class Car {
//    private String carId;
//    private String carName;
//    private String carImageUrl;
//    private int availableQuantity;
//    private double pricePerDay;

    private String carId;
    private String name;
    private String brand;
    private double price;
    private String imageUrl;
    public Car() {
        // Firestore requires empty constructor
    }

//    public Car(String carId, String carName, String carImageUrl, int availableQuantity, double pricePerDay) {
//        this.carId = carId;
//        this.carName = carName;
//        this.carImageUrl = carImageUrl;
//        this.availableQuantity = availableQuantity;
//        this.pricePerDay = pricePerDay;
//    }

    public Car(String name, String brand, double price, String imageUrl) {
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.imageUrl = imageUrl;
    }

        public String getCarId() {
        return carId;
    }

    public String getCarName() {
        return name;
    }

    public String getCarImageUrl() {
        return imageUrl;
    }

//    public int getAvailableQuantity() {
//        return availableQuantity;
//    }

    public double getPricePerDay() {
        return price;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

//    public void setCarName(String carName) {
//        this.carName = carName;
//    }

    public void setCarName(String name)
    { this.name = name; }

    public void setPrice(Double price) {
        this.price = price;
    }

//    public void setCarImageUrl(String carImageUrl) {
//        this.carImageUrl = carImageUrl;
//    }

    public void setImageUrl(String imageUrl)
    { this.imageUrl = imageUrl; }
//    public void setAvailableQuantity(int availableQuantity) {
//        this.availableQuantity = availableQuantity;
//    }

//
//    public void setPricePerDay(double pricePerDay) {
//        this.pricePerDay = pricePerDay;
//    }
}
