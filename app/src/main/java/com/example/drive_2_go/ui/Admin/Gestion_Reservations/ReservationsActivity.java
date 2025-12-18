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

// IMPORTS CRUCIAUX
import com.example.drive_2_go.data.model.Reservation;
import com.example.drive_2_go.data.model.User;
import com.example.drive_2_go.data.model.ReservationDisplayModel;
import com.example.drive_2_go.ui.adapter.ReservationsAdapter;

// IMPORTS FIREBASE
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ReservationsActivity extends BaseAdminActivity {

    private RecyclerView recyclerViewReservations;
    private TextView tvTotalCount;
    private TextView tvConfirmedCount;
    private TextView tvPendingCount;
    private TextView tvCancelledCount;
    private TextView tvCompletedCount;

    private AutoCompleteTextView autoCompleteTextView;
    private ArrayAdapter<String> adapterItems;

    // NOUVEAU: Pour la recherche textuelle
    private androidx.appcompat.widget.SearchView searchView;

    private ReservationsAdapter reservationsAdapter;
    private List<ReservationDisplayModel> displayReservationList;

    // NOUVEAU: Stocke la liste complète chargée (filtrée par statut) pour la recherche
    private List<ReservationDisplayModel> masterReservationList;

    private FirebaseFirestore db;
    private static final String TAG = "ReservationsActivity";
    private static final String COLLECTION_RESERVATIONS = "reservations";
    private static final String COLLECTION_USERS = "users";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        tvConfirmedCount = findViewById(R.id.tv_confirmed_count);
        tvPendingCount = findViewById(R.id.tv_pending_count);
        tvCancelledCount = findViewById(R.id.tv_cancelled_count);
        //tvCompletedCount = findViewById(R.id.tv_completed_count); // À décommenter si l'ID existe dans le XML

        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);
        searchView = findViewById(R.id.searchView); // Initialisation du SearchView
        db = FirebaseFirestore.getInstance();

        // Initialisation des listes
        masterReservationList = new ArrayList<>(); // Liste principale
        displayReservationList = new ArrayList<>();

        // Initialisation et configuration du RecyclerView
        recyclerViewReservations = findViewById(R.id.recyclerViewReservations);
        if (recyclerViewReservations != null) {
            recyclerViewReservations.setLayoutManager(new LinearLayoutManager(this));

            reservationsAdapter = new ReservationsAdapter(this, displayReservationList);
            recyclerViewReservations.setAdapter(reservationsAdapter);
        }

        // --- Lancement des fonctions de Comptage ---
        loadTotalReservationsCount();
        loadConfirmedReservationsCount();
        loadPendingReservationsCount();
        loadCancelledReservationsCount();
        // loadCompletedReservationsCount();

        // --- Configuration Dropdown et Filtrage ---
        if (autoCompleteTextView != null) {
            // Ces valeurs sont les valeurs affichées à l'utilisateur
            String[] statusItems = {"Tous les statuts", "Confirmée", "En attente de validation", "Annulée", "Terminée"};
            adapterItems = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statusItems);
            autoCompleteTextView.setAdapter(adapterItems);

            autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
                String selectedStatus = parent.getItemAtPosition(position).toString();
                loadReservations(selectedStatus);
                // Réinitialise la barre de recherche après avoir filtré par statut
                if (searchView != null) {
                    searchView.setQuery("", false);
                    searchView.clearFocus();
                }
                Toast.makeText(this, "Filtre appliqué : " + selectedStatus, Toast.LENGTH_SHORT).show();
            });
        }

        // Configuration de l'écouteur de recherche
        if (searchView != null) {
            setupSearchView();
        }

        // --- Chargement initial de toutes les réservations ---
        loadReservations("Tous les statuts");
    }

    // =========================================================
    // NOUVELLE FONCTION : CONFIGURATION ET FILTRAGE DE LA RECHERCHE
    // =========================================================
    private void setupSearchView() {
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterReservations(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Le filtrage se fait à chaque frappe
                filterReservations(newText);
                return true;
            }
        });
    }

    /**
     * Filtre la liste des réservations par préfixe (début de chaîne)
     * sur le nom, la voiture, le numéro de réservation ou l'email.
     * @param query Le texte saisi par l'utilisateur.
     */
    private void filterReservations(String query) {
        String lowerCaseQuery = query.toLowerCase(Locale.ROOT).trim();
        List<ReservationDisplayModel> filteredList = new ArrayList<>();

        // Si la requête est vide, afficher la liste complète actuelle (master)
        if (lowerCaseQuery.isEmpty()) {
            reservationsAdapter.updateReservations(masterReservationList);
            return;
        }

        for (ReservationDisplayModel item : masterReservationList) {

            // 1. Recherche sur le Nom d'utilisateur
            String userName = item.getUserName().toLowerCase(Locale.ROOT);

            // 2. Recherche sur le Nom de la voiture
            String carName = item.getCarName().toLowerCase(Locale.ROOT);

            // 3. Recherche sur le Numéro de réservation
            String reservationNumber = item.getReservationNumber().toLowerCase(Locale.ROOT);

            // 4. Recherche sur l'Email
            String email = item.getEmail().toLowerCase(Locale.ROOT); // *** MODIFIÉ: Utilise getEmail()

            // VÉRIFICATION PAR PRÉFIXE (startsWith)
            if (userName.startsWith(lowerCaseQuery) ||
                    carName.startsWith(lowerCaseQuery) ||
                    reservationNumber.startsWith(lowerCaseQuery) ||
                    email.startsWith(lowerCaseQuery))
            {
                filteredList.add(item);
            }
        }

        // Mise à jour de l'Adapter avec la liste filtrée
        reservationsAdapter.updateReservations(filteredList);
    }

    // =========================================================
    // 5. FONCTION : CHARGEMENT ET FILTRAGE DE LA LISTE DES RÉSERVATIONS
    // =========================================================
    private void loadReservations(String filterStatus) {

        Query query = db.collection(COLLECTION_RESERVATIONS);
        String firestoreStatus = null; // Initialisation à null

        // Mappage des statuts affichés aux statuts Firestore
        switch (filterStatus) {
            case "Confirmée":
                firestoreStatus = "acceptée";
                break;
            case "En attente de validation":
                firestoreStatus = "En attente";
                break;
            case "Annulée":
                firestoreStatus = "refusée";
                break;
            case "Terminée":
                firestoreStatus = "Terminée"; // Assurez-vous que cette valeur existe dans Firestore
                break;
            case "Tous les statuts":
            default:
                // Pas de filtre par statut si "Tous les statuts" ou un statut inconnu
                break;
        }


        if (firestoreStatus != null) {
            query = query.whereEqualTo("status", firestoreStatus);
        }

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Reservation> rawReservations = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        try {
                            Reservation reservation = document.toObject(Reservation.class);
                            if (reservation != null) {
                                String docId = document.getId();
                                reservation.setReservationNumber(docId.substring(0, Math.min(docId.length(), 8)).toUpperCase(Locale.ROOT));

                                rawReservations.add(reservation);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur de conversion du document de réservation : " + e.getMessage());
                        }
                    }
                    fetchUserDetailsForReservations(rawReservations);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors du chargement des réservations: " + e.getMessage());
                    Toast.makeText(ReservationsActivity.this, "Impossible de charger la liste des réservations.", Toast.LENGTH_LONG).show();
                    // Assurez-vous que les deux listes sont mises à jour en cas d'échec
                    masterReservationList = new ArrayList<>();
                    reservationsAdapter.updateReservations(new ArrayList<>());
                });
    }

    private void fetchUserDetailsForReservations(List<Reservation> rawReservations) {
        if (rawReservations.isEmpty()) {
            masterReservationList = new ArrayList<>();
            reservationsAdapter.updateReservations(new ArrayList<>());
            return;
        }

        Map<String, User> userCache = new HashMap<>();
        List<ReservationDisplayModel> finalDisplayList = new ArrayList<>();
        AtomicInteger completedFetches = new AtomicInteger(0);

        for (Reservation reservation : rawReservations) {
            final String currentReservationId = reservation.getReservationNumber();
            final String userId = reservation.getUserId();

            if (userId == null || userId.isEmpty()) {
                Log.w(TAG, "Réservation #" + currentReservationId + " sans userId.");
                finalDisplayList.add(new ReservationDisplayModel(
                        reservation.getReservationNumber(),
                        reservation.getUserName() != null ? reservation.getUserName() : "Utilisateur Inconnu",
                        reservation.getCarName(),
                        reservation.getStartDate(),
                        reservation.getEndDate(),
                        "Email Non Fourni", "Tél. Non Fourni",
                        reservation.getStatus(),
                        reservation.getTotalPrice()
                ));
                if (completedFetches.incrementAndGet() == rawReservations.size()) {
                    runOnUiThread(() -> {
                        masterReservationList = new ArrayList<>(finalDisplayList); // Mise à jour de la master list
                        reservationsAdapter.updateReservations(masterReservationList);
                    });
                }
                continue;
            }

            if (userCache.containsKey(userId)) {
                User user = userCache.get(userId);
                String fullUserName = user.getNom() + " " + user.getPrenom();
                finalDisplayList.add(new ReservationDisplayModel(
                        reservation.getReservationNumber(),
                        fullUserName,
                        reservation.getCarName(),
                        reservation.getStartDate(),
                        reservation.getEndDate(),
                        user.getEmail(),
                        user.getTelephone(),
                        reservation.getStatus(),
                        reservation.getTotalPrice()
                ));
                if (completedFetches.incrementAndGet() == rawReservations.size()) {
                    runOnUiThread(() -> {
                        masterReservationList = new ArrayList<>(finalDisplayList); // Mise à jour de la master list
                        reservationsAdapter.updateReservations(masterReservationList);
                    });
                }
            } else {
                db.collection(COLLECTION_USERS).document(userId)
                        .get()
                        .addOnSuccessListener(userDocumentSnapshot -> {
                            User user = userDocumentSnapshot.toObject(User.class);
                            String fullUserName = reservation.getUserName() != null ? reservation.getUserName() : "Utilisateur Inconnu";
                            String userEmail = "Email Non Trouvé";
                            String userPhone = "Tél. Non Trouvé";

                            if (user != null) {
                                fullUserName = user.getNom() + " " + user.getPrenom();
                                userEmail = user.getEmail();
                                userPhone = user.getTelephone();
                                userCache.put(userId, user);
                            } else {
                                Log.w(TAG, "Utilisateur non trouvé dans la collection 'users' pour userId: " + userId);
                            }

                            finalDisplayList.add(new ReservationDisplayModel(
                                    reservation.getReservationNumber(),
                                    fullUserName,
                                    reservation.getCarName(),
                                    reservation.getStartDate(),
                                    reservation.getEndDate(),
                                    userEmail,
                                    userPhone,
                                    reservation.getStatus(),
                                    reservation.getTotalPrice()
                            ));

                            if (completedFetches.incrementAndGet() == rawReservations.size()) {
                                runOnUiThread(() -> {
                                    masterReservationList = new ArrayList<>(finalDisplayList); // Mise à jour de la master list
                                    reservationsAdapter.updateReservations(masterReservationList);
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Erreur lors de la récupération de l'utilisateur " + userId + ": " + e.getMessage());
                            finalDisplayList.add(new ReservationDisplayModel(
                                    reservation.getReservationNumber(),
                                    reservation.getUserName() != null ? reservation.getUserName() : "Erreur Utilisateur",
                                    reservation.getCarName(),
                                    reservation.getStartDate(),
                                    reservation.getEndDate(),
                                    "Erreur Email", "Erreur Tél.",
                                    reservation.getStatus(),
                                    reservation.getTotalPrice()
                            ));
                            if (completedFetches.incrementAndGet() == rawReservations.size()) {
                                runOnUiThread(() -> {
                                    masterReservationList = new ArrayList<>(finalDisplayList); // Mise à jour de la master list
                                    reservationsAdapter.updateReservations(masterReservationList);
                                });
                            }
                        });
            }
        }
    }


    private void loadTotalReservationsCount() {
        tvTotalCount.setText("...");
        db.collection(COLLECTION_RESERVATIONS)
                .get()
                .addOnSuccessListener(querySnapshot -> tvTotalCount.setText(String.valueOf(querySnapshot.size())))
                .addOnFailureListener(e -> {
                    tvTotalCount.setText("Err");
                    Log.e(TAG, "Erreur total:", e);
                });
    }

    private void loadConfirmedReservationsCount() {
        if (tvConfirmedCount == null) return;
        tvConfirmedCount.setText("...");
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("status", "acceptée")
                .get()
                .addOnSuccessListener(querySnapshot -> tvConfirmedCount.setText(String.valueOf(querySnapshot.size())))
                .addOnFailureListener(e -> {
                    tvConfirmedCount.setText("Err");
                    Log.e(TAG, "Erreur Confirmées:", e);
                });
    }

    private void loadPendingReservationsCount() {
        if (tvPendingCount == null) return;
        tvPendingCount.setText("...");
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("status", "En attente")
                .get()
                .addOnSuccessListener(querySnapshot -> tvPendingCount.setText(String.valueOf(querySnapshot.size())))
                .addOnFailureListener(e -> {
                    tvPendingCount.setText("Err");
                    Log.e(TAG, "Erreur En attente:", e);
                });
    }

    private void loadCancelledReservationsCount() {
        if (tvCancelledCount == null) return;
        tvCancelledCount.setText("...");
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("status", "refusée")
                .get()
                .addOnSuccessListener(querySnapshot -> tvCancelledCount.setText(String.valueOf(querySnapshot.size())))
                .addOnFailureListener(e -> {
                    tvCancelledCount.setText("Err");
                    Log.e(TAG, "Erreur Annulées:", e);
                });
    }

    private void loadCompletedReservationsCount() {
        // Cette fonction doit rester commentée si l'ID tv_completed_count n'existe pas dans le XML.
        /*
        if (tvCompletedCount == null) return;
        tvCompletedCount.setText("...");
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("status", "Terminée")
                .get()
                .addOnSuccessListener(querySnapshot -> tvCompletedCount.setText(String.valueOf(querySnapshot.size())))
                .addOnFailureListener(e -> {
                    tvCompletedCount.setText("Err");
                    Log.e(TAG, "Erreur Terminées:", e);
                });
        */
    }

}