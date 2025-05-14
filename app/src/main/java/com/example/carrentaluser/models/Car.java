package com.example.carrentaluser.models;

public class Car {
    private String name;
    private String brand;
    private String imageUrl;
    private int price;
    private int availablequant;

    public Car() {} // Firestore requires empty constructor

    public Car(String name, String brand, String imageUrl, int price, int availablequant) {
        this.name = name;
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.price = price;
        this.availablequant = availablequant;
    }

    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getImageUrl() { return imageUrl; }
    public int getPrice() { return price; }
    public int getAvailablequant() { return availablequant; }
}
