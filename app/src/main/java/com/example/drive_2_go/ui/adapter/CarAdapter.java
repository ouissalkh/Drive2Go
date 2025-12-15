package com.example.drive_2_go.ui.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.Car;
import com.example.drive_2_go.ui.Admin.addeditCar.AddEditVehicleActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    private Context context;
    private List<Car> carList;
    private boolean isAdmin; // Pour savoir si on affiche les boutons Modifier/Supprimer
    private FirebaseFirestore db;

    // Constructeur
    public CarAdapter(Context context, List<Car> carList, boolean isAdmin) {
        this.context = context;
        this.carList = carList;
        this.isAdmin = isAdmin;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Liaison avec le nouveau fichier XML item_car_admin
        View view = LayoutInflater.from(context).inflate(R.layout.item_car_admin, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = carList.get(position);

        // --- 1. Remplissage des Textes ---
        // Marque + Modèle (ex: "Renault Clio")
        String fullName = (car.getBrand() != null ? car.getBrand() : "") + " " +
                (car.getModel() != null ? car.getModel() : "");
        holder.tvCarName.setText(fullName.trim());

        holder.tvLicensePlate.setText(car.getLicensePlate());
        holder.tvPrice.setText(car.getPrice() + " DH/jour");

        holder.tvFuelType.setText(car.getFuelType());
        holder.tvMaxKm.setText(car.getMaxKm() + " km");
        holder.tvGearType.setText(car.getGearType()); // "Manuelle" ou "Automatique"
        holder.tvDoorCount.setText(String.valueOf(car.getDoorCount()));
        holder.tvPeopleCount.setText(String.valueOf(car.getPeopleCount()));

        // --- 2. Gestion Disponibilité (Couleur et Texte) ---
        if (car.isAvailable()) {
            holder.tvAvailability.setText("Disponible");
            holder.tvAvailability.setTextColor(Color.parseColor("#FFFFFF"));
        } else {
            holder.tvAvailability.setText("Louée/Indispo");
            holder.tvAvailability.setTextColor(Color.parseColor("#EEEEEE"));
        }
        // 1. On définit l'image du flocon (toujours la même)
        // Assurez-vous d'avoir l'image ic_snowflake dans vos drawables, sinon utilisez une autre image
        if (holder.imgAcStatus != null) {
            holder.imgAcStatus.setImageResource(R.drawable.ic_snowflake);
        }

        // 2. On affiche le Check ou le Not Check selon le boolean hasAC
        // Vérifiez dans votre modèle Car.java si le getter s'appelle isHasAC(), hasAC() ou getHasAC()
        if (car.isHasAC()) { // Si le véhicule a une climatisation
            holder.imgCheckStatus.setVisibility(View.VISIBLE);     // Affiche le V
            holder.imgNotCheckStatus.setVisibility(View.GONE);     // Cache le X
        } else {
            holder.imgCheckStatus.setVisibility(View.GONE);        // Cache le V
            holder.imgNotCheckStatus.setVisibility(View.VISIBLE);  // Affiche le X
        }

        // --- 3. Chargement de l'Image (Glide) ---
        if (car.getImageUrl() != null && !car.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(car.getImageUrl())
                    .placeholder(R.drawable.ic_car) // Image par défaut si chargement en cours
                    .error(R.drawable.ic_car)       // Image par défaut si erreur
                    .centerCrop()
                    .into(holder.imgCar);
        } else {
            holder.imgCar.setImageResource(R.drawable.ic_car);
        }

        // --- 4. Gestion Mode Admin (Boutons Modifier/Supprimer) ---
        if (isAdmin) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);

            // Clic sur MODIFIER
            holder.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(context, AddEditVehicleActivity.class);
                intent.putExtra("CAR_ID", car.getId()); // On passe l'ID pour que l'activité sache quoi modifier
                // On peut aussi passer l'objet entier si Car implémente Serializable
                intent.putExtra("CAR_OBJECT", car);
                context.startActivity(intent);
            });

            // Clic sur SUPPRIMER
            holder.btnDelete.setOnClickListener(v -> {
                showDeleteConfirmDialog(car);
            });

        } else {
            // Si c'est un utilisateur simple, on cache les boutons d'administration
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    // --- Logique de Suppression ---
    private void showDeleteConfirmDialog(Car car) {
        new AlertDialog.Builder(context)
                .setTitle("Confirmation")
                .setMessage("Voulez-vous vraiment supprimer " + car.getBrand() + " " + car.getModel() + " ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    deleteCarFromFirestore(car.getId());
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void deleteCarFromFirestore(String carId) {
        // Attention : Vérifiez bien le nom de la collection ("Vehicles" ou "cars" ?)
        // D'après vos fichiers précédents, c'était parfois "Vehicles" et parfois "cars".
        // Je mets "Vehicles" ici car c'était dans votre AdminVehiclesActivity.
        // SI CA NE MARCHE PAS, REMPLACEZ "Vehicles" PAR "cars".
        db.collection("Vehicles").document(carId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Véhicule supprimé avec succès", Toast.LENGTH_SHORT).show();
                    // Pas besoin de supprimer de la liste manuellement ici,
                    // le SnapshotListener de l'Activity le fera automatiquement.
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                });
    }


    // --- ViewHolder : Liaison avec les IDs du XML ---
    public static class CarViewHolder extends RecyclerView.ViewHolder {

        public ImageView imgAcStatus;
        public ImageView imgCheckStatus, imgNotCheckStatus;

        TextView tvCarName, tvLicensePlate, tvPrice, tvAvailability;
        TextView tvFuelType, tvMaxKm, tvGearType, tvDoorCount, tvPeopleCount;
        ImageView imgCar;
        ImageView btnEdit, btnDelete;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);

            // IDs venant de item_car_admin.xml
            tvCarName = itemView.findViewById(R.id.tv_car_name);
            tvLicensePlate = itemView.findViewById(R.id.tv_license_plate);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvAvailability = itemView.findViewById(R.id.tv_availability);

            tvFuelType = itemView.findViewById(R.id.tv_fuel_type);
            tvMaxKm = itemView.findViewById(R.id.tv_max_km);
            tvGearType = itemView.findViewById(R.id.tv_gear_type);
            tvDoorCount = itemView.findViewById(R.id.tv_door_count);
            tvPeopleCount = itemView.findViewById(R.id.tv_people_count);

            imgCar = itemView.findViewById(R.id.img_car);

            // Climatisation
            imgAcStatus = itemView.findViewById(R.id.img_ac_status);
            imgCheckStatus = itemView.findViewById(R.id.img_check_status);
            imgNotCheckStatus = itemView.findViewById(R.id.img_not_check_status);

            // Boutons d'action
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}


