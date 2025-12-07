package com.example.drive_2_go.ui.Client.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.drive_2_go.R;
// IMPORTANT : Vérifiez ces imports vers VOS activités HomeActivityAdmin et AccueilActivity
import com.example.drive_2_go.ui.Admin.Table_bord.adminActivity;
import com.example.drive_2_go.ui.Client.accueil.AccueilActivity;
import com.example.drive_2_go.ui.Client.creationCompte.RegisterActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // 🔑 IDENTIFIANTS ADMIN PAR DÉFAUT (Hardcoded)
    private static final String ADMIN_EMAIL = "admin@drive2go.com";
    private static final String ADMIN_PASSWORD = "admin123";

    // UI Elements
    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Vérifiez que votre layout s'appelle bien activity_login

        // Initialisation Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Liaison des vues
        inputEmail = findViewById(R.id.inputEmail);       // Vérifiez les IDs dans votre XML
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvGoToRegister);
        progressBar = findViewById(R.id.progressBar);

        // Clic sur "S'inscrire"
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // Clic sur "Se connecter"
        btnLogin.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                inputEmail.setError("L'email est requis");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                inputPassword.setError("Le mot de passe est requis");
                return;
            }

            // Afficher le chargement
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);

            // 1. DÉTECTION SPÉCIALE ADMIN
            if (email.equals(ADMIN_EMAIL) && password.equals(ADMIN_PASSWORD)) {
                loginAdminHardcoded();
            } else {
                // 2. CONNEXION NORMALE (CLIENT)
                loginUser(email, password);
            }
        });
    }

    // ----------------------------------------------------------------
    //  PARTIE 1 : LOGIQUE ADMIN (Corrigée pour Firestore)
    // ----------------------------------------------------------------

    private void loginAdminHardcoded() {
        // Tentative de connexion avec Auth
        mAuth.signInWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "✅ Connexion Admin réussie via Auth");
                    // VÉRIFICATION CRUCIALE : Est-ce que le profil existe dans Firestore ?
                    checkAndCreateAdminProfile(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "⚠️ Compte admin inexistant dans Auth, création en cours...");
                    // Si le compte n'existe pas du tout (premier lancement), on le crée
                    createAdminAccount();
                });
    }

    private void createAdminAccount() {
        mAuth.createUserWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "✅ Compte Admin créé dans Auth");
                    // Maintenant on crée le profil Firestore obligatoirement
                    checkAndCreateAdminProfile(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Erreur création Admin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Cette méthode vérifie si le document Admin existe dans 'users'.
     * S'il n'existe pas, elle le crée. C'est ça qui résout votre problème de permission.
     */
    private void checkAndCreateAdminProfile(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;

        String uid = firebaseUser.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Tout est bon, le profil existe
                        Log.d(TAG, "✅ Profil Admin existant. Redirection...");
                        goToAdminHome();
                    } else {
                        // LE PROFIL MANQUE : ON LE CRÉE
                        Log.d(TAG, "⚡ Profil Admin manquant dans Firestore. Création...");

                        Map<String, Object> adminData = new HashMap<>();
                        adminData.put("id", uid);
                        adminData.put("email", ADMIN_EMAIL);
                        adminData.put("nom", "Administrateur");
                        adminData.put("prenom", "Principal");
                        adminData.put("favoriteCarIds", "null");
                        adminData.put("role", "admin"); // Champ essentiel pour vos règles de sécurité
                        adminData.put("telephone", "0000000000");

                        db.collection("users").document(uid)
                                .set(adminData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "✅ Profil Admin sauvegardé dans Firestore !");
                                    goToAdminHome();
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    btnLogin.setEnabled(true);
                                    Toast.makeText(this, "Erreur sauvegarde Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "Erreur lecture Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void goToAdminHome() {
        progressBar.setVisibility(View.GONE);
        // Redirection vers l'interface Admin
        Intent intent = new Intent(LoginActivity.this, adminActivity.class);
        startActivity(intent);
        finish();
    }

    // ----------------------------------------------------------------
    //  PARTIE 2 : LOGIQUE CLIENT (Classique)
    // ----------------------------------------------------------------

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Connexion réussie, maintenant on vérifie le rôle dans Firestore
                    checkUserRoleInFirestore(authResult.getUser().getUid());
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Erreur de connexion : " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkUserRoleInFirestore(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    if (documentSnapshot.exists()) {
                        // Récupérer le rôle (si vous avez un champ "role")
                        String role = documentSnapshot.getString("role");

                        if ("admin".equals(role)) {
                            // C'est un admin connecté via le formulaire standard
                            Intent intent = new Intent(LoginActivity.this, adminActivity.class);
                            startActivity(intent);
                        } else {
                            // C'est un client
                            Intent intent = new Intent(LoginActivity.this, AccueilActivity.class);
                            startActivity(intent);
                        }
                        finish();
                    } else {
                        // Cas rare : Utilisateur dans Auth mais pas dans Firestore
                        Toast.makeText(this, "Erreur: Profil introuvable", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        btnLogin.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "Erreur vérification rôle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Si l'utilisateur est déjà connecté, on peut le rediriger directement
        // (Optionnel : vous pouvez décommenter si vous voulez la reconnexion auto)
        /*
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
             // checkUserRoleInFirestore(currentUser.getUid());
        }
        */
    }
}