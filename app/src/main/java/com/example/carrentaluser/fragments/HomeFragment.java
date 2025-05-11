package com.example.carrentaluser.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentaluser.BookCarActivity;
import com.example.carrentaluser.R;
import com.example.carrentaluser.adapters.CarAdapter;
import com.example.carrentaluser.models.Car;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements CarAdapter.OnBookClickListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private List<Car> carList;
    private CarAdapter carAdapter;
    private FirebaseFirestore firestore;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.carRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        carList = new ArrayList<>();
        carAdapter = new CarAdapter(getContext(), carList, this);
        recyclerView.setAdapter(carAdapter);

        firestore = FirebaseFirestore.getInstance();
        loadCarsFromFirestore();

        return view;
    }

    private void loadCarsFromFirestore() {
        progressBar.setVisibility(View.VISIBLE);
        firestore.collection("cars")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        carList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Car car = document.toObject(Car.class);
                            carList.add(car);
                        }
                        carAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Failed to load cars", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onBookClick(Car car) {
        Intent intent = new Intent(getContext(), BookCarActivity.class);
        intent.putExtra("carId", car.getCarId());
        intent.putExtra("carName", car.getCarName());
        intent.putExtra("carImageUrl", car.getCarImageUrl());
        intent.putExtra("carPrice", car.getPricePerDay());
        startActivity(intent);
    }
}
