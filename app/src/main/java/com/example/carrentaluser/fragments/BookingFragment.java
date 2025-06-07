package com.example.carrentaluser.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BookingFragment extends Fragment {

    private RecyclerView recyclerView;
    private BookingAdapter adapter;
    private List<Booking> bookings = new ArrayList<>();
    private List<Booking> filteredBookings = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView noBookingsText;
    private LinearLayout emptyStateContainer;
    private TextInputEditText searchEditText;
    private ChipGroup filterChipGroup;
    private Chip chipAll, chipPending, chipConfirmed, chipCancelled;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    
    private String currentSearchQuery = "";
    private String currentFilter = "all";

    public BookingFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking, container, false);

        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupFilters();
        loadBookings();

        return view;
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_bookings);
        progressBar = view.findViewById(R.id.progress_bar);
        noBookingsText = view.findViewById(R.id.no_bookings_text);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        searchEditText = view.findViewById(R.id.search_edit_text);
        filterChipGroup = view.findViewById(R.id.filter_chip_group);
        chipAll = view.findViewById(R.id.chip_all);
        chipPending = view.findViewById(R.id.chip_pending);
        chipConfirmed = view.findViewById(R.id.chip_confirmed);
        chipCancelled = view.findViewById(R.id.chip_cancelled);
        
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new BookingAdapter(filteredBookings);
        recyclerView.setAdapter(adapter);
    }
    
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                filterBookings();
            }
        });
    }
    
    private void setupFilters() {
        // Set initial filter
        currentFilter = "all";
        
        // Listen for filter changes
        filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_all) {
                currentFilter = "all";
            } else if (checkedId == R.id.chip_pending) {
                currentFilter = "Pending";
            } else if (checkedId == R.id.chip_confirmed) {
                currentFilter = "Confirmed";
            } else if (checkedId == R.id.chip_cancelled) {
                currentFilter = "Cancelled";
            }
            filterBookings();
        });
    }

    private void loadBookings() {
        showLoading();

        if (auth.getCurrentUser() == null) {
            showEmpty("Please log in to view bookings");
            return;
        }

        db.collection("bookings")
                .whereEqualTo("user_id", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookings.clear();
                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        Booking booking = snapshot.toObject(Booking.class);
                        booking.setBooking_id(snapshot.getId());
                        bookings.add(booking);
                    }

                    filterBookings();
                })
                .addOnFailureListener(e -> {
                    showEmpty("Failed to load bookings");
                });
    }
    
    private void filterBookings() {
        filteredBookings.clear();
        
        for (Booking booking : bookings) {
            // First check if it matches search query
            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                    booking.getCar_name().toLowerCase().contains(currentSearchQuery) ||
                    booking.getPickup_location().toLowerCase().contains(currentSearchQuery) ||
                    booking.getStatus().toLowerCase().contains(currentSearchQuery);
            
            // Then check if it matches active filter
            boolean matchesFilter = currentFilter.equals("all") || 
                    booking.getStatus().equalsIgnoreCase(currentFilter);
            
            if (matchesSearch && matchesFilter) {
                filteredBookings.add(booking);
            }
        }
        
        adapter.notifyDataSetChanged();
        
        // Show empty state if no results
        if (filteredBookings.isEmpty()) {
            if (currentSearchQuery.isEmpty() && currentFilter.equals("all")) {
                showEmpty("No bookings yet");
            } else {
                showEmpty("No matching bookings found");
            }
        } else {
            hideEmpty();
        }
    }
    
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }
    
    private void showEmpty(String message) {
        progressBar.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        noBookingsText.setText(message);
    }
    
    private void hideEmpty() {
        progressBar.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh bookings when returning to the fragment
        loadBookings();
    }
}
