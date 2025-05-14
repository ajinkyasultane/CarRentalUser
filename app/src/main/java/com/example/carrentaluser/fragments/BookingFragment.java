package com.example.carrentaluser.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentaluser.R;
import com.example.carrentaluser.adapters.BookingAdapter;
import com.example.carrentaluser.models.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BookingFragment extends Fragment {

    private RecyclerView recyclerView;
    private BookingAdapter adapter;
    private List<Booking> bookings = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView noBookingsText;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public BookingFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking, container, false);

        recyclerView = view.findViewById(R.id.recycler_bookings);
        progressBar = view.findViewById(R.id.progress_bar);
        noBookingsText = view.findViewById(R.id.no_bookings_text);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadBookings();

        return view;
    }

    private void loadBookings() {
        progressBar.setVisibility(View.VISIBLE);
        noBookingsText.setVisibility(View.GONE);

        db.collection("bookings")
                .whereEqualTo("user_id", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookings.clear();
                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        Booking booking = snapshot.toObject(Booking.class);
                        bookings.add(booking);
                    }

                    progressBar.setVisibility(View.GONE);

                    if (bookings.isEmpty()) {
                        noBookingsText.setVisibility(View.VISIBLE);
                    } else {
                        adapter = new BookingAdapter(bookings);
                        recyclerView.setAdapter(adapter);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    noBookingsText.setVisibility(View.VISIBLE);
                    noBookingsText.setText("Failed to load bookings.");
                });
    }
}
