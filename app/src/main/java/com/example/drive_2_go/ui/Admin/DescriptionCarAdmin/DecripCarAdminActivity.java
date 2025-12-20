
package com.example.drive_2_go.ui.Admin.DescriptionCarAdmin;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.example.drive_2_go.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DecripCarAdminActivity extends AppCompatActivity {

    // Déclaration des variables UI
    private ImageView imgHeaderCar;
    private TextView tvCarName, tvLicensePlate, tvBrandModelYear, tvDescription;
    private TextView tvColor, tvFuel, tvGear, tvPeople, tvPrice; // tvAc et tvMaxKm retirés si gérés autrement

    // Ajout des nouvelles TextViews pour les Badges
    private TextView tvMaxkm, tvBagages, tvclima;

    private FirebaseFirestore db;
    // Dans les déclarations de variables en haut
    private TextView tvAcceptedCount;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrip_car_admin);

        // 1. Init Cloudinary (Avec sécurité)
        initCloudinary();

        db = FirebaseFirestore.getInstance();

        // 2. Initialisation des Vues
        initViews();

        // 3. Récupération ID
        String carId = getIntent().getStringExtra("CAR_ID");

        if (carId != null) {
            loadCarDetails(carId);
        } else {
            Toast.makeText(this, "Erreur : ID Voiture manquant", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 4. Bouton Retour (Avec vérification)
        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        // On récupère les vues. Si l'ID n'existe pas dans le XML, la variable sera null.
        imgHeaderCar = findViewById(R.id.img_header_car);
        tvCarName = findViewById(R.id.tv_car_name_detail);
        tvLicensePlate = findViewById(R.id.tv_license_plate_detail);
        tvBrandModelYear = findViewById(R.id.tv_brand_model_year);
        tvDescription = findViewById(R.id.tv_description);

        tvColor = findViewById(R.id.tv_color);
        tvFuel = findViewById(R.id.tv_fuel_type_detail); // Vérifié: ID Correct
        tvGear = findViewById(R.id.tv_gear_type_detail);
        tvPeople = findViewById(R.id.tv_people_count_detail);
        tvPrice = findViewById(R.id.tv_price_detail);

        // Nouveaux champs
        tvMaxkm = findViewById(R.id.tv_maxkm);
        tvBagages = findViewById(R.id.tv_bagages);
        tvclima = findViewById(R.id.tv_climatisation);
    }

    private void loadCarDetails(String carId) {
        db.collection("cars").document(carId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        fillData(documentSnapshot);
                    } else {
                        Toast.makeText(this, "Voiture introuvable", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erreur connexion", Toast.LENGTH_SHORT).show());
    }

    // VERSION SÉCURISÉE : Empêche le crash si un champ est vide ou une vue manquante
    private void fillData(DocumentSnapshot doc) {
        if (doc == null) return;

        // 1. Textes de base
        String brand = doc.getString("brand");
        String model = doc.getString("model");
        String year = doc.getString("year");
        String fullName = (brand != null ? brand : "") + " " + (model != null ? model : "");

        if (tvCarName != null) tvCarName.setText(fullName);
        if (tvBrandModelYear != null) tvBrandModelYear.setText("Série " + (year != null ? year : ""));
        if (tvLicensePlate != null) tvLicensePlate.setText(doc.getString("licensePlate"));
        if (tvDescription != null) tvDescription.setText(doc.getString("description"));

        // 2. Caractéristiques
        if (tvColor != null) tvColor.setText(doc.getString("color"));
        if (tvFuel != null) tvFuel.setText(doc.getString("fuelType"));
        if (tvGear != null) tvGear.setText(doc.getString("gearType"));

        if (tvPeople != null) {
            Object seats = doc.get("peopleCount");
            tvPeople.setText(seats != null ? seats.toString() : "5");
        }

        if (tvPrice != null) {
            Object price = doc.get("price"); // Utiliser get() est plus sûr pour les nombres
            tvPrice.setText(price != null ? price + " DH" : "0 DH");
        }

        // --- AJOUT DES NOUVELLES INFOS ---
        if (tvMaxkm != null) {
            Object maxKm = doc.get("maxKm");
            tvMaxkm.setText(maxKm != null ? maxKm.toString() + " km" : "N/A");
        }

        if (tvBagages != null) {
            Object baggages = doc.get("baggageCount");
            tvBagages.setText(baggages != null ? baggages.toString() : "0");
        }

        // --- GESTION CLIMATISATION ---
        if (tvclima != null) {
            Boolean hasAC = doc.getBoolean("hasAC");
            if (Boolean.TRUE.equals(hasAC)) {
                tvclima.setText("Oui");
            } else {
                tvclima.setText("Non");
            }
        }

        // 3. Image
        String imageUrl = doc.getString("imageUrl");
        if (imgHeaderCar != null && imageUrl != null && !imageUrl.isEmpty()) {
            try {
                String url = imageUrl.startsWith("http") ? imageUrl : MediaManager.get().url().generate(imageUrl);
                Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.img_renault_captur) // Image par défaut si erreur
                        .into(imgHeaderCar);
            } catch (Exception e) {
                Log.e("CarDetail", "Err Image", e);
            }
        }
    }

    private void initCloudinary() {
        try {
            MediaManager.get();
        } catch (Exception e) {
            try {
                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", "datr9fmfp");
                config.put("api_key", "953344295627375");
                config.put("api_secret", "jPnIjBzEtR8Z2H6jLVbwNqCrhjc");
                MediaManager.init(this, config);
            } catch (Exception ex) {
                Log.e("InitCloudinary", "Erreur", ex);
            }
        }
    }
}
