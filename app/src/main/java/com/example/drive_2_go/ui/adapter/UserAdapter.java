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
import androidx.cardview.widget.CardView;
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

        // Avatar initiales
        String initials = user.getPrenom().substring(0, 1).toUpperCase() +
                user.getNom().substring(0, 1).toUpperCase();
        holder.tvAvatar.setText(initials);

        // Nom complet
        holder.tvClientName.setText(user.getPrenom() + " " + user.getNom());

        // Email
        holder.tvEmailValue.setText(user.getEmail());

        // Téléphone
        holder.tvPhoneValue.setText(user.getTelephone());

        // Badges rôle et statut
        LinearLayout badgeContainer = (LinearLayout) holder.tvClientName.getParent().getParent();
        TextView badgeRole = (TextView) badgeContainer.getChildAt(0);
        TextView badgeStatus = (TextView) badgeContainer.getChildAt(1);

        if ("admin".equalsIgnoreCase(user.getRole())) {
            badgeRole.setText("Administrateur");
            badgeRole.setBackgroundResource(R.drawable.bg_badge_blue);
        } else {
            badgeRole.setText("Client");
            badgeRole.setBackgroundResource(R.drawable.bg_badge_blue);
        }

        if (user.isVerified()) {
            badgeStatus.setText("Actif");
            badgeStatus.setBackgroundResource(R.drawable.bg_badge_green);
        } else {
            badgeStatus.setText("En attente");
            badgeStatus.setBackgroundResource(R.drawable.bg_badge_green);
        }

        // Date d'inscription
        if (user.getDateInscription() != null) {
            Date date = new Date(user.getDateInscription());
            holder.tvDateInscription.setText(dateFormat.format(date));
        } else {
            holder.tvDateInscription.setText("N/A");
        }

        // Nombre de réservations
        loadUserReservationsCount(user.getId(), holder.tvReservationsCount);

        // Bouton gérer utilisateur
        holder.btnManage.setOnClickListener(v -> {
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

        // Récupérer les vues du dialog
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
        String initials = user.getPrenom().substring(0, 1).toUpperCase() +
                user.getNom().substring(0, 1).toUpperCase();
        tvDialogInitials.setText(initials);
        tvDialogUserName.setText(user.getPrenom() + " " + user.getNom());
        tvDialogEmail.setText(user.getEmail());
        tvDialogPhone.setText(user.getTelephone());

        if (user.getDateInscription() != null) {
            Date date = new Date(user.getDateInscription());
            tvDialogDateInscription.setText(dateFormat.format(date));
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
        CardView cardView;
        TextView tvAvatar;
        TextView tvClientName;
        TextView tvEmailValue;
        TextView tvPhoneValue;
        TextView tvDateInscription;
        TextView tvReservationsCount;
        Button btnManage;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            // cardView = itemView.findViewById(R.id.cardView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvEmailValue = itemView.findViewById(R.id.tvEmailValue);
            tvPhoneValue = itemView.findViewById(R.id.tvPhoneValue);

            // Pour les statistiques, on va chercher les vues dans le layoutStats
            LinearLayout layoutStats = itemView.findViewById(R.id.layoutStats);

            // La première LinearLayout dans layoutStats contient la date d'inscription
            LinearLayout inscriptionLayout = (LinearLayout) layoutStats.getChildAt(0);
            tvDateInscription = (TextView) inscriptionLayout.getChildAt(1);

            // La deuxième LinearLayout dans layoutStats contient le nombre de réservations
            LinearLayout reservationsLayout = (LinearLayout) layoutStats.getChildAt(1);
            tvReservationsCount = (TextView) reservationsLayout.getChildAt(1);

            btnManage = itemView.findViewById(R.id.btnManage);
        }
    }
}