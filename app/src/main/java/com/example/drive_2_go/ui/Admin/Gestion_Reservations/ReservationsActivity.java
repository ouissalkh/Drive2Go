package com.example.drive_2_go.ui.Admin.Gestion_Reservations;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Admin.ComposantCommunAdmin.BaseAdminActivity;
import com.example.drive_2_go.data.model.Reservation;
import com.example.drive_2_go.data.model.User;
import com.example.drive_2_go.data.model.ReservationDisplayModel;
import com.example.drive_2_go.ui.adapter.ReservationsAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ReservationsActivity extends BaseAdminActivity {

    private RecyclerView recyclerView;
    private ReservationsAdapter adapter;
    private List<ReservationDisplayModel> masterList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Déclaration des TextViews pour les statistiques
    private TextView tvTotal, tvConfirmed, tvPending, tvCancelled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservations);

        // Liaison des vues statistiques
        tvTotal = findViewById(R.id.tv_total_count);
        tvConfirmed = findViewById(R.id.tv_confirmed_count);
        tvPending = findViewById(R.id.tv_pending_count);
        tvCancelled = findViewById(R.id.tv_cancelled_count);

        // Configuration du RecyclerView
        recyclerView = findViewById(R.id.recyclerViewReservations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReservationsAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Chargement des données
        loadReservations();
    }

    private void loadReservations() {
        db.collection("reservations").get().addOnSuccessListener(querySnapshot -> {

            // --- 1. Partie Statistiques (Compteurs) ---
            int totalCount = querySnapshot.size();
            int confirmedCount = 0;
            int pendingCount = 0;
            int cancelledCount = 0;

            // On parcourt une première fois pour compter (c'est très rapide)
            for (QueryDocumentSnapshot doc : querySnapshot) {
                Reservation res = doc.toObject(Reservation.class);

                if (res.getStatus() != null) {
                    // On met en minuscule pour éviter les problèmes de majuscules (ex: "En attente" vs "en attente")
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

            // Mise à jour de l'affichage des cartes
            tvTotal.setText(String.valueOf(totalCount));
            tvConfirmed.setText(String.valueOf(confirmedCount));
            tvPending.setText(String.valueOf(pendingCount));
            tvCancelled.setText(String.valueOf(cancelledCount));


            // --- 2. Partie Liste (RecyclerView) ---
            List<ReservationDisplayModel> tempList = new ArrayList<>();

            // Si la liste est vide, on arrête là
            if (querySnapshot.isEmpty()) {
                adapter.updateReservations(new ArrayList<>());
                return;
            }

            for (QueryDocumentSnapshot doc : querySnapshot) {
                Reservation res = doc.toObject(Reservation.class);
                String resId = doc.getId().substring(0, Math.min(doc.getId().length(), 8)).toUpperCase();

                // Récupération de l'utilisateur lié pour avoir son nom
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

                        // Quand on a chargé tous les utilisateurs, on met à jour la liste
                        if (tempList.size() == querySnapshot.size()) {
                            masterList = new ArrayList<>(tempList);
                            adapter.updateReservations(masterList);
                        }
                    }).addOnFailureListener(e -> {
                        // En cas d'erreur sur un user, on l'ajoute quand même avec "Erreur User"
                        tempList.add(new ReservationDisplayModel(resId, "Erreur User", res.getCarName(), res.getStartDate(), res.getEndDate(), "", "", res.getStatus(), res.getTotalPrice()));
                        if (tempList.size() == querySnapshot.size()) {
                            masterList = new ArrayList<>(tempList);
                            adapter.updateReservations(masterList);
                        }
                    });
                } else {
                    // Cas rare où il n'y a pas d'ID utilisateur
                    tempList.add(new ReservationDisplayModel(resId, "Sans User ID", res.getCarName(), res.getStartDate(), res.getEndDate(), "", "", res.getStatus(), res.getTotalPrice()));
                    if (tempList.size() == querySnapshot.size()) {
                        masterList = new ArrayList<>(tempList);
                        adapter.updateReservations(masterList);
                    }
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Erreur lors du chargement : " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}