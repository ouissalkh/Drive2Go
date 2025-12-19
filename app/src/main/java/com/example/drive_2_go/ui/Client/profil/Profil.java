package com.example.drive_2_go.ui.Client.profil;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Client.accueil.AccueilActivity;
import com.example.drive_2_go.ui.Client.favoris.Favoris;
import com.example.drive_2_go.ui.Client.history.HistoryActivity;
import com.example.drive_2_go.ui.Client.login.LoginActivity;
import com.example.drive_2_go.ui.Client.notification.NotificationClientActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import androidx.appcompat.app.AlertDialog;


public class Profil extends AppCompatActivity {

    private static final String TAG = "ProfilActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;
    private ShapeableImageView profileImage;
    private TextView tvNomPrenom;
    private TextView tvVerificationStatus;
    private ImageButton buttonCamera;
    private TextView idReservation; // ID dans XML : id_reservation
    private TextView idNbrFavoris; // ID dans XML : id_nbrfavoris
    private TextView idEncours;    // ID dans XML : id_encours

    // ... Autres ImageViews pour la navigation ...
    private ImageView buttonBack;
    private TextView btn_notification, btn_logout;
    private View notificationBadge;
    private TextView btn_settings;
    private ImageButton buttonFavoris;
    private ImageButton buttonHome;
    private ImageButton buttonHistory;
    private ImageButton buttonProfil;

    // Références Firebase
    private com.google.firebase.firestore.ListenerRegistration notificationListener;
    private long lastReadTimestamp = 0;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private Uri selectedImageUri;

