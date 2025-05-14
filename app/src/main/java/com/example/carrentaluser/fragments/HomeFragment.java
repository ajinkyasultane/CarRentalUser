package com.example.carrentaluser.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentaluser.R;
import com.example.carrentaluser.adapters.CarAdapter;
import com.example.carrentaluser.models.Car;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private CarAdapter carAdapter;
    private List<Car> carList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recycler_home);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        loadCars();

        return view;
    }

    private void loadCars() {
        db.collection("cars").get().addOnSuccessListener(queryDocumentSnapshots -> {
            carList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Car car = doc.toObject(Car.class);
                carList.add(car);
            }
            carAdapter = new CarAdapter(getContext(), carList);
            recyclerView.setAdapter(carAdapter);
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to load cars", Toast.LENGTH_SHORT).show();
        });
    }
}

