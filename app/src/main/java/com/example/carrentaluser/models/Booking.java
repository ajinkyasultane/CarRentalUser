package com.example.carrentaluser.models;

public class Booking {
    private String car_name, car_image, start_date, end_date, pickup_location, status;

    public Booking() {} // Needed for Firestore

    public Booking(String car_name, String car_image, String start_date, String end_date, String pickup_location, String status) {
        this.car_name = car_name;
        this.car_image = car_image;
        this.start_date = start_date;
        this.end_date = end_date;
        this.pickup_location = pickup_location;
        this.status = status;
    }

    public String getCar_name() { return car_name; }
    public String getCar_image() { return car_image; }
    public String getStart_date() { return start_date; }
    public String getEnd_date() { return end_date; }
    public String getPickup_location() { return pickup_location; }
    public String getStatus() { return status; }
}
