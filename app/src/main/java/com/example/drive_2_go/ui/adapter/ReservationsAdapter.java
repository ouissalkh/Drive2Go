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
        ReservationDisplayModel item = reservationList.get(position);

        try {
            // Sécurité Affichage Nom
            String name = (item.getUserName() != null) ? item.getUserName() : "Utilisateur Inconnu";
            holder.tvName.setText(name);

            // Numéro Réservation
            holder.tvResNum.setText("Réservation #" + (item.getReservationNumber() != null ? item.getReservationNumber() : "---"));

            // Véhicule
            holder.tvVehicleName.setText(item.getCarName() != null ? item.getCarName() : "Véhicule non défini");

            // Période
            String startDate = item.getStartDate() != null ? item.getStartDate() : "?";
            String endDate = item.getEndDate() != null ? item.getEndDate() : "?";
            holder.tvPeriodDates.setText(startDate + " au " + endDate);

            // Contact
            holder.tvEmail.setText(item.getEmail() != null ? item.getEmail() : "Pas d'email");
            holder.tvPhone.setText(item.getPhone() != null ? item.getPhone() : "Pas de téléphone");

            // Prix (Correction du crash formatage)
            Double price = item.getTotalPrice();
            holder.tvTotalPrice.setText(String.format(Locale.FRENCH, "%.2f DH", (price != null ? price : 0.0)));

            // Statut
            holder.tvStatus.setText(getStatusText(item.getStatus()));
            updateStatusStyle(holder.headerBg, holder.tvStatus, item.getStatus());

            // Actions Clics
            holder.tvPhone.setOnClickListener(v -> openDialer(item.getPhone()));
            holder.tvEmail.setOnClickListener(v -> openEmail(item.getEmail(), item.getReservationNumber()));

        } catch (Exception e) {
            Log.e("Adapter", "Erreur binding: " + e.getMessage());
            holder.tvName.setText("Erreur d'affichage");
        }
    }

    private void openDialer(String phone) {
        if (phone != null && !phone.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            context.startActivity(intent);
        }
    }

    private void openEmail(String email, String resId) {
        if (email != null && !email.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Drive2Go: Réservation #" + resId);
            context.startActivity(intent);
        }
    }

    @Override
    public int getItemCount() { return reservationList.size(); }

    public void updateReservations(List<ReservationDisplayModel> newList) {
        this.reservationList.clear();
        this.reservationList.addAll(newList);
        notifyDataSetChanged();
    }

    private String getStatusText(String status) {
        if (status == null) return "En attente";
        if (status.equalsIgnoreCase("acceptée")) return "Confirmée";
        if (status.equalsIgnoreCase("refusée")) return "Annulée";
        return status;
    }

    private void updateStatusStyle(View header, TextView tvStatus, String status) {
        int colorRes = R.color.teal_primary; // Par défaut
        if (status != null) {
            if (status.equalsIgnoreCase("refusée")) colorRes = android.R.color.holo_red_dark;
            else if (status.equalsIgnoreCase("En attente")) colorRes = android.R.color.holo_orange_dark;
        }
        header.setBackgroundColor(ContextCompat.getColor(context, colorRes));
    }

    public static class ReservationViewHolder extends RecyclerView.ViewHolder {
        TextView tvResNum, tvName, tvStatus, tvVehicleName, tvPeriodDates, tvEmail, tvPhone, tvTotalPrice;
        View headerBg;

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
        }
    }
}
