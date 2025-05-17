package com.example.carrentaluser.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentaluser.R;
import com.example.carrentaluser.adapters.CarAdapter;
import com.example.carrentaluser.models.Car;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private CarAdapter adapter;
    private List<Car> carList;
    private ProgressBar progressBar;
    private SearchView searchView;

    private FirebaseFirestore db;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recycler_cars);
        progressBar = view.findViewById(R.id.progress_bar);
        searchView = view.findViewById(R.id.search_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        carList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        loadCars();

        // 🔍 Search logic
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // handled by real-time search
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.getFilter().filter(newText);
                }
                return true;
            }
        });

        return view;
    }

    private void loadCars() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("cars")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Check if fragment is still attached to avoid IllegalStateException
                    if (!isAdded()) {
                        return;
                    }
                    
                    carList.clear();
                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        Car car = snapshot.toObject(Car.class);
                        carList.add(car);
                    }

                    adapter = new CarAdapter(requireContext(), carList);
                    recyclerView.setAdapter(adapter);
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    // Check if fragment is still attached to avoid IllegalStateException
                    if (!isAdded()) {
                        return;
                    }
                    
                    Toast.makeText(getContext(), "Failed to load cars", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }
}
