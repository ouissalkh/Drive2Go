package com.example.drive_2_go.ui.Admin.ComposantCommunAdmin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import android.graphics.Color; // Import ajouté pour les couleurs

// --- IMPORTE TES ACTIVITÉS ICI ---
import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Admin.Table_bord.adminActivity;
import com.example.drive_2_go.ui.Admin.Parck_automobiles.AdminVehiclesActivity;
import com.example.drive_2_go.ui.Admin.Gestion_Reservations.ReservationsActivity;
import com.example.drive_2_go.ui.Admin.Gestion_Users.GestionUsersActivity;
import com.example.drive_2_go.ui.Client.login.LoginActivity;

public abstract class BaseAdminActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        setupNavigation();
    }

    protected void setupNavigation() {
        // 1. Récupération des boutons
        ImageButton btnHome = findViewById(R.id.buttonHomeAdmin);
        ImageButton btnParck = findViewById(R.id.buttonParck);
        ImageButton btnResa = findViewById(R.id.buttonReservation);
        ImageButton btnUsers = findViewById(R.id.buttonUsers);
        ImageButton btnLogout = findViewById(R.id.buttonLogOut);

        // 2. Configuration des liens de navigation
        gererBouton(btnHome, adminActivity.class);
        gererBouton(btnParck, AdminVehiclesActivity.class);
        gererBouton(btnResa, ReservationsActivity.class);
        gererBouton(btnUsers, GestionUsersActivity.class);

        // 3. Cas spécial Déconnexion avec confirmation et couleurs
        if (btnLogout != null) {
            btnLogout.setColorFilter(ContextCompat.getColor(this, android.R.color.white));

            btnLogout.setOnClickListener(v -> {
                // A. On prépare la fenêtre
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Déconnexion")
                        .setMessage("Voulez-vous vraiment vous déconnecter ?")
                        .setCancelable(false)
                        .setPositiveButton("Oui", (dialog, which) -> {
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("Non", (dialog, which) -> {
                            dialog.dismiss();
                        });

                // B. On crée l'alerte et on l'affiche
                AlertDialog alert = builder.create();
                alert.show();

                // C. UNE FOIS AFFICHÉE, on change les couleurs des boutons
                // "Oui" en Vert
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GREEN);
                // "Non" en Rouge
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED);
            });
        }
    }

    // Cette méthode doit être DANS la classe (avant la dernière accolade)
    private void gererBouton(ImageButton btn, Class<?> classeCible) {
        if (btn != null) {
            if (this.getClass() == classeCible) {
                // Page active : Couleur verte (ou teal) et clic désactivé
                btn.setColorFilter(ContextCompat.getColor(this, R.color.teal_700));
                btn.setEnabled(false);
            } else {
                // Autre page : Couleur blanche et clic activé
                btn.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
                btn.setEnabled(true);
                btn.setOnClickListener(v -> {
                    Intent intent = new Intent(this, classeCible);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                });
            }
        }
    }
}