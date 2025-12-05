package com.example.drive_2_go.ui.adapter;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private List<User> userList;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
        this.db = FirebaseFirestore.getInstance();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        // Initiales
        String initials = user.getPrenom().substring(0, 1).toUpperCase() +
                user.getNom().substring(0, 1).toUpperCase();
        holder.tvInitials.setText(initials);

        // Nom complet
        holder.tvUserName.setText(user.getPrenom() + " " + user.getNom());

        // Email
        holder.tvEmail.setText(user.getEmail());

        // Téléphone
        holder.tvPhone.setText(user.getTelephone());

        // Rôle
        if ("admin".equalsIgnoreCase(user.getRole())) {
            holder.tvRole.setText("Administrateur");
            holder.tvRole.setBackgroundResource(R.drawable.bg_role_admin);
        } else {
            holder.tvRole.setText("Client");
            holder.tvRole.setBackgroundResource(R.drawable.bg_role_client);
        }

        // Statut
        if (user.isVerified()) {
            holder.tvStatus.setText("Actif");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active);
        } else {
            holder.tvStatus.setText("En attente");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        }

        // Date d'inscription
        if (user.getDateInscription() != null) {
            Date date = new Date(user.getDateInscription());
            holder.tvDateInscription.setText(dateFormat.format(date));
        }

        // Nombre de réservations (à charger depuis Firestore)
        loadUserReservationsCount(user.getId(), holder.tvReservations);

        // Bouton voir détails
        holder.btnDetails.setOnClickListener(v -> {
            showUserDetailsDialog(user);
        });
    }

    private void loadUserReservationsCount(String userId, TextView textView) {
        db.collection("reservations")
                .whereEqualTo("utilisateur_id", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    textView.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    textView.setText("0");
                });
    }

    private void showUserDetailsDialog(User user) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.log_user_details);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Vues du dialog
        TextView tvInitials = dialog.findViewById(R.id.tvInitials);
        TextView tvUserName = dialog.findViewById(R.id.tvUserName);
        TextView tvEmail = dialog.findViewById(R.id.tvEmail);
        TextView tvPhone = dialog.findViewById(R.id.tvPhone);
        TextView tvDateInscription = dialog.findViewById(R.id.tvDateInscription);
        TextView tvReservations = dialog.findViewById(R.id.tvReservations);
        TextView tvCurrentRole = dialog.findViewById(R.id.tvCurrentRole);
        TextView tvCurrentStatus = dialog.findViewById(R.id.tvCurrentStatus);

        RadioGroup radioGroupRole = dialog.findViewById(R.id.radioGroupRole);
        RadioButton radioClient = dialog.findViewById(R.id.radioClient);
        RadioButton radioAdmin = dialog.findViewById(R.id.radioAdmin);

        RadioGroup radioGroupStatus = dialog.findViewById(R.id.radioGroupStatus);
        RadioButton radioActive = dialog.findViewById(R.id.radioActive);
        RadioButton radioSuspended = dialog.findViewById(R.id.radioSuspended);

        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnClose = dialog.findViewById(R.id.btnClose);

        // Remplir les informations
        String initials = user.getPrenom().substring(0, 1).toUpperCase() +
                user.getNom().substring(0, 1).toUpperCase();
        tvInitials.setText(initials);
        tvUserName.setText(user.getPrenom() + " " + user.getNom());
        tvEmail.setText(user.getEmail());
        tvPhone.setText(user.getTelephone());

        if (user.getDateInscription() != null) {
            Date date = new Date(user.getDateInscription());
            tvDateInscription.setText(dateFormat.format(date));
        }

        tvCurrentRole.setText("admin".equalsIgnoreCase(user.getRole()) ? "Administrateur" : "Client");
        tvCurrentStatus.setText(user.isVerified() ? "Actif" : "En attente");

        // Pré-sélectionner les radio buttons
        if ("admin".equalsIgnoreCase(user.getRole())) {
            radioAdmin.setChecked(true);
        } else {
            radioClient.setChecked(true);
        }

        if (user.isVerified()) {
            radioActive.setChecked(true);
        } else {
            radioSuspended.setChecked(true);
        }

        // Charger le nombre de réservations
        loadUserReservationsCount(user.getId(), tvReservations);

        // Bouton sauvegarder
        btnSave.setOnClickListener(v -> {
            String newRole = radioClient.isChecked() ? "client" : "admin";
            boolean newStatus = radioActive.isChecked();

            updateUser(user.getId(), newRole, newStatus, dialog);
        });

        // Bouton fermer
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateUser(String userId, String newRole, boolean newStatus, Dialog dialog) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("role", newRole);
        updates.put("isVerified", newStatus);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(context, "✅ Utilisateur mis à jour", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                    // Recharger la liste
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "❌ Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvUserName, tvEmail, tvPhone, tvRole, tvStatus, tvDateInscription, tvReservations;
        ImageView btnDetails;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tvInitials);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDateInscription = itemView.findViewById(R.id.tvDateInscription);
            tvReservations = itemView.findViewById(R.id.tvReservations);
            btnDetails = itemView.findViewById(R.id.btnDetails);
        }
    }
}