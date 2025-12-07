package com.example.drive_2_go.ui.Admin.Parck_automobiles;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Admin.addeditCar.AddEditVehicleActivity;
import com.example.drive_2_go.ui.adapter.CarAdapter;
import com.example.drive_2_go.data.model.Car;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminVehiculesActivity extends AppCompatActivity {

    private RecyclerView recyclerViewCars;
    private CarAdapter carAdapter;
    private List<Car> carList;
    private List<Car> carListFiltered; // Liste utilisée pour la recherche
    private FloatingActionButton fabAddCar;
    private SearchView searchView;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_vehicules);

        db = FirebaseFirestore.getInstance();

        // 1. Initialiser les vues
        recyclerViewCars = findViewById(R.id.recyclerViewCars);
        fabAddCar = findViewById(R.id.fabAddCar);
        searchView = findViewById(R.id.searchView);
        progressBar = findViewById(R.id.progressBar);

        // 2. Configuration RecyclerView
        // C'est ce qu'il faut pour avoir de GRANDES cartes
        recyclerViewCars.setLayoutManager(new LinearLayoutManager(this));

        carList = new ArrayList<>();
        carListFiltered = new ArrayList<>();

        // IMPORTANT : Le 3ème paramètre est 'true' car c'est l'interface Admin
        // Cela active les boutons Modifier et Supprimer dans l'adaptateur
        carAdapter = new CarAdapter(this, carListFiltered, true);
        recyclerViewCars.setAdapter(carAdapter);

        // 3. Bouton Ajouter Voiture (Ouvre l'activité vide)
        fabAddCar.setOnClickListener(v -> {
            Intent intent = new Intent(AdminVehiculesActivity.this, AddEditVehicleActivity.class);
            startActivity(intent);
        });

        // 4. Gestion de la recherche
        setupSearchView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharge la liste à chaque fois qu'on revient sur cet écran
        // (ex: après avoir ajouté ou modifié une voiture)
        loadCars();
    }

    private void loadCars() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("cars")
                .get()
                .addOnCompleteListener(task -> {
                    if (isFinishing() || isDestroyed()) return; // Vérification de sécurité si l'activité est fermée

                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        carList.clear();
                        carListFiltered.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Firestore convertit automatiquement vers votre nouveau modèle Car
                            Car car = document.toObject(Car.class);
                            // On s'assure que l'ID est bien défini
                            car.setId(document.getId());
                            carList.add(car);
                        }

                        // Au départ, la liste filtrée contient tout
                        carListFiltered.addAll(carList);
                        carAdapter.notifyDataSetChanged();

                        if (carList.isEmpty()) {
                            Toast.makeText(AdminVehiculesActivity.this, "Aucun véhicule. Ajoutez-en un !", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AdminVehiculesActivity.this, "Erreur de chargement", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCars(newText);
                return true;
            }
        });
    }

    private void filterCars(String query) {
        carListFiltered.clear();

        if (query.isEmpty()) {
            carListFiltered.addAll(carList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Car car : carList) {
                // Recherche sur le nom, la marque ou le modèle
                // Adaptez ces conditions selon vos besoins
                boolean matchesName = car.getName() != null && car.getName().toLowerCase().contains(lowerQuery);
                boolean matchesBrand = car.getBrand() != null && car.getBrand().toLowerCase().contains(lowerQuery);
                boolean matchesModel = car.getModel() != null && car.getModel().toLowerCase().contains(lowerQuery);

                if (matchesName || matchesBrand || matchesModel) {
                    carListFiltered.add(car);
                }
            }
        }
        carAdapter.notifyDataSetChanged();
    }
}