package com.example.carrentaluser.models;

public class Booking {

    private String car_name;
    private String car_image;
    private int car_price;
    private String start_date;
    private String end_date;
    private String pickup_location;
    private int total_price;
    private String status;
    private String user_id;
    private String user_email;

    // Required empty constructor for Firestore
    public Booking() {
    }

    public Booking(String car_name, String car_image, int car_price, String start_date, String end_date,
                   String pickup_location, int total_price, String status, String user_id, String user_email) {
        this.car_name = car_name;
        this.car_image = car_image;
        this.car_price = car_price;
        this.start_date = start_date;
        this.end_date = end_date;
        this.pickup_location = pickup_location;
        this.total_price = total_price;
        this.status = status;
        this.user_id = user_id;
        this.user_email = user_email;
    }

    public String getCar_name() {
        return car_name;
    }

    public String getCar_image() {
        return car_image;
    }

    public int getCar_price() {
        return car_price;
    }

    public String getStart_date() {
        return start_date;
    }

    public String getEnd_date() {
        return end_date;
    }

    public String getPickup_location() {
        return pickup_location;
    }

    public int getTotal_price() {
        return total_price;
    }

    public String getStatus() {
        return status;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getUser_email() {
        return user_email;
    }

    // Setters if needed (optional for Firestore but helpful in app logic)

    public void setStatus(String status) {
        this.status = status;
    }
}
