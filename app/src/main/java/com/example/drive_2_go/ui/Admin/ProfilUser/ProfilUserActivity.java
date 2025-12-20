package com.example.drive_2_go.ui.Admin.ProfilUser;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.example.drive_2_go.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfilUserActivity extends AppCompatActivity {

    private TextView tvNomPrenom;
    private EditText editEmail;
    private EditText editPhone;
    private ShapeableImageView profileImage;
    private FirebaseFirestore db;
    private static final String TAG = "ProfilUserActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil_user);

        // 1. Initialisation Cloudinary
        initCloudinary();

        db = FirebaseFirestore.getInstance();

        // Liaison avec les vues
        tvNomPrenom = findViewById(R.id.tv_nom_prenom);
        editEmail = findViewById(R.id.edit_data_email);
        editPhone = findViewById(R.id.edit_data_telephone);
        profileImage = findViewById(R.id.profileImage);
        ImageView btnBack = findViewById(R.id.btnBack);

        // Liaison des conteneurs cliquables
        LinearLayout containerEmail = findViewById(R.id.mail);
        LinearLayout containerPhone = findViewById(R.id.phone);

        // Récupération des données de l'Intent
        String userId = getIntent().getStringExtra("USER_ID");
        String userName = getIntent().getStringExtra("USER_NAME");

        if (userName != null) {
            tvNomPrenom.setText(userName);
        }

        if (userId != null) {
            loadUserData(userId);
        } else {
            Toast.makeText(this, "Impossible de charger les infos", Toast.LENGTH_SHORT).show();
        }

        // --- ACTIONS DE CLIC ---

        // Retour
        btnBack.setOnClickListener(v -> finish());

        // Clic sur l'Email
        containerEmail.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            if (!email.isEmpty() && !email.equals("Non renseigné") && !email.equals("Chargement...")) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + email)); // Ouvre uniquement les apps d'email
                try {
                    startActivity(Intent.createChooser(intent, "Envoyer un email..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this, "Aucune application d'email installée", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Clic sur le Téléphone
        containerPhone.setOnClickListener(v -> {
            String phone = editPhone.getText().toString().trim();
            if (!phone.isEmpty() && !phone.equals("Non renseigné") && !phone.equals("Chargement...")) {
                Intent intent = new Intent(Intent.ACTION_DIAL); // Ouvre le clavier avec le numéro
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Numéro non disponible", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initCloudinary() {
        try {
            MediaManager.get();
        } catch (IllegalStateException e) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "datr9fmfp");
            config.put("api_key", "953344295627375");
            config.put("api_secret", "jPnIjBzEtR8Z2H6jLVbwNqCrhjc");
            MediaManager.init(this, config);
        }
    }

    private void loadUserData(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String email = documentSnapshot.getString("email");
                        String phone = documentSnapshot.getString("telephone");
                        String photoUrl = documentSnapshot.getString("photoUrl");

                        if (email != null) editEmail.setText(email);
                        if (phone != null) editPhone.setText(phone);
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            loadImage(photoUrl);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Erreur DB", e));
    }

    private void loadImage(String photoUrl) {
        String urlToLoad = photoUrl.startsWith("http") ? photoUrl : MediaManager.get().url().generate(photoUrl);
        Glide.with(this)
                .load(urlToLoad)
                .placeholder(R.drawable.imageprofil)
                .error(R.drawable.imageprofil)
                .centerCrop()
                .into(profileImage);
    }
}