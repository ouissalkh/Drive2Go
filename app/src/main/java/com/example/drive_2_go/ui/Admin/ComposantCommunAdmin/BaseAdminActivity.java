package com.example.drive_2_go.ui.Admin.ComposantCommunAdmin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

// --- IMPORTE TES ACTIVITÉS ICI ---
// Vérifie bien que le chemin (package) est correct pour chaque ligne
import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Admin.Table_bord.adminActivity;
import com.example.drive_2_go.ui.Admin.Parck_automobiles.AdminVehiclesActivity; // Chemin à vérifier
import com.example.drive_2_go.ui.Admin.Gestion_Reservations.ReservationsActivity;     // Chemin à vérifier
import com.example.drive_2_go.ui.Admin.Gestion_Users.GestionUsersActivity;    // Chemin à vérifier
import com.example.drive_2_go.ui.Client.login.LoginActivity;

public abstract class BaseAdminActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        // On appelle setupNavigation ici pour être sûr que la mise à jour se fait bien
        setupNavigation();
    }

    protected void setupNavigation() {
        // 1. Récupération des boutons (Ids de ton XML layout_navbar_admin)
        ImageButton btnHome = findViewById(R.id.buttonHomeAdmin);
        ImageButton btnParck = findViewById(R.id.buttonParck);
        ImageButton btnResa = findViewById(R.id.buttonReservation);
        ImageButton btnUsers = findViewById(R.id.buttonUsers);
        ImageButton btnLogout = findViewById(R.id.buttonLogOut);

        // 2. Configuration des liens ET de la couleur "Active"
        // Le code va comparer "this" (la page actuelle) avec la classe cible

        gererBouton(btnHome, adminActivity.class);
        gererBouton(btnParck, AdminVehiclesActivity.class);
        gererBouton(btnResa, ReservationsActivity.class);
        gererBouton(btnUsers, GestionUsersActivity.class);

        // 3. Cas spécial pour la Déconnexion
        if (btnLogout != null) {
            // Le bouton logout reste toujours noir (ou gris), pas besoin de le colorer en "actif"
            btnLogout.setColorFilter(ContextCompat.getColor(this, android.R.color.white));

            btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void gererBouton(ImageButton btn, Class<?> classeCible) {
        if (btn != null) {
            // --- C'est ICI que la magie opère pour la couleur "Par défaut" ---

            // Si la page actuelle (this) EST la page du bouton (classeCible)
            if (this.getClass() == classeCible) {
                // 1. On change la couleur en VERT (ou ta couleur principale) pour dire "C'est ici"
                // Tu peux changer R.color.teal_700 par la couleur que tu veux
                btn.setColorFilter(ContextCompat.getColor(this, R.color.teal_700));

                // 2. On désactive le clic (inutile de recharger la page où on est déjà)
                btn.setEnabled(false);
            }

            // Si ce n'est PAS la page actuelle
            else {
                // 1. On remet la couleur en NOIR (ou gris)
                btn.setColorFilter(ContextCompat.getColor(this, android.R.color.white));

                // 2. On active le clic pour aller vers la page cible
                btn.setEnabled(true);
                btn.setOnClickListener(v -> {
                    Intent intent = new Intent(this, classeCible);
                    startActivity(intent);
                    overridePendingTransition(0, 0); // Supprime l'animation de transition (plus fluide)
                    finish(); // Ferme l'ancienne activité pour économiser la mémoire
                });
            }
        }
    }
}