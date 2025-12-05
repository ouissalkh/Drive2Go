package com.example.drive_2_go.ui.Admin.addeditCar;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
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

    // UI Elements liés au XML
    private ImageView ivCarImage, btnBack, btnDecreaseDoors, btnIncreaseDoors,
            btnDecreasePeople, btnIncreasePeople, btnDecreaseBaggage, btnIncreaseBaggage;
    private Button btnSelectImage;
    private MaterialButton btnSaveCar;
    private TextView tvTitle, tvDoorCount, tvPeopleCount, tvBaggageCount;
    private TextInputEditText etName, etBrand, etModel, etLicensePlate, etYear, etColor,
            etPrice, etMaxKm, etLocation, etDescription;
    private AutoCompleteTextView spinnerFuelType, spinnerGearType;
    private SwitchMaterial switchAC, switchChecked, switchAvailable;

    // Variables de données
    private int doorCount = 4;
    private int peopleCount = 5;
    private int baggageCount = 2;
    private Uri selectedImageUri; // URI temporaire de la galerie
    private Car carToEdit; // Objet reçu si modification
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_vehicule);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupSpinners();
        setupCounters();

        // Gestionnaire de résultat pour la sélection d'image dans la galerie
        ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        ivCarImage.setImageURI(uri); // Aperçu immédiat
                    }
                }
        );

        btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Vérifier si on est en mode ÉDITION (Update)
        if (getIntent().hasExtra("CAR_OBJECT")) {
            carToEdit = (Car) getIntent().getSerializableExtra("CAR_OBJECT");
            if (carToEdit != null) {
                fillFormWithData(carToEdit);
                tvTitle.setText("Modifier le véhicule");
                btnSaveCar.setText("Mettre à jour");
            }
        }

        btnBack.setOnClickListener(v -> finish());
        btnSaveCar.setOnClickListener(v -> validateAndSave());
    }

    private void initViews() {
        // Liaison avec tous les IDs du XML
        ivCarImage = findViewById(R.id.ivCarImage);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSaveCar = findViewById(R.id.btnSaveCar);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);

        etName = findViewById(R.id.etName);
        etBrand = findViewById(R.id.etBrand);
        etModel = findViewById(R.id.etModel);
        etLicensePlate = findViewById(R.id.etLicensePlate);
        etYear = findViewById(R.id.etYear);
        etColor = findViewById(R.id.etColor);
        etPrice = findViewById(R.id.etPrice);
        etMaxKm = findViewById(R.id.etMaxKm);
        etLocation = findViewById(R.id.etLocation);
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
    }

    private void setupSpinners() {
        String[] fuels = {"Essence", "Diesel", "Électrique", "Hybride"};
        spinnerFuelType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, fuels));

        String[] gears = {"Manuelle", "Automatique"};
        spinnerGearType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, gears));
    }

    private void setupCounters() {
        // Logique des boutons + et -
        btnDecreaseDoors.setOnClickListener(v -> { if (doorCount > 2) tvDoorCount.setText(String.valueOf(--doorCount)); });
        btnIncreaseDoors.setOnClickListener(v -> { if (doorCount < 10) tvDoorCount.setText(String.valueOf(++doorCount)); });

        btnDecreasePeople.setOnClickListener(v -> { if (peopleCount > 1) tvPeopleCount.setText(String.valueOf(--peopleCount)); });
        btnIncreasePeople.setOnClickListener(v -> { if (peopleCount < 50) tvPeopleCount.setText(String.valueOf(++peopleCount)); });

        btnDecreaseBaggage.setOnClickListener(v -> { if (baggageCount > 0) tvBaggageCount.setText(String.valueOf(--baggageCount)); });
        btnIncreaseBaggage.setOnClickListener(v -> { if (baggageCount < 10) tvBaggageCount.setText(String.valueOf(++baggageCount)); });
    }

    private void fillFormWithData(Car car) {
        etName.setText(car.getName());
        etBrand.setText(car.getBrand());
        etModel.setText(car.getModel());
        etLicensePlate.setText(car.getLicensePlate());
        etYear.setText(car.getYear());
        etColor.setText(car.getColor());
        etPrice.setText(car.getPrice());
        etMaxKm.setText(car.getMaxKm());
        etLocation.setText(car.getLocation());
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

        // Affichage de l'image locale existante
        if (car.getImageUrl() != null && !car.getImageUrl().isEmpty()) {
            File imgFile = new File(car.getImageUrl());
            if (imgFile.exists()) {
                Glide.with(this).load(imgFile).into(ivCarImage);
            }
        }
    }

    private void validateAndSave() {
        String name = etName.getText().toString().trim();
        String price = etPrice.getText().toString().trim();

        if (name.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir le nom et le prix", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Enregistrement en cours...");
        pd.setCancelable(false);
        pd.show();

        String finalImagePath = "";

        // CAS 1 : L'utilisateur a choisi une nouvelle image
        if (selectedImageUri != null) {
            String uniqueName = ImageUtils.createUniqueFileName();
            // Utilisation de notre classe utilitaire ImageUtils
            finalImagePath = ImageUtils.copyImageToInternalStorage(this, selectedImageUri, uniqueName);

            // Si c'est une modification, on supprime l'ancienne image pour nettoyer
            if (carToEdit != null && carToEdit.getImageUrl() != null) {
                ImageUtils.deleteImage(carToEdit.getImageUrl());
            }
        }
        // CAS 2 : Pas de nouvelle image, on garde l'ancienne (en modif)
        else if (carToEdit != null) {
            finalImagePath = carToEdit.getImageUrl();
        }

        saveToFirestore(finalImagePath, pd);
    }

    private void saveToFirestore(String imagePath, ProgressDialog pd) {
        String carId = (carToEdit != null) ? carToEdit.getId() : db.collection("cars").document().getId();
        String gearShort = spinnerGearType.getText().toString().equals("Automatique") ? "A" : "M";

        // Création de l'objet Car
        Car car = new Car(
                carId,
                etName.getText().toString(),
                etLicensePlate.getText().toString(),
                etPrice.getText().toString(),
                imagePath,
                spinnerFuelType.getText().toString(),
                etMaxKm.getText().toString(),
                baggageCount,
                switchAC.isChecked(),
                gearShort,
                doorCount,
                peopleCount,
                switchChecked.isChecked(),
                false, // ICI : On met 'false' pour satisfaire le constructeur sans gérer les favoris
                etDescription.getText().toString(),
                etBrand.getText().toString(),
                etModel.getText().toString(),
                etYear.getText().toString(),
                etColor.getText().toString(),
                etLocation.getText().toString(),
                switchAvailable.isChecked()
        );

        db.collection("cars").document(carId).set(car)
                .addOnSuccessListener(unused -> {
                    pd.dismiss();
                    Toast.makeText(this, "Véhicule enregistré !", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(this, "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show();
                });
    }


    }
