package com.example.drive_2_go.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Reservation {

    // Champs correspondant exactement à votre Firestore
    private String carId;
    private String userId;       // Clé pour trouver le profil utilisateur
    private String userName;     // Nom affiché dans la notification
    private String carName;      // Nom de la voiture (Ford Fusion, etc.)
    private String startDate;
    private String endDate;
    private String status;       // "En attente", "acceptée", etc.
    private double totalPrice;
    private String paymentMethod; // "Chèque", etc. (Vu dans votre capture d'écran)

    // Champs de dates (Timestamp Firestore)
    private Date timeConfirmationAdmin;
    private Date timeReservationClient;

    // Champ local (ne vient pas forcément de la base, mais utile dans l'app)
    private String reservationNumber;

    // 1. Constructeur vide (OBLIGATOIRE pour Firestore)
    public Reservation() {
    }

    // 2. Constructeur complet (Optionnel, utile pour vos tests)
    public Reservation(String userId, String userName, String carId, String carName,
                       String startDate, String endDate, String status, double totalPrice,
                       String reservationNumber, Date timeConfirmationAdmin) {
        this.userId = userId;
        this.userName = userName;
        this.carId = carId;
        this.carName = carName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.totalPrice = totalPrice;
        this.reservationNumber = reservationNumber;
        this.timeConfirmationAdmin = timeConfirmationAdmin;
    }

    // --- GETTERS ET SETTERS ---

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getCarId() { return carId; }
    public void setCarId(String carId) { this.carId = carId; }

    public String getCarName() { return carName; }
    public void setCarName(String carName) { this.carName = carName; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    // Gestion de l'ID du document (Numéro de réservation)
    // On utilise @Exclude pour ne pas réécrire l'ID à l'intérieur du document si on renvoie l'objet
    @Exclude
    public String getReservationNumber() { return reservationNumber; }
    public void setReservationNumber(String reservationNumber) { this.reservationNumber = reservationNumber; }

    // Gestion des dates avec @ServerTimestamp
    @ServerTimestamp
    public Date getTimeConfirmationAdmin() { return timeConfirmationAdmin; }
    public void setTimeConfirmationAdmin(Date timeConfirmationAdmin) { this.timeConfirmationAdmin = timeConfirmationAdmin; }

    @ServerTimestamp
    public Date getTimeReservationClient() { return timeReservationClient; }
    public void setTimeReservationClient(Date timeReservationClient) { this.timeReservationClient = timeReservationClient; }
}