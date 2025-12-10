package com.example.drive_2_go.ui.Client.description;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.drive_2_go.data.model.User;

import com.bumptech.glide.Glide; // Nécessite l'implémentation de Glide
import com.example.drive_2_go.R; // Assurez-vous que l'import R est correct
import com.example.drive_2_go.data.model.Car; // Importez votre classe Car
import com.example.drive_2_go.ui.Client.login.LoginActivity;
import com.example.drive_2_go.ui.main.MainActivity;
import com.example.drive_2_go.ui.reservation.ReservationFormActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class DescriptionCarActivity extends AppCompatActivity {

    public static final String EXTRA_CAR = "extra_car";

    // Vues (variables membres)
    private TextView tvCarName, tvLicensePlate, tvBrandModelYear, tvPriceDetail;
    private TextView tvDescription, tvColor, tvFuelType, tvGearType, tvMaxKm;
    private TextView tvPeopleCount, tvBaggageCount, tvHasAc;
    private ImageView imgHeaderCar;
    private ImageButton btnBack;
    private Button orderBtn;
    private ImageButton btnFav;
    // Données
    private Car mCar; // ⭐️ Stocker l'objet Car
    private User currentUser; // ⭐️ Stocker l'objet User
    private boolean isFavorite = false; // ⭐️ État de favori
    private FirebaseFirestore db;
    private FirebaseAuth auth;



    // Ajouter une nouvelle méthode pour charger l'utilisateur
    private void loadCurrentUser() {
        FirebaseUser fUser = auth.getCurrentUser(); // Récupère l'utilisateur de Firebase Auth

        if (fUser == null) {
            // L'utilisateur n'est pas connecté, rediriger vers la connexion
            Toast.makeText(this, "Veuillez vous connecter pour gérer les favoris.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Récupérer le document de l'utilisateur dans la collection "users"
        db.collection("users").document(fUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Convertir le DocumentSnapshot en objet User
                        currentUser = documentSnapshot.toObject(User.class);

                        // --- ⭐️ ÉTAPE CRITIQUE : DÉPLACER L'INITIALISATION ICI ⭐️ ---
                        // Maintenant que currentUser n'est PLUS NULL, on peut initialiser l'état des favoris.
                        if (mCar != null && currentUser != null) {
                            // 4. Initialiser l'état de favori
                            isFavorite = currentUser.getFavoriteCarIds().contains(mCar.getId());
                            updateFavoriteIcon(); // Mettre à jour l'icône

                            // 5. Configurer l'écouteur du bouton Fav
                            setupFavButtonListener(); // Séparer l'écouteur Fav
                            // 6. Vérifier le statut de réservation et configurer le bouton Order
                            checkReservationStatus(); // ⭐️ NOUVEL APPEL ⭐️
                        } else {
                            Toast.makeText(this, "Erreur de chargement des données de voiture ou utilisateur.", Toast.LENGTH_LONG).show();
                            finish();
                        }

                    } else {
                        Toast.makeText(this, "Erreur: Profil utilisateur introuvable dans Firestore.", Toast.LENGTH_LONG).show();
                        auth.signOut(); // Déconnecter l'utilisateur Auth si son document Firestore est manquant
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur de récupération du profil : " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }


    /**
     * Vérifie si la voiture actuelle a déjà une réservation en attente de validation.
     */
    private void checkReservationStatus() {
        if (mCar == null) {
            // Ne peut pas vérifier si la voiture n'est pas chargée
            updateOrderButton(false, "Voiture non chargée", "#AAAAAA"); // Grisé par défaut
            return;
        }

        // Requête : Chercher dans la collection 'reservations'
        db.collection("reservations")
                .whereEqualTo("carId", mCar.getId()) // 1. Pour la voiture actuelle
                .whereEqualTo("status", "En attente de validation") // 2. Et qui est en attente
                .limit(1) // On a besoin de savoir s'il y en a au moins une
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Une réservation en attente existe pour cette voiture
                        Log.d("RESERVATION", "Réservation en attente trouvée pour la voiture: " + mCar.getName());

                        // ⭐️ Mettre à jour le bouton pour indiquer "En attente" et le désactiver
                        updateOrderButton(false, "En attente", "#8A9D80"); // Couleur demandée

                    } else {
                        // Aucune réservation en attente, le bouton doit être actif
                        Log.d("RESERVATION", "Aucune réservation en attente pour la voiture: " + mCar.getName());

                        // ⭐️ Mettre à jour le bouton pour indiquer "Louer maintenant" et l'activer
                        updateOrderButton(true, "Louer maintenant", "#4D7836"); // Couleur verte active (supposée)

                        // Reconfigurer l'écouteur de clic (car il peut avoir été désactivé)
                        setupOrderButtonListener();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RESERVATION", "Erreur lors de la vérification du statut de réservation: " + e.getMessage());
                    Toast.makeText(this, "Erreur de connexion : impossible de vérifier le statut.", Toast.LENGTH_SHORT).show();
                    // En cas d'échec de la vérification, nous laissons le bouton désactivé par sécurité.
                    updateOrderButton(false, "Erreur", "#AAAAAA");
                });
    }


    /**
     * Méthode utilitaire pour mettre à jour l'état du bouton de commande.
     */
    private void updateOrderButton(boolean enabled, String text, String colorHex) {
        orderBtn.setEnabled(enabled);
        orderBtn.setText(text);
        // Convertir la chaîne hexadécimale en couleur entière (int)
        try {
            int color = android.graphics.Color.parseColor(colorHex);
            orderBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        } catch (IllegalArgumentException e) {
            Log.e("COLOR", "Couleur hexadécimale invalide: " + colorHex);
            // Utiliser une couleur par défaut si l'hex est invalide
            orderBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
        }
    }


    // Nouvelle méthode pour centraliser la configuration des listeners après le chargement


    // Listener de retour et de favori
    private void setupFavButtonListener() {
        btnBack.setOnClickListener(v -> finish());
        btnFav.setOnClickListener(v -> toggleFavorite());
    }

    // Listener de commande (appelé si la voiture est disponible)
    private void setupOrderButtonListener() {
        orderBtn.setOnClickListener(v -> {
            if (mCar != null) {
                Intent intent = new Intent(DescriptionCarActivity.this, ReservationFormActivity.class);
                intent.putExtra(ReservationFormActivity.EXTRA_CAR_DATA, mCar);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Erreur: Données de voiture manquantes.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.description_car); // Assurez-vous que le nom du layout est correct

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 1. Initialiser les vues
        initViews();

        // 2. Récupérer l'objet Car
        mCar = getIncomingIntentData();

        // ⭐️ IMPORTANT : Appel initial pour configurer les écouteurs simples (Fav et Back)
        setupFavButtonListener();

        // 3. Charger l'utilisateur connecté (ASYNCHRONE)
        // Le reste de l'initialisation (vérification des favoris et listeners)
        // sera effectué DANS la méthode loadCurrentUser() après succès.
        loadCurrentUser();

    }


    private void initViews() {
        // En-tête
        imgHeaderCar = findViewById(R.id.img_header_car);
        btnBack = findViewById(R.id.btn_back);
        btnFav = findViewById(R.id.btn_fav); // ⭐️ Initialisation du bouton favori
        tvCarName = findViewById(R.id.tv_car_name_detail);
        tvLicensePlate = findViewById(R.id.tv_license_plate_detail);
        orderBtn = findViewById(R.id.orderBtn);

        // Conteneur de détails
        tvBrandModelYear = findViewById(R.id.tv_brand_model_year);
        tvDescription = findViewById(R.id.tv_description);
        tvColor = findViewById(R.id.tv_color);
        tvFuelType = findViewById(R.id.tv_fuel_type_detail);
        tvGearType = findViewById(R.id.tv_gear_type_detail);
        tvMaxKm = findViewById(R.id.tv_max_km_detail);
        tvPeopleCount = findViewById(R.id.tv_people_count_detail);
        tvBaggageCount = findViewById(R.id.tv_baggage_count_detail);
        tvHasAc = findViewById(R.id.tv_has_ac_detail);
        tvPriceDetail = findViewById(R.id.tv_price_detail);
    }

    private Car getIncomingIntentData() {
        if (getIntent().hasExtra(EXTRA_CAR)) {
            Car car = (Car) getIntent().getSerializableExtra(EXTRA_CAR);
            if (car != null) {
                displayCarDetails(car);
                return car;
            } else {
                Toast.makeText(this, "Erreur: Données de voiture nulles.", Toast.LENGTH_LONG).show();
                return null;
            }
        } else {
            Toast.makeText(this, "Erreur: Aucune donnée de voiture trouvée.", Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private void displayCarDetails(Car car) {
        // ... Votre code d'affichage des détails de la voiture ...
        Glide.with(this)
                .load(car.getImageUrl())
                .placeholder(R.drawable.img_renault_captur)
                .into(imgHeaderCar);

        tvCarName.setText(car.getName());
        tvLicensePlate.setText(car.getLicensePlate());
        String brandModelYear = car.getBrand() + " " + car.getModel() + " - " + car.getYear();
        tvBrandModelYear.setText(brandModelYear);
        tvPriceDetail.setText(car.getPrice() + " Dh / Jour");
        tvDescription.setText(car.getDescription());
        tvColor.setText(car.getColor());
        tvFuelType.setText(car.getFuelType());
        String gearText = car.getGearType().equals("M") ? "Manuelle (M)" : "Automatique (A)";
        tvGearType.setText(gearText);
        tvMaxKm.setText(car.getMaxKm() + " km");
        tvPeopleCount.setText(car.getPeopleCount() + " personnes");
        tvBaggageCount.setText(String.valueOf(car.getBaggageCount()));
        String acStatus = car.isHasAC() ? "Oui" : "Non";
        tvHasAc.setText(acStatus);
    }

    // ⭐️ Méthode pour basculer l'état de favori
    private void toggleFavorite() {
        if (mCar == null || currentUser == null) {
            Toast.makeText(this, "Erreur: Voiture ou utilisateur non chargé.", Toast.LENGTH_SHORT).show();
            return;
        }

        isFavorite = !isFavorite; // Bascule l'état local

        if (isFavorite) {
            // Ajouter aux favoris (logique locale)
            if (!currentUser.getFavoriteCarIds().contains(mCar.getId())) {
                currentUser.getFavoriteCarIds().add(mCar.getId());
            }
            Toast.makeText(this, mCar.getName() + " ajouté aux favoris.", Toast.LENGTH_SHORT).show();
        } else {
            // Retirer des favoris (logique locale)
            currentUser.getFavoriteCarIds().remove(mCar.getId());
            Toast.makeText(this, mCar.getName() + " retiré des favoris.", Toast.LENGTH_SHORT).show();
        }

        updateFavoriteIcon(); // Mettre à jour l'icône visuellement
        updateUserFavoritesInFirestore(); // ⭐️ Mettre à jour la base de données (Firestore)
    }
    private void updateFavoriteIcon() {
        if (isFavorite) {
            // Icône pleine (jaune) - Vous devez avoir ce drawable (ex: ic_star_filled)
            btnFav.setImageResource(R.drawable.ic_star_filled);
            // Si vous utilisez un tint pour la couleur :
            // btnFav.setColorFilter(ContextCompat.getColor(this, R.color.yellow_star_color));
        } else {
            // Icône vide (bordure) - Vous devez avoir ce drawable (ex: ic_star_outline)
            btnFav.setImageResource(R.drawable.ic_star_outline);
            // Si vous utilisez un tint pour la couleur :
            // btnFav.setColorFilter(ContextCompat.getColor(this, R.color.white_icon_color));
        }
    }
        // ⭐️ Méthode pour mettre à jour la liste des favoris de l'utilisateur dans Firestore
        // CECI DOIT ÊTRE IMPLÉMENTÉ AVEC VOTRE LOGIQUE FIREBASE
    // Dans DescriptionCarActivity.java

    private void updateUserFavoritesInFirestore() {
        if (currentUser == null) return;

        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser != null) {
            // 🚨 VÉRIFIEZ CELA : Doit correspondre exactement au nom de votre collection
            // Si la collection s'appelle 'users', utilisez 'users'
            DocumentReference userRef = db.collection("users").document(fUser.getUid());

            // Mettre à jour uniquement le champ de la liste des favoris
            userRef.update("favoriteCarIds", currentUser.getFavoriteCarIds())
                    .addOnSuccessListener(aVoid -> {
                        // Succès de la mise à jour
                        Log.d("FAVORIS", "Liste des favoris mise à jour avec succès dans Firestore.");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Erreur de mise à jour des favoris: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("FAVORIS", "Erreur d'écriture Firestore: " + e.getMessage());

                        // 💡 ACTION CRITIQUE EN CAS D'ÉCHEC : Revenir à l'état précédent
                        // Si la DB échoue, l'état local ne devrait pas changer.
                        // Revenir à l'état de favori précédent si l'écriture échoue.
                        isFavorite = !isFavorite;
                        updateFavoriteIcon();
                    });
        }
    }


}