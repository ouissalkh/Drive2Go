package com.example.drive_2_go.ui.reservation;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.Car; // Importez votre classe Car
import com.example.drive_2_go.ui.Client.accueil.AccueilActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth; // Importez FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReservationFormActivity extends AppCompatActivity {

    private static final String TAG = "ReservationForm";
    public static final String EXTRA_CAR_DATA = "extra_car_data";

    // UI Elements
    private TextView tvCarModel, tvStartDate, tvEndDate, tvTotalPrice;
    private Button btnConfirm;
    private ImageButton btnSelectStartDate, btnSelectEndDate;
    private RadioGroup rgPaymentMethod;
    private EditText etName, etPhone;

    // Firebase Instance
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Variables de Données
    private Car currentCar;
    private String currentUserId;
    private int carDailyPrice = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_form);

        // Initialisation de Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 1. Initialisation des Vues (liées au fichier XML)
        tvCarModel = findViewById(R.id.tv_car_model);
        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        btnConfirm = findViewById(R.id.btn_confirm_reservation);
        btnSelectStartDate = findViewById(R.id.btn_select_start_date);
        btnSelectEndDate = findViewById(R.id.btn_select_end_date);
        rgPaymentMethod = findViewById(R.id.rg_payment_method);
        etName = findViewById(R.id.et_client_name);
        etPhone = findViewById(R.id.et_client_phone);

        // 2. Récupérer les données de la voiture et de l'utilisateur
        if (!retrieveCarDataAndUser()) {
            Toast.makeText(this, "Erreur de chargement de la réservation (voiture ou utilisateur).", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Mise à jour des textes avec les données de la voiture
        tvCarModel.setText(currentCar.getName() + " (" + carDailyPrice + " DH / jr)");
        tvTotalPrice.setText("Total : 0 DH");

        // 3. Charger les informations client depuis Firebase
        loadUserDataFromFirebase();

        // 4. Configuration des sélecteurs de date
        btnSelectStartDate.setOnClickListener(v -> showDatePicker(tvStartDate));
        btnSelectEndDate.setOnClickListener(v -> showDatePicker(tvEndDate));

        // Écouteurs pour le calcul du prix automatique
        tvStartDate.addTextChangedListener(new PriceUpdateWatcher());
        tvEndDate.addTextChangedListener(new PriceUpdateWatcher());

        // 5. Gestion du bouton Confirmer
        btnConfirm.setOnClickListener(this::handleConfirmation);
    }

    /**
     * Récupère l'objet Car de l'Intent et l'ID utilisateur de FirebaseAuth.
     * @return true si les données minimales sont chargées, false sinon.
     */
    private boolean retrieveCarDataAndUser() {
        if (getIntent().hasExtra(EXTRA_CAR_DATA)) {
            currentCar = (Car) getIntent().getSerializableExtra(EXTRA_CAR_DATA);
            if (currentCar != null) {
                carDailyPrice = currentCar.getPrice();
            } else {
                Log.e(TAG, "Objet Car récupéré est null.");
                return false;
            }
        } else {
            Log.e(TAG, "Pas d'extra EXTRA_CAR_DATA trouvé dans l'Intent.");
            return false;
        }

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        } else {
            Log.e(TAG, "Utilisateur Firebase non connecté.");
            return false;
        }

        return true;
    }

    /**
     * Ouvre le DatePickerDialog (mini-calendrier).
     */
    private void showDatePicker(TextView dateTextView) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%d",
                            selectedDay, selectedMonth + 1, selectedYear);
                    dateTextView.setText(date);
                }, year, month, day);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    /**
     * Tente de charger le nom et le téléphone de l'utilisateur connecté depuis Firestore.
     * Utilise les clés nom, prenom et telephone.
     */
    private void loadUserDataFromFirebase() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Erreur: ID utilisateur manquant.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("prenom");
                        String lastName = documentSnapshot.getString("nom");
                        String phone = documentSnapshot.getString("telephone");

                        String fullName = "";
                        if (firstName != null) {
                            fullName += firstName;
                        }
                        if (lastName != null) {
                            if (!fullName.isEmpty()) {
                                fullName += " ";
                            }
                            fullName += lastName;
                        }

                        // Remplissage des EditText
                        if (!fullName.isEmpty()) {
                            etName.setText(fullName);
                        } else {
                            etName.setText("Nom complet manquant");
                        }

                        if (phone != null) {
                            etPhone.setText(phone);
                        } else {
                            etPhone.setText("Téléphone manquant");
                        }

                    } else {
                        Log.w(TAG, "Document utilisateur non trouvé pour ID: " + currentUserId);
                        etName.setText("Profil Inconnu");
                        etPhone.setText("Non disponible");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur de connexion Firestore : ", e);
                    Toast.makeText(this, "Erreur de chargement des données client.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Calcule le prix total en fonction des dates de début et de fin.
     */
    private void calculateTotalPrice() {
        String startDateStr = tvStartDate.getText().toString();
        String endDateStr = tvEndDate.getText().toString();

        if (startDateStr.isEmpty() || endDateStr.isEmpty()) {
            tvTotalPrice.setText("Total : 0 DH");
            return;
        }

        if (carDailyPrice <= 0) {
            tvTotalPrice.setText("Total : Prix Inconnu");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);

            if (startDate == null || endDate == null || endDate.before(startDate)) {
                tvTotalPrice.setText("Total : Erreur Date");
                return;
            }

            long diff = endDate.getTime() - startDate.getTime();
            int days = (int) (diff / (1000 * 60 * 60 * 24)) + 1;

            int totalPrice = days * carDailyPrice;
            tvTotalPrice.setText("Total : " + totalPrice + " DH");

        } catch (ParseException e) {
            tvTotalPrice.setText("Total : Erreur Format");
        }
    }


    /**
     * Enregistre la réservation dans Firebase avec les nouveaux champs de temps et de statut.
     */
    private void handleConfirmation(View view) {
        String startDate = tvStartDate.getText().toString();
        String endDate = tvEndDate.getText().toString();
        String totalPriceStr = tvTotalPrice.getText().toString().replace("Total : ", "").replace(" DH", "");

        if (startDate.isEmpty() || endDate.isEmpty() || totalPriceStr.contains("Erreur") || currentUserId == null || currentCar == null) {
            Toast.makeText(this, "Veuillez vérifier les dates et l'état du formulaire.", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = rgPaymentMethod.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedId);
        String paymentMethod = selectedRadioButton.getText().toString();

        // Création de l'objet de réservation pour Firestore
        Map<String, Object> reservation = new HashMap<>();
        reservation.put("userId", currentUserId);

        // Données de la voiture
        reservation.put("carId", currentCar.getId());
        reservation.put("carName", currentCar.getName());
        reservation.put("carLicensePlate", currentCar.getLicensePlate());
        reservation.put("carDailyPrice", currentCar.getPrice());

        // Données utilisateur et location
        reservation.put("userName", etName.getText().toString());
        reservation.put("startDate", startDate);
        reservation.put("endDate", endDate);
        reservation.put("totalPrice", Integer.parseInt(totalPriceStr));
        reservation.put("paymentMethod", paymentMethod);

        // ⭐️ NOUVEAUX CHAMPS DE STATUT ET DE TEMPS ⭐️

        // 1. Statut (par défaut 'En attente de validation')
        reservation.put("status", "En attente de validation");

        // 2. Temps d'envoi de la réservation par le client (curentTime)
        reservation.put("timeReservationClient", Timestamp.now());

        // 3. Temps de confirmation par l'Admin (par défaut null)
        // La valeur 'null' sera envoyée à Firestore si l'objet n'est pas un type primitif.
        reservation.put("timeConfirmationAdmin", null);

        // Enregistrement dans la collection 'reservations'
        db.collection("reservations")
                .add(reservation)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Réservation enregistrée avec ID: " + documentReference.getId());
                    Toast.makeText(this, "Demande envoyée ! En attente de validation de l'Administrateur.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ReservationFormActivity.this, AccueilActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors de l'enregistrement de la réservation", e);
                    Toast.makeText(this, "Erreur: Échec de la confirmation. Veuillez réessayer.", Toast.LENGTH_LONG).show();
                });
    }

    /**
     * TextWatcher pour déclencher le calcul du prix chaque fois que les dates sont mises à jour.
     */
    private class PriceUpdateWatcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void afterTextChanged(Editable s) {
            calculateTotalPrice();
        }
    }
}