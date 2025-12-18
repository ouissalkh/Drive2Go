package com.example.drive_2_go.ui.Admin.Notifications_admin; // Assurez-vous que le package correspond au vôtre

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Cette classe représente une notification de réservation spécifique pour l'administrateur
public class ReservationNotification {
    private String reservationId;
    private String userId;
    private String userName; // Nom de l'utilisateur qui a fait la demande
    private String userProfileImageUrl; // URL de l'image de profil de l'utilisateur
    private String carId;
    private String carModel; // Modèle de la voiture demandée
    private Date startDate; // Date de début de la location
    private Date endDate; // Date de fin de la location
    private Date requestDate; // Date et heure de la demande de réservation

    // Constructeur pour initialiser les champs obligatoires depuis Firestore
    public ReservationNotification(String reservationId, String userId, String carId, Date startDate, Date endDate, Date requestDate) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.carId = carId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.requestDate = requestDate;
        // userName, userProfileImageUrl, carModel seront définis ultérieurement via les setters
        // une fois que les détails de l'utilisateur et de la voiture sont récupérés.
    }

    // --- Getters ---
    public String getReservationId() {
        return reservationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserProfileImageUrl() {
        return userProfileImageUrl;
    }

    public String getCarId() {
        return carId;
    }

    public String getCarModel() {
        return carModel;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    // --- Setters (utilisés après la récupération des détails utilisateur/voiture) ---
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserProfileImageUrl(String userProfileImageUrl) {
        this.userProfileImageUrl = userProfileImageUrl;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    // --- Méthodes utilitaires pour le formatage des dates ---

    /**
     * Retourne une chaîne formatée pour la durée de la location (ex: "3 jours (du 13/10 au 16/10)").
     * Gère les cas où les dates sont nulles.
     * @return La durée formatée.
     */
    public String getFormattedDuration() {
        if (startDate != null && endDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.FRENCH);
            long diff = endDate.getTime() - startDate.getTime();
            // Ajoute 1 jour pour inclure la date de fin dans le calcul de la durée
            long days = (diff / (1000 * 60 * 60 * 24)) + 1;
            return String.format(Locale.FRENCH, "%d jours (du %s au %s)", days, sdf.format(startDate), sdf.format(endDate));
        }
        return "Durée inconnue";
    }

    /**
     * Retourne une chaîne formatée pour la date et l'heure de la demande (ex: "12/10/2026 à 10:30").
     * Gère le cas où la date est nulle.
     * @return La date de demande formatée.
     */
    public String getFormattedRequestDate() {
        if (requestDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH);
            return sdf.format(requestDate);
        }
        return "Date de demande inconnue";
    }
}