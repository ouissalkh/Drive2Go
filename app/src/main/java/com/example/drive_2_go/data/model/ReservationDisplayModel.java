package com.example.drive_2_go.data.model; // Ou un nouveau package comme .ui.model, si vous préférez

public class ReservationDisplayModel {
    private String reservationNumber;
    private String userName;      // De la Reservation (ou combiné du User)
    private String carName;
    private String startDate;
    private String endDate;
    private String email;         // Du User
    private String phone;         // Du User
    private String status;
    private double totalPrice;

    public ReservationDisplayModel(String reservationNumber, String userName, String carName,
                                   String startDate, String endDate, String email, String phone,
                                   String status, double totalPrice) {
        this.reservationNumber = reservationNumber;
        this.userName = userName;
        this.carName = carName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.totalPrice = totalPrice;
    }

    // Getters pour tous les champs
    public String getReservationNumber() { return reservationNumber; }
    public String getUserName() { return userName; }
    public String getCarName() { return carName; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getStatus() { return status; }
    public double getTotalPrice() { return totalPrice; }

    // Setters si nécessaire (pour modification ou si certains champs sont facultatifs)
    public void setReservationNumber(String reservationNumber) { this.reservationNumber = reservationNumber; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setCarName(String carName) { this.carName = carName; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setStatus(String status) { this.status = status; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}