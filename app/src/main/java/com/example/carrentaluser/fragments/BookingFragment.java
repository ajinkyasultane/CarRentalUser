package com.example.carrentaluser.fragments;

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

import com.example.carrentaluser.R;
import com.example.carrentaluser.adapter.BookingAdapter;
import com.example.carrentaluser.models.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BookingFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private List<Booking> bookingList;
    private BookingAdapter bookingAdapter;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    public BookingFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking, container, false);

        recyclerView = view.findViewById(R.id.bookingRecyclerView);
        progressBar = view.findViewById(R.id.bookingProgressBar);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        bookingList = new ArrayList<>();
        bookingAdapter = new BookingAdapter(getContext(), bookingList);
        recyclerView.setAdapter(bookingAdapter);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadBookings();

        return view;
    }

    private void loadBookings() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = auth.getCurrentUser().getUid();

        firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        bookingList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Booking booking = doc.toObject(Booking.class);
                            bookingList.add(booking);
                        }
                        bookingAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Failed to load bookings", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
