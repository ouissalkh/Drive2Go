

/*package com.example.drive_2_go.ui.Admin.Gestion_Users;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.User;
import com.example.drive_2_go.ui.adapter.UserAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GestionUsersActivity extends AppCompatActivity { // 1. Étendre AppCompatActivity

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<User> userList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_gestion);

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerViewUsers);
        userList = new ArrayList<>();

        // 2. Correction du constructeur de l'adaptateur
        adapter = new UserAdapter(GestionUsersActivity.this, userList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadUsers();
    }

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
}*/


