package com.example.drive_2_go.ui.Client.login;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.drive_2_go.ui.Admin.Table_bord.adminActivity;
import com.example.drive_2_go.ui.Client.accueil.AccueilActivity;
import com.example.drive_2_go.ui.Client.creationCompte.RegisterActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        progressBar = findViewById(R.id.progressBar);

        // Pré-remplir l'email si reçu depuis VerifyCodeActivity
        String emailFromIntent = getIntent().getStringExtra("email");
        if (emailFromIntent != null) {
            inputEmail.setText(emailFromIntent);
        }

        btnLogin.setOnClickListener(v -> loginUser());

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        //  APPEL DE LA MÉTHODE
        checkAndCreateDefaultAdmin();
    } // FIN DE ONCREATE

    //  DÉFINITION DE LA MÉTHODE EN DEHORS DE ONCREATE
    private void checkAndCreateDefaultAdmin() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean adminCreated = prefs.getBoolean("admin_created", false);

        if (!adminCreated) {
            // Vérifier si l'admin existe déjà dans Firestore
            db.collection("users")
                    .whereEqualTo("email", "admin@drive2go.com")
                    .whereEqualTo("role", "admin")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            // Pas d'admin, on le crée
                            createDefaultAdmin();
                        } else {
                            // Admin existe déjà
                            prefs.edit().putBoolean("admin_created", true).apply();
                            Log.d(TAG, "Admin par défaut existe déjà, drapeau mis à jour.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erreur vérification admin Firestore: " + e.getMessage());
                    });
        }
    }

    //DÉFINITION DE LA MÉTHODE EN DEHORS DE ONCREATE
    private void createDefaultAdmin() {
        String adminEmail = "admin@drive2go.com";
        String adminPassword = "Drive2Go2025!"; // Mot de passe sécurisé

        mAuth.createUserWithEmailAndPassword(adminEmail, adminPassword)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    Map<String, Object> admin = new HashMap<>();
                    admin.put("id", uid);
                    admin.put("email", adminEmail);
                    admin.put("nom", "Admin");
                    admin.put("prenom", "Principal");
                    admin.put("telephone", "0600000000");
                    admin.put("role", "admin");
                    admin.put("isVerified", true);
                    admin.put("dateInscription", System.currentTimeMillis());

                    db.collection("users").document(uid)
                            .set(admin)
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "✅ Admin par défaut créé");
                                getSharedPreferences("app_prefs", MODE_PRIVATE)
                                        .edit()
                                        .putBoolean("admin_created", true)
                                        .apply();

                                // Optionnel : Afficher une notification discrète
                                Toast.makeText(this,
                                        "Admin créé : " + adminEmail,
                                        Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Erreur création doc admin Firestore: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur création admin Firebase Auth: " + e.getMessage());

                    if (e.getMessage() != null && e.getMessage().contains("email address is already in use")) {
                        // Si le compte existe déjà, on considère qu'il est créé
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                                .edit()
                                .putBoolean("admin_created", true)
                                .apply();
                    }
                });
    }


    private void loginUser() {
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

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        Log.d(TAG, "Tentative de connexion pour : " + email);

        // Étape 1 : Essayer de se connecter directement avec Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Connexion Firebase Auth réussie");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            String userId = user.getUid();
                            Log.d(TAG, "User ID : " + userId);

                            // Vérifier le rôle dans Firestore
                            checkUserRoleAndRedirect(userId, email);
                        }
                    } else {
                        // Échec de connexion Firebase Auth
                        Log.e(TAG, "❌ Échec connexion Firebase Auth");

                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            Log.e(TAG, "Erreur : " + error);

                            if (error.contains("no user record") || error.contains("user not found")) {
                                // Le compte n'existe pas dans Firebase Auth
                                // Vérifier s'il existe dans Firestore (cas de l'admin créé manuellement)
                                Log.d(TAG, "Compte inexistant dans Auth, vérification Firestore...");
                                checkFirestoreForManualUser(email, password);
                            } else {
                                progressBar.setVisibility(View.GONE);
                                btnLogin.setEnabled(true);

                                String errorMessage;
                                if (error.contains("password is invalid") || error.contains("wrong-password")) {
                                    errorMessage = "Mot de passe incorrect";
                                } else if (error.contains("too-many-requests")) {
                                    errorMessage = "Trop de tentatives. Réessayez plus tard";
                                } else {
                                    errorMessage = "Email ou mot de passe incorrect";
                                }

                                Toast.makeText(this, "❌ " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            progressBar.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);
                            Toast.makeText(this, "❌ Erreur de connexion", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Log.e(TAG, "❌ Exception : " + e.getMessage());
                    Toast.makeText(this, "❌ Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkFirestoreForManualUser(String email, String password) {
        Log.d(TAG, "Recherche utilisateur dans Firestore : " + email);

        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        Log.d(TAG, "✅ Utilisateur trouvé dans Firestore");
                        DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                        String role = userDoc.getString("role");
                        Boolean isVerified = userDoc.getBoolean("isVerified");

                        Log.d(TAG, "Rôle : " + role + ", Vérifié : " + isVerified);

                        if ("admin".equalsIgnoreCase(role)) {
                            // Admin créé manuellement, créer le compte Firebase Auth
                            Log.d(TAG, "Admin détecté, création compte Firebase Auth...");
                            createAdminInFirebaseAuth(email, password, userDoc);
                        } else if (isVerified != null && isVerified) {
                            // Client vérifié mais pas de compte Auth (cas anormal)
                            Log.e(TAG, "Client vérifié sans compte Auth (erreur)");
                            progressBar.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);
                            Toast.makeText(this,
                                    "❌ Erreur de compte. Contactez le support",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // Client non vérifié
                            progressBar.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);
                            Toast.makeText(this,
                                    "⚠️ Veuillez vérifier votre compte avec le code reçu par email",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Utilisateur n'existe ni dans Auth ni dans Firestore
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Log.e(TAG, "❌ Utilisateur introuvable");
                        Toast.makeText(this,
                                "❌ Compte introuvable. Veuillez vous inscrire",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Log.e(TAG, "❌ Erreur Firestore : " + e.getMessage());
                    Toast.makeText(this, "❌ Erreur de connexion", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkUserRoleAndRedirect(String userId, String email) {
        Log.d(TAG, "Vérification du rôle pour userId : " + userId);

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        String role = document.getString("role");
                        Boolean isVerified = document.getBoolean("isVerified");

                        Log.d(TAG, "Rôle : " + role + ", Vérifié : " + isVerified);

                        Intent intent;
                        if ("admin".equalsIgnoreCase(role)) {
                            intent = new Intent(LoginActivity.this, adminActivity.class);
                            Toast.makeText(this, "🔐 Bienvenue Admin !", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "✅ Redirection vers AdminMainActivity");
                        } else {
                            if (isVerified != null && isVerified) {
                                intent = new Intent(LoginActivity.this, AccueilActivity.class);
                                Toast.makeText(this, "👋 Bienvenue !", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "✅ Redirection vers HomeActivityClient");
                            } else {
                                Toast.makeText(this,
                                        "⚠️ Veuillez vérifier votre compte",
                                        Toast.LENGTH_LONG).show();
                                Log.e(TAG, "❌ Compte non vérifié");
                                return;
                            }
                        }

                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // Document n'existe pas dans Firestore
                        Log.e(TAG, "❌ Document utilisateur introuvable dans Firestore");
                        Toast.makeText(this,
                                "❌ Erreur : Profil utilisateur introuvable",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Log.e(TAG, "❌ Erreur récupération profil : " + e.getMessage());
                    Toast.makeText(this, "❌ Erreur de connexion", Toast.LENGTH_SHORT).show();
                });
    }

    private void createAdminInFirebaseAuth(String email, String password, DocumentSnapshot userDoc) {
        Log.d(TAG, "Création compte Admin dans Firebase Auth...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Compte admin créé dans Firebase Auth");

                        String newUserId = mAuth.getCurrentUser().getUid();
                        String oldDocId = userDoc.getId();

                        Log.d(TAG, "Ancien ID : " + oldDocId + ", Nouveau ID : " + newUserId);

                        // Mettre à jour le document Firestore avec le nouveau ID
                        updateAdminDocument(oldDocId, newUserId, userDoc);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Log.e(TAG, "❌ Erreur création admin : " + task.getException().getMessage());
                        Toast.makeText(this,
                                "❌ Erreur : " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateAdminDocument(String oldDocId, String newUserId, DocumentSnapshot oldDoc) {
        Log.d(TAG, "Mise à jour document admin...");

        Map<String, Object> userData = oldDoc.getData();
        userData.put("id", newUserId);

        db.collection("users").document(newUserId)
                .set(userData)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✅ Nouveau document créé avec ID : " + newUserId);

                    // Supprimer l'ancien document si différent
                    if (!oldDocId.equals(newUserId)) {
                        db.collection("users").document(oldDocId)
                                .delete()
                                .addOnSuccessListener(v -> {
                                    Log.d(TAG, "✅ Ancien document supprimé : " + oldDocId);
                                });
                    }

                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    // Rediriger vers l'interface admin
                    Intent intent = new Intent(LoginActivity.this, adminActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                    Toast.makeText(this, "🔐 Bienvenue Admin !", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Log.e(TAG, "❌ Erreur mise à jour document : " + e.getMessage());
                    Toast.makeText(this, "❌ Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}