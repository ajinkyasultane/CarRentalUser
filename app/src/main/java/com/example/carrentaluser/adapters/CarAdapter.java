package com.example.carrentaluser.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carrentaluser.BookingActivity;
import com.example.carrentaluser.R;
import com.example.carrentaluser.models.Car;

import java.util.ArrayList;
import java.util.List;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> implements Filterable {

    private Context context;
    private List<Car> carList;
    private List<Car> carListFull; // full copy for filtering

    public CarAdapter(Context context, List<Car> carList) {
        this.context = context;
        this.carList = carList;
        this.carListFull = new ArrayList<>(carList);
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

        holder.name.setText(car.getName());
        holder.brand.setText(car.getBrand());
        holder.price.setText("₹" + car.getPrice());
        holder.available.setText("Available: " + car.getAvailablequant());

        Glide.with(context)
                .load(car.getImageUrl())
                .into(holder.image);

        holder.bookButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookingActivity.class);
            intent.putExtra("car_name", car.getName());
            intent.putExtra("car_price", car.getPrice());
            intent.putExtra("car_image", car.getImageUrl());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    @Override
    public Filter getFilter() {
        return carFilter;
    }

    private final Filter carFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Car> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(carListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (Car car : carListFull) {
                    if (car.getName().toLowerCase().contains(filterPattern)
                            || car.getBrand().toLowerCase().contains(filterPattern)) {
                        filteredList.add(car);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            carList.clear();
            carList.addAll((List<Car>) results.values);
            notifyDataSetChanged();
        }
    };

    public static class CarViewHolder extends RecyclerView.ViewHolder {
        TextView name, brand, price, available;
        ImageView image;
        Button bookButton;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.car_name);
            brand = itemView.findViewById(R.id.car_brand);
            price = itemView.findViewById(R.id.car_price);
            available = itemView.findViewById(R.id.car_available);
            image = itemView.findViewById(R.id.car_image);
            bookButton = itemView.findViewById(R.id.btn_book);
        }
    }
}
