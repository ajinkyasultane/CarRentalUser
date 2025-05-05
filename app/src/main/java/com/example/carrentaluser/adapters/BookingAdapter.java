package com.example.carrentaluser.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentaluser.R;
import com.example.carrentaluser.models.BookingModel;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private Context context;
    private List<BookingModel> bookingList;

    public BookingAdapter(Context context, List<BookingModel> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
    }

    @Override
    public BookingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BookingViewHolder holder, int position) {
        BookingModel booking = bookingList.get(position);

        holder.carNameTextView.setText(booking.getCarName());
        holder.startDateTextView.setText("From: " + booking.getStartDate());
        holder.endDateTextView.setText("To: " + booking.getEndDate());
        holder.statusTextView.setText("Status: " + booking.getStatus());
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {

        TextView carNameTextView;
        TextView startDateTextView;
        TextView endDateTextView;
        TextView statusTextView;

        public BookingViewHolder(View itemView) {
            super(itemView);
            carNameTextView = itemView.findViewById(R.id.text_car_name);
            startDateTextView = itemView.findViewById(R.id.text_start_date);
            endDateTextView = itemView.findViewById(R.id.text_end_date);
            statusTextView = itemView.findViewById(R.id.text_status);
        }
    }
}
