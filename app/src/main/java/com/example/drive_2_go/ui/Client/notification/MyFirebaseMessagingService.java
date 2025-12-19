package com.example.drive_2_go.ui.Client.notification;


import static androidx.core.content.ContextCompat.getSystemService;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Client.description.DescriptionCarActivity;
import com.example.drive_2_go.ui.Client.history.HistoryActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "APP_ALERTS_CHANNEL";
    // Utilisé pour assurer que les PendingIntents sont uniques
    private static int NOTIFICATION_REQUEST_CODE = 0;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Appelé lorsqu'un message FCM est reçu
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // La vérification remoteMessage.getNotification() est essentielle pour les messages "Notification Payload"
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Map<String, String> data = remoteMessage.getData();

            Log.d(TAG, "Notification Received. Data: " + data.toString());

            sendNotification(title, body, data);
        }
    }

    /**
     * Crée et affiche la notification Heads-up avec redirection intelligente.
     */
    private void sendNotification(String title, String messageBody, Map<String, String> data) {

        Intent intent;
        String notificationType = data.get("notificationType"); // Clé de décision

        // ************************************************************
        // LOGIQUE DE REDIRECTION INTELLIGENTE
        // ************************************************************

        if ("New_Car_Added".equals(notificationType) && data.containsKey("carId")) {
            // 1. Nouvelle Voiture : Redirige vers les détails de la voiture
            intent = new Intent(this, DescriptionCarActivity.class);
            intent.putExtra("CAR_ID", data.get("carId"));
            Log.d(TAG, "Redirection vers CarDetails pour nouvelle voiture.");

        } else if ("RESERVATION_ENDING".equals(notificationType)) {
            // 2. Fin de Location : Redirige vers l'historique (pour voir la réservation concernée)
            intent = new Intent(this, HistoryActivity.class);
            Log.d(TAG, "Redirection vers History pour fin de location.");

        } else if ("RESERVATION_STATUS".equals(notificationType) && data.containsKey("carId")) {
            // 3. Statut (Validée/Annulée) : Redirige vers les détails de la voiture (si l'ID est fourni)
            intent = new Intent(this, DescriptionCarActivity.class);
            intent.putExtra("CAR_ID", data.get("carId"));
            Log.d(TAG, "Redirection vers CarDetails pour statut de réservation.");

        } else {
            // Par défaut : Redirige vers la liste des notifications
            intent = new Intent(this, NotificationClientActivity.class);
            Log.d(TAG, "Redirection par défaut vers NotificationsActivity.");
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // FLAG_IMMUTABLE est requis à partir d'Android S (API 31)
        PendingIntent pendingIntent = PendingIntent.getActivity(this, NOTIFICATION_REQUEST_CODE++, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Assurez-vous que R.drawable.ic_bell existe et est une icône blanche/transparente
        int icon = R.drawable.ic_bell;

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(icon)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH); // Permet l'affichage en Heads-up

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Configuration du canal de notification (obligatoire)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Alertes Clés de l'Application",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }
}
