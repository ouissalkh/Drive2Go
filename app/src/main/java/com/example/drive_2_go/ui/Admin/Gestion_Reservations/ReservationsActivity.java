package com.example.drive_2_go.ui.Admin.Gestion_Reservations;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Admin.ComposantCommunAdmin.BaseAdminActivity;

public class ReservationsActivity extends BaseAdminActivity {

    private TextView tvTotalCount;
    private AutoCompleteTextView autoCompleteTextView;
    private ArrayAdapter<String> adapterItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Charger le layout en sécurité
        try {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            setContentView(R.layout.activity_reservations);
        } catch (Exception e) {
            Log.e("DEBUG_ERROR", "Erreur lors du setContentView: " + e.getMessage());
            Toast.makeText(this, "Erreur XML critique", Toast.LENGTH_LONG).show();
            return;
        }

        // Setup Navigation
        try {
            setupNavigation();
        } catch (Exception e) {
            Log.e("DEBUG_ERROR", "Erreur dans setupNavigation: " + e.getMessage());
        }

        // Gérer les marges système
        View drawerLayout = findViewById(R.id.drawerLayout);
        if (drawerLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        } else {
            Log.e("DEBUG_ERROR", "drawerLayout introuvable");
        }

        // Initialisation des vues
        tvTotalCount = findViewById(R.id.tv_total_count);
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);

        // Configuration Dropdown (statuts)
        if (autoCompleteTextView != null) {
            String[] statusItems = {"Tous les statuts", "Confirmée", "En attente", "Annulée", "Terminée"};
            adapterItems = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statusItems);
            autoCompleteTextView.setAdapter(adapterItems);

            autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
                String item = parent.getItemAtPosition(position).toString();
                Toast.makeText(this, "Filtre : " + item, Toast.LENGTH_SHORT).show();
            });
        }
    }
}
