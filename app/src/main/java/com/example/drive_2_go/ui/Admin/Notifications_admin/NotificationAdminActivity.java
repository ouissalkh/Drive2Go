package com.example.drive_2_go.ui.Admin.Notifications_admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.Reservation;
import com.example.drive_2_go.ui.Admin.DescriptionCarAdmin.DecripCarAdminActivity; // IMPORTANT
import com.example.drive_2_go.ui.Admin.ProfilUser.ProfilUserActivity;
import com.example.drive_2_go.ui.adapter.NotificationAdminAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationAdminActivity extends AppCompatActivity implements NotificationAdminAdapter.OnActionButtonClickListener {

    private RecyclerView recyclerViewNotifications;
    private NotificationAdminAdapter adapter;
    private List<Reservation> reservationList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_admin);

        initCloudinary(); // Init Cloudinary

        db = FirebaseFirestore.getInstance();

        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));

        reservationList = new ArrayList<>();
        adapter = new NotificationAdminAdapter(this, reservationList, this);
        recyclerViewNotifications.setAdapter(adapter);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        loadPendingReservations();
    }

    private void initCloudinary() {
        try {
            MediaManager.get();
        } catch (IllegalStateException e) {
            try {
                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", "datr9fmfp");
                config.put("api_key", "953344295627375");
                config.put("api_secret", "jPnIjBzEtR8Z2H6jLVbwNqCrhjc");
                MediaManager.init(this, config);
            } catch (Exception ex) {
                Log.e("NotifActivity", "Erreur init Cloudinary", ex);
            }
        }
    }

    private void loadPendingReservations() {
        db.collection("reservations")
                //.whereEqualTo("status", "En attente") // Décommentez pour filtrer
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Reservation> tempList = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Reservation r = document.toObject(Reservation.class);
                            if (r != null) {
                                // Filtre manuel ou via query
                                if ("En attente".equals(r.getStatus()) || "En attente de validation".equals(r.getStatus())) {
                                    r.setReservationNumber(document.getId());
                                    tempList.add(r);
                                }
                            }
                        }
                        adapter.updateReservations(tempList);
                    }
                });
    }

    // --- C'EST ICI QUE CA SE JOUE ---
    @Override
    public void onCarNameClick(Reservation reservation) {
        // Redirection vers l'activité Admin de description
        Intent intent = new Intent(this, DecripCarAdminActivity.class);

        if (reservation.getCarId() != null) {
            intent.putExtra("CAR_ID", reservation.getCarId());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Erreur : Pas d'ID de voiture", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUserNameClick(Reservation reservation) {
        Intent intent = new Intent(this, ProfilUserActivity.class);
        if (reservation.getUserId() != null) {
            intent.putExtra("USER_ID", reservation.getUserId());
            intent.putExtra("USER_NAME", reservation.getUserName());
            startActivity(intent);
        }
    }

    @Override
    public void onAcceptClick(Reservation reservation, int position) {
        updateStatus(reservation.getReservationNumber(), "acceptée", position);
    }

    @Override
    public void onCancelClick(Reservation reservation, int position) {
        updateStatus(reservation.getReservationNumber(), "refusée", position);
    }

    private void updateStatus(String docId, String newStatus, int position) {
        if (docId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("timeConfirmationAdmin", FieldValue.serverTimestamp());

        db.collection("reservations").document(docId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (position >= 0 && position < reservationList.size()) {
                        reservationList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, reservationList.size());
                    } else {
                        loadPendingReservations();
                    }
                    Toast.makeText(this, "Réservation " + newStatus, Toast.LENGTH_SHORT).show();
                });
    }
}