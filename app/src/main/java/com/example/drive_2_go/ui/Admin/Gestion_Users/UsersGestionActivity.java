package com.example.drive_2_go.ui.Admin.Gestion_Users;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Admin.ComposantCommunAdmin.BaseAdminActivity;
import com.example.drive_2_go.ui.Admin.Gestion_Reservations.ReservationsActivity;
import com.example.drive_2_go.ui.Admin.Table_bord.adminActivity;
import com.example.drive_2_go.ui.Client.favoris.Favoris;
import com.example.drive_2_go.ui.Client.history.HistoryActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.auth.User;

import java.util.ArrayList;
import java.util.List;

public class UsersGestionActivity extends BaseAdminActivity {


    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<User> userList;
    private FirebaseFirestore db;

    private AutoCompleteTextView autoCompleteRole;
    private AutoCompleteTextView autoCompleteStatus;

    private ArrayAdapter<String> adapterRole;
    private ArrayAdapter<String> adapterStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // Garde-le si tu l'utilises
        setContentView(R.layout.activity_users_gestion);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerViewUsers);
        userList = new ArrayList<>();

        // 2. Correction du constructeur de l'adaptateur
        adapter = new UserAdapter(GestionUsersActivity.this, userList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadUsers();

        // C'est elle qui va colorer le bouton "Users" automatiquement en vert
        // et rendre les autres boutons (Home, Parc, Resa) cliquables.
        setupNavigation();

        // 2. CONFIGURATION DU MENU DÉROULANT "RÔLES"
        autoCompleteRole = findViewById(R.id.autoCompleteTxtRole);

        // La liste des choix
        String[] roles = {"Tous les rôles", "Client", "Admin"};

        // Création de l'adaptateur
        adapterRole = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roles);
        autoCompleteRole.setAdapter(adapterRole);

        // Action quand on clique sur un élément
        autoCompleteRole.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String roleSelectionne = parent.getItemAtPosition(position).toString();

                // TEST : Affiche ce qu'on a choisi
                Toast.makeText(UsersGestionActivity.this, "Filtre choisi : " + roleSelectionne, Toast.LENGTH_SHORT).show();

                // PLUS TARD : Ici tu appelleras ta fonction pour filtrer ta liste d'utilisateurs
                // exemple: filtrerListeParRole(roleSelectionne);
            }
        });

// 3. CONFIGURATION DU MENU DÉROULANT "STATUT" (Le 2ème)
        // -----------------------------------------------------------
        autoCompleteStatus = findViewById(R.id.autoCompleteTxtStatus);

        String[] status = {"Tous les statuts", "Actif", "Bloqué"};

        adapterStatus = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, status);
        autoCompleteStatus.setAdapter(adapterStatus);

        autoCompleteStatus.setOnItemClickListener((parent, view, position, id) -> {
            String statusSelectionne = parent.getItemAtPosition(position).toString();
            Toast.makeText(UsersGestionActivity.this, "Statut : " + statusSelectionne, Toast.LENGTH_SHORT).show();
        });

        private void loadUsers() {
            db.collection("users")
                    .get() // 3. Récupérer tous les utilisateurs, pas seulement les clients
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        userList.clear();

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            User user = document.toObject(User.class);
                            user.setId(document.getId()); // 4. IMPORTANT: Définir l'ID du document
                            userList.add(user);
                        }

                        adapter.notifyDataSetChanged();

                        if (userList.isEmpty()) {
                            Toast.makeText(GestionUsersActivity.this, "Aucun utilisateur", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(GestionUsersActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
    }






