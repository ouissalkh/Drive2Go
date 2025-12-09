package com.example.drive_2_go.ui.Client.creationCompte;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.drive_2_go.ui.Client.login.LoginActivity;
import com.example.drive_2_go.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RegisterActivity extends AppCompatActivity {

    private EditText inputNom, inputPrenom, inputTelephone, inputEmail, inputPassword, inputConfirmPassword;
    private Button btnRegister, btnGoToLogin;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EmailSender emailSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialiser Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
       emailSender = new EmailSender();

        // Récupérer les vues
        inputNom = findViewById(R.id.inputNom);
        inputPrenom = findViewById(R.id.inputPrenom);
        inputTelephone = findViewById(R.id.inputTelephone);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        progressBar = findViewById(R.id.progressBar);

        // Bouton inscription
        btnRegister.setOnClickListener(v -> registerUser());

        // Bouton retour connexion
        btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String nom = inputNom.getText().toString().trim();
        String prenom = inputPrenom.getText().toString().trim();
        String telephone = inputTelephone.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();

        // Validation des champs
        if (TextUtils.isEmpty(nom)) {
            inputNom.setError("Le nom est requis");
            return;
        }

        if (TextUtils.isEmpty(prenom)) {
            inputPrenom.setError("Le prénom est requis");
            return;
        }

        if (TextUtils.isEmpty(telephone)) {
            inputTelephone.setError("Le téléphone est requis");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            inputEmail.setError("L'email est requis");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            inputPassword.setError("Le mot de passe est requis");
            return;
        }

        if (password.length() < 6) {
            inputPassword.setError("Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Les mots de passe ne correspondent pas");
            return;
        }

        // Afficher le loader
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // Générer un code de vérification à 6 chiffres
        String verificationCode = generateVerificationCode();

        // 🔥 IMPORTANT : Ne PAS créer l'utilisateur dans Firebase Auth tout de suite
        // On le créera seulement APRÈS la vérification du code

        // Sauvegarder temporairement dans Firestore
        saveTemporaryUser(email, password, nom, prenom, telephone, verificationCode);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Code à 6 chiffres
        return String.valueOf(code);
    }

    private void saveTemporaryUser(String email, String password, String nom,
                                   String prenom, String telephone, String verificationCode) {
        // Créer un ID temporaire
        String tempId = "temp_" + System.currentTimeMillis();

        Map<String, Object> tempUser = new HashMap<>();
        tempUser.put("email", email);
        tempUser.put("password", password); // ⚠️ Temporaire, sera supprimé après vérification
        tempUser.put("nom", nom);
        tempUser.put("prenom", prenom);
        tempUser.put("telephone", telephone);
        tempUser.put("role", "client");
        tempUser.put("isVerified", false);
        tempUser.put("verificationCode", verificationCode);
        tempUser.put("createdAt", System.currentTimeMillis());

        // Sauvegarder dans une collection temporaire
        db.collection("temp_users").document(tempId)
                .set(tempUser)
                .addOnSuccessListener(unused -> {
                    // Envoyer le code par email via Brevo
                    sendVerificationEmail(email, verificationCode, prenom);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this,
                            "❌ Erreur : " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void sendVerificationEmail(String email, String code, String prenom) {
        // Envoyer l'email avec le code via Brevo
        emailSender.sendVerificationCode(email, code, prenom, new EmailSender.EmailCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);

                Toast.makeText(RegisterActivity.this,
                        "✅ Code envoyé à " + email,
                        Toast.LENGTH_LONG).show();

                // Rediriger vers la page de vérification
                Intent intent = new Intent(RegisterActivity.this, VerifyCodeActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);

                Toast.makeText(RegisterActivity.this,
                        "⚠️ Erreur d'envoi email : " + error + "\nVeuillez réessayer",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}