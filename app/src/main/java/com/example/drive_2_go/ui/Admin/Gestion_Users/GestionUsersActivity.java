package com.example.drive_2_go.ui.Admin.Gestion_Users;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Admin.ComposantCommunAdmin.BaseAdminActivity;
import com.example.drive_2_go.ui.adapter.UserAdapter;
import com.example.drive_2_go.data.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GestionUsersActivity extends BaseAdminActivity {

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<User> userList;
    private List<User> filteredList;
    private FirebaseFirestore db;

    private AutoCompleteTextView autoCompleteRole;
    private AutoCompleteTextView autoCompleteStatus;
    private SearchView searchView;

    // TextViews pour les statistiques
    private TextView tvTotalUsers;
    private TextView tvTotalClients;
    private TextView tvTotalAdmins;
    private TextView tvTotalActifs;

    private String selectedRole = "Tous les rôles";
    private String selectedStatus = "Tous les statuts";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_gestion);

        // Initialisation Firebase
        db = FirebaseFirestore.getInstance();

        // Initialisation des vues
        initViews();

        // Configuration de la navigation
        setupNavigation();

        // Configuration des filtres
        setupFilters();

        // Chargement des utilisateurs
        loadUsers();
    }

    private void initViews() {
        // RecyclerView
        recyclerView = findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new UserAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        // TextViews statistiques
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalClients = findViewById(R.id.tvTotalClients);
        tvTotalAdmins = findViewById(R.id.tvTotalAdmins);
        tvTotalActifs = findViewById(R.id.tvTotalActifs);

        // Filtres
        autoCompleteRole = findViewById(R.id.autoCompleteRole);
        autoCompleteStatus = findViewById(R.id.autoCompleteStatus);
        searchView = findViewById(R.id.searchView);
    }

    private void setupFilters() {
        // Configuration du filtre par rôle
        String[] roles = {"Tous les rôles", "client", "admin"};
        ArrayAdapter<String> adapterRole = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                roles
        );
        autoCompleteRole.setAdapter(adapterRole);
        autoCompleteRole.setText("Tous les rôles", false);

        autoCompleteRole.setOnItemClickListener((parent, view, position, id) -> {
            selectedRole = parent.getItemAtPosition(position).toString();
            applyFilters();
        });

        // Configuration du filtre par statut
        String[] status = {"Tous les statuts", "Actif", "Bloqué"};
        ArrayAdapter<String> adapterStatus = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                status
        );
        autoCompleteStatus.setAdapter(adapterStatus);
        autoCompleteStatus.setText("Tous les statuts", false);

        autoCompleteStatus.setOnItemClickListener((parent, view, position, id) -> {
            selectedStatus = parent.getItemAtPosition(position).toString();
            applyFilters();
        });

        // Configuration de la recherche
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchQuery = query.toLowerCase();
                applyFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText.toLowerCase();
                applyFilters();
                return true;
            }
        });
    }

    private void loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setId(document.getId());
                        userList.add(user);
                    }

                    // Mettre à jour les statistiques
                    updateStatistics();

                    // Appliquer les filtres (au début, afficher tous)
                    applyFilters();

                    if (userList.isEmpty()) {
                        Toast.makeText(this, "Aucun utilisateur trouvé", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilters() {
        filteredList.clear();

        for (User user : userList) {
            boolean matchRole = true;
            boolean matchStatus = true;
            boolean matchSearch = true;

            // Filtre par rôle
            if (!selectedRole.equals("Tous les rôles")) {
                matchRole = user.getRole() != null &&
                        user.getRole().equalsIgnoreCase(selectedRole);
            }

            // Filtre par statut
            if (!selectedStatus.equals("Tous les statuts")) {
                if (selectedStatus.equals("Actif")) {
                    matchStatus = user.isVerified();
                } else if (selectedStatus.equals("Bloqué")) {
                    matchStatus = !user.isVerified();
                }
            }

            // Filtre par recherche (nom, prénom, email)
            if (!searchQuery.isEmpty()) {
                String nom = user.getNom() != null ? user.getNom().toLowerCase() : "";
                String prenom = user.getPrenom() != null ? user.getPrenom().toLowerCase() : "";
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";

                matchSearch = nom.contains(searchQuery) ||
                        prenom.contains(searchQuery) ||
                        email.contains(searchQuery);
            }

            // Ajouter si tous les critères sont respectés
            if (matchRole && matchStatus && matchSearch) {
                filteredList.add(user);
            }
        }

        adapter.notifyDataSetChanged();

        // Message si aucun résultat
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "Aucun utilisateur ne correspond aux critères", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatistics() {
        int totalUsers = userList.size();
        int totalClients = 0;
        int totalAdmins = 0;
        int totalActifs = 0;

        for (User user : userList) {
            // Compter les clients
            if ("client".equalsIgnoreCase(user.getRole())) {
                totalClients++;
            }

            // Compter les admins
            if ("admin".equalsIgnoreCase(user.getRole())) {
                totalAdmins++;
            }

            // Compter les actifs (vérifiés)
            if (user.isVerified()) {
                totalActifs++;
            }
        }

        // Mettre à jour les TextViews
        tvTotalUsers.setText(String.valueOf(totalUsers));
        tvTotalClients.setText(String.valueOf(totalClients));
        tvTotalAdmins.setText(String.valueOf(totalAdmins));
        tvTotalActifs.setText(String.valueOf(totalActifs));
    }

    // Méthode publique pour rafraîchir la liste (appelée par l'adapter)
    public void refreshUsers() {
        loadUsers();
    }
}