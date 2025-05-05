package com.example.carrentaluser.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.carrentaluser.R;
import com.example.carrentaluser.adapters.CarAdapter;
import com.example.carrentaluser.models.CarModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<CarModel> carList;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Dummy data for now
        carList = new ArrayList<>();
        carList.add(new CarModel("1", "Swift", 1200, "https://example.com/swift.jpg"));
        carList.add(new CarModel("2", "Innova", 2000, "https://example.com/innova.jpg"));
        carList.add(new CarModel("3", "Baleno", 1300, "https://example.com/baleno.jpg"));
        carList.add(new CarModel("4", "Fortuner", 3000, "https://example.com/fortuner.jpg"));

        CarAdapter adapter = new CarAdapter(getContext(), carList);
        recyclerView.setAdapter(adapter);

        return view;
    }
}
