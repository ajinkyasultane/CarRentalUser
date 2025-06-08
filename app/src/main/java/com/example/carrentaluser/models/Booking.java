package com.example.carrentaluser.models;

public class Booking {

    private String booking_id; // Document ID from Firestore
    private String car_name;
    private String car_image;
    private int car_price;
    private String start_date;
    private String end_date;
    private String pickup_location;
    private String branch_name;
    private String branch_id;
    private boolean with_driver;
    private int total_price;
    private String status;
    private String user_id;
    private String user_email;
    
    // Payment related fields
    private boolean advance_payment_done;
    private int advance_payment_amount;
    private int remaining_payment;
    private String payment_method;
    private String payment_id;
    
    // Refund related fields
    private boolean refund_processed;
    private String refund_id;
    private int refund_amount;
    private String refund_date;
    private boolean credited_to_wallet;

    // Required empty constructor for Firestore
    public Booking() {
    }

    public Booking(String booking_id, String car_name, String car_image, int car_price, String start_date, String end_date,
                   String pickup_location, String branch_name, boolean with_driver, int total_price, 
                   String status, String user_id, String user_email) {
        this.booking_id = booking_id;
        this.car_name = car_name;
        this.car_image = car_image;
        this.car_price = car_price;
        this.start_date = start_date;
        this.end_date = end_date;
        this.pickup_location = pickup_location;
        this.branch_name = branch_name;
        this.with_driver = with_driver;
        this.total_price = total_price;
        this.status = status;
        this.user_id = user_id;
        this.user_email = user_email;
    }

    public String getBooking_id() {
        return booking_id;
    }
    
    public void setBooking_id(String booking_id) {
        this.booking_id = booking_id;
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
    
    public String getBranch_name() {
        return branch_name;
    }
    
    public String getBranch_id() {
        return branch_id;
    }
    
    public boolean isWith_driver() {
        return with_driver;
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
    
    public boolean isAdvance_payment_done() {
        return advance_payment_done;
    }
    
    public int getAdvance_payment_amount() {
        return advance_payment_amount;
    }
    
    public int getRemaining_payment() {
        return remaining_payment;
    }
    
    public String getPayment_method() {
        return payment_method;
    }
    
    public String getPayment_id() {
        return payment_id;
    }
    
    public boolean isRefund_processed() {
        return refund_processed;
    }
    
    public String getRefund_id() {
        return refund_id;
    }
    
    public int getRefund_amount() {
        return refund_amount;
    }
    
    public String getRefund_date() {
        return refund_date;
    }

    public boolean isCredited_to_wallet() {
        return credited_to_wallet;
    }

    // Setters if needed (optional for Firestore but helpful in app logic)

    public void setStatus(String status) {
        this.status = status;
    }
    
    public void setAdvance_payment_done(boolean advance_payment_done) {
        this.advance_payment_done = advance_payment_done;
    }
    
    public void setPayment_method(String payment_method) {
        this.payment_method = payment_method;
    }
    
    public void setPayment_id(String payment_id) {
        this.payment_id = payment_id;
    }
    
    public void setRefund_processed(boolean refund_processed) {
        this.refund_processed = refund_processed;
    }
    
    public void setRefund_id(String refund_id) {
        this.refund_id = refund_id;
    }
    
    public void setRefund_amount(int refund_amount) {
        this.refund_amount = refund_amount;
    }
    
    public void setRefund_date(String refund_date) {
        this.refund_date = refund_date;
    }
    
    public void setCredited_to_wallet(boolean credited_to_wallet) {
        this.credited_to_wallet = credited_to_wallet;
    }
}
