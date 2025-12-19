package com.example.drive_2_go.ui.Client.history;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Client.accueil.AccueilActivity;
import com.example.drive_2_go.ui.Client.favoris.Favoris;
import com.example.drive_2_go.ui.Client.profil.Profil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.drive_2_go.ui.Client.description.DescriptionCarActivity;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private ImageButton buttonProfil;
    private ImageButton buttonHome;
    private ImageButton buttonFavoris;
    private ImageButton buttonHistory;
    private ImageView btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId = "";

    private static final boolean IS_CAR_AVAILABLE_SIMULATION = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialiser Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            this.currentUserId = currentUser.getUid(); // <-- Utilisation de l'ID réel
        } else {
            // Gérer le cas où l'utilisateur n'est pas connecté
            Toast.makeText(this, "Erreur: Utilisateur non connecté.", Toast.LENGTH_LONG).show();
            // Optionnel : rediriger vers l'écran de connexion
            // finish();
            return;
        }

        buttonProfil = findViewById(R.id.buttonProfil);
        buttonHome = findViewById(R.id.buttonHome);
        buttonFavoris = findViewById(R.id.buttonFavoris);
        buttonHistory = findViewById(R.id.buttonHistory);

        // Initialisation et listener pour le bouton de retour
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        buttonProfil.setOnClickListener(v -> openProfil());
        buttonFavoris.setOnClickListener(v -> openFavoris());
        buttonHistory.setOnClickListener(v -> Toast.makeText(this,"Déjà ici", Toast.LENGTH_SHORT).show());
        buttonHome.setOnClickListener(v -> openAccueil());
        selectButton(buttonHistory);

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.rv_history_listings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<ReservationHistoryItem> historyList = new ArrayList<>();
        adapter = new HistoryAdapter(this, historyList);
        recyclerView.setAdapter(adapter);


        loadHistoryData();
    }

    /**
     * Charge toutes les réservations de l'utilisateur depuis Firestore
     * et récupère les détails de la voiture (image) pour chaque réservation.
     */
    private void loadHistoryData() {


        db.collection("reservations")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timeReservationClient", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> reservationDocs = task.getResult().getDocuments();
                        List<ReservationHistoryItem> newHistoryList = new ArrayList<>();

                        if (reservationDocs.isEmpty()) {
                            adapter.updateData(newHistoryList); // Afficher une liste vide
                            return;
                        }

                        // Compteur pour savoir quand toutes les requêtes de voitures sont terminées
                        final int totalReservations = reservationDocs.size();
                        final int[] loadedCarCount = {0};

                        for (DocumentSnapshot reservationDoc : reservationDocs) {
                            String carId = reservationDoc.getString("carId");

                            if (carId != null && !carId.isEmpty()) {
                                // Deuxième requête: Récupérer les détails de la voiture
                                db.collection("cars").document(carId).get()
                                        .addOnSuccessListener(carDoc -> {
                                            // On s'assure que le document voiture existe
                                            if (carDoc.exists()) {
                                                String imageUrl = carDoc.getString("imageUrl"); // Assurez-vous que le champ est 'imageUrl' dans Cars

                                                // Créer l'élément après avoir l'imageUrl
                                                ReservationHistoryItem item = convertDocumentToHistoryItem(reservationDoc, imageUrl);
                                                newHistoryList.add(item);
                                            } else {
                                                // Créer l'élément avec une image null si la voiture n'existe plus
                                                Log.w(TAG, "Voiture introuvable pour carId: " + carId);
                                                ReservationHistoryItem item = convertDocumentToHistoryItem(reservationDoc, null);
                                                newHistoryList.add(item);
                                            }

                                            // Vérifier si toutes les données sont chargées
                                            loadedCarCount[0]++;
                                            if (loadedCarCount[0] == totalReservations) {
                                                adapter.updateData(newHistoryList);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Erreur lors du chargement de la voiture: ", e);
                                            // Créer l'élément avec une image null en cas d'erreur
                                            ReservationHistoryItem item = convertDocumentToHistoryItem(reservationDoc, null);
                                            newHistoryList.add(item);

                                            loadedCarCount[0]++;
                                            if (loadedCarCount[0] == totalReservations) {
                                                adapter.updateData(newHistoryList);
                                            }
                                        });
                            } else {
                                // Cas où carId est manquant dans la réservation
                                ReservationHistoryItem item = convertDocumentToHistoryItem(reservationDoc, null);
                                newHistoryList.add(item);

                                loadedCarCount[0]++;
                                if (loadedCarCount[0] == totalReservations) {
                                    adapter.updateData(newHistoryList);
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Erreur lors du chargement de l'historique: ", task.getException());

                    }
                });
    }

    /**
     * Convertit un DocumentSnapshot de réservation en ReservationHistoryItem.
     * @param doc Le document de la réservation.
     * @param imageUrl L'URL de l'image récupérée de la collection Cars.
     */
    private ReservationHistoryItem convertDocumentToHistoryItem(DocumentSnapshot doc, String imageUrl) {
        // Cette logique doit correspondre aux champs de votre BDD
        String startDate = doc.getString("startDate");
        String endDate = doc.getString("endDate");
        String status = doc.getString("status");
        Double price = doc.getDouble("totalPrice");
        String carId = doc.getString("carId");
        String carName = doc.getString("carName");
        // Suppression de l'ancienne ligne: String imageUrl = doc.getString("carImageUrl");

        // On considère qu'une voiture n'est relouable que si elle a été "Retournée"
        boolean isRelouable = "Retournée".equals(status);

        // Utilisez le nom et l'ID du document pour la relouer
        return new ReservationHistoryItem(
                carId,
                carName,
                imageUrl, // <-- Utilise l'URL passée en paramètre
                String.format(Locale.getDefault(), "Du %s au %s", startDate, endDate),
                String.format(Locale.getDefault(), "%.0f dh", price != null ? price : 0.0),
                status,
                isRelouable
        );
    }


    /**
     * Tente d'initier une nouvelle réservation (Relouer).
     * Crée un nouveau document de réservation avec le statut "En attente de validation".
     * @param oldItem L'élément de l'historique à relouer.
     */
    private void attemptReReservation(ReservationHistoryItem oldItem) {
        if (!IS_CAR_AVAILABLE_SIMULATION) {
            showCustomErrorToast("VOITURE NON DISPONIBLE ACTUELLEMENT.");
            return;
        }

        // Simuler la création d'une nouvelle réservation
        Map<String, Object> newReservation = new HashMap<>();
        newReservation.put("userId", currentUserId);
        newReservation.put("carId", oldItem.carId);
        newReservation.put("carName", oldItem.carName);

        // ATTENTION : Pour une vraie relocation, ces dates et prix devraient
        // être saisis par l'utilisateur ou simulés pour l'exemple.
        newReservation.put("startDate", "NOUVELLE DATE DEBUT (simulée)");
        newReservation.put("endDate", "NOUVELLE DATE FIN (simulée)");
        newReservation.put("totalPrice", 0.0); // Prix non connu avant la sélection des dates

        newReservation.put("status", "En attente de validation"); // Statut clé
        newReservation.put("timestamp", Timestamp.now());
        newReservation.put("isRead", true); // L'utilisateur est sur l'écran, donc c'est lu

        db.collection("reservations").add(newReservation)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Nouvelle réservation ajoutée: " + documentReference.getId());

                    // Mettre à jour immédiatement l'interface utilisateur pour afficher la nouvelle entrée
                    Toast.makeText(HistoryActivity.this, "Demande de location pour " + oldItem.carName + " envoyée. En attente de validation.", Toast.LENGTH_LONG).show();

                    // Recharge les données pour afficher la nouvelle entrée en haut
                    loadHistoryData();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors de la création de la nouvelle réservation: ", e);
                    showCustomErrorToast("ÉCHEC DE LA DEMANDE DE RELOCATION.");
                });
    }


    /**
     * Affiche un Toast personnalisé (nécessite le layout custom_error_toast.xml).
     */
    private void showCustomErrorToast(String message) {

    }

    /**
     * Lance la DescriptionCarActivity en chargeant d'abord tous les détails de la voiture
     * depuis la collection 'cars' en utilisant le carId.
     */
    public void openCarDescription(String carId) {
        if (carId == null || carId.isEmpty()) {
            Toast.makeText(this, "ID de voiture manquant.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Afficher un message de chargement (optionnel)
        Toast.makeText(this, "Chargement des détails de la voiture...", Toast.LENGTH_SHORT).show();

        // Récupérer le document Car complet depuis Firestore
        db.collection("cars").document(carId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Tenter de convertir le DocumentSnapshot en objet Car.
                        // NOTE : Assurez-vous que votre classe 'Car' a un constructeur public sans arguments
                        // et des setters/champs publics pour que .toObject(Car.class) fonctionne.
                        // Importez la classe Car si ce n'est pas déjà fait : com.example.drive_2_go.data.model.Car
                        com.example.drive_2_go.data.model.Car car = documentSnapshot.toObject(com.example.drive_2_go.data.model.Car.class);

                        if (car != null) {
                            // Lancer DescriptionCarActivity avec l'objet Car complet
                            Intent intent = new Intent(HistoryActivity.this, com.example.drive_2_go.ui.Client.description.DescriptionCarActivity.class);
                            // Utiliser la même clé d'extra que dans AccueilActivity
                            intent.putExtra(com.example.drive_2_go.ui.Client.description.DescriptionCarActivity.EXTRA_CAR, car);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Erreur de conversion des données de la voiture.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Voiture introuvable dans la base de données.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors de l'ouverture de la description de la voiture:", e);
                    Toast.makeText(this, "Erreur de connexion : impossible de charger les détails.", Toast.LENGTH_SHORT).show();
                });
    }

    // ***************************************************************
    // CLASSE DE MODÈLE DE DONNÉES
    // ***************************************************************
    public static class ReservationHistoryItem {
        public String carId;
        public String carName;
        public String imageUrl;
        public String datesPeriod;
        public String price;
        public String status;
        public boolean isRelouable;

        public ReservationHistoryItem(String carId, String carName, String imageUrl, String datesPeriod, String price, String status, boolean isRelouable) {
            this.carId = carId;
            this.carName = carName;
            this.imageUrl = imageUrl;
            this.datesPeriod = datesPeriod;
            this.price = price;
            this.status = status;
            this.isRelouable = isRelouable;
        }
    }


    // ***************************************************************
    // ADAPTER POUR LA RECYCLERVIEW
    // ***************************************************************
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<ReservationHistoryItem> mData;
        private final Context mContext;

        HistoryAdapter(Context context, List<ReservationHistoryItem> data) {
            this.mContext = context;
            this.mData = data;
        }

        public void updateData(List<ReservationHistoryItem> newData) {
            this.mData = newData;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_listing, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReservationHistoryItem item = mData.get(position);

            holder.tvCarName.setText(item.carName);
            holder.tvDates.setText(item.datesPeriod);
            holder.tvFinalPrice.setText(item.price);
            holder.tvStatus.setText(item.status);

            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Glide.with(mContext)
                        .load(item.imageUrl)
                        // Utilisez un placeholder si l'image n'est pas chargée (ex: R.drawable.car)
                        .placeholder(R.drawable.car)
                        .into(holder.imgCar);
            } else {
                // Afficher une image par défaut ou masquer l'ImageView
                holder.imgCar.setImageResource(R.drawable.car);
            }

            // Gestion du bouton "Relouer"
            if (item.isRelouable) {
                holder.btnRepeat.setVisibility(View.VISIBLE);
                // Le clic appelle la méthode de l'Activity en passant l'item complet
                holder.btnRepeat.setOnClickListener(v -> ((HistoryActivity) mContext).attemptReReservation(item));
            } else {
                holder.btnRepeat.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                // Appeler la méthode de l'Activity pour charger et ouvrir la description
                ((HistoryActivity) mContext).openCarDescription(item.carId);
            });
            // *****************************************************************
            // LOGIQUE MISE À JOUR : Gérer le statut "En attente"
            // *****************************************************************
            String status = item.status;

            if ("Retournée".equals(status)) {
                holder.tvStatus.setTextColor(Color.parseColor("#FF8C00"));
            } else if ("refusée".equals(status)) {
                holder.tvStatus.setTextColor(Color.RED);
            } else if ("acceptée".equals(status)) {
                holder.tvStatus.setTextColor(Color.GREEN);
            } else if ("En attente".equals(status)) {
                holder.tvStatus.setTextColor(Color.parseColor("#cccc00"));
            } else {
                holder.tvStatus.setTextColor(ContextCompat.getColor(mContext, R.color.black));
            }
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCarName;
            TextView tvDates;
            TextView tvFinalPrice;
            TextView tvStatus;
            Button btnRepeat;
            ImageView imgCar;

            public ViewHolder(View itemView) {
                super(itemView);
                tvCarName = itemView.findViewById(R.id.tv_car_name_history);
                tvDates = itemView.findViewById(R.id.tv_dates_history);
                tvFinalPrice = itemView.findViewById(R.id.tv_final_price);
                tvStatus = itemView.findViewById(R.id.tv_status);
                btnRepeat = itemView.findViewById(R.id.btn_repeat_reservation);
                imgCar = itemView.findViewById(R.id.img_car_history);
            }
        }
    }

    private void selectButton(ImageButton button) {
        button.setSelected(true);
    }

    private void openProfil(){
        startActivity(new Intent(HistoryActivity.this, Profil.class));
    }

    private void openAccueil(){
        startActivity(new Intent(HistoryActivity.this, AccueilActivity.class));
    }
    private  void openFavoris (){
        startActivity(new Intent(HistoryActivity.this, Favoris.class));
    }
}