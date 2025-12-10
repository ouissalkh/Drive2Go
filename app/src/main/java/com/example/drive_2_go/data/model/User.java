package com.example.drive_2_go.data.model;

public class User {
    private String id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String role; // "client" ou "admin"
    private boolean isVerified;
    private String verificationCode;
    private Number dateInscription;

    public User() {
        // Constructeur vide nécessaire pour Firestore
    }

    public User(String id, String nom, String prenom, String email, String telephone,
                String role, boolean isVerified, Long dateInscription) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.role = role;
        this.isVerified = isVerified;
        this.dateInscription = dateInscription;
    }

    // Getters
    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getTelephone() { return telephone; }
    public String getRole() { return role; }
    public boolean isVerified() { return isVerified; }
    public String getVerificationCode() { return verificationCode; }
    public Long getDateInscription() { return dateInscription.longValue(); }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setNom(String nom) { this.nom = nom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public void setEmail(String email) { this.email = email; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public void setRole(String role) { this.role = role; }
    public void setVerified(boolean verified) { isVerified = verified; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    public void setDateInscription(Long dateInscription) { this.dateInscription = dateInscription; }
}