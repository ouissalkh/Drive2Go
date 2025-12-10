package com.example.drive_2_go.ui.Admin.Parck_automobiles;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Admin.ComposantCommunAdmin.BaseAdminActivity;
import com.example.drive_2_go.ui.Admin.addeditCar.AddEditVehicleActivity;
import com.example.drive_2_go.ui.adapter.CarAdapter;
import com.example.drive_2_go.data.model.Car;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminVehiclesActivity  extends BaseAdminActivity {

    private static final String TAG = "AdminVehicles";

    private RecyclerView recyclerViewCars;
    private CarAdapter carAdapter;
    private List<Car> carList;
    private List<Car> carListFiltered;
    private FloatingActionButton fabAddCar;
    private SearchView searchView;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_parck_automobile);

            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "✅ Activity créée");

            initViews();
            setupRecyclerView();
            setupListeners();

        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur onCreate : " + e.getMessage(), e);
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        recyclerViewCars = findViewById(R.id.recyclerViewCars);
        fabAddCar = findViewById(R.id.fabAddCar);
        searchView = findViewById(R.id.searchView);
        progressBar = findViewById(R.id.progressBar);

        Log.d(TAG, "✅ Vues initialisées");
    }

    private void setupRecyclerView() {
        //  CONFIGURATION POUR GRANDES CARTES
        recyclerViewCars.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCars.setHasFixedSize(true);

        carList = new ArrayList<>();
        carListFiltered = new ArrayList<>();

        // Mode Admin = true (affiche boutons modifier/supprimer)
        carAdapter = new CarAdapter(this, carListFiltered, true);
        recyclerViewCars.setAdapter(carAdapter);

        Log.d(TAG, "✅ RecyclerView configuré (mode admin)");
    }

    private void setupListeners() {
        // Bouton Ajouter
        fabAddCar.setOnClickListener(v -> {
            try {
                Log.d(TAG, "🔘 Clic sur Ajouter véhicule");
                Intent intent = new Intent(AdminVehiclesActivity.this, AddEditVehicleActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "❌ Erreur ouverture AddEdit : " + e.getMessage(), e);
                Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Recherche
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

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 onResume - Rechargement des voitures");
        loadCars();
    }

    private void loadCars() {
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Début du chargement des voitures..."); // Log de début

        db.collection("cars") // Vérifiez ce nom
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        carList.clear();
                        carListFiltered.clear();

                        Log.d(TAG, "Documents trouvés : " + task.getResult().size()); // Combien de docs ?

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Log.d(TAG, "Traitement du doc : " + document.getId()); // On essaie quel doc ?
                                Car car = document.toObject(Car.class);
                                car.setId(document.getId());
                                carList.add(car);
                            } catch (Exception e) {
                                // C'est ICI que l'erreur s'affiche
                                Log.e(TAG, "ERREUR CONVERSION sur " + document.getId() + " : " + e.getMessage());
                            }
                        }

                        carListFiltered.addAll(carList);
                        carAdapter.notifyDataSetChanged();

                    } else {
                        Log.e(TAG, "Erreur connexion Firestore", task.getException());
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
                boolean matchesName = car.getName() != null &&
                        car.getName().toLowerCase().contains(lowerQuery);
                boolean matchesBrand = car.getBrand() != null &&
                        car.getBrand().toLowerCase().contains(lowerQuery);
                boolean matchesModel = car.getModel() != null &&
                        car.getModel().toLowerCase().contains(lowerQuery);
                boolean matchesPlate = car.getLicensePlate() != null &&
                        car.getLicensePlate().toLowerCase().contains(lowerQuery);

                if (matchesName || matchesBrand || matchesModel || matchesPlate) {
                    carListFiltered.add(car);
                }
            }
        }

        carAdapter.notifyDataSetChanged();
        Log.d(TAG, "🔍 Recherche '" + query + "' : " + carListFiltered.size() + " résultat(s)");
    }
}