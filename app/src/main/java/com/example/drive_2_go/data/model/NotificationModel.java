package com.example.drive_2_go.data.model;


import com.google.firebase.Timestamp;

/**
 * Modèle de données pour une notification affichée dans la liste.
 */
public class NotificationModel {

    private String id;
    private String title;
    private String message;
    private Timestamp timestamp;
    private Timestamp timeConfirmationAdmin;
    private boolean clientRead;
    private String carId;

    // Constructeur sans argument requis par Firestore
    public NotificationModel() {}

    public NotificationModel(String id, String title, String message, Timestamp timestamp, boolean clientRead, String carId, Timestamp timeConfirmationAdmin) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.clientRead = clientRead;
        this.carId = carId;
        this.timeConfirmationAdmin = timeConfirmationAdmin;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Timestamp getTimestamp() { return timestamp; }
    public boolean isClientRead() { return clientRead; }
    public String getCarId() { return carId; }
    public Timestamp getTimeConfirmationAdmin() { return timeConfirmationAdmin; }

    // Setters (si nécessaire, mais souvent omis pour les modèles de données de liste)
}