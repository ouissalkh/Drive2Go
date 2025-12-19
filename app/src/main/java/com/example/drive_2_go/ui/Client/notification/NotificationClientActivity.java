package com.example.drive_2_go.ui.Client.notification;

// Imports (inchangés)
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.NotificationModel;
import com.example.drive_2_go.ui.Client.description.DescriptionCarActivity;
import com.example.drive_2_go.ui.adapter.NotificationsAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationClientActivity extends AppCompatActivity
        implements NotificationsAdapter.OnCarNameClickListener {

    private static final String TAG = "NotificationsActivity";

    private ImageView btnBack;
    private RecyclerView rvNotifications;
    private NotificationsAdapter adapter;
    private List<NotificationModel> notificationList;
    private FirebaseFirestore db;

    private FirebaseAuth mAuth;
    private String currentUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications_client);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Log.e(TAG, "Utilisateur non connecté. Ne peut pas charger les notifications.");
            Toast.makeText(this, "Veuillez vous connecter pour voir les notifications.", Toast.LENGTH_LONG).show();
        }

        rvNotifications = findViewById(R.id.rv_notifications_list);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        notificationList = new ArrayList<>();

        adapter = new NotificationsAdapter(this, notificationList, this);
        rvNotifications.setAdapter(adapter);
        // Initialisation et listener pour le bouton de retour
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        if (!currentUserId.isEmpty()) {
            fetchNotifications();
        }
    }

    /**
     * Récupère les notifications pour l'utilisateur connecté et les alertes globales.
     */
    private void fetchNotifications() {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Erreur: Utilisateur non identifié.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Requête A : Statuts de Réservation (Personnel)
        Task<QuerySnapshot> reservationsTask = db.collection("reservations")
                .whereEqualTo("userId", currentUserId)
                .whereIn("status", List.of("acceptée", "refusée"))
                .get();

        // Requête B : Alertes Spécifiques à l'Utilisateur (Personnel)
        Task<QuerySnapshot> userAlertsTask = db.collection("user_alerts")
                .whereEqualTo("userId", currentUserId)
                .get();

        // 🆕 Requête C : Alertes Globales (Nouvelles Voitures)
        // La collection est 'global_alerts' et nous filtrons sur le type spécifique.
        Task<QuerySnapshot> globalAlertsTask = db.collection("global_alerts")
                .whereEqualTo("type", "New_Car_Added")
                .get();

        // Combinaison des TROIS requêtes
        // Utilisation de Tasks.whenAllSuccess() pour obtenir une List<QuerySnapshot>
        Tasks.whenAllSuccess(reservationsTask, userAlertsTask, globalAlertsTask)
                .addOnCompleteListener(new OnCompleteListener<List<Object>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Object>> task) {
                        if (task.isSuccessful()) {
                            // Récupération des résultats A, B et C
                            // L'ordre correspond à l'ordre dans whenAllSuccess
                            List<Object> results = task.getResult();

                            // Cast des résultats
                            QuerySnapshot reservationsResult = (QuerySnapshot) results.get(0);
                            QuerySnapshot userAlertsResult = (QuerySnapshot) results.get(1);
                            QuerySnapshot globalAlertsResult = (QuerySnapshot) results.get(2);

                            notificationList.clear();
                            List<Task<Void>> updateTasks = new ArrayList<>();

                            // Traitement des résultats des Réservations (A)
                            for (DocumentSnapshot doc : reservationsResult.getDocuments()) {
                                NotificationModel model = convertReservationToNotification(doc);
                                notificationList.add(model);
                                // Marquer comme lu
                                if (model.isClientRead() == false) {
                                    updateTasks.add(doc.getReference().update("clientRead", true));
                                }
                            }

                            // Traitement des résultats des Alertes Spécifiques (B)
                            for (DocumentSnapshot doc : userAlertsResult.getDocuments()) {
                                NotificationModel model = convertAlertToNotification(doc);
                                notificationList.add(model);
                                // Marquer comme lu
                                if (model.isClientRead() == false) {
                                    updateTasks.add(doc.getReference().update("clientRead", true));
                                }
                            }

                            // 🆕 Traitement des Alertes Globales (C)
                            for (DocumentSnapshot doc : globalAlertsResult.getDocuments()) {
                                NotificationModel model = convertNewCarAlertToNotification(doc);
                                notificationList.add(model);
                                // Les alertes globales ne sont PAS marquées comme lues
                                // car elles s'appliquent à tous et n'ont pas de champ 'isRead' par utilisateur.
                            }

                            // --- Trier la liste par date (Décroissant : plus récent en premier) ---
                            notificationList.sort((n1, n2) -> {
                                // 1. Définir la date de référence pour N1
                                // Priorité : Heure de confirmation Admin > Timestamp de création
                                Timestamp t1 = n1.getTimeConfirmationAdmin();
                                if (t1 == null) t1 = n1.getTimestamp();

                                // 2. Définir la date de référence pour N2
                                Timestamp t2 = n2.getTimeConfirmationAdmin();
                                if (t2 == null) t2 = n2.getTimestamp();

                                // 3. Gestion de la sécurité (null check final)
                                if (t1 == null && t2 == null) return 0;
                                if (t1 == null) return 1;
                                if (t2 == null) return -1;

                                // 4. Tri DESCENDANT (t2 comparé à t1 pour avoir le plus récent en haut)
                                return t2.compareTo(t1);
                            });

// Rafraîchir l'UI
                            adapter.notifyDataSetChanged();

                            // Mettre à jour les statuts 'isRead' (uniquement pour A et B)
                            if (!updateTasks.isEmpty()) {
                                Tasks.whenAll(updateTasks)
                                        .addOnFailureListener(e -> Log.e(TAG, "Erreur lors du marquage comme lu", e));
                            }

                        } else {
                            Log.e(TAG, "Erreur lors du chargement des notifications combinées: ", task.getException());
                            Toast.makeText(NotificationClientActivity.this, "Impossible de charger les notifications.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Gère le clic sur le nom de la voiture.
     */
    @Override
    public void onCarNameClick(String carId) {
        Intent intent = new Intent(NotificationClientActivity.this, DescriptionCarActivity.class);
        intent.putExtra("CAR_ID", carId);
        startActivity(intent);

        Log.d(TAG, "Redirection vers les détails de la voiture ID: " + carId);
    }

    // --- Méthodes de Conversion (Inchangées) ---

    private NotificationModel convertReservationToNotification(DocumentSnapshot doc) {
        String status = doc.getString("status");
        String carName = doc.getString("carName") != null ? doc.getString("carName") : "Véhicule";
        String carId = doc.getString("carId") != null ? doc.getString("carId") : "";
        Timestamp timeConfirmationAdmin = doc.getTimestamp("timeConfirmationAdmin");

        String title;
        String message;

        if ("acceptée".equals(status)) {
            title = "Réservation Confirmée !";
            message = "Votre location de la " + carName + " est officiellement confirmée.";
        } else if ("refusée".equals(status)) {
            title = "Réservation Annulée";
            message = "La demande de location de la " + carName + " a été annulée.";
        } else {
            title = "Mise à jour de statut";
            message = "Un changement de statut est survenu concernant la location de la " + carName + ".";
        }

        Boolean clientRead = doc.getBoolean("clientRead");
        if (clientRead == null) clientRead = false;

        return new NotificationModel(
                doc.getId(),
                title,
                message,
                doc.getTimestamp("timestamp"),
                clientRead,
                carId,
                timeConfirmationAdmin
        );
    }


    private NotificationModel convertAlertToNotification(DocumentSnapshot doc) {
        String type = doc.getString("type");
        String title = doc.getString("title");
        String message = doc.getString("message");
        String carId = doc.getString("carId") != null ? doc.getString("carId") : "";
        String carName = doc.getString("carName") != null ? doc.getString("carName") : "Véhicule"; // Assurez-vous d'inclure 'carName' dans user_alerts

        Boolean clientRead = doc.getBoolean("clientRead");
        if (clientRead == null) clientRead = false;

        // ⭐️ LOGIQUE DE FIN DE LOCATION ⭐️
        if ("RESERVATION_ENDING".equals(type)) {
            title = "Location Expirée / À Rendre";
            // Message doit inclure le nom de la voiture pour le clic
            message = "Le temps de location pour la " + carName + " a expiré. Veuillez procéder au retour.";
        }

        // Si ce n'est pas "RESERVATION_ENDING", on utilise les champs title/message de Firestore.

        return new NotificationModel(
                doc.getId(),
                title,
                message,
                doc.getTimestamp("timestamp"),
                clientRead,
                carId,
                // ⭐️ AJOUT DE NULL pour timeConfirmationAdmin ⭐️
                null
        );
    }

// 🆕 Méthode de Conversion pour les alertes de Nouvelle Voiture
// Dans NotificationClientActivity.java

    private NotificationModel convertNewCarAlertToNotification(DocumentSnapshot doc) {
        String carName = doc.getString("carName") != null ? doc.getString("carName") : "Nouvelle voiture"; // Assurez-vous d'avoir 'carName' dans votre alerte globale Firestore
        String carId = doc.getString("carId") != null ? doc.getString("carId") : "";
        Timestamp timestamp = doc.getTimestamp("timestamp");

        // ⭐️ Titre et Message Fixes / Améliorés ⭐️
        String title = "Nouvelle Voiture Ajoutée !";
        // Message doit permettre d'isoler le nom de la voiture
        String message = "Une nouvelle voiture a été ajoutée : " + carName + ". Découvrez ses détails !";

        Timestamp finalTimestamp = timestamp != null ? timestamp : Timestamp.now();
        boolean clientRead = true;

        return new NotificationModel(
                doc.getId(),
                title,
                message,
                finalTimestamp,
                clientRead,
                carId, // carId est essentiel pour le clic
                // ⭐️ AJOUT DE NULL pour timeConfirmationAdmin ⭐️
                null
        );
    }
}