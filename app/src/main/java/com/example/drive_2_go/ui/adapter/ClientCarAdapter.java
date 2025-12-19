package com.example.drive_2_go.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color; // Pour la couleur de disponibilité
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // 👈 Import manquant ajouté
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.Car;
import com.example.drive_2_go.ui.Client.description.DescriptionCarActivity;

import java.io.File;
import java.util.List;

// Changement : On utilise ClientCarViewHolder comme type générique
public class ClientCarAdapter extends RecyclerView.Adapter<ClientCarAdapter.ClientCarViewHolder> {

    // --- Interface de Clic ---
    public interface OnCarClickListener {
        void onCarClick(Car car);
    }
    private final Context context;
    private final List<Car> carList;

    public ClientCarAdapter(Context context, List<Car> carList) {
        this.context = context;
        this.carList = carList;
    }

    @NonNull
    @Override
    public ClientCarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // CHARGE LE LAYOUT CLIENT (R.layout.cartecar)
        View view = LayoutInflater.from(context).inflate(R.layout.cartecar, parent, false);
        return new ClientCarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClientCarViewHolder holder, int position) {
        Car car = carList.get(position);

        // --- 1. Liaison des données de base et des caractéristiques ---
        holder.tvCarName.setText(car.getBrand() + " " + car.getModel());
        holder.tvLicensePlate.setText(car.getLicensePlate());
        holder.tvPrice.setText(car.getPrice() + " Dh");
        holder.tvFuelType.setText(car.getFuelType());
        holder.tvMaxKm.setText("max " + car.getMaxKm() + " km");

        // --- 2. Statut de Disponibilité ---
        if (car.isAvailable()) {
            holder.tvAvailability.setText("Disponible");
            // Utilisez Color.parseColor ou context.getResources().getColor(R.color.votre_couleur)
            holder.tvAvailability.setTextColor(Color.parseColor("#0CE729")); // Vert
        } else {
            holder.tvAvailability.setText("Indisponible");
            holder.tvAvailability.setTextColor(Color.parseColor("#F44336")); // Rouge
        }

        // --- 3. Icônes et décompte (Ligne du bas) ---
        holder.tvBaggageCount.setText(String.valueOf(car.getBaggageCount()));
        holder.tvGearType.setText(car.getGearType());
        holder.tvDoorCount.setText(String.valueOf(car.getDoorCount()));
        holder.tvPeopleCount.setText(String.valueOf(car.getPeopleCount()));


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



        // --- 4. Image ---
        String imageUrl = car.getImageUrl();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Utilisez Glide pour charger l'image
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.car) // Optionnel: utilisez R.drawable.car comme placeholder
                    .into(holder.imgCar);

            // NOTE : Le bloc de vérification 'http' et 'File' peut être conservé
            // SEULEMENT si vous avez BESOIN de gérer les anciens chemins de fichiers locaux.
            // Si toutes vos données sont maintenant des URL, vous pouvez simplifier à :
            /*
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.car)
                .into(holder.imgCar);
            */

        } else {
            // Si l'URL est vide/null, chargez l'image par défaut
            holder.imgCar.setImageResource(R.drawable.car);
        }



        // --- 5. GESTION DU CLIC CLIENT ---
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DescriptionCarActivity.class);
            // Assurez-vous que l'objet Car est sérialisable (implemente Serializable ou Parcelable)
            intent.putExtra(DescriptionCarActivity.EXTRA_CAR, car);
            context.startActivity(intent);
        });
    }

    // 👈 MÉTHODE OBLIGATOIRE AJOUTÉE
    @Override
    public int getItemCount() {
        return carList.size();
    }

    // --- ViewHolder : Liaison avec cartecar.xml ---
    public static class ClientCarViewHolder extends RecyclerView.ViewHolder {

        // Déclaration complète de TOUTES les vues de cartecar.xml
        TextView tvPrice, tvFuelType, tvMaxKm, tvAvailability, tvCarName, tvLicensePlate;
        TextView tvBaggageCount, tvGearType, tvDoorCount, tvPeopleCount;
        ImageView imgCar, imgCheckStatus; // 👈 Les ImageView étaient manquantes
        ImageView imgAcStatus;

        public ClientCarViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialisation des TextViews
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvAvailability = itemView.findViewById(R.id.tv_availability);
            tvCarName = itemView.findViewById(R.id.tv_car_name);
            tvLicensePlate = itemView.findViewById(R.id.tv_license_plate_detail);
            tvFuelType = itemView.findViewById(R.id.tv_fuel_type);
            tvMaxKm = itemView.findViewById(R.id.tv_max_km);

            // Initialisation des TextViews pour les icônes du bas
            tvBaggageCount = itemView.findViewById(R.id.tv_baggage_count);
            tvGearType = itemView.findViewById(R.id.tv_gear_type);
            tvDoorCount = itemView.findViewById(R.id.tv_door_count);
            tvPeopleCount = itemView.findViewById(R.id.tv_people_count);

            // Initialisation des ImageViews
            imgCar = itemView.findViewById(R.id.img_car);
            imgCheckStatus = itemView.findViewById(R.id.img_check_status);
            imgAcStatus = itemView.findViewById(R.id.img_ac_status);
        }
    }
}