package com.example.drive_2_go.ui.Admin.Gestion_Reservations;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservations);

        recyclerView = findViewById(R.id.recyclerViewReservations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReservationsAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        loadReservations();
    }

    private void loadReservations() {
        db.collection("reservations").get().addOnSuccessListener(querySnapshot -> {
            List<ReservationDisplayModel> tempList = new ArrayList<>();
            for (QueryDocumentSnapshot doc : querySnapshot) {
                Reservation res = doc.toObject(Reservation.class);
                String resId = doc.getId().substring(0, 8).toUpperCase();

                // Récupération de l'utilisateur lié
                db.collection("users").document(res.getUserId()).get().addOnSuccessListener(userDoc -> {
                    User user = userDoc.toObject(User.class);
                    String fullName = (user != null) ? user.getNom() + " " + user.getPrenom() : "Inconnu";

                    tempList.add(new ReservationDisplayModel(
                            resId, fullName, res.getCarName(), res.getStartDate(), res.getEndDate(),
                            (user != null ? user.getEmail() : ""), (user != null ? user.getTelephone() : ""),
                            res.getStatus(), res.getTotalPrice()
                    ));

                    if (tempList.size() == querySnapshot.size()) {
                        masterList = new ArrayList<>(tempList);
                        adapter.updateReservations(masterList);
                    }
                });
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Erreur Firebase", Toast.LENGTH_SHORT).show());
    }
}
