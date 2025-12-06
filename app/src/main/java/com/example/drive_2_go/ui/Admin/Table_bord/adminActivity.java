package com.example.drive_2_go.ui.Admin.Table_bord;

import android.os.Bundle;

// 1. IMPORT IMPORTANT : Ta classe de base
import com.example.drive_2_go.ui.Admin.ComposantCommunAdmin.BaseAdminActivity;
import com.example.drive_2_go.R;

import com.example.drive_2_go.ui.Admin.Gestion_Reservations.ReservationsActivity;

// 2. CHANGEMENT ICI : On hérite de BaseAdminActivity
public class adminActivity extends BaseAdminActivity {

    //private Button btnreservation;
    // On supprime tous les ImageButton ici, ils sont gérés par BaseAdminActivity !


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // Tu peux le garder si tu l'utilises
        setContentView(R.layout.activity_admin);


        // 3. LA LIGNE MAGIQUE : Active la barre de navigation et la gestion des couleurs
        setupNavigation();

        // --- Logique spécifique à CETTE page seulement ---

        // Ce bouton est au milieu de ton écran (pas dans la barre), donc on le garde ici


    }


}