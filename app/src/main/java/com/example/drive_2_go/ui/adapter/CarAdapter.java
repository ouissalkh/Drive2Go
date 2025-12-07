package com.example.drive_2_go.ui.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Admin.addeditCar.AddEditVehicleActivity;
import com.example.drive_2_go.data.model.Car;
import com.example.drive_2_go.utils.ImageUtils;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.List;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    private Context context;
    private List<Car> carList;
    private boolean isAdmin;

    public CarAdapter(Context context, List<Car> carList, boolean isAdmin) {
        this.context = context;
        this.carList = carList;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Utilise le NOUVEAU layout XML complexe
        View view = LayoutInflater.from(context).inflate(R.layout.item_car_admin, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = carList.get(position);

        // --- 1. Textes Principaux ---
        holder.tvCarName.setText(car.getName());
        holder.tvLicensePlate.setText(car.getLicensePlate());
        holder.tvPrice.setText(car.getPrice() + " Dh"); // Format "450 Dh"
        holder.tvFuelType.setText(car.getFuelType());
        holder.tvMaxKm.setText("Max " + car.getMaxKm() + " km");

        // --- 2. Détails techniques (Icônes du bas) ---
        holder.tvBaggageCount.setText(String.valueOf(car.getBaggageCount()));
        holder.tvDoorCount.setText(String.valueOf(car.getDoorCount()));
        holder.tvPeopleCount.setText(String.valueOf(car.getPeopleCount()));

        // Boîte de vitesse (M ou A)
        String gearShort = "A".equals(car.getGearType()) ? "A" : "M";
        holder.tvGearType.setText(gearShort);

        // Climatisation (Afficher/Cacher ou colorer)
        if (car.isHasAC()) {
            holder.imgAcStatus.setVisibility(View.VISIBLE);
            holder.imgAcStatus.setAlpha(1.0f); // Opaque
        } else {
            holder.imgAcStatus.setVisibility(View.GONE); // Ou setAlpha(0.3f) pour griser
        }

        // Vérifié (Badge Check)
        if (car.isChecked()) {
            holder.imgCheckStatus.setVisibility(View.VISIBLE);
        } else {
            holder.imgCheckStatus.setVisibility(View.GONE);
        }

        // --- 3. Barre de Disponibilité (Nouveau) ---
        if (car.isAvailable()) {
            holder.tvAvailability.setText("Disponible");
            holder.tvAvailability.setBackgroundColor(Color.parseColor("#4CAF50")); // Vert
        } else {
            holder.tvAvailability.setText("Loué");
            holder.tvAvailability.setBackgroundColor(Color.parseColor("#F44336")); // Rouge
        }

        // --- 4. Image ---
        String imagePath = car.getImageUrl();
        if (imagePath != null && !imagePath.isEmpty()) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Glide.with(context)
                        .load(imgFile)
                        .placeholder(R.drawable.car)
                        .centerCrop()
                        .into(holder.imgCar);
            } else {
                holder.imgCar.setImageResource(R.drawable.car);
            }
        } else {
            holder.imgCar.setImageResource(R.drawable.car);
        }


        // --- 6. Boutons Admin (Modifier / Supprimer) ---
        if (isAdmin) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);

            holder.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(context, AddEditVehicleActivity.class);
                intent.putExtra("CAR_OBJECT", car);
                context.startActivity(intent);
            });

            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Suppression")
                        .setMessage("Supprimer " + car.getName() + " ?")
                        .setPositiveButton("Oui", (dialog, which) -> deleteCar(car, position))
                        .setNegativeButton("Non", null)
                        .show();
            });
        } else {
            // Si c'est l'interface client, on cache les outils d'édition
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    private void deleteCar(Car car, int position) {
        FirebaseFirestore.getInstance().collection("cars")
                .document(car.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    ImageUtils.deleteImage(car.getImageUrl());
                    carList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, carList.size());
                    Toast.makeText(context, "Supprimé", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    // --- ViewHolder : Liaison avec le XML complexe ---
    public static class CarViewHolder extends RecyclerView.ViewHolder {

        // Textes
        TextView tvPrice, tvAvailability, tvCarName, tvLicensePlate, tvFuelType, tvMaxKm;
        TextView tvBaggageCount, tvGearType, tvDoorCount, tvPeopleCount;

        // Images
        ImageView imgCar, imgAcStatus, imgCheckStatus, btnFavorite;

        // Boutons Admin
        ImageView btnEdit, btnDelete;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);

            tvPrice = itemView.findViewById(R.id.tv_price);
            tvAvailability = itemView.findViewById(R.id.tv_availability);
            tvCarName = itemView.findViewById(R.id.tv_car_name);
            tvLicensePlate = itemView.findViewById(R.id.tv_license_plate);
            tvFuelType = itemView.findViewById(R.id.tv_fuel_type);
            tvMaxKm = itemView.findViewById(R.id.tv_max_km);

            tvBaggageCount = itemView.findViewById(R.id.tv_baggage_count);
            tvGearType = itemView.findViewById(R.id.tv_gear_type);
            tvDoorCount = itemView.findViewById(R.id.tv_door_count);
            tvPeopleCount = itemView.findViewById(R.id.tv_people_count);

            imgCar = itemView.findViewById(R.id.img_car);
            imgAcStatus = itemView.findViewById(R.id.img_ac_status);
            imgCheckStatus = itemView.findViewById(R.id.img_check_status);

            //btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}