package com.example.drive_2_go.ui.Admin.Gestion_Reservations;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Admin.ComposantCommunAdmin.BaseAdminActivity;
import com.example.drive_2_go.data.model.Reservation;
import com.example.drive_2_go.data.model.User;
import com.example.drive_2_go.data.model.ReservationDisplayModel;
import com.example.drive_2_go.ui.adapter.ReservationsAdapter;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ReservationsActivity extends BaseAdminActivity {

    private RecyclerView recyclerViewReservations;
    private TextView tvTotalCount, tvConfirmedCount, tvPendingCount, tvCancelledCount;
    private AutoCompleteTextView autoCompleteTextView;
    private androidx.appcompat.widget.SearchView searchView;

    private ReservationsAdapter reservationsAdapter;
    private List<ReservationDisplayModel> masterReservationList = new ArrayList<>();
    private FirebaseFirestore db;
    private static final String TAG = "ReservationsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_reservations);

        setupNavigation();
        db = FirebaseFirestore.getInstance();

        // --- 1. INITIALISATION DES CARTES (STATISTIQUES) ---
        tvTotalCount = findViewById(R.id.tv_total_count);
        tvConfirmedCount = findViewById(R.id.tv_confirmed_count);
        tvPendingCount = findViewById(R.id.tv_pending_count);
        tvCancelledCount = findViewById(R.id.tv_cancelled_count);

        // --- 2. INITIALISATION RECYCLERVIEW ---
        recyclerViewReservations = findViewById(R.id.recyclerViewReservations);
        recyclerViewReservations.setLayoutManager(new LinearLayoutManager(this));
        reservationsAdapter = new ReservationsAdapter(this, new ArrayList<>());
        recyclerViewReservations.setAdapter(reservationsAdapter);

        // --- 3. RECHERCHE ET FILTRE ---
        searchView = findViewById(R.id.searchView);
        setupSearchView();

        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);
        setupStatusDropdown();

        // --- 4. CHARGEMENT INITIAL ---
        refreshDashboard();
    }

    private void refreshDashboard() {
        loadStats();
        loadReservations("Tous les statuts");
    }

    private void loadStats() {
        // Total
        db.collection("reservations").get().addOnSuccessListener(q -> tvTotalCount.setText(String.valueOf(q.size())));
        // Confirmées (acceptée)
        db.collection("reservations").whereEqualTo("status", "acceptée").get().addOnSuccessListener(q -> tvConfirmedCount.setText(String.valueOf(q.size())));
        // En attente
        db.collection("reservations").whereEqualTo("status", "En attente").get().addOnSuccessListener(q -> tvPendingCount.setText(String.valueOf(q.size())));
        // Annulées (refusée)
        db.collection("reservations").whereEqualTo("status", "refusée").get().addOnSuccessListener(q -> tvCancelledCount.setText(String.valueOf(q.size())));
    }

    private void setupStatusDropdown() {
        String[] statusItems = {"Tous les statuts", "Confirmée", "En attente de validation", "Annulée"};
        ArrayAdapter<String> adapterItems = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statusItems);
        autoCompleteTextView.setAdapter(adapterItems);
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            loadReservations(parent.getItemAtPosition(position).toString());
        });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { filter(query); return true; }
            @Override
            public boolean onQueryTextChange(String newText) { filter(newText); return true; }
        });
    }

    private void filter(String query) {
        List<ReservationDisplayModel> filtered = new ArrayList<>();
        for (ReservationDisplayModel item : masterReservationList) {
            if (item.getUserName().toLowerCase().contains(query.toLowerCase()) ||
                    item.getCarName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(item);
            }
        }
        reservationsAdapter.updateReservations(filtered);
    }

    private void loadReservations(String filterStatus) {
        Query query = db.collection("reservations");

        if (filterStatus.equals("Confirmée")) query = query.whereEqualTo("status", "acceptée");
        else if (filterStatus.equals("En attente de validation")) query = query.whereEqualTo("status", "En attente");
        else if (filterStatus.equals("Annulée")) query = query.whereEqualTo("status", "refusée");

        query.get().addOnSuccessListener(querySnapshot -> {
            List<Reservation> rawList = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Reservation res = doc.toObject(Reservation.class);
                if (res != null) {
                    res.setReservationNumber(doc.getId().substring(0, 8).toUpperCase());
                    rawList.add(res);
                }
            }
            fetchUsersAndDisplay(rawList);
        });
    }

    private void fetchUsersAndDisplay(List<Reservation> rawList) {
        if (rawList.isEmpty()) {
            masterReservationList.clear();
            reservationsAdapter.updateReservations(new ArrayList<>());
            return;
        }

        List<ReservationDisplayModel> finalList = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(0);

        for (Reservation res : rawList) {
            db.collection("users").document(res.getUserId()).get().addOnSuccessListener(userDoc -> {
                User user = userDoc.toObject(User.class);
                String name = (user != null) ? user.getNom() + " " + user.getPrenom() : "Inconnu";

                finalList.add(new ReservationDisplayModel(
                        res.getReservationNumber(), name, res.getCarName(),
                        res.getStartDate(), res.getEndDate(),
                        (user != null ? user.getEmail() : ""), (user != null ? user.getTelephone() : ""),
                        res.getStatus(), res.getTotalPrice()
                ));

                if (count.incrementAndGet() == rawList.size()) {
                    masterReservationList = new ArrayList<>(finalList);
                    reservationsAdapter.updateReservations(masterReservationList);
                }
            }).addOnFailureListener(e -> {
                if (count.incrementAndGet() == rawList.size()) {
                    masterReservationList = new ArrayList<>(finalList);
                    reservationsAdapter.updateReservations(masterReservationList);
                }
            });
        }
    }
}