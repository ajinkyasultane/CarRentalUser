package com.example.carrentaluser.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.carrentaluser.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class TrackingFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap gMap;

    public TrackingFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tracking, container, false);

        mapView = new MapView(requireContext());
        mapView.onCreate(savedInstanceState);
        ((ViewGroup) view.findViewById(R.id.map_container)).addView(mapView);
        mapView.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        // Mock location: Show a car in Mumbai
        LatLng carLocation = new LatLng(19.0760, 72.8777);
        gMap.addMarker(new MarkerOptions().position(carLocation).title("Your Car"));
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, 15f));

      /*  FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("cars").document("car123")
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        double lat = document.getDouble("latitude");
                        double lng = document.getDouble("longitude");
                        LatLng carLocation = new LatLng(lat, lng);

                        gMap.clear();
                        gMap.addMarker(new MarkerOptions().position(carLocation).title("Live Car Location"));
                        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, 15f));
                    }
                });*/

    }

    // Lifecycle methods for MapView
    @Override public void onResume() { super.onResume(); mapView.onResume(); }
    @Override public void onPause() { super.onPause(); mapView.onPause(); }
    @Override public void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}
