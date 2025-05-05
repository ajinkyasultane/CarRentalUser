package com.example.carrentaluser.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carrentaluser.CarDetailsActivity;
import com.example.carrentaluser.R;
import com.example.carrentaluser.models.CarModel;

import java.util.List;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    private Context context;
    private List<CarModel> carList;

    public CarAdapter(Context context, List<CarModel> carList) {
        this.context = context;
        this.carList = carList;
    }

    @Override
    public CarViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_car, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CarViewHolder holder, int position) {
        CarModel car = carList.get(position);
        holder.name.setText(car.getName());
        holder.price.setText("₹" + car.getPricePerDay() + "/day");

        Glide.with(context).load(car.getImageUrl()).into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CarDetailsActivity.class);
            intent.putExtra("carId", car.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    static class CarViewHolder extends RecyclerView.ViewHolder {
        TextView name, price;
        ImageView image;

        CarViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.car_name);
            price = itemView.findViewById(R.id.car_price);
            image = itemView.findViewById(R.id.car_image);
        }
    }
}
