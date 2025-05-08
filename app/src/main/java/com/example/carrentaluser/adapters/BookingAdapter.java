package com.example.carrentaluser.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carrentaluser.R;
import com.example.carrentaluser.models.Booking;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private Context context;
    private List<Booking> bookingList;

    public BookingAdapter(Context context, List<Booking> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        holder.carName.setText(booking.getCarName());
        holder.dates.setText(booking.getStartDate() + " to " + booking.getEndDate());
        holder.location.setText("Pickup: " + booking.getPickupLocation());
        holder.status.setText("Status: " + booking.getStatus());

        Glide.with(context).load(booking.getCarImageUrl()).into(holder.carImage);
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        ImageView carImage;
        TextView carName, dates, location, status;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            carImage = itemView.findViewById(R.id.bookingCarImage);
            carName = itemView.findViewById(R.id.bookingCarName);
            dates = itemView.findViewById(R.id.bookingDates);
            location = itemView.findViewById(R.id.bookingLocation);
            status = itemView.findViewById(R.id.bookingStatus);
        }
    }
}
