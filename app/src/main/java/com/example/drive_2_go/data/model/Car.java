package com.example.drive_2_go.data.model;

import java.io.Serializable;

/**
 * Modèle de données pour une voiture
 * Implémente Serializable pour pouvoir passer l'objet entre les activités
 */
public class Car implements Serializable {

    // IMPORTANT : Un serialVersionUID est recommandé
    private static final long serialVersionUID = 1L;
    private String id; // ID Firebase
    private String name;
    private String licensePlate;
    private int price; // Prix par jour
    private String imageUrl; // URL de l'image dans Firebase Storage
    private String fuelType; // Essence, Diesel, Électrique, Hybride
    private String maxKm; // Kilométrage maximum
    private int baggageCount;
    private boolean hasAC; // Climatisation
    private String gearType; // M (Manuelle) ou A (Automatique)
    private int doorCount;
    private int peopleCount;
    private boolean isChecked;
    private String description; // Description détaillée de la voiture
    private String brand; // Marque (Renault, Peugeot, etc.)
    private String model; // Modèle (Captur, 208, etc.)
    private String year; // Année de fabrication
    private String color; // Couleur
    private String location; // Localisation de la voiture
    private boolean isAvailable; // Disponibilité
    private long timestamp; // Date d'ajout

    // Constructeur vide requis pour Firebase
    public Car() {
    }

    // Constructeur complet
    public Car(String id, String name, String licensePlate, int price, String imageUrl,
               String fuelType, String maxKm, int baggageCount, boolean hasAC,
               String gearType, int doorCount, int peopleCount, boolean isChecked,
               boolean isFavorite, String description, String brand, String model,
               String year, String color, String location, boolean isAvailable) {
        this.id = id;
        this.name = name;
        this.licensePlate = licensePlate;
        this.price = price;
        this.imageUrl = imageUrl;
        this.fuelType = fuelType;
        this.maxKm = maxKm;
        this.baggageCount = baggageCount;
        this.hasAC = hasAC;
        this.gearType = gearType;
        this.doorCount = doorCount;
        this.peopleCount = peopleCount;
        this.isChecked = isChecked;
        //this.isFavorite = isFavorite;
        this.description = description;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.color = color;
        this.location = location;
        this.isAvailable = isAvailable;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getMaxKm() { return maxKm; }
    public void setMaxKm(String maxKm) { this.maxKm = maxKm; }

    public int getBaggageCount() { return baggageCount; }
    public void setBaggageCount(int baggageCount) { this.baggageCount = baggageCount; }

    public boolean isHasAC() { return hasAC; }
    public void setHasAC(boolean hasAC) { this.hasAC = hasAC; }

    public String getGearType() { return gearType; }
    public void setGearType(String gearType) { this.gearType = gearType; }

    public int getDoorCount() { return doorCount; }
    public void setDoorCount(int doorCount) { this.doorCount = doorCount; }

    public int getPeopleCount() { return peopleCount; }
    public void setPeopleCount(int peopleCount) { this.peopleCount = peopleCount; }

    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }

    // public boolean isFavorite() { return isFavorite; }
    // public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

}