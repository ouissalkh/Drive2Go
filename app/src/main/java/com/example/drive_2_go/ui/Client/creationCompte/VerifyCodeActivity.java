package com.example.drive_2_go.ui.Client.creationCompte;

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

import com.example.drive_2_go.ui.Client.login.LoginActivity;
import com.example.drive_2_go.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class VerifyCodeActivity extends AppCompatActivity {

    private static final String TAG = "VerifyCodeActivity";

    private TextView tvEmail, tvInfo;
    private EditText inputCode;
    private Button btnVerify, btnResendCode, btnGoToLogin;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EmailSender emailSender;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        emailSender = new EmailSender();

        userEmail = getIntent().getStringExtra("email");

        tvEmail = findViewById(R.id.tvEmail);
        tvInfo = findViewById(R.id.tvInfo);
        inputCode = findViewById(R.id.inputCode);
        btnVerify = findViewById(R.id.btnVerify);
        btnResendCode = findViewById(R.id.btnResendCode);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        progressBar = findViewById(R.id.progressBar);

        if (userEmail != null) {
            tvEmail.setText(userEmail);
        }

        btnVerify.setOnClickListener(v -> verifyCode());
        btnResendCode.setOnClickListener(v -> resendVerificationCode());
        btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(VerifyCodeActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void verifyCode() {
        String code = inputCode.getText().toString().trim();

        if (TextUtils.isEmpty(code)) {
            inputCode.setError("Entrez le code reçu");
            return;
        }

        if (code.length() != 6) {
            inputCode.setError("Le code doit contenir 6 chiffres");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnVerify.setEnabled(false);

        Log.d(TAG, "Vérification du code pour : " + userEmail);

        // Récupérer l'utilisateur temporaire
        db.collection("temp_users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String storedCode = document.getString("verificationCode");

                        Log.d(TAG, "Code stocké : " + storedCode + ", Code entré : " + code);

                        if (code.equals(storedCode)) {
                            Log.d(TAG, "✅ Code correct, création du compte Firebase");
                            createFirebaseAccount(document);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            btnVerify.setEnabled(true);
                            inputCode.setError("Code incorrect");
                            Toast.makeText(this,
                                    "❌ Code incorrect. Vérifiez votre email",
                                    Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "❌ Code incorrect");
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnVerify.setEnabled(true);
                        Toast.makeText(this,
                                "❌ Session expirée. Veuillez vous réinscrire",
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "❌ Aucun utilisateur temporaire trouvé");
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnVerify.setEnabled(true);
                    Toast.makeText(this, "❌ Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "❌ Erreur Firestore : " + e.getMessage());
                });
    }

    private void createFirebaseAccount(DocumentSnapshot tempUserDoc) {
        String email = tempUserDoc.getString("email");
        String password = tempUserDoc.getString("password");
        String nom = tempUserDoc.getString("nom");
        String prenom = tempUserDoc.getString("prenom");
        String telephone = tempUserDoc.getString("telephone");

        Log.d(TAG, "Création compte Firebase Auth pour : " + email);

        // Créer le compte dans Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Compte Firebase Auth créé avec succès");

                        String userId = mAuth.getCurrentUser().getUid();
                        Log.d(TAG, "User ID Firebase : " + userId);

                        // Sauvegarder dans la vraie collection "users"
                        saveUserToFirestore(userId, nom, prenom, telephone, email, tempUserDoc.getId());
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnVerify.setEnabled(true);

                        String errorMessage = "Erreur de création du compte";
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            Log.e(TAG, "❌ Erreur Firebase Auth : " + error);

                            if (error.contains("email address is already in use")) {
                                errorMessage = "Cet email est déjà utilisé. Le compte existe peut-être déjà.";

                                // SOLUTION : Si le compte existe déjà dans Auth,
                                // on récupère juste son ID et on le sauvegarde dans Firestore
                                mAuth.signInWithEmailAndPassword(email, password)
                                        .addOnSuccessListener(authResult -> {
                                            String existingUserId = authResult.getUser().getUid();
                                            Log.d(TAG, "Compte existant trouvé, ID : " + existingUserId);
                                            saveUserToFirestore(existingUserId, nom, prenom, telephone, email, tempUserDoc.getId());
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "❌ Erreur : Le compte existe mais le mot de passe est incorrect", Toast.LENGTH_LONG).show();
                                        });
                                return;
                            } else {
                                errorMessage = error;
                            }
                        }

                        Toast.makeText(this, "❌ " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnVerify.setEnabled(true);
                    Log.e(TAG, "❌ Exception Firebase Auth : " + e.getMessage());
                    Toast.makeText(this, "❌ Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserToFirestore(String userId, String nom, String prenom,
                                     String telephone, String email, String tempDocId) {

        Log.d(TAG, "Sauvegarde dans Firestore users/" + userId);

        Map<String, Object> user = new HashMap<>();
        user.put("id", userId);
        user.put("nom", nom);
        user.put("prenom", prenom);
        user.put("telephone", telephone);
        user.put("email", email);
        user.put("role", "client");
        user.put("isVerified", true);
        user.put("dateInscription", System.currentTimeMillis());

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✅ Utilisateur sauvegardé dans Firestore");

                    // Supprimer l'utilisateur temporaire
                    db.collection("temp_users").document(tempDocId)
                            .delete()
                            .addOnSuccessListener(v -> {
                                Log.d(TAG, "✅ Utilisateur temporaire supprimé");
                            });

                    progressBar.setVisibility(View.GONE);
                    btnVerify.setEnabled(true);

                    Toast.makeText(this,
                            "✅ Compte créé avec succès ! Vous pouvez vous connecter",
                            Toast.LENGTH_LONG).show();

                    // Déconnecter et rediriger vers la connexion
                    mAuth.signOut();
                    Log.d(TAG, "✅ Déconnexion, redirection vers login");

                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.putExtra("email", email); // Pré-remplir l'email
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnVerify.setEnabled(true);
                    Log.e(TAG, "❌ Erreur sauvegarde Firestore : " + e.getMessage());
                    Toast.makeText(this,
                            "❌ Erreur : " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void resendVerificationCode() {
        progressBar.setVisibility(View.VISIBLE);
        btnResendCode.setEnabled(false);

        String newCode = generateVerificationCode();

        db.collection("temp_users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String prenom = document.getString("prenom");

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("verificationCode", newCode);

                        db.collection("temp_users").document(document.getId())
                                .update(updates)
                                .addOnSuccessListener(unused -> {
                                    emailSender.sendVerificationCode(userEmail, newCode, prenom,
                                            new EmailSender.EmailCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    progressBar.setVisibility(View.GONE);
                                                    btnResendCode.setEnabled(true);
                                                    Toast.makeText(VerifyCodeActivity.this,
                                                            "✅ Nouveau code envoyé à " + userEmail,
                                                            Toast.LENGTH_SHORT).show();
                                                }

                                                @Override
                                                public void onFailure(String error) {
                                                    progressBar.setVisibility(View.GONE);
                                                    btnResendCode.setEnabled(true);
                                                    Toast.makeText(VerifyCodeActivity.this,
                                                            "❌ Erreur d'envoi : " + error,
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnResendCode.setEnabled(true);
                        Toast.makeText(this,
                                "❌ Session expirée",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}