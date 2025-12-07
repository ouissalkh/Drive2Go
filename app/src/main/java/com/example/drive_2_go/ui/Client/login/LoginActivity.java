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
import com.example.drive_2_go.ui.Admin.Table_bord.HomeActivityAdmin;
import com.example.drive_2_go.ui.Admin.Table_bord.adminActivity;
import com.example.drive_2_go.ui.Client.accueil.AccueilActivity;
import com.example.drive_2_go.ui.Client.creationCompte.RegisterActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // 🔑 IDENTIFIANTS ADMIN PAR DÉFAUT
    private static final String ADMIN_EMAIL = "admin@drive2go.com";
    private static final String ADMIN_PASSWORD = "admin123";

    private EditText inputEmail, inputPassword;
    private TextView  btnGoToRegister;

    private Button btnLogin;

    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialiser Firebase avec la NOUVELLE base de données
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Vérifier la connexion Firebase
        Log.d(TAG, "📱 Firebase initialisé : " + db.getApp().getName());

        // Récupérer les vues
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.tvGoToRegister);

        progressBar = findViewById(R.id.progressBar);

        // Pré-remplir l'email si fourni (depuis VerifyCodeActivity)
        String prefilledEmail = getIntent().getStringExtra("email");
        if (prefilledEmail != null) {
            inputEmail.setText(prefilledEmail);
        }

        // Bouton connexion
        btnLogin.setOnClickListener(v -> loginUser());

        // Bouton inscription
        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });


    }

    private void loginUser() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        // Validation
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

        Log.d(TAG, "🔐 Tentative de connexion pour : " + email);

        //  VÉRIFICATION ADMIN EN PREMIER (sans Firebase Auth)
        if (email.equalsIgnoreCase(ADMIN_EMAIL) && password.equals(ADMIN_PASSWORD)) {
            Log.d(TAG, "✅ Admin détecté - Connexion directe");
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);

            Toast.makeText(this, "✅ Bienvenue Administrateur", Toast.LENGTH_SHORT).show();

            // Redirection vers l'interface admin
            Intent intent = new Intent(LoginActivity.this, adminActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // ✅ CONNEXION CLIENT via Firebase Authentication
        Log.d(TAG, "👤 Tentative de connexion client via Firebase Auth");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            Log.d(TAG, "✅ Firebase Auth réussi, UID : " + userId);

                            // Vérifier le rôle dans Firestore
                            checkUserRoleInFirestore(userId);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);

                        String errorMessage = "❌ Email ou mot de passe incorrect";

                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            Log.e(TAG, "❌ Erreur Firebase Auth : " + error);

                            if (error.contains("no user record")) {
                                errorMessage = "❌ Aucun compte trouvé avec cet email";
                            } else if (error.contains("password is invalid")) {
                                errorMessage = "❌ Mot de passe incorrect";
                            } else if (error.contains("network error")) {
                                errorMessage = "❌ Erreur réseau. Vérifiez votre connexion";
                            }
                        }

                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        Log.e(TAG, errorMessage);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    Log.e(TAG, "❌ Exception lors de la connexion : " + e.getMessage());
                    Toast.makeText(this, "❌ Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkUserRoleInFirestore(String userId) {
        Log.d(TAG, "🔍 Vérification du rôle dans Firestore pour UID : " + userId);

        // ✅ UTILISATION DE LA NOUVELLE BASE "LocationDeVoiture"
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();

                        if (document.exists()) {
                            String role = document.getString("role");
                            String nom = document.getString("nom");
                            String prenom = document.getString("prenom");

                            Log.d(TAG, "✅ Utilisateur trouvé : " + prenom + " " + nom + ", Rôle : " + role);

                            Toast.makeText(this, "✅ Bienvenue " + prenom + " !", Toast.LENGTH_SHORT).show();

                            // Redirection selon le rôle
                            if ("admin".equalsIgnoreCase(role)) {
                                Intent intent = new Intent(LoginActivity.this, adminActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            } else {
                                Intent intent = new Intent(LoginActivity.this, AccueilActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                            finish();
                        } else {
                            Log.e(TAG, "❌ Document utilisateur introuvable dans Firestore");
                            Toast.makeText(this,
                                    "❌ Profil utilisateur incomplet. Contactez le support",
                                    Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        }
                    } else {
                        Log.e(TAG, "❌ Erreur lors de la récupération des données Firestore");
                        Toast.makeText(this,
                                "❌ Erreur de connexion. Réessayez",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    Log.e(TAG, "❌ Exception Firestore : " + e.getMessage());
                    Toast.makeText(this, "❌ Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Vérifier si un utilisateur est déjà connecté
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "👤 Utilisateur déjà connecté : " + currentUser.getEmail());
            // Optionnel : rediriger automatiquement
            // checkUserRoleInFirestore(currentUser.getUid());
        }
    }
}