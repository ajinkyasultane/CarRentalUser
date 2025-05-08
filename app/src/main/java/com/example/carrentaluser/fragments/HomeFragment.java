package com.example.carrentaluser.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentaluser.BookCarActivity;
import com.example.carrentaluser.R;
import com.example.carrentaluser.adapters.CarAdapter;
import com.example.carrentaluser.models.Booking;
import com.example.carrentaluser.models.Car;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView carRecyclerView;
    private List<Car> carList;
    private CarAdapter carAdapter;
    private FirebaseFirestore db;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        carRecyclerView = view.findViewById(R.id.carRecyclerView);
        carRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        carList = new ArrayList<>();
        carAdapter = new CarAdapter(getContext(), carList, car -> {
            // Handle the book button click
            // Navigate to BookCarActivity with the car details
            Intent intent = new Intent(getContext(), BookCarActivity.class);
            intent.putExtra("carId", car.getCarId());
            intent.putExtra("carName", car.getCarName());
            intent.putExtra("carImage", car.getCarImage());
            intent.putExtra("pricePerDay", car.getPricePerDay());
            startActivity(intent);
        });

        carRecyclerView.setAdapter(carAdapter);

        db = FirebaseFirestore.getInstance();
        fetchCars();

        return view;
    }

    private void fetchCars() {
        db.collection("cars").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Booking booking = documentSnapshot.toObject(Booking.class);
                            LinkedList<Booking> bookingList = null;
                            bookingList.add(booking);
                        }

                        carAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle the error (e.g., display a message)
                });
    }
}
