package com.example.drive_2_go.ui.Admin.Table_bord;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

// IMPORTS FIREBASE
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Admin.ComposantCommunAdmin.BaseAdminActivity;

import java.util.Calendar;
import java.util.Date;

public class adminActivity extends BaseAdminActivity {

    // VUES POUR LES COMPTEURS
    private TextView tvVehiculeDispo; // Utilisé pour le ratio (Voitures dispo / Total Voitures)
    private TextView tvReservationActives;
    private TextView tvUsersCount;
    private TextView tvRevenuMensuel;

    // FIREBASE ET LOGGING
    private FirebaseFirestore db;
    private static final String TAG = "AdminActivityDashboard";
    private static final String COLLECTION_RESERVATIONS = "reservations";
    private static final String COLLECTION_CARS = "cars";
    private static final String COLLECTION_USERS = "users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        setupNavigation();

        // 1. Initialisation de Firebase
        db = FirebaseFirestore.getInstance();

        // 2. Initialisation des Vues (TextViews)
        tvVehiculeDispo = findViewById(R.id.vehiculedispo);
        tvReservationActives = findViewById(R.id.reservationactives);
        tvUsersCount = findViewById(R.id.users);
        tvRevenuMensuel = findViewById(R.id.revenuemensulle);

        // 3. Lancement des fonctions de chargement des données
        loadCarAvailabilityRatio();        // Voitures dispo / Total Voitures (CORRIGÉ)
        loadActiveReservationsCount();    // Réservations actives (Confirmées)
        loadUsersCount();                 // Total des utilisateurs
        loadMonthlyRevenue();             // Revenus du mois courant
    }

    // =========================================================
    // 1. RATIO DISPONIBILITÉ VOITURES / TOTAL VOITURES
    // Met à jour tvVehiculeDispo
    // =========================================================
    private void loadCarAvailabilityRatio() {
        tvVehiculeDispo.setText("Chargement...");

        // 1. Première requête : Compter le nombre de voitures disponibles (Numérateur)
        db.collection(COLLECTION_CARS)
                .whereEqualTo("available", true) // Utilisation du booléen 'true'
                .get()
                .addOnSuccessListener(availableCarsSnapshot -> {
                    final int availableCarsCount = availableCarsSnapshot.size(); // Déclaration locale

                    // 2. Deuxième requête : Compter le nombre total de voitures (Dénominateur)
                    db.collection(COLLECTION_CARS)
                            .get()
                            .addOnSuccessListener(totalCarsSnapshot -> {
                                int totalCarsCount = totalCarsSnapshot.size(); // Déclaration locale

                                // 3. Affichage du ratio final : Voitures disponibles / Total des voitures
                                tvVehiculeDispo.setText(availableCarsCount + " / " + totalCarsCount);
                            })
                            .addOnFailureListener(e -> {
                                tvVehiculeDispo.setText("Erreur Total");
                                Log.e(TAG, "Erreur lors du chargement du total des voitures:", e);
                            });
                })
                .addOnFailureListener(e -> {
                    tvVehiculeDispo.setText("Erreur Dispo");
                    Log.e(TAG, "Erreur lors du chargement des voitures disponibles:", e);
                });
    }

    // =========================================================
    // 2. COMPTAGE DES RÉSERVATIONS ACTIVES (STATUS: Confirmée)
    // =========================================================
    private void loadActiveReservationsCount() {
        tvReservationActives.setText("Chargement...");

        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("status", "Confirmée")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int activeCount = querySnapshot.size();
                    tvReservationActives.setText(String.valueOf(activeCount));
                })
                .addOnFailureListener(e -> {
                    tvReservationActives.setText("Erreur");
                    Log.e(TAG, "Erreur lors du chargement des réservations actives:", e);
                });
    }

    // =========================================================
    // 3. COMPTAGE DES UTILISATEURS
    // =========================================================
    private void loadUsersCount() {
        tvUsersCount.setText("Chargement...");

        db.collection(COLLECTION_USERS)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        int totalUsers = querySnapshot.size();
                        tvUsersCount.setText(String.valueOf(totalUsers));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        tvUsersCount.setText("Erreur");
                        Log.e(TAG, "Erreur lors du chargement des utilisateurs:", e);
                    }
                });
    }

    // =========================================================
    // 4. CALCUL DES REVENUS DU MOIS COURANT (Simulé)
    // =========================================================
    private void loadMonthlyRevenue() {
        tvRevenuMensuel.setText("Chargement...");

        // 1. Déterminer la période (Début et Fin du mois courant)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        // Début du mois
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startOfMonth = calendar.getTime();

        // Fin du mois
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endOfMonth = calendar.getTime();

        // 2. Requête Firestore
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("status", "Terminée")
                .whereGreaterThanOrEqualTo("paymentDate", startOfMonth)
                .whereLessThanOrEqualTo("paymentDate", endOfMonth)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalRevenue = 0.0;
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Double price = document.getDouble("totalPrice");
                        if (price != null) {
                            totalRevenue += price;
                        } else {
                            Long longPrice = document.getLong("totalPrice");
                            if (longPrice != null) {
                                totalRevenue += longPrice.doubleValue();
                            }
                        }
                    }
                    // Affichage du résultat formaté
                    tvRevenuMensuel.setText(String.format("%.2f €", totalRevenue));
                })
                .addOnFailureListener(e -> {
                    tvRevenuMensuel.setText("Erreur");
                    Log.e(TAG, "Erreur lors du calcul du revenu mensuel:", e);
                });
    }
}