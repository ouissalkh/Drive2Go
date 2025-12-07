package com.example.drive_2_go.ui.Admin.addeditCar;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.Car;
import com.example.drive_2_go.utils.ImageUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;

public class AddEditVehicleActivity extends AppCompatActivity {

    private static final String TAG = "AddEditVehicle";

    // UI Elements
    private ImageView ivCarImage, btnBack, btnDecreaseDoors, btnIncreaseDoors,
            btnDecreasePeople, btnIncreasePeople, btnDecreaseBaggage, btnIncreaseBaggage;
    private Button btnSelectImage;
    private MaterialButton btnSaveCar;
    private TextView tvTitle, tvDoorCount, tvPeopleCount, tvBaggageCount;
    private TextInputEditText etName, etModel, etLicensePlate, etYear, etColor,
            etPrice, etMaxKm, etDescription;
    private AutoCompleteTextView spinnerBrand, spinnerFuelType, spinnerGearType;
    private SwitchMaterial switchAC, switchChecked, switchAvailable;

    // Données
    private int doorCount = 4;
    private int peopleCount = 5;
    private int baggageCount = 2;
    private Uri selectedImageUri;
    private Car carToEdit;
    private FirebaseFirestore db;
    private ActivityResultLauncher<String> imagePickerLauncher;

    // ✅ MARQUES DISPONIBLES (utilisées pour le filtrage client)
    private static final String[] BRANDS = {
            "BMW",
            "Ford",
            "Audi",
            "Mercedes",
            "Volkswagen",
            "Renault",
            "Peugeot",
            "Toyota",
            "Tesla"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_add_edit_vehicule);

            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "✅ Activity créée, Firebase initialisé");

            initViews();
            setupSpinners();
            setupCounters();
            setupImagePicker();

            // Vérifier si mode ÉDITION
            if (getIntent().hasExtra("CAR_OBJECT")) {
                carToEdit = (Car) getIntent().getSerializableExtra("CAR_OBJECT");
                if (carToEdit != null) {
                    fillFormWithData(carToEdit);
                    tvTitle.setText("Modifier le véhicule");
                    btnSaveCar.setText("Mettre à jour");
                    Log.d(TAG, "Mode édition : " + carToEdit.getName());
                }
            } else {
                Log.d(TAG, "Mode ajout d'un nouveau véhicule");
            }

            btnBack.setOnClickListener(v -> finish());
            btnSaveCar.setOnClickListener(v -> validateAndSave());

        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur dans onCreate : " + e.getMessage(), e);
            Toast.makeText(this, "Erreur de chargement : " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        try {
            ivCarImage = findViewById(R.id.ivCarImage);
            btnSelectImage = findViewById(R.id.btnSelectImage);
            btnSaveCar = findViewById(R.id.btnSaveCar);
            btnBack = findViewById(R.id.btnBack);
            tvTitle = findViewById(R.id.tvTitle);

            etName = findViewById(R.id.etName);
            spinnerBrand = findViewById(R.id.getBrand);
            etModel = findViewById(R.id.etModel);
            etLicensePlate = findViewById(R.id.etLicensePlate);
            etYear = findViewById(R.id.etYear);
            etColor = findViewById(R.id.etColor);
            etPrice = findViewById(R.id.etPrice);
            etMaxKm = findViewById(R.id.etMaxKm);
            etDescription = findViewById(R.id.etDescription);

            spinnerFuelType = findViewById(R.id.spinnerFuelType);
            spinnerGearType = findViewById(R.id.spinnerGearType);

            tvDoorCount = findViewById(R.id.tvDoorCount);
            btnDecreaseDoors = findViewById(R.id.btnDecreaseDoors);
            btnIncreaseDoors = findViewById(R.id.btnIncreaseDoors);

            tvPeopleCount = findViewById(R.id.tvPeopleCount);
            btnDecreasePeople = findViewById(R.id.btnDecreasePeople);
            btnIncreasePeople = findViewById(R.id.btnIncreasePeople);

            tvBaggageCount = findViewById(R.id.tvBaggageCount);
            btnDecreaseBaggage = findViewById(R.id.btnDecreaseBaggage);
            btnIncreaseBaggage = findViewById(R.id.btnIncreaseBaggage);

            switchAC = findViewById(R.id.switchAC);
            switchChecked = findViewById(R.id.switchChecked);
            switchAvailable = findViewById(R.id.switchAvailable);

            Log.d(TAG, "✅ Toutes les vues initialisées");
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur initViews : " + e.getMessage(), e);
            throw e;
        }
    }

    private void setupSpinners() {
        try {
            // SPINNER MARQUES
            ArrayAdapter<String> brandAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    BRANDS
            );
            spinnerBrand.setAdapter(brandAdapter);
            spinnerBrand.setText(BRANDS[0], false);

            // SPINNER CARBURANT
            String[] fuels = {"Essence", "Diesel", "Électrique", "Hybride"};
            ArrayAdapter<String> fuelAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    fuels
            );
            spinnerFuelType.setAdapter(fuelAdapter);
            spinnerFuelType.setText("Diesel", false);

            // SPINNER BOÎTE
            String[] gears = {"Manuelle", "Automatique"};
            ArrayAdapter<String> gearAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    gears
            );
            spinnerGearType.setAdapter(gearAdapter);
            spinnerGearType.setText("Manuelle", false);

            Log.d(TAG, "✅ Spinners configurés");
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur setupSpinners : " + e.getMessage(), e);
        }
    }

    private void setupCounters() {
        // Portes
        btnDecreaseDoors.setOnClickListener(v -> {
            if (doorCount > 2) {
                doorCount--;
                tvDoorCount.setText(String.valueOf(doorCount));
            }
        });

        btnIncreaseDoors.setOnClickListener(v -> {
            if (doorCount < 10) {
                doorCount++;
                tvDoorCount.setText(String.valueOf(doorCount));
            }
        });

        // Passagers
        btnDecreasePeople.setOnClickListener(v -> {
            if (peopleCount > 1) {
                peopleCount--;
                tvPeopleCount.setText(String.valueOf(peopleCount));
            }
        });

        btnIncreasePeople.setOnClickListener(v -> {
            if (peopleCount < 50) {
                peopleCount++;
                tvPeopleCount.setText(String.valueOf(peopleCount));
            }
        });

        // Bagages
        btnDecreaseBaggage.setOnClickListener(v -> {
            if (baggageCount > 0) {
                baggageCount--;
                tvBaggageCount.setText(String.valueOf(baggageCount));
            }
        });

        btnIncreaseBaggage.setOnClickListener(v -> {
            if (baggageCount < 10) {
                baggageCount++;
                tvBaggageCount.setText(String.valueOf(baggageCount));
            }
        });
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        ivCarImage.setImageURI(uri);
                        Log.d(TAG, "✅ Image sélectionnée : " + uri);
                    }
                }
        );

        btnSelectImage.setOnClickListener(v -> {
            try {
                imagePickerLauncher.launch("image/*");
            } catch (Exception e) {
                Log.e(TAG, "❌ Erreur ouverture galerie : " + e.getMessage());
                Toast.makeText(this, "Impossible d'ouvrir la galerie", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fillFormWithData(Car car) {
        try {
            etName.setText(car.getName());
            spinnerBrand.setText(car.getBrand(), false);
            etModel.setText(car.getModel());
            etLicensePlate.setText(car.getLicensePlate());
            etYear.setText(car.getYear());
            etColor.setText(car.getColor());
            etPrice.setText(String.valueOf(car.getPrice()));
            etMaxKm.setText(car.getMaxKm());
            etDescription.setText(car.getDescription());

            spinnerFuelType.setText(car.getFuelType(), false);
            String gearLabel = "A".equals(car.getGearType()) ? "Automatique" : "Manuelle";
            spinnerGearType.setText(gearLabel, false);

            doorCount = car.getDoorCount();
            peopleCount = car.getPeopleCount();
            baggageCount = car.getBaggageCount();
            tvDoorCount.setText(String.valueOf(doorCount));
            tvPeopleCount.setText(String.valueOf(peopleCount));
            tvBaggageCount.setText(String.valueOf(baggageCount));

            switchAC.setChecked(car.isHasAC());
            switchChecked.setChecked(car.isChecked());
            switchAvailable.setChecked(car.isAvailable());

            // Affichage image existante
            if (car.getImageUrl() != null && !car.getImageUrl().isEmpty()) {
                File imgFile = new File(car.getImageUrl());
                if (imgFile.exists()) {
                    Glide.with(this).load(imgFile).into(ivCarImage);
                }
            }

            Log.d(TAG, "✅ Formulaire rempli avec les données existantes");
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur fillFormWithData : " + e.getMessage(), e);
        }
    }

    private void validateAndSave() {
        // Validation
        String name = etName.getText().toString().trim();
        String brand = spinnerBrand.getText().toString().trim();
        String model = etModel.getText().toString().trim();
        String licensePlate = etLicensePlate.getText().toString().trim();
        String price = etPrice.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Le nom est requis");
            etName.requestFocus();
            return;
        }

        if (brand.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner une marque", Toast.LENGTH_SHORT).show();
            return;
        }

        if (model.isEmpty()) {
            etModel.setError("Le modèle est requis");
            etModel.requestFocus();
            return;
        }

        if (licensePlate.isEmpty()) {
            etLicensePlate.setError("La plaque d'immatriculation est requise");
            etLicensePlate.requestFocus();
            return;
        }

        if (price.isEmpty()) {
            etPrice.setError("Le prix est requis");
            etPrice.requestFocus();
            return;
        }

        // Loader
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Enregistrement en cours...");
        pd.setCancelable(false);
        pd.show();

        String finalImagePath = "";

        // Gestion image
        if (selectedImageUri != null) {
            String uniqueName = ImageUtils.createUniqueFileName();
            finalImagePath = ImageUtils.copyImageToInternalStorage(this, selectedImageUri, uniqueName);

            if (carToEdit != null && carToEdit.getImageUrl() != null) {
                ImageUtils.deleteImage(carToEdit.getImageUrl());
            }
        } else if (carToEdit != null) {
            finalImagePath = carToEdit.getImageUrl();
        }

        saveToFirestore(finalImagePath, pd);
    }

    private void saveToFirestore(String imagePath, ProgressDialog pd) {
        try {
            // ID
            String carId = (carToEdit != null) ? carToEdit.getId() : db.collection("cars").document().getId();

            // Conversion boîte
            String gearShort = spinnerGearType.getText().toString().equals("Automatique") ? "A" : "M";

            // Valeurs
            String year = etYear.getText().toString().trim();
            String maxKm = etMaxKm.getText().toString().trim();
            String color = etColor.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String priceString = etPrice.getText().toString().trim();

            // Conversion du prix en INT (avec gestion d'erreur)
            int priceInt;
            try {
                // Tenter la conversion du String en Int
                priceInt = Integer.parseInt(priceString);
            } catch (NumberFormatException e) {
                // Gérer le cas où l'entrée n'est pas un nombre valide
                pd.dismiss();
                Log.e(TAG, "❌ Le prix n'est pas un nombre valide : " + priceString, e);
                Toast.makeText(this, "❌ Erreur : Le prix doit être un nombre entier valide.", Toast.LENGTH_LONG).show();
                // Assurez-vous que l'activité ne se termine pas si la conversion échoue
                return;
            }

            // Valeurs par défaut
            if (year.isEmpty()) year = "2024";
            if (maxKm.isEmpty()) maxKm = "1000";
            if (color.isEmpty()) color = "Non spécifié";
            if (description.isEmpty()) description = "Aucune description";



            // Création objet Car
            Car car = new Car(
                    carId,
                    etName.getText().toString().trim(),
                    etLicensePlate.getText().toString().trim(),
                    priceInt,
                    imagePath,
                    spinnerFuelType.getText().toString(),
                    maxKm,
                    baggageCount,
                    switchAC.isChecked(),
                    gearShort,
                    doorCount,
                    peopleCount,
                    switchChecked.isChecked(),
                    description,
                    spinnerBrand.getText().toString(), // ✅ MARQUE pour filtrage
                    etModel.getText().toString().trim(),
                    year,
                    color,
                    "",
                    switchAvailable.isChecked()
            );

            Log.d(TAG, "📝 Sauvegarde voiture : " + car.getName() + " (Marque: " + car.getBrand() + ")");

            // Sauvegarde Firestore
            db.collection("cars").document(carId).set(car)
                    .addOnSuccessListener(unused -> {
                        pd.dismiss();
                        Log.d(TAG, "✅ Véhicule enregistré avec succès");
                        Toast.makeText(this, "✅ Véhicule enregistré !", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        pd.dismiss();
                        Log.e(TAG, "❌ Erreur sauvegarde : " + e.getMessage(), e);
                        Toast.makeText(this, "❌ Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            pd.dismiss();
            Log.e(TAG, "❌ Exception saveToFirestore : " + e.getMessage(), e);
            Toast.makeText(this, "❌ Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}