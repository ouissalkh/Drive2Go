package com.example.drive_2_go.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.Reservation;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class NotificationAdminAdapter extends RecyclerView.Adapter<NotificationAdminAdapter.ViewHolder> {

    private final Context context;
    private final List<Reservation> reservationList;
    private final OnActionButtonClickListener listener;
    private final FirebaseFirestore db;

    public interface OnActionButtonClickListener {
        void onAcceptClick(Reservation reservation, int position);
        void onCancelClick(Reservation reservation, int position);
        void onUserNameClick(Reservation reservation);
        void onCarNameClick(Reservation reservation);
    }

    public NotificationAdminAdapter(Context context, List<Reservation> reservationList, OnActionButtonClickListener listener) {
        this.context = context;
        this.reservationList = reservationList;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reservation reservation = reservationList.get(position);

        holder.tvUserName.setText(reservation.getUserName() != null ? reservation.getUserName() : "Inconnu");
        holder.tvCarModel.setText(reservation.getCarName() != null ? reservation.getCarName() : "Voiture inconnue");

        String start = reservation.getStartDate();
        String end = reservation.getEndDate();
        holder.tvDuration.setText(String.format("Du %s au %s", start, end));
        holder.tvRequestDate.setText(String.format(Locale.getDefault(), "Total : %.2f DH", reservation.getTotalPrice()));

        // --- IMAGE PROFIL USER ---
        String userId = reservation.getUserId();
        holder.imgProfile.setImageResource(R.drawable.ic_default_avatar);

        if (userId != null && !userId.isEmpty()) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String photoUrl = documentSnapshot.getString("photoUrl");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                try {
                                    String url = photoUrl.startsWith("http") ? photoUrl : MediaManager.get().url().generate(photoUrl);
                                    Glide.with(context).load(url).circleCrop().into(holder.imgProfile);
                                } catch (Exception e) { e.printStackTrace(); }
                            }
                        }
                    });
        }

        // --- CLICS ---
        holder.tvCarModel.setOnClickListener(v -> {
            if (listener != null) listener.onCarNameClick(reservation);
        });

        holder.tvUserName.setOnClickListener(v -> {
            if (listener != null) listener.onUserNameClick(reservation);
        });

        holder.imgProfile.setOnClickListener(v -> {
            if (listener != null) listener.onUserNameClick(reservation);
        });

        holder.btnAccept.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (listener != null && currentPos != RecyclerView.NO_POSITION) {
                listener.onAcceptClick(reservation, currentPos);
            }
        });

        holder.btnCancel.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (listener != null && currentPos != RecyclerView.NO_POSITION) {
                listener.onCancelClick(reservation, currentPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reservationList.size();
    }

    // --- C'EST LA MÉTHODE QUI MANQUAIT ---
    public void updateReservations(List<Reservation> newReservations) {
        this.reservationList.clear();
        this.reservationList.addAll(newReservations);
        notifyDataSetChanged();
    }
    // -------------------------------------

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvCarModel, tvDuration, tvRequestDate;
        ImageButton btnAccept, btnCancel;
        ImageView imgProfile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvCarModel = itemView.findViewById(R.id.tv_car_model);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvRequestDate = itemView.findViewById(R.id.tv_request_date);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
            imgProfile = itemView.findViewById(R.id.img_profile);
        }
    }
}