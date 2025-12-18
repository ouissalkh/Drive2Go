package com.example.drive_2_go.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.ReservationDisplayModel;

import java.util.List;
import java.util.Locale;

public class ReservationsAdapter extends RecyclerView.Adapter<ReservationsAdapter.ReservationViewHolder> {

    private List<ReservationDisplayModel> reservationList;
    private final Context context;

    public ReservationsAdapter(Context context, List<ReservationDisplayModel> reservationList) {
        this.context = context;
        this.reservationList = reservationList;
    }

    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reservation_admin, parent, false);
        return new ReservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
        ReservationDisplayModel currentDisplayItem = reservationList.get(position);

        try {
            // 1. Remplissage des textes de base
            holder.tvResNum.setText(String.format(Locale.FRENCH, "Réservation #%s", currentDisplayItem.getReservationNumber()));
            holder.tvName.setText(currentDisplayItem.getUserName());
            holder.tvVehicleName.setText(currentDisplayItem.getCarName());

            String period = String.format(Locale.FRENCH, "%s\nau %s", currentDisplayItem.getStartDate(), currentDisplayItem.getEndDate());
            holder.tvPeriodDates.setText(period);

            holder.tvEmail.setText(currentDisplayItem.getEmail());
            holder.tvPhone.setText(currentDisplayItem.getPhone());
            holder.tvTotalPrice.setText(String.format(Locale.FRENCH, "%.2f DH", currentDisplayItem.getTotalPrice())); // J'ai remis DH ou € selon ta préférence

            // 2. Gestion des Statuts et Couleurs
            holder.tvStatus.setText(getStatusText(currentDisplayItem.getStatus()));
            updateStatusStyle(holder.headerBg, holder.tvStatus, currentDisplayItem.getStatus());

            // =========================================================
            // ACTION 1 : CLIC SUR LE TÉLÉPHONE -> APPEL
            // =========================================================
            holder.tvPhone.setOnClickListener(v -> {
                String phoneNumber = currentDisplayItem.getPhone();
                if (phoneNumber != null && !phoneNumber.trim().isEmpty() && !phoneNumber.contains("Non Trouvé")) {
                    try {
                        // ACTION_DIAL ouvre le clavier sans lancer l'appel directement (plus sûr)
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phoneNumber.trim()));
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(context, "Impossible d'ouvrir le téléphone.", Toast.LENGTH_SHORT).show();
                        Log.e("Adapter", "Erreur téléphone: " + e.getMessage());
                    }
                } else {
                    Toast.makeText(context, "Numéro de téléphone indisponible.", Toast.LENGTH_SHORT).show();
                }
            });

            // =========================================================
            // ACTION 2 : CLIC SUR L'EMAIL -> ENVOYER UN MAIL
            // =========================================================
            holder.tvEmail.setOnClickListener(v -> {
                String email = currentDisplayItem.getEmail();
                if (email != null && !email.trim().isEmpty() && !email.contains("Non Trouvé")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setData(Uri.parse("mailto:" + email.trim()));
                        // Sujet automatique
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Drive2Go: Réservation #" + currentDisplayItem.getReservationNumber());
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(context, "Aucune application d'email trouvée.", Toast.LENGTH_SHORT).show();
                        Log.e("Adapter", "Erreur email: " + e.getMessage());
                    }
                } else {
                    Toast.makeText(context, "Adresse email indisponible.", Toast.LENGTH_SHORT).show();
                }
            });

            // Action bouton Détails
            holder.btnDetails.setOnClickListener(v -> {
                Log.d("Adapter", "Détails de la réservation n°" + currentDisplayItem.getReservationNumber());
                Toast.makeText(context, "Détails: " + currentDisplayItem.getReservationNumber(), Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            Log.e("Adapter", "Erreur de liaison des données pour la réservation " + position + ": " + e.getMessage());
            holder.tvName.setText("Erreur de données");
        }
    }

    @Override
    public int getItemCount() {
        return reservationList.size();
    }

    public void updateReservations(List<ReservationDisplayModel> newReservations) {
        this.reservationList.clear();
        this.reservationList.addAll(newReservations);
        notifyDataSetChanged();
    }

    // =========================================================
    // MÉTHODES UTILITAIRES
    // =========================================================

    private String getStatusText(String status) {
        if (status == null) {
            return "Inconnu";
        }
        switch (status) {
            case "En attente de validation":
            case "En attente":
                return "En attente";
            case "acceptée":
            case "Confirmée":
                return "Confirmée";
            case "refusée":
            case "Annulée":
                return "Annulée";
            case "Terminée":
                return "Terminée";
            default:
                return status;
        }
    }

    private void updateStatusStyle(View headerBg, TextView tvStatus, String status) {
        int bgColorResId;
        int textColorResId;

        if (status == null) {
            bgColorResId = R.color.grey;
            textColorResId = R.color.white;
        } else {
            // Attention aux noms exacts venant de Firestore (sensible à la casse)
            switch (status) {
                case "En attente":
                case "En attente de validation":
                    bgColorResId = R.color.orange; // Assure-toi que cette couleur existe dans colors.xml
                    textColorResId = R.color.black;
                    break;
                case "acceptée":
                case "Confirmée":
                    bgColorResId = R.color.green_primary; // ou une couleur verte définie
                    textColorResId = R.color.white;
                    break;
                case "refusée":
                case "Annulée":
                    bgColorResId = R.color.red_primary; // ou une couleur rouge définie
                    textColorResId = R.color.white;
                    break;
                default:
                    bgColorResId = R.color.grey;
                    textColorResId = R.color.white;
                    break;
            }
        }

        // Utilisation de ContextCompat pour éviter les erreurs de version Android
        try {
            headerBg.setBackgroundColor(ContextCompat.getColor(context, bgColorResId));
            tvStatus.setTextColor(ContextCompat.getColor(context, textColorResId));
        } catch (Exception e) {
            // Fallback si la couleur n'existe pas
            Log.e("Adapter", "Erreur couleur: " + e.getMessage());
        }
    }

    public static class ReservationViewHolder extends RecyclerView.ViewHolder {
        final TextView tvResNum;
        final TextView tvName;
        final TextView tvStatus;
        final View headerBg;

        final TextView tvVehicleName;
        final TextView tvPeriodDates;

        final TextView tvEmail;
        final TextView tvPhone;

        final TextView tvTotalPrice;
        final com.google.android.material.button.MaterialButton btnDetails;

        public ReservationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvResNum = itemView.findViewById(R.id.tvResNum);
            tvName = itemView.findViewById(R.id.tvName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            headerBg = itemView.findViewById(R.id.headerBg);

            tvVehicleName = itemView.findViewById(R.id.tvVehicleName);
            tvPeriodDates = itemView.findViewById(R.id.tvPeriodDates);

            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);

            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            btnDetails = itemView.findViewById(R.id.btnDetails);
        }
    }
}