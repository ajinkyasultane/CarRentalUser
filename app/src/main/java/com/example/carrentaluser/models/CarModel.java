package com.example.carrentaluser.models;

public class CarModel {
    private String id;
    private String name;
    private int pricePerDay;
    private String imageUrl;

    public CarModel() {}

    public CarModel(String id, String name, int pricePerDay, String imageUrl) {
        this.id = id;
        this.name = name;
        this.pricePerDay = pricePerDay;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPricePerDay() {
        return pricePerDay;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
