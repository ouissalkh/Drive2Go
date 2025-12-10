package com.example.drive_2_go.data.model;

import java.io.Serializable;

public class Car implements Serializable {
    private String id;
    private String name;
    private String licensePlate;
    private String price;
    private String imageUrl;
    private String fuelType;
    private String maxKm;
    private int baggageCount;
    private boolean hasAC;
    private String gearType; // M (Manuelle) ou A (Automatique)
    private int doorCount;
    private int peopleCount;
    private boolean isChecked;
    private boolean isFavorite;
    private String description;
    private String brand;
    private String model;
    private String year;
    private String color;
    private String location;
    private boolean isAvailable;
    private long timestamp;

    // Constructeur vide requis pour Firebase
    public Car() {
    }

    // Constructeur complet
    public Car(String id, String name, String licensePlate, String price, String imageUrl,
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
        this.isFavorite = isFavorite; // Initialisé dans le constructeur
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

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

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

    // ✅ Getters et Setters pour isFavorite
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

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