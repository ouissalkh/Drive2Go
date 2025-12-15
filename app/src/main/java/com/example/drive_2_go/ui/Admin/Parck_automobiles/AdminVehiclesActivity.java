package com.example.drive_2_go.ui.Admin.Parck_automobiles;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.Car;
import com.example.drive_2_go.ui.Admin.ComposantCommunAdmin.BaseAdminActivity;
import com.example.drive_2_go.ui.Admin.addeditCar.AddEditVehicleActivity;
import com.example.drive_2_go.ui.adapter.CarAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AdminVehiclesActivity extends BaseAdminActivity {

    private static final String TAG = "AdminVehicles";

    // --- Vues ---
    private RecyclerView recyclerView;
    private FloatingActionButton fabAddCar;
    private SearchView searchView;
    private ProgressBar progressBar;
    private View layoutEmptyState;

    // --- TextViews Statistiques ---
    private TextView tvTotalCars, tvAvailableCars, tvRentedCars, tvMaintenanceCars;

    // --- Données & Adapter ---
    private CarAdapter carAdapter;
    private List<Car> carList;
    private List<Car> carListFiltered;

    // --- Firebase ---
    private FirebaseFirestore db;
    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parck_automobile);

        // 1. Initialisation
        initViews();
        initFirebase();
        setupRecyclerView();

        // 2. Chargement des données (Temps réel)
        loadCarsRealtime();

        // 3. Gestion de la recherche
        setupSearch();

        // 4. Bouton Ajouter
        fabAddCar.setOnClickListener(v -> {
            Intent intent = new Intent(AdminVehiclesActivity.this, AddEditVehicleActivity.class);
            startActivity(intent);
        });
    }

    private void initViews() {
        // Stats
        tvTotalCars = findViewById(R.id.tvTotalCars);
        tvAvailableCars = findViewById(R.id.tvAvailableCars);
        tvRentedCars = findViewById(R.id.tvRentedCars);
        tvMaintenanceCars = findViewById(R.id.tvMaintenanceCars);

        // Composants principaux
        recyclerView = findViewById(R.id.recyclerViewCars);
        searchView = findViewById(R.id.searchViewCars);
        fabAddCar = findViewById(R.id.fabAddCar);
        progressBar = findViewById(R.id.progressBar); // Vérifiez si vous l'avez gardé dans le XML, sinon supprimez
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        // Si la progressBar n'est pas dans le nouveau XML, on évite le crash
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        carList = new ArrayList<>();
        carListFiltered = new ArrayList<>();
    }

    private void setupRecyclerView() {

        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        carAdapter = new CarAdapter(this, carListFiltered, true);
        recyclerView.setAdapter(carAdapter);
    }


    private void loadCarsRealtime() {

        firestoreListener = db.collection("cars")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur chargement Firestore", error);
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        return;
                    }

                    if (value != null) {
                        carList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Car car = doc.toObject(Car.class);
                            if (car != null) {
                                car.setId(doc.getId()); // Récupération de l'ID Firestore
                                carList.add(car);
                            }
                        }

                        // Appliquer le filtre actuel (s'il y a une recherche en cours)
                        String currentQuery = searchView.getQuery().toString();
                        filterCars(currentQuery);

                        // Mettre à jour les statistiques
                        updateStatistics();

                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });

    }

    private void updateStatistics() {
        int total = carList.size();
        int available = 0;
        int rented = 0;
        int maintenance = 0;

        for (Car car : carList) {
            if (car.isAvailable()) {
                available++;
            } else {
                rented++; // Considéré comme Louée ou Indisponible
            }
        }

        // Mise à jour de l'UI
        if (tvTotalCars != null) tvTotalCars.setText(String.valueOf(total));
        if (tvAvailableCars != null) tvAvailableCars.setText(String.valueOf(available));
        if (tvRentedCars != null) tvRentedCars.setText(String.valueOf(rented));

        // Pour maintenance, on met 0 ou on cache car l'info n'existe pas dans Car.java
        if (tvMaintenanceCars != null) tvMaintenanceCars.setText("0");
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterCars(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCars(newText);
                return false;
            }
        });
    }

    private void filterCars(String query) {
        carListFiltered.clear();

        if (query == null || query.trim().isEmpty()) {
            carListFiltered.addAll(carList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Car car : carList) {
                // Logique de recherche conservée depuis votre ancien fichier
                boolean matchesName = car.getName() != null && car.getName().toLowerCase().contains(lowerQuery);
                boolean matchesBrand = car.getBrand() != null && car.getBrand().toLowerCase().contains(lowerQuery);
                boolean matchesModel = car.getModel() != null && car.getModel().toLowerCase().contains(lowerQuery);
                boolean matchesPlate = car.getLicensePlate() != null && car.getLicensePlate().toLowerCase().contains(lowerQuery);

                if (matchesName || matchesBrand || matchesModel || matchesPlate) {
                    carListFiltered.add(car);
                }
            }
        }

        carAdapter.notifyDataSetChanged();

        // Gestion visuelle si liste vide
        if (carListFiltered.isEmpty() && !carList.isEmpty()) {
            // On a des voitures mais la recherche ne donne rien
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else if (carList.isEmpty()) {
            // Pas de voitures du tout dans la base
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            // Affichage normal
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    public class Ride {
        private String destination;
        private int price; //  Your code expects a number (Integer)

        public Ride() {
        } // Empty constructor needed for Firestore
        // getters and setters...
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Arrêter l'écoute Firestore quand on quitte l'écran pour économiser la batterie/data
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}