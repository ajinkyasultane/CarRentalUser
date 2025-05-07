package com.example.carrentaluser.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;


import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;


import com.example.carrentaluser.R;
import com.example.carrentaluser.adapters.BookingAdapter;
import com.example.carrentaluser.models.BookingModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class BookingFragment extends Fragment {

    private RecyclerView recyclerView;
    private BookingAdapter adapter;
    private List<BookingModel> bookingList;

    public BookingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_booking, container, false);

        recyclerView = rootView.findViewById(R.id.recycler_view_booking);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        bookingList = new ArrayList<>();
        adapter = new BookingAdapter(getContext(), bookingList);
        recyclerView.setAdapter(adapter);

        // Fetch bookings from Firestore
        fetchBookings();

        return rootView;
    }

    private void fetchBookings() {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("Bookings")
                .whereEqualTo("userId", userId)  // Only fetch bookings for the current user
                .orderBy("startDate", Query.Direction.DESCENDING)  // Sort by start date (latest first)
                .get()
                .addOnSuccessListener(this::onFetchBookingsSuccess)
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load bookings", Toast.LENGTH_SHORT).show());
    }

    private void onFetchBookingsSuccess(QuerySnapshot queryDocumentSnapshots) {
        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
            for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                BookingModel booking = document.toObject(BookingModel.class);
                if (booking != null) {
                    bookingList.add(booking);
                }
            }
            adapter.notifyDataSetChanged();  // Update RecyclerView after data is fetched
        }
    }
}
