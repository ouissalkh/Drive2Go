package com.example.drive_2_go.ui.Client.profil;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Client.accueil.AccueilActivity;
import com.example.drive_2_go.ui.Client.favoris.Favoris;
import com.example.drive_2_go.ui.Client.history.HistoryActivity;
import com.example.drive_2_go.ui.Client.login.LoginActivity;
import com.example.drive_2_go.ui.Client.notification.NotificationClientActivity;
import com.example.drive_2_go.ui.main.MainActivity;
import com.google.android.material.imageview.ShapeableImageView;

public class Profil extends AppCompatActivity {

    private ShapeableImageView profileImage;
    private ImageButton buttonCamera;
    private Uri imageUri;
    private ImageView buttonBack;
    private ImageView btn_notification;
    private ImageView btn_settings;
    private ImageButton buttonFavoris;
    private ImageButton buttonHome;
    private ImageButton buttonHistory;
    private ImageButton buttonProfil;

    // Lanceur pour ouvrir la galerie et récupérer l'image
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        imageUri = selectedImageUri;
                        profileImage.setImageURI(imageUri);
                    }
                } else {
                    Toast.makeText(this, "Aucune image sélectionnée", Toast.LENGTH_SHORT).show();
                }
            });


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil); // remplace par ton vrai nom XML si différent

        profileImage = findViewById(R.id.profileImage);
        buttonCamera = findViewById(R.id.buttonCamera);
        buttonFavoris = findViewById(R.id.buttonFavoris);
        buttonHome = findViewById(R.id.buttonHome);
        buttonHistory = findViewById(R.id.buttonHistory);
        buttonProfil = findViewById(R.id.buttonProfil);

        buttonCamera.setOnClickListener(v -> openGallery());
        buttonFavoris.setOnClickListener(v -> openFavoris());
        buttonHome.setOnClickListener(v -> openAccueil());
        buttonHistory.setOnClickListener(v -> openHistory());
        buttonProfil.setOnClickListener(v -> Toast.makeText(this,"Déjà ici", Toast.LENGTH_SHORT).show());
        selectButton(buttonProfil);
        //bouton retour
        buttonBack = findViewById(R.id.btnBack);

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(Profil.this, AccueilActivity.class);
            startActivity(intent);
            finish();
        });
        btn_notification = findViewById(R.id.btn_notification);

        btn_notification.setOnClickListener(v -> {
            Intent intent = new Intent(Profil.this, NotificationClientActivity.class);
            startActivity(intent);
            finish();
        });

        btn_settings = findViewById(R.id.btn_settings);

        btn_settings.setOnClickListener(v -> {
            Intent intent = new Intent(Profil.this, Settings.class);
            startActivity(intent);
            finish();
        });
    }

    private void selectButton(ImageButton button) {
        button.setSelected(true);
    }

    private void openFavoris(){
        Intent intent = new Intent(Profil.this, Favoris.class);
        startActivity(intent);
    }
    private void openAccueil(){
        Intent intent = new Intent(Profil.this, AccueilActivity.class);
        startActivity(intent);
    }
    private void openHistory(){
        startActivity(new Intent(Profil.this, HistoryActivity.class));
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }


}
