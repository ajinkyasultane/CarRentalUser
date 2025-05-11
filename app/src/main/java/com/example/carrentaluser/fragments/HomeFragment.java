package com.example.carrentaluser.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentaluser.R;
import com.example.carrentaluser.adapters.CarAdapter;
import com.example.carrentaluser.model.Car;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArrayList<Car> carList;
    private CarAdapter carAdapter;
    private FirebaseFirestore db;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        carList = new ArrayList<>();
        carAdapter = new CarAdapter(getContext(), carList);
        recyclerView.setAdapter(carAdapter);

        db = FirebaseFirestore.getInstance();
        fetchCars();

        return root;
    }

    private void fetchCars() {
        db.collection("cars")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    carList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Car car = doc.toObject(Car.class);
                        carList.add(car);
                    }
                    carAdapter.notifyDataSetChanged();
                });
    }
}
