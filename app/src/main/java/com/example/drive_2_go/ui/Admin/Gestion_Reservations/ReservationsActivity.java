package com.example.drive_2_go.ui.Admin.Gestion_Reservations;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView; // Import important
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Admin.ComposantCommunAdmin.BaseAdminActivity;
import com.example.drive_2_go.data.model.Reservation;
import com.example.drive_2_go.data.model.User;
import com.example.drive_2_go.data.model.ReservationDisplayModel;
import com.example.drive_2_go.ui.adapter.ReservationsAdapter;
import com.google.android.material.textfield.MaterialAutoCompleteTextView; // Import important
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ReservationsActivity extends BaseAdminActivity {

    private RecyclerView recyclerView;
    private ReservationsAdapter adapter;
    private List<ReservationDisplayModel> masterList = new ArrayList<>(); // Contient TOUTES les données
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Déclaration des TextViews pour les statistiques
    private TextView tvTotal, tvConfirmed, tvPending, tvCancelled;

    // Déclaration pour les filtres
    private SearchView searchView;
    private MaterialAutoCompleteTextView statusDropdown;
    private String currentSearchText = "";
    private String currentStatusFilter = "Tous les statuts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservations);

        // Liaison des vues statistiques
        tvTotal = findViewById(R.id.tv_total_count);
        tvConfirmed = findViewById(R.id.tv_confirmed_count);
        tvPending = findViewById(R.id.tv_pending_count);
        tvCancelled = findViewById(R.id.tv_cancelled_count);

        // Liaison des vues de filtre
        searchView = findViewById(R.id.searchView);
        statusDropdown = findViewById(R.id.autoCompleteTextView);

        // Configuration du RecyclerView
        recyclerView = findViewById(R.id.recyclerViewReservations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReservationsAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // 1. Initialiser les filtres (Dropdown et Recherche)
        setupFilters();

        // 2. Chargement des données
        loadReservations();
    }

    private void setupFilters() {
        // --- A. Configuration du Menu Déroulant (Dropdown) ---
        String[] statuts = new String[]{"Tous les statuts", "En attente", "Confirmée", "Annulée"};
        ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statuts);
        statusDropdown.setAdapter(dropdownAdapter);

        // Empêcher le clavier de s'ouvrir quand on clique sur le dropdown
        statusDropdown.setKeyListener(null);

        // Action quand on choisit un statut
        statusDropdown.setOnItemClickListener((parent, view, position, id) -> {
            currentStatusFilter = parent.getItemAtPosition(position).toString();
            applyFilters(); // On relance le filtrage
        });

        // --- B. Configuration de la Barre de Recherche ---
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchText = newText.toLowerCase();
                applyFilters(); // On relance le filtrage à chaque lettre tapée
                return true;
            }
        });
    }

    // Méthode principale de filtrage
    private void applyFilters() {
        List<ReservationDisplayModel> filteredList = new ArrayList<>();

        for (ReservationDisplayModel item : masterList) {
            // 1. Vérification du texte (Nom, ID, ou Voiture)
            boolean matchesSearch = item.getUserName().toLowerCase().contains(currentSearchText) ||
                    item.getReservationNumber().toLowerCase().contains(currentSearchText) ||
                    item.getCarName().toLowerCase().contains(currentSearchText);

            // 2. Vérification du statut
            boolean matchesStatus = true;
            String itemStatus = item.getStatus() != null ? item.getStatus().toLowerCase() : "";

            if (!currentStatusFilter.equals("Tous les statuts")) {
                if (currentStatusFilter.equals("Confirmée")) {
                    matchesStatus = itemStatus.contains("accept") || itemStatus.contains("confirm");
                } else if (currentStatusFilter.equals("Annulée")) {
                    matchesStatus = itemStatus.contains("refus") || itemStatus.contains("annul");
                } else if (currentStatusFilter.equals("En attente")) {
                    matchesStatus = itemStatus.contains("attente") || itemStatus.contains("pending");
                }
            }

            // Si les deux conditions sont vraies, on ajoute à la liste affichée
            if (matchesSearch && matchesStatus) {
                filteredList.add(item);
            }
        }

        adapter.updateReservations(filteredList);
    }

    private void loadReservations() {
        db.collection("reservations").get().addOnSuccessListener(querySnapshot -> {

            // --- 1. Partie Statistiques ---
            int totalCount = querySnapshot.size();
            int confirmedCount = 0;
            int pendingCount = 0;
            int cancelledCount = 0;

            for (QueryDocumentSnapshot doc : querySnapshot) {
                Reservation res = doc.toObject(Reservation.class);
                if (res.getStatus() != null) {
                    String status = res.getStatus().toLowerCase().trim();
                    if (status.contains("confirm") || status.contains("accept")) {
                        confirmedCount++;
                    } else if (status.contains("attente") || status.contains("pending")) {
                        pendingCount++;
                    } else if (status.contains("annul") || status.contains("refus") || status.contains("cancel")) {
                        cancelledCount++;
                    }
                }
            }

            tvTotal.setText(String.valueOf(totalCount));
            tvConfirmed.setText(String.valueOf(confirmedCount));
            tvPending.setText(String.valueOf(pendingCount));
            tvCancelled.setText(String.valueOf(cancelledCount));

            // --- 2. Partie Liste ---
            List<ReservationDisplayModel> tempList = new ArrayList<>();

            if (querySnapshot.isEmpty()) {
                adapter.updateReservations(new ArrayList<>());
                return;
            }

            for (QueryDocumentSnapshot doc : querySnapshot) {
                Reservation res = doc.toObject(Reservation.class);
                String resId = doc.getId().substring(0, Math.min(doc.getId().length(), 8)).toUpperCase();

                if (res.getUserId() != null) {
                    db.collection("users").document(res.getUserId()).get().addOnSuccessListener(userDoc -> {
                        User user = userDoc.toObject(User.class);
                        String fullName = (user != null) ? user.getNom() + " " + user.getPrenom() : "Inconnu";
                        String email = (user != null) ? user.getEmail() : "";
                        String phone = (user != null) ? user.getTelephone() : "";

                        tempList.add(new ReservationDisplayModel(
                                resId, fullName, res.getCarName(), res.getStartDate(), res.getEndDate(),
                                email, phone, res.getStatus(), res.getTotalPrice()
                        ));

                        checkAndUpdateList(tempList, querySnapshot.size());

                    }).addOnFailureListener(e -> {
                        tempList.add(new ReservationDisplayModel(resId, "Erreur User", res.getCarName(), res.getStartDate(), res.getEndDate(), "", "", res.getStatus(), res.getTotalPrice()));
                        checkAndUpdateList(tempList, querySnapshot.size());
                    });
                } else {
                    tempList.add(new ReservationDisplayModel(resId, "Sans User ID", res.getCarName(), res.getStartDate(), res.getEndDate(), "", "", res.getStatus(), res.getTotalPrice()));
                    checkAndUpdateList(tempList, querySnapshot.size());
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Erreur chargement : " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Petite fonction utilitaire pour éviter la duplication de code
    private void checkAndUpdateList(List<ReservationDisplayModel> tempList, int totalExpected) {
        if (tempList.size() == totalExpected) {
            masterList = new ArrayList<>(tempList); // Sauvegarde la liste complète
            applyFilters(); // Applique les filtres (par défaut "Tous") et affiche
        }
    }
}