    private ActivityResultLauncher<Intent> galleryLauncher;




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil);

        initCloudinary();

        // Initialisation Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1. Initialisation des Vues
        profileImage = findViewById(R.id.profileImage);
        buttonCamera = findViewById(R.id.buttonCamera);
        tvNomPrenom = findViewById(R.id.tv_nom_prenom);
        tvVerificationStatus = findViewById(R.id.tv_verification_status);

        // Initialisation de la navigation
        buttonFavoris = findViewById(R.id.buttonFavoris);
        buttonHome = findViewById(R.id.buttonHome);
        buttonHistory = findViewById(R.id.buttonHistory);
        buttonProfil = findViewById(R.id.buttonProfil);
        buttonBack = findViewById(R.id.btnBack);
        btn_notification = findViewById(R.id.btn_notification);
        btn_logout = findViewById(R.id.btn_logout);
        btn_settings = findViewById(R.id.btn_settings);
        notificationBadge = findViewById(R.id.notification_badge);
        idReservation = findViewById(R.id.id_reservation);
        idNbrFavoris = findViewById(R.id.id_nbrfavoris);
        idEncours = findViewById(R.id.id_encours);

        // 2. Vérification de l'utilisateur et chargement des données
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            // AJOUTÉ : Initialisation du lanceur AVANT le chargement des données (bonne pratique)
            setupActivityResultLaunchers();
            loadUserProfileData(); // Chargement des données ici
        } else {
            Toast.makeText(this, "Veuillez vous connecter.", Toast.LENGTH_LONG).show();
            finish();
        }

        // 3. Configuration des Clics
        buttonCamera.setOnClickListener(v -> checkPermissionAndLaunchGallery());
        buttonFavoris.setOnClickListener(v -> openFavoris());
        buttonHome.setOnClickListener(v -> openAccueil());
        buttonHistory.setOnClickListener(v -> openHistory());
        buttonProfil.setOnClickListener(v -> Toast.makeText(this,"Déjà ici", Toast.LENGTH_SHORT).show());
        selectButton(buttonProfil);

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(Profil.this, AccueilActivity.class);
            startActivity(intent);
            finish();
        });

        btn_notification.setOnClickListener(v -> {
            notificationBadge.setVisibility(View.GONE);

            // 1. Marquer localement le temps pour les alertes globales
            long currentTime = System.currentTimeMillis() / 1000;
            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .edit()
                    .putLong("last_global_read_timestamp", currentTime)
                    .apply();

            // 2. Ouvrir l'activité
            Intent intent = new Intent(Profil.this, NotificationClientActivity.class);
            startActivity(intent);
        });

        btn_logout.setOnClickListener(v -> {

            AlertDialog dialog = new AlertDialog.Builder(Profil.this)
                    .setTitle("Déconnexion")
                    .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
                    .setCancelable(false)
                    .setPositiveButton("Oui", (d, which) -> {
                        FirebaseAuth.getInstance().signOut();

                        Intent intent = new Intent(Profil.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Annuler", (d, which) -> d.dismiss())
                    .create();

            dialog.show();

            // 🎨 Changer les couleurs des boutons
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.green_700));

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.red_500));
        });



        // Clic pour la modification de profil (Settings.class)
        btn_settings.setOnClickListener(v -> {
            Intent intent = new Intent(Profil.this, Settings.class);
            startActivity(intent);
        });
    }

    /**
     * Compte le nombre de notifications non lues (statut 'isRead' = false)
     * pour les alertes personnelles (A et B).
     */
    private void checkUnreadNotifications() {
        if (currentUserId == null) return;

        // Requête A : Réservations non lues
        Task<QuerySnapshot> unreadReservationsTask = db.collection("reservations")
                .whereEqualTo("userId", currentUserId)
                .whereIn("status", List.of("acceptée", "refusée"))
                .whereEqualTo("clientRead", false) // ⭐️ FILTRE CLÉ ⭐️
                .get();

        // Requête B : Alertes Spécifiques non lues
        Task<QuerySnapshot> unreadUserAlertsTask = db.collection("user_alerts")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("clientRead", false) // ⭐️ FILTRE CLÉ ⭐️
                .get();

        // Les alertes globales (Nouvelles Voitures) ne sont PAS incluses dans le compte
        // car elles n'ont pas de champ 'isRead' par utilisateur.

        Tasks.whenAllSuccess(unreadReservationsTask, unreadUserAlertsTask)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Object> results = task.getResult();
                        QuerySnapshot reservationsResult = (QuerySnapshot) results.get(0);
                        QuerySnapshot userAlertsResult = (QuerySnapshot) results.get(1);

                        int totalUnread = reservationsResult.size() + userAlertsResult.size();

                        // ⭐️ MISE À JOUR DE LA VISIBILITÉ DU BADGE ⭐️
                        if (totalUnread > 0) {
                            notificationBadge.setVisibility(View.VISIBLE);
                        } else {
                            notificationBadge.setVisibility(View.GONE);
                        }

                        Log.d(TAG, "Notifications non lues trouvées: " + totalUnread);

                    } else {
                        Log.e(TAG, "Erreur lors de la vérification des notifications non lues: " + task.getException());
                        // En cas d'erreur, le badge reste caché par défaut (GONE).
                    }
                });
    }

    private void initCloudinary() {
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

    // Le 'setupActivityResultLaunchers' est maintenant la seule source pour 'galleryLauncher'
    private void setupActivityResultLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        profileImage.setImageURI(selectedImageUri); // Afficher immédiatement
                        uploadProfilePhoto(); // Déclencher l'upload immédiatement
                    }
                });
    }



    private void loadUserProfileData() {
        if (currentUserId == null) return;

        db.collection("users").document(currentUserId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {

                                // --- 1. CHARGEMENT DES DONNÉES DE BASE (Nom, Prénom, Statut, Photo) ---
                                // ... (Votre code pour Nom/Prénom et Statut de Vérification) ...

                                String nom = document.getString("nom");
                                String prenom = document.getString("prenom");
                                if (nom != null && prenom != null) {
                                    tvNomPrenom.setText(prenom + " " + nom);
                                } else {
                                    tvNomPrenom.setText("Utilisateur inconnu");
                                }

                                Boolean isVerified = document.getBoolean("isVerified");
                                if (isVerified != null && isVerified) {
                                    tvVerificationStatus.setText("Profil Vérifié");
                                    tvVerificationStatus.setTextColor(ContextCompat.getColor(Profil.this, R.color.green_700));
                                } else {
                                    tvVerificationStatus.setText("Profil Non Vérifié");
                                    tvVerificationStatus.setTextColor(ContextCompat.getColor(Profil.this, R.color.red_500));
                                }

                                String photoUrl = document.getString("photoUrl");
                                if (photoUrl != null && !photoUrl.isEmpty()) {
                                    Glide.with(Profil.this)
                                            .load(photoUrl)
                                            .placeholder(R.drawable.imageprofil)
                                            .error(R.drawable.imageprofil)
                                            .into(profileImage);
                                } else {
                                    profileImage.setImageResource(R.drawable.imageprofil);
                                }

                                // --- 2. CHARGEMENT DES STATISTIQUES ---
                                countConfirmedReservations(); // Compter les réservations confirmées
                                countFavorites(document);     // Compter les favoris à partir du document utilisateur
                                countOngoingRentals();        // Compter les locations en cours
                                checkUnreadNotifications();

                            } else {
                                Log.d(TAG, "Aucun document trouvé pour l'utilisateur.");
                                Toast.makeText(Profil.this, "Profil incomplet. Mettez à jour vos infos.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e(TAG, "Erreur lors de la récupération Firestore : " + task.getException());
                            Toast.makeText(Profil.this, "Erreur de connexion aux données.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Compte le nombre de réservations confirmées de l'utilisateur.
     */
    private void countConfirmedReservations() {
        if (currentUserId == null) return;

        // Requête dans la collection 'reservations'
        db.collection("reservations")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("status", "acceptée") // ASSUMEZ que le statut est 'Confirmed'
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        idReservation.setText(String.valueOf(count));
                    } else {
                        Log.e(TAG, "Erreur lors du comptage des réservations : " + task.getException());
                        idReservation.setText("0");
                    }
                });
    }

    /**
     * Compte le nombre de voitures favorites à partir du document utilisateur.
     */
    private void countFavorites(DocumentSnapshot userDocument) {
        // Le champ 'favoriteCarIds' est une liste d'IDs de voitures dans le document 'users'.

        try {
            // Récupérer la liste des IDs de favoris en utilisant le nom de champ correct
            @SuppressWarnings("unchecked")
            java.util.List<String> favoritesList = (java.util.List<String>) userDocument.get("favoriteCarIds");

            int count = (favoritesList != null) ? favoritesList.size() : 0;
            idNbrFavoris.setText(String.valueOf(count));
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du comptage des favoris: Le champ 'favoriteCarIds' est manquant ou n'est pas une liste.", e);
            idNbrFavoris.setText("0");
        }
    }


    /**
     * Compte le nombre de voitures en cours de location (statut "En Cours").
     */
    private void countOngoingRentals() {
        if (currentUserId == null) return;

        // Requête dans la collection 'reservations'
        db.collection("reservations")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("status", "en cours") // ASSUMEZ que le statut est 'Ongoing' (ou 'En cours')
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        idEncours.setText(String.valueOf(count));
                    } else {
                        Log.e(TAG, "Erreur lors du comptage des locations en cours : " + task.getException());
                        idEncours.setText("0");
                    }
                });
    }

    private void selectButton(ImageButton button) {
        button.setSelected(true);
    }

    private void openFavoris(){
        startActivity(new Intent(Profil.this, Favoris.class));
    }
    private void openAccueil(){
        startActivity(new Intent(Profil.this, AccueilActivity.class));
    }
    private void openHistory(){
        startActivity(new Intent(Profil.this, HistoryActivity.class));
    }

    // Reste des méthodes checkPermissionAndLaunchGallery, launchGallery, onRequestPermissionsResult, uploadProfilePhoto
    // qui sont correctement implémentées
    private void checkPermissionAndLaunchGallery() {
        String permissionToAsk;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionToAsk = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permissionToAsk = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permissionToAsk) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permissionToAsk}, PERMISSION_REQUEST_CODE);
        } else {
            launchGallery();
        }
    }

    private void launchGallery() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(pickPhoto);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchGallery();
            } else {
                Toast.makeText(this, "Permission de lecture nécessaire pour accéder à la galerie.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadProfilePhoto() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Sélectionnez d'abord une photo.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(TAG, "Début de l'upload pour User ID: " + currentUserId);
        String folderName = "profile_images";
        String filePublicId = currentUserId;

        MediaManager.get().upload(selectedImageUri)
                .option("folder", folderName)
                .option("public_id", filePublicId)
                .option("overwrite", true)
                .option("upload_preset", "drive_2_go_unsigned")
                .callback(new UploadCallback() {

                    @Override
                    public void onStart(String requestId) {
                        Toast.makeText(Profil.this, "Début du téléchargement...", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String photoUrl = (String) resultData.get("secure_url");

                        if (photoUrl == null || photoUrl.isEmpty()) {
                            photoUrl = (String) resultData.get("url");
                        }

                        Log.d(TAG, "URL Cloudinary Récupérée: " + photoUrl);

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            // 2. Mise à jour du document utilisateur dans Firestore
                            db.collection("users").document(currentUserId).update("photoUrl", photoUrl)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            Toast.makeText(Profil.this, "Photo de profil mise à jour avec succès.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.e(TAG, "Erreur Firestore lors de la MAJ de photoUrl: " + updateTask.getException());
                                        }
                                    });
                        } else {
                            Log.e(TAG, "❌ photoUrl est null ou vide après l'upload Cloudinary.");
                            Toast.makeText(Profil.this, "Échec: URL Cloudinary non trouvée.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(Profil.this, "Erreur Cloudinary: " + error.getDescription(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Cloudinary Error: " + error.getDescription());
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Optionnel: Afficher la progression
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Gérer la reprise
                    }
                }).dispatch();
    }

    private void startNotificationRealtimeListener() {
        if (currentUserId == null || currentUserId.isEmpty()) return;

        // Écoute en temps réel des réservations non lues
        notificationListener = db.collection("reservations")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("clientRead", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Erreur d'écoute notifications", e);
                        return;
                    }

                    if (snapshots != null) {
                        // Si on a des réservations non lues, on affiche le badge
                        if (!snapshots.isEmpty()) {
                            notificationBadge.setVisibility(View.VISIBLE);
                        } else {
                            // Sinon on vérifie rapidement les alertes une seule fois
                            checkUserAlertsOnce();
                        }
                    }
                });
    }

    private void checkUserAlertsOnce() {
        db.collection("user_alerts")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("clientRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        notificationBadge.setVisibility(View.VISIBLE);
                    } else {
                        notificationBadge.setVisibility(View.GONE);
                    }
                });
    }




    @Override
    protected void onResume() {
        super.onResume();
        // Re-calculer le badge au retour sur l'écran
        fetchUnreadNotificationCount();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startNotificationListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    /**
     * Charge le nombre de notifications au démarrage (Statique)
     */
    private void fetchUnreadNotificationCount() {
        if (currentUserId == null || currentUserId.isEmpty()) return;

        lastReadTimestamp = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getLong("last_global_read_timestamp", 0);

        Task<QuerySnapshot> resTask = db.collection("reservations")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("clientRead", false).get();

        Task<QuerySnapshot> userAlertsTask = db.collection("user_alerts")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("clientRead", false).get();

        Task<QuerySnapshot> globalAlertsTask = db.collection("global_alerts")
                .whereEqualTo("type", "New_Car_Added")
                .whereGreaterThan("timestamp", lastReadTimestamp)
                .get();

        Tasks.whenAllSuccess(resTask, userAlertsTask, globalAlertsTask)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int total = 0;
                        for (Object res : task.getResult()) {
                            total += ((QuerySnapshot) res).size();
                        }
                        updateNotificationBadge(total > 0);
                    }
                });
    }

    /**
     * Écoute les changements en temps réel (Dynamique)
     */
    private void startNotificationListener() {
        if (currentUserId == null || currentUserId.isEmpty()) return;

        // Écoute des réservations et alertes privées
        notificationListener = db.collection("reservations")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("clientRead", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null && !snapshots.isEmpty()) {
                        updateNotificationBadge(true);
                    } else {
                        // Si vide, on revérifie les autres sources
                        fetchUnreadNotificationCount();
                    }
                });
    }

    /**
     * Met à jour l'UI du badge
     */
    private void updateNotificationBadge(boolean show) {
        if (notificationBadge != null) {
            notificationBadge.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}