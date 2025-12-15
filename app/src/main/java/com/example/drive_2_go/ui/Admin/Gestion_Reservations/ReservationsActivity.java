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

// IMPORTS FIREBASE
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

// REMARQUE : Vous devrez créer et importer votre propre Adapter pour le RecyclerView
// import com.example.drive_2_go.adapters.ReservationsAdapter;


public class ReservationsActivity extends BaseAdminActivity {

    private RecyclerView recyclerViewReservations;
    private TextView tvTotalCount;
    private TextView tvConfirmedCount;
    private TextView tvPendingCount;
    private TextView tvCancelledCount;

    private AutoCompleteTextView autoCompleteTextView;
    private ArrayAdapter<String> adapterItems;

    // FIREBASE ET LOGGING
    private FirebaseFirestore db;
    private static final String TAG = "ReservationsActivity";
    private static final String COLLECTION_NAME = "reservations"; // Nom de la collection Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Configuration initiale ---
        try {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            setContentView(R.layout.activity_reservations);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du setContentView: " + e.getMessage());
            Toast.makeText(this, "Erreur XML critique", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            setupNavigation();
        } catch (Exception e) {
            Log.e(TAG, "Erreur dans setupNavigation: " + e.getMessage());
        }

        View drawerLayout = findViewById(R.id.drawerLayout);
        if (drawerLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // --- Initialisation des Vues et de Firebase ---
        tvTotalCount = findViewById(R.id.tv_total_count);

        // Initialisation des autres compteurs (Nécessite les IDs dans le XML)
        tvConfirmedCount = findViewById(R.id.tv_confirmed_count);
        tvPendingCount = findViewById(R.id.tv_pending_count);
        tvCancelledCount = findViewById(R.id.tv_cancelled_count);

        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);
        db = FirebaseFirestore.getInstance();

        // Initialisation et configuration du RecyclerView
        recyclerViewReservations = findViewById(R.id.recyclerViewReservations);
        if (recyclerViewReservations != null) {
            recyclerViewReservations.setLayoutManager(new LinearLayoutManager(this));
            // IMPORTANT : Ceci empêche l'erreur "No adapter attached" dans le logcat
            // Vous devez remplacer ceci par votre véritable Adapter de réservation plus tard.
            // Example:
            // recyclerViewReservations.setAdapter(new ReservationsAdapter(new ArrayList<>()));
        }


        // --- Lancement des fonctions de Comptage ---
        loadTotalReservationsCount();
        loadConfirmedReservationsCount();
        loadPendingReservationsCount();
        loadCancelledReservationsCount();

        // --- Configuration Dropdown ---
        if (autoCompleteTextView != null) {
            String[] statusItems = {"Tous les statuts", "Confirmée", "En attente", "Annulée", "Terminée"};
            adapterItems = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statusItems);
            autoCompleteTextView.setAdapter(adapterItems);

            autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
                String item = parent.getItemAtPosition(position).toString();
                Toast.makeText(this, "Filtre : " + item, Toast.LENGTH_SHORT).show();
            });
        }
    }

    // =========================================================
    // 1. FONCTION : COMPTAGE TOTAL
    // =========================================================
    private void loadTotalReservationsCount() {
        tvTotalCount.setText("Chargement...");

        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        int totalCount = querySnapshot.size();
                        tvTotalCount.setText(String.valueOf(totalCount));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        tvTotalCount.setText("Erreur");
                        Log.e(TAG, "Erreur total:", e);
                    }
                });
    }

    // =========================================================
    // 2. FONCTION : COMPTAGE CONFIRMÉES
    // =========================================================
    private void loadConfirmedReservationsCount() {
        if (tvConfirmedCount == null) return; // Sécurité si l'ID n'est pas dans le XML
        tvConfirmedCount.setText("...");

        db.collection(COLLECTION_NAME)
                .whereEqualTo("status", "Confirmée") // Vérifiez le nom du champ et la valeur
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    tvConfirmedCount.setText(String.valueOf(querySnapshot.size()));
                })
                .addOnFailureListener(e -> {
                    tvConfirmedCount.setText("Erreur");
                    Log.e(TAG, "Erreur Confirmées:", e);
                });
    }


    // =========================================================
    // 3. FONCTION : COMPTAGE EN ATTENTE
    // =========================================================
    private void loadPendingReservationsCount() {
        if (tvPendingCount == null) return; // Sécurité si l'ID n'est pas dans le XML
        tvPendingCount.setText("...");

        db.collection(COLLECTION_NAME)
                .whereEqualTo("status", "En attente de validation") // Vérifiez le nom du champ et la valeur
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    tvPendingCount.setText(String.valueOf(querySnapshot.size()));
                })
                .addOnFailureListener(e -> {
                    tvPendingCount.setText("Erreur");
                    Log.e(TAG, "Erreur En attente:", e);
                });
    }


    // =========================================================
    // 4. FONCTION : COMPTAGE ANNULÉES
    // =========================================================
    private void loadCancelledReservationsCount() {
        if (tvCancelledCount == null) return; // Sécurité si l'ID n'est pas dans le XML
        tvCancelledCount.setText("...");

        db.collection(COLLECTION_NAME)
                .whereEqualTo("status", "Annulée") // Vérifiez le nom du champ et la valeur
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    tvCancelledCount.setText(String.valueOf(querySnapshot.size()));
                })
                .addOnFailureListener(e -> {
                    tvCancelledCount.setText("Erreur");
                    Log.e(TAG, "Erreur Annulées:", e);
                });
    }

}