package com.example.drive_2_go.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.Car;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private final List<Car> carList;
    private final Context context;
    private final CarClickListener clickListener;

    // Interface pour gérer le clic sur un élément de la carte
    public interface CarClickListener {
        void onCarClick(Car car);
    }

    public FavoritesAdapter(List<Car> carList, CarClickListener clickListener) {
        this.carList = carList;
        this.clickListener = clickListener;
        this.context = null; // Sera initialisé dans onCreateViewHolder
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        // ⭐️ Utilisation de votre layout 'cartecar.xml' ⭐️
        View view = LayoutInflater.from(context).inflate(R.layout.cartecar, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Car car = carList.get(position);

        // --- 1. Affichage des détails de la voiture (basé sur cartecar.xml) ---
        holder.tvCarName.setText(car.getName());
        holder.tvLicensePlate.setText(car.getLicensePlate());
        holder.tvPrice.setText(car.getPrice() + " Dh");
        holder.tvFuelType.setText(car.getFuelType());
        holder.tvMaxKm.setText("max " + car.getMaxKm() + " km");
        holder.tvBaggageCount.setText(String.valueOf(car.getBaggageCount()));
        holder.tvPeopleCount.setText(String.valueOf(car.getPeopleCount()));

        // Gestion du type de boite de vitesses
        String gearText = car.getGearType().equals("M") ? "M" : "A";
        holder.tvGearType.setText(gearText);

        // Gestion de la climatisation (AC)
        // ⭐️ LOGIQUE DE CLIMATISATION (hasAC) ⭐️
        if (car.isHasAC()) {
            holder.imgAcStatus.setImageResource(R.drawable.ic_snowflake);
            // Si la clim existe (true), affiche l'icône de vérification
            holder.imgCheckStatus.setImageResource(R.drawable.ic_check); // Utilisez votre drawable ic_check
            holder.imgCheckStatus.setVisibility(View.VISIBLE); // Optionnel, mais préférable de la garder visible
        } else {
            holder.imgAcStatus.setImageResource(R.drawable.ic_snowflake);
            // Si la clim n'existe pas (false), affiche l'icône de non-vérification
            holder.imgCheckStatus.setImageResource(R.drawable.ic_not_check); // Utilisez votre drawable ic_not_check
            holder.imgCheckStatus.setVisibility(View.VISIBLE); // La rendre visible pour montrer le statut négatif
        }
        // --- 2. Chargement de l'image avec Glide ---
        Glide.with(holder.imgCar.getContext())
                .load(car.getImageUrl())
                .placeholder(R.drawable.img_renault_captur) // Placeholder
                .into(holder.imgCar);

        // --- 3. Gestion du clic sur l'élément (cliquable) ---
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCarClick(car);
            }
        });
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    // ⭐️ ViewHolder mis à jour avec les IDs de cartecar.xml ⭐️
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Layout principal
        ImageView imgCar;
        TextView tvCarName;
        TextView tvLicensePlate;
        TextView tvPrice;

        // Vertical Details (Carburant, KM)
        TextView tvFuelType;
        TextView tvMaxKm;

        // Bottom Icons (Bagage, AC, Vitesse, Personnes)
        TextView tvBaggageCount;
        ImageView imgAcStatus; // L'icône (snowflake)
        ImageView imgCheckStatus;
        TextView tvGearType;
        TextView tvPeopleCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Vues principales
            imgCar = itemView.findViewById(R.id.img_car);
            tvCarName = itemView.findViewById(R.id.tv_car_name);
            tvLicensePlate = itemView.findViewById(R.id.tv_license_plate_detail);
            tvPrice = itemView.findViewById(R.id.tv_price);

            // Détails Vertical
            tvFuelType = itemView.findViewById(R.id.tv_fuel_type);
            tvMaxKm = itemView.findViewById(R.id.tv_max_km);

            // Icônes du bas
            tvBaggageCount = itemView.findViewById(R.id.tv_baggage_count);
            imgAcStatus = itemView.findViewById(R.id.img_ac_status);
            imgCheckStatus = itemView.findViewById(R.id.img_check_status);
            tvGearType = itemView.findViewById(R.id.tv_gear_type);
            tvPeopleCount = itemView.findViewById(R.id.tv_people_count);
        }
    }
}