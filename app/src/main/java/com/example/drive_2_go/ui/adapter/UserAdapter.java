package com.example.drive_2_go.ui.adapter;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.User;
import com.example.drive_2_go.ui.Admin.Gestion_Users.GestionUsersActivity;
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
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
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

        // Avatar avec initiales
        String initials = "";
        if (user.getPrenom() != null && !user.getPrenom().isEmpty() &&
                user.getNom() != null && !user.getNom().isEmpty()) {
            initials = user.getPrenom().substring(0, 1).toUpperCase() +
                    user.getNom().substring(0, 1).toUpperCase();
        }
        holder.tvAvatar.setText(initials);

        // Nom complet
        String fullName = (user.getPrenom() != null ? user.getPrenom() : "") + " " +
                (user.getNom() != null ? user.getNom() : "");
        holder.tvClientName.setText(fullName.trim());

        // Badge rôle
        if ("admin".equalsIgnoreCase(user.getRole())) {
            holder.badgeRole.setText("Administrateur");
            holder.badgeRole.setBackgroundResource(R.drawable.bg_badge_blue);
        } else {
            holder.badgeRole.setText("Client");
            holder.badgeRole.setBackgroundResource(R.drawable.bg_badge_blue);
        }

        // Badge statut
        if (user.isVerified()) {
            holder.badgeStatus.setText("Actif");
            holder.badgeStatus.setBackgroundResource(R.drawable.bg_badge_green);
        } else {
            holder.badgeStatus.setText("En attente");
            holder.badgeStatus.setBackgroundResource(R.drawable.bg_badge_jaune);
        }

        // Email
        holder.tvEmailValue.setText(user.getEmail() != null ? user.getEmail() : "N/A");

        // Téléphone
        holder.tvPhoneValue.setText(user.getTelephone() != null ? user.getTelephone() : "N/A");

        // Date d'inscription
        if (user.getDateInscription() != null) {
            Date date = new Date(user.getDateInscription());
            holder.tvDateInscription.setText(dateFormat.format(date));
        } else {
            holder.tvDateInscription.setText("N/A");
        }

        // Nombre de réservations
        loadUserReservationsCount(user.getId(), holder.tvReservationsCount);

        // Bouton gérer
        holder.btnManage.setOnClickListener(v -> showUserDetailsDialog(user));
    }

    private void loadUserReservationsCount(String userId, TextView textView) {
        if (userId == null || userId.isEmpty()) {
            textView.setText("0");
            return;
        }

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

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Récupération des vues
        TextView tvDialogInitials = dialog.findViewById(R.id.tvInitials);
        TextView tvDialogUserName = dialog.findViewById(R.id.tvUserName);
        TextView tvDialogEmail = dialog.findViewById(R.id.tvEmail);
        TextView tvDialogPhone = dialog.findViewById(R.id.tvPhone);
        TextView tvDialogDateInscription = dialog.findViewById(R.id.tvDateInscription);
        TextView tvDialogReservations = dialog.findViewById(R.id.tvReservations);
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
        String initials = "";
        if (user.getPrenom() != null && !user.getPrenom().isEmpty() &&
                user.getNom() != null && !user.getNom().isEmpty()) {
            initials = user.getPrenom().substring(0, 1).toUpperCase() +
                    user.getNom().substring(0, 1).toUpperCase();
        }
        tvDialogInitials.setText(initials);

        String fullName = (user.getPrenom() != null ? user.getPrenom() : "") + " " +
                (user.getNom() != null ? user.getNom() : "");
        tvDialogUserName.setText(fullName.trim());

        tvDialogEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
        tvDialogPhone.setText(user.getTelephone() != null ? user.getTelephone() : "N/A");

        if (user.getDateInscription() != null) {
            Date date = new Date(user.getDateInscription());
            tvDialogDateInscription.setText(dateFormat.format(date));
        } else {
            tvDialogDateInscription.setText("N/A");
        }

        // État actuel
        tvCurrentRole.setText("admin".equalsIgnoreCase(user.getRole()) ? "Administrateur" : "Client");
        tvCurrentStatus.setText(user.isVerified() ? "Actif" : "En attente");

        // Pré-sélection radio buttons
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

        // Charger les réservations
        loadUserReservationsCount(user.getId(), tvDialogReservations);

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

                    // Rafraîchir la liste
                    if (context instanceof GestionUsersActivity) {
                        ((GestionUsersActivity) context).refreshUsers();
                    }
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
        TextView tvAvatar, tvClientName, badgeRole, badgeStatus;
        TextView tvEmailValue, tvPhoneValue, tvDateInscription, tvReservationsCount;
        Button btnManage;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            badgeRole = itemView.findViewById(R.id.badgeRole);
            badgeStatus = itemView.findViewById(R.id.badgeStatus);
            tvEmailValue = itemView.findViewById(R.id.tvEmailValue);
            tvPhoneValue = itemView.findViewById(R.id.tvPhoneValue);

            // Récupération des TextViews dans layoutStats
            LinearLayout layoutStats = itemView.findViewById(R.id.layoutStats);
            if (layoutStats != null && layoutStats.getChildCount() >= 2) {
                LinearLayout inscriptionLayout = (LinearLayout) layoutStats.getChildAt(0);
                if (inscriptionLayout != null && inscriptionLayout.getChildCount() >= 2) {
                    tvDateInscription = (TextView) inscriptionLayout.getChildAt(1);
                }

                LinearLayout reservationsLayout = (LinearLayout) layoutStats.getChildAt(2);
                if (reservationsLayout != null && reservationsLayout.getChildCount() >= 2) {
                    tvReservationsCount = (TextView) reservationsLayout.getChildAt(1);
                }
            }

            btnManage = itemView.findViewById(R.id.btnManage);
        }
    }
}