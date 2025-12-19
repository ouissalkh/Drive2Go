package com.example.drive_2_go.ui.adapter;


import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.NotificationModel;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter pour afficher la liste des notifications.
 * Maintenant dans un fichier séparé.
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    // ***************************************************************
    // INTERFACE DÉPLACÉE ICI (ou dans NotificationClientActivity si vous préférez)
    // Pour simplifier, nous la laissons dans l'Adapter comme convention,
    // mais elle pourrait être dans l'Activity ou un fichier Utils.
    // ***************************************************************
    public interface OnCarNameClickListener {
        void onCarNameClick(String carId);
    }

    private final List<NotificationModel> notificationList;
    private final Context context;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
    private final SimpleDateFormat adminTimeFormat = new SimpleDateFormat("à HH:mm", Locale.getDefault());

    private final OnCarNameClickListener listener;

    public NotificationsAdapter(Context context, List<NotificationModel> notificationList, OnCarNameClickListener listener) {
        this.context = context;
        this.notificationList = notificationList;
        // L'Activity DOIT implémenter cette interface
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification_client, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel notification = notificationList.get(position);

        holder.tvTitle.setText(notification.getTitle());

        Timestamp timeToShow = notification.getTimeConfirmationAdmin();

        if (timeToShow == null) {
            timeToShow = notification.getTimestamp();
        }

        if (timeToShow != null) {
            Date date = timeToShow.toDate();
            // Vous pouvez utiliser dateFormat ("dd MMM, HH:mm")
            // ou adminTimeFormat ("à HH:mm") selon votre préférence visuelle
            holder.tvTime.setText(dateFormat.format(date));
        } else {
            holder.tvTime.setText("");
        }

        if (!notification.isClientRead()) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.light_gray_unread));
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }

        // Gérer l'icône selon le type/statut (inchangée)
        if (notification.getTitle().contains("Confirmée")) {
            holder.imgIcon.setImageResource(R.drawable.confirmer);
        } else if (notification.getTitle().contains("Annulée")) {
            holder.imgIcon.setImageResource(R.drawable.annuler);
        } else if (notification.getTitle().contains("Fin de Location")) {
            holder.imgIcon.setImageResource(R.drawable.ic_warning);
        }else if(notification.getTitle().contains("Nouvelle Voiture")){
            holder.imgIcon.setImageResource(R.drawable.ic_check_circle);
        }

        // *****************************************************************
        // LOGIQUE DE CLIQUABILITÉ DU TEXTE (Spannable) - AMÉLIORÉE
        // *****************************************************************

        String message = notification.getMessage();
        final String carId = notification.getCarId();

        if (notification.getTimeConfirmationAdmin() != null && notification.getTitle().contains("Confirmée")) {
            Date adminDate = notification.getTimeConfirmationAdmin().toDate();
            // Ajoute la date de confirmation (ex: "à 14:30") à la fin du message principal.
            // Utilisez un séparateur clair (comme un point ou une nouvelle phrase).
            message += " Confirmée le " + dateFormat.format(adminDate) + ".";
        }

        holder.tvMessage.setText(message); // Définir le texte initial

        if (carId == null || carId.isEmpty()) {
            holder.tvMessage.setMovementMethod(null);
            holder.tvMessage.setTextColor(ContextCompat.getColor(context, R.color.black));
            return;
        }

        // ⚠️ NOUVEAU : Trouver la position de début et de fin du NOM de la voiture.

        int start = -1;
        int end = -1;

        // 1. Chercher le début du nom de la voiture
        if (message.contains("de la ")) {
            start = message.indexOf("de la ") + "de la ".length();
        } else if (message.contains(": ")) {
            start = message.indexOf(": ") + ": ".length();
        }

        // 2. Si un début est trouvé, chercher la fin
        if (start != -1) {
            // La fin est soit un séparateur de phrase (comme ' est', ' a expiré', '!')
            // ou la fin de la chaîne.

            // Essayer de trouver la fin de la phrase
            int endIndicator;
            if (message.contains(" est ")) {
                endIndicator = message.indexOf(" est ", start);
            } else if (message.contains(" a été ")) {
                endIndicator = message.indexOf(" a été ", start);
            } else if (message.contains(" a expiré")) {
                endIndicator = message.indexOf(" a expiré", start);
            } else {
                endIndicator = -1; // Pas de séparateur trouvé
            }

            if (endIndicator != -1) {
                end = endIndicator; // Le nom se termine juste avant le séparateur
            } else {
                end = message.length(); // Le nom va jusqu'à la fin de la chaîne
            }
        }

        if (start != -1 && end > start) {
            SpannableString spannableMessage = new SpannableString(message);

            ClickableSpan clickableSpan = new ClickableSpan() {
                // ... (ClickableSpan identique)
                @Override
                public void onClick(View widget) {
                    listener.onCarNameClick(carId);
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);

                    ds.setUnderlineText(false); // pas underline
                    ds.setColor(Color.BLACK);   // couleur noire
                    ds.setFakeBoldText(true);   // texte en bold
                }

            };

            spannableMessage.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            holder.tvMessage.setText(spannableMessage);
            holder.tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
            holder.tvMessage.setHighlightColor(ContextCompat.getColor(context, android.R.color.transparent));
        } else {
            // Reste non cliquable si le nom n'a pas pu être isolé
            holder.tvMessage.setMovementMethod(null);
            holder.tvMessage.setTextColor(ContextCompat.getColor(context, R.color.black));
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    /**
     * ViewHolder (maintenant statique dans la classe séparée).
     */
    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgIcon;
        final TextView tvTitle;
        final TextView tvMessage;
        final TextView tvTime;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_notification_icon);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvMessage = itemView.findViewById(R.id.tv_notification_message);
            tvTime = itemView.findViewById(R.id.tv_notification_time);
        }
    }
}