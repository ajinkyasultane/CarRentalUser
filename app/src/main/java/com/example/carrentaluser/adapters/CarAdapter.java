package com.example.carrentaluser.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carrentaluser.R;
import com.example.carrentaluser.models.Car;

import java.util.List;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    private Context context;
    private List<Car> carList;
    private OnBookClickListener listener;

    public interface OnBookClickListener {
        void onBookClick(Car car);
    }

    public CarAdapter(Context context, List<Car> carList, OnBookClickListener listener) {
        this.context = context;
        this.carList = carList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_car, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = carList.get(position);
        holder.carNameTextView.setText(car.getCarName());
        holder.carPriceTextView.setText("₹" + car.getPricePerDay() + " / day");
        holder.carQuantityTextView.setText("Available: " + car.getAvailableQuantity());

        Glide.with(context).load(car.getCarImageUrl()).into(holder.carImageView);

        holder.bookButton.setOnClickListener(v -> listener.onBookClick(car));
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    static class CarViewHolder extends RecyclerView.ViewHolder {
        ImageView carImageView;
        TextView carNameTextView, carPriceTextView, carQuantityTextView;
        Button bookButton;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);
            carImageView = itemView.findViewById(R.id.carImageView);
            carNameTextView = itemView.findViewById(R.id.carNameTextView);
            carPriceTextView = itemView.findViewById(R.id.carPriceTextView);
            carQuantityTextView = itemView.findViewById(R.id.carQuantityTextView);
            bookButton = itemView.findViewById(R.id.bookButton);
        }
    }
}
