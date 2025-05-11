package com.example.carrentaluser.model;

public class Car {
    private String name;
    private String brand;
    private int availablequant;
    private String imageUrl;
    private int price;

    public Car() {
        // Needed for Firestore
    }

    public Car(String name, String brand, int availablequant, String imageUrl, int price) {
        this.name = name;
        this.brand = brand;
        this.availablequant = availablequant;
        this.imageUrl = imageUrl;
        this.price = price;
    }

    public String getName() { return name; }
    public String getBrand() { return brand; }
    public int getAvailablequant() { return availablequant; }
    public String getImageUrl() { return imageUrl; }
    public int getPrice() { return price; }
}
