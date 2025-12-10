package com.example.drive_2_go.ui.Client.favoris;

import com.example.drive_2_go.ui.Client.login.LoginActivity;
import com.example.drive_2_go.ui.adapter.FavoritesAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.Car;
import com.example.drive_2_go.ui.Client.accueil.AccueilActivity;
import com.example.drive_2_go.ui.Client.history.HistoryActivity;
import com.example.drive_2_go.ui.Client.profil.Profil;

import java.util.ArrayList;
import java.util.List;

import com.example.drive_2_go.data.model.User;
import com.example.drive_2_go.ui.Client.description.DescriptionCarActivity; // Pour l'intention de clic
import com.example.drive_2_go.ui.adapter.FavoritesAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class Favoris extends AppCompatActivity{

    private ImageButton buttonProfil;
    private ImageButton buttonHome;
    private ImageButton buttonFavoris;
    private ImageButton buttonHistory;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private User currentUser;
    RecyclerView rvFavorites;
    ImageView ivEmptyImage;
    TextView tvEmpty;
    private ImageView btnBack;

    List<Car> allCars = new ArrayList<>();
    List<Car> favoriteCars = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favoris);

        // Initialisation Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        rvFavorites = findViewById(R.id.rv_favorites);
        ivEmptyImage = findViewById(R.id.iv_empty_image);
        tvEmpty = findViewById(R.id.tv_empty);

        rvFavorites.setLayoutManager(new LinearLayoutManager(this));

        // Initialisation et listener pour le bouton de retour
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        buttonProfil = findViewById(R.id.buttonProfil);
        buttonHome = findViewById(R.id.buttonHome);
        buttonFavoris = findViewById(R.id.buttonFavoris);
        buttonHistory = findViewById(R.id.buttonHistory);

        buttonProfil.setOnClickListener(v -> openProfil());
        buttonHome.setOnClickListener(v -> openAccueil());
        buttonFavoris.setOnClickListener(v -> Toast.makeText(this,"Déjà ici", Toast.LENGTH_SHORT).show());
        buttonHistory.setOnClickListener(v -> openHistory());
        selectButton(buttonFavoris); // si tu es dans Favoris
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ⭐️ Recharger les données des favoris à chaque fois que l'activité est affichée
        loadFavoritesData();
    }

    private void selectButton(ImageButton button) {
        button.setSelected(true);
    }

    private void openProfil(){
        startActivity(new Intent(Favoris.this, Profil.class));
    }

    private void openAccueil(){
        startActivity(new Intent(Favoris.this, AccueilActivity.class));
    }
    private void openHistory(){
        startActivity(new Intent(Favoris.this, HistoryActivity.class));
    }


    // Dans Favoris.java

    private void loadFavoritesData() {
        FirebaseUser fUser = auth.getCurrentUser();

        if (fUser == null) {
            Toast.makeText(this, "Veuillez vous connecter.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // 1. Récupérer l'objet User pour obtenir les favoriteCarIds
        db.collection("users").document(fUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        // ⭐️ Assurez-vous que la liste n'est pas null si elle est vide
                        List<String> favoriteIds = (currentUser != null) ? currentUser.getFavoriteCarIds() : new ArrayList<>();

                        if (!favoriteIds.isEmpty()) {
                            // 2. Si des IDs sont trouvés, charger les voitures
                            fetchFavoriteCars(favoriteIds);
                        } else {
                            // Pas de favoris
                            favoriteCars.clear(); // Vider l'ancienne liste
                            setupRecyclerView(favoriteCars); // Mettre à jour l'adaptateur
                            checkEmptyState(); // ⭐️ Afficher l'état vide
                        }
                    } else {
                        Toast.makeText(this, "Erreur: Profil utilisateur introuvable.", Toast.LENGTH_LONG).show();
                        favoriteCars.clear();
                        checkEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur de chargement des favoris: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    favoriteCars.clear();
                    checkEmptyState();
                });
    }

    private void fetchFavoriteCars(List<String> favoriteIds) {
        // 3. Charger les voitures correspondant aux IDs
        db.collection("cars") // 💡 Assurez-vous que le nom de votre collection de voitures est "cars"
                .whereIn("id", favoriteIds) // Filtrer les voitures par leurs IDs
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    favoriteCars.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Car car = document.toObject(Car.class);
                        favoriteCars.add(car);
                    }

                    // 4. Mettre à jour l'affichage
                    setupRecyclerView(favoriteCars);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur de récupération des voitures: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    favoriteCars.clear();
                    checkEmptyState();
                });
    }

    private void setupRecyclerView(List<Car> cars) {
        // 5. Créer et définir l'adaptateur, en passant la méthode onCarClick en tant que listener
        FavoritesAdapter adapter = new FavoritesAdapter(cars, this::onCarClick);
        rvFavorites.setAdapter(adapter);
        checkEmptyState(); // Vérifier et afficher le RecyclerView
    }

    // 6. Méthode de gestion du clic (implémentation de CarClickListener)
    public void onCarClick(Car car) {
        // Importez DescriptionCarActivity si ce n'est pas déjà fait
        // import com.example.drive_2_go.ui.Client.description.DescriptionCarActivity;

        Intent intent = new Intent(Favoris.this, DescriptionCarActivity.class);
        // Passer l'objet Car. Assurez-vous que Car implémente Serializable ou Parcelable.
        intent.putExtra(DescriptionCarActivity.EXTRA_CAR, car);
        startActivity(intent);
    }

    // Mettre à jour checkEmptyState pour utiliser la liste remplie
    // Dans Favoris.java
    private void checkEmptyState() {
        if (favoriteCars.isEmpty()) {
            rvFavorites.setVisibility(View.GONE);
            ivEmptyImage.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.VISIBLE); // ⭐️ Afficher le texte
            tvEmpty.setText("Aucune voiture favorite trouvée."); // Texte à afficher
        } else {
            rvFavorites.setVisibility(View.VISIBLE);
            ivEmptyImage.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE); // ⭐️ Masquer le texte
        }
    }



}

