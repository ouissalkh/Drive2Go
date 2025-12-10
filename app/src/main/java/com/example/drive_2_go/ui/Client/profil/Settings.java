package com.example.drive_2_go.ui.Client.profil;


import android.Manifest;
import android.app.AlertDialog;
// ... autres imports ...
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText; // Maintenant nous utilisons EditText ici
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.drive_2_go.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Settings extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;

    // Composants UI (Maintenant des EditTexts pour les données)
    private ShapeableImageView imgProfilePhoto;
    private ImageButton buttonCamera;
    private TextView tvModifierProfil; // Lien "Modifier vos infos"

    private EditText etNom, etPrenom, etEmail, etTelephone; // Les champs modifiables
    private TextView tvDeconnexion;
    private Button btnSaveProfile; // Le bouton "Sauvegarder"

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private DocumentReference userRef;

    // Pour la gestion de la photo
    private Uri selectedImageUri;

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    private boolean isEditing = false; // État d'édition

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initCloudinary();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            userRef = db.collection("Clients").document(currentUserId);
        } else {
            Toast.makeText(this, "Utilisateur non connecté.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 1. Initialisation des Vues
        imgProfilePhoto = findViewById(R.id.img_profile_photo);
        buttonCamera = findViewById(R.id.buttonCamera);
        tvModifierProfil = findViewById(R.id.mofifier_profil);
        btnSaveProfile = findViewById(R.id.btn_save_profile); // Bouton Sauvegarder

        // Initialisation des EditTexts (NOUVEAUX IDs du XML modifié)
        etNom = findViewById(R.id.edit_data_nom);
        etPrenom = findViewById(R.id.edit_data_prenom);
        etEmail = findViewById(R.id.edit_data_email);
        etTelephone = findViewById(R.id.edit_data_telephone);
        tvDeconnexion = findViewById(R.id.text_deconnexion);

        // 2. Configuration initiale : masquer le bouton Sauvegarder
        btnSaveProfile.setVisibility(View.GONE);
        setFieldsEditable(false); // S'assurer qu'ils sont en lecture seule au démarrage

        // 3. Configuration des Launchers
        setupActivityResultLaunchers();

        // 4. Chargement initial des données et de la photo
        loadUserProfileData();

        // 5. Gestion des Clics
        buttonCamera.setOnClickListener(v -> checkPermissionAndShowImageSourceDialog());

        // Clic sur "Modifier vos infos" (Active/Désactive le mode édition)
        tvModifierProfil.setOnClickListener(v -> toggleEditMode());

        // Clic sur "Sauvegarder"
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());

        // Clic sur "Déconnexion"
        if (tvDeconnexion != null) {
            tvDeconnexion.setOnClickListener(v -> logoutUser());
        }

        // Bouton de retour
        findViewById(R.id.img_back_button).setOnClickListener(v -> finish());
    }

    /**
     * Bascule entre le mode Affichage et le mode Édition.
     */
    private void toggleEditMode() {
        isEditing = !isEditing;
        setFieldsEditable(isEditing);

        if (isEditing) {
            // Passer en mode édition
            tvModifierProfil.setText("Annuler la modification");
            btnSaveProfile.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Mode édition activé.", Toast.LENGTH_SHORT).show();
        } else {
            // Passer en mode affichage (Annulation)
            tvModifierProfil.setText("Modifier vos infos");
            btnSaveProfile.setVisibility(View.GONE);
            loadUserProfileData(); // Recharger les données originales en cas d'annulation
            Toast.makeText(this, "Modification annulée.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Rend les champs de texte éditables ou non.
     */
    private void setFieldsEditable(boolean editable) {
        EditText[] fields = {etNom, etPrenom, etEmail, etTelephone};
        int color = editable ? ContextCompat.getColor(this, R.color.black) : ContextCompat.getColor(this, R.color.gray_6D6D6D); // R.color.gray_6D6D6D doit exister

        for (EditText field : fields) {
            field.setEnabled(editable);
            field.setFocusable(editable);
            field.setFocusableInTouchMode(editable);
            field.setCursorVisible(editable);
            field.setClickable(editable);

            // Pour enlever le trait de soulignement par défaut des EditTexts en mode lecture seule
            field.setBackgroundResource(editable ? R.drawable.edit_text_border_bottum : 0); // R.drawable.edit_text_border_bottom est une ressource à créer
            field.setTextColor(color);
        }

        // L'email peut être une exception, souvent non modifiable directement via le profil
        if (editable) {
            etNom.requestFocus();
        }
    }

    /**
     * Enregistre les modifications dans Firebase Firestore.
     */
    private void saveProfileChanges() {
        String newNom = etNom.getText().toString().trim();
        String newPrenom = etPrenom.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newTelephone = etTelephone.getText().toString().trim();

        updateProfileData(newNom, newPrenom, newEmail, newTelephone);

        // Quitter le mode édition après la sauvegarde (le succès de la mise à jour gérera le rechargement)
        isEditing = false;
        tvModifierProfil.setText("Modifier vos infos");
        btnSaveProfile.setVisibility(View.GONE);
        setFieldsEditable(false);
    }

    //---------------------------------------------------------
    // Méthodes Cloudinary et Firebase (similaires à la réponse précédente)
    //---------------------------------------------------------

    private void initCloudinary() {
        // ... (votre code d'initialisation Cloudinary) ...
        try {
            Map config = new HashMap();
            config.put("cloud_name", "datr9fmfp");
            config.put("api_key", "953344295627375");
            config.put("api_secret", "jPnIjBzEtR8Z2H6jLVbwNqCrhjc");
            MediaManager.init(this, config);
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur initialisation Cloudinary : " + e.getMessage(), e);
        }
    }

    private void loadUserProfileData() {
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String photoUrl = document.getString("photoUrl");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(Settings.this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.imageprofil)
                                    .into(imgProfilePhoto);
                        }

                        // Afficher les données dans les EditTexts
                        etNom.setText(document.getString("nom"));
                        etPrenom.setText(document.getString("prenom"));
                        etEmail.setText(document.getString("email"));
                        etTelephone.setText(document.getString("telephone"));

                    } else {
                        Toast.makeText(Settings.this, "Aucune donnée de profil trouvée.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Settings.this, "Erreur lors du chargement des données.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateProfileData(String nom, String prenom, String email, String telephone) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("nom", nom);
        updates.put("prenom", prenom);
        updates.put("email", email);
        updates.put("telephone", telephone);

        userRef.update(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Settings.this, "Informations mises à jour avec succès.", Toast.LENGTH_SHORT).show();
                        // Les EditTexts contiennent déjà les nouvelles valeurs
                    } else {
                        Toast.makeText(Settings.this, "Erreur lors de la mise à jour : " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        // En cas d'échec, recharger les anciennes données
                        loadUserProfileData();
                    }
                });
    }

    // ... (Reste des méthodes : setupActivityResultLaunchers, uploadProfilePhoto, checkPermissionAndShowImageSourceDialog, etc.) ...
    private void setupActivityResultLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        imgProfilePhoto.setImageURI(selectedImageUri);
                        uploadProfilePhoto();
                    }
                });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Toast.makeText(this, "Photo prise (Téléchargement non implémenté pour l'intent simple de caméra)", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Dans Settings.java

    private void uploadProfilePhoto() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Sélectionnez d'abord une photo.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Le chemin d'upload est l'URI du fichier local
        MediaManager.get().upload(selectedImageUri)
                .option("public_id", "profile_images/" + currentUserId) // Dossier/nom de l'image
                .option("overwrite", true) // Écrase l'ancienne image pour cet utilisateur
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Toast.makeText(Settings.this, "Début du téléchargement vers Cloudinary...", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Afficher la progression si nécessaire (facultatif)
                    }


                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // 1. Récupération de l'URL de l'image depuis la réponse Cloudinary
                        String photoUrl = (String) resultData.get("url");

                        // 2. Mise à jour du document utilisateur dans Firestore
                        userRef.update("photoUrl", photoUrl) // <-- CECI ENVOIE L'URL À FIREBASE/FIRESTORE
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(Settings.this, "Photo de profil mise à jour.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Gérer l'erreur de mise à jour Firestore
                                        Log.e(TAG, "Erreur Firestore lors de la MAJ de photoUrl: " + updateTask.getException());
                                    }
                                });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(Settings.this, "Erreur Cloudinary: " + error.getDescription(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Cloudinary Error: " + error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Gérer la reprise
                    }
                }).dispatch();
    }


    // Permissions et Dialogue Source Image (laissez ces méthodes si vous en avez besoin)
    private void checkPermissionAndShowImageSourceDialog() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        } else {
            showImageSourceDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                showImageSourceDialog();
            } else {
                Toast.makeText(this, "Permissions nécessaires pour la caméra et la galerie.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showImageSourceDialog() {
        final CharSequence[] options = {"Prendre une photo", "Choisir depuis la galerie", "Annuler"};
        AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
        builder.setTitle("Changer la photo de profil");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Prendre une photo")) {
                Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(takePicture);
            } else if (options[item].equals("Choisir depuis la galerie")) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(pickPhoto);
            } else if (options[item].equals("Annuler")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Déconnexion réussie. Redirection...", Toast.LENGTH_SHORT).show();
        // Redirection vers l'activité de connexion
        finish();
    }
}