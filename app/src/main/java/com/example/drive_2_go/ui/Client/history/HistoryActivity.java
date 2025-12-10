package com.example.drive_2_go.ui.Client.history;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Client.accueil.AccueilActivity;
import com.example.drive_2_go.ui.Client.favoris.Favoris;
import com.example.drive_2_go.ui.Client.profil.Profil;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private ImageButton buttonProfil;
    private ImageButton buttonHome;
    private ImageButton buttonFavoris;
    private ImageButton buttonHistory;
    private ImageView btnBack;

    // TODO: Remplacez ceci par un appel API/BDD
    private static final boolean IS_CAR_AVAILABLE_SIMULATION = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        buttonProfil = findViewById(R.id.buttonProfil);
        buttonHome = findViewById(R.id.buttonHome);
        buttonFavoris = findViewById(R.id.buttonFavoris);
        buttonHistory = findViewById(R.id.buttonHistory);

        // Initialisation et listener pour le bouton de retour
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        buttonProfil.setOnClickListener(v -> openProfil());
        buttonFavoris.setOnClickListener(v -> openFavoris());
        buttonHistory.setOnClickListener(v -> Toast.makeText(this,"Déjà ici", Toast.LENGTH_SHORT).show());
        buttonHome.setOnClickListener(v -> openAccueil());
        selectButton(buttonHistory);

        // 1. Initialisation de la RecyclerView
        recyclerView = findViewById(R.id.rv_history_listings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Remplissage avec des données de simulation
        List<HistoryItem> historyList = createDummyHistoryData();
        adapter = new HistoryAdapter(this, historyList);
        recyclerView.setAdapter(adapter);



    }

    /**
     * Crée une liste d'éléments d'historique factices pour la démonstration.
     */
    private List<HistoryItem> createDummyHistoryData() {
        List<HistoryItem> list = new ArrayList<>();
        list.add(new HistoryItem("Renault Captur Auto", "Du 15/09/25 au 20/09/25", "275 dh", "En cours", true));
        list.add(new HistoryItem("BMW M4 Coupé", "Du 01/08/25 au 07/08/25", "550 dh", "Annulée", false));
        list.add(new HistoryItem("Tesla Model 3", "Du 10/06/25 au 12/06/25", "210 dh", "Retournée", true));
        return list;
    }


    /**
     * Simule la logique de nouvelle réservation.
     * Envoie une notification à l'administrateur si disponible, sinon affiche une erreur.
     * * @param carName Nom de la voiture.
     */
    private void attemptReReservation(String carName) {
        if (IS_CAR_AVAILABLE_SIMULATION) {
            // Logique de disponibilité (Succès)
            // Dans une application réelle, ceci enverrait un message ou une entrée dans une table 'notifications'
            String successMsg = "Demande de nouvelle location pour " + carName + " envoyée à l'administrateur pour validation.";
            Toast.makeText(this, successMsg, Toast.LENGTH_LONG).show();

            // TODO: Implémenter la notification réelle à l'admin ici (ex: Firebase, BDD)

        } else {
            // Logique de non-disponibilité (Échec)
            showCustomErrorToast("VOITURE NON DISPONIBLE ACTUELLEMENT.");
        }
    }

    /**
     * Affiche un Toast personnalisé avec fond rouge, texte blanc et gras.
     */
    private void showCustomErrorToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_error_toast,
                (ViewGroup) findViewById(R.id.custom_toast_container));

        TextView text = layout.findViewById(R.id.tv_toast_message);
        text.setText(message);
        text.setTextColor(Color.WHITE);
        text.setTypeface(null, Typeface.BOLD);

        // Le background rouge est géré dans le layout XML 'custom_error_toast.xml'

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        // Afficher le message en haut
        toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
        toast.setView(layout);
        toast.show();
    }


    // ***************************************************************
    // CLASSE DE MODÈLE DE DONNÉES
    // ***************************************************************
    public static class HistoryItem {
        public String name;
        public String dates;
        public String price;
        public String status;
        public boolean isRelouable; // Pour simuler si le bouton 'Relouer' apparaît

        public HistoryItem(String name, String dates, String price, String status, boolean isRelouable) {
            this.name = name;
            this.dates = dates;
            this.price = price;
            this.status = status;
            this.isRelouable = isRelouable;
        }
    }


    // ***************************************************************
    // ADAPTER POUR LA RECYCLERVIEW
    // ***************************************************************
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<HistoryItem> mData;
        private final Context mContext;

        HistoryAdapter(Context context, List<HistoryItem> data) {
            this.mContext = context;
            this.mData = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_listing, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryItem item = mData.get(position);

            holder.tvCarName.setText(item.name);
            holder.tvDates.setText(item.dates);
            holder.tvFinalPrice.setText(item.price);
            holder.tvStatus.setText(item.status);

            // Gestion du bouton "Relouer"
            if (item.isRelouable) {
                holder.btnRepeat.setVisibility(View.VISIBLE);
                holder.btnRepeat.setOnClickListener(v -> ((HistoryActivity) mContext).attemptReReservation(item.name));
            } else {
                holder.btnRepeat.setVisibility(View.GONE);
            }

            // Mise à jour de la couleur du statut
            if (item.status.equals("Retournée")) {
                // Statut "Retournée" -> Texte en ORANGE
                holder.tvStatus.setTextColor(Color.parseColor("#FF8C00")); // Orange (DarkOrange)

            } else if (item.status.equals("Annulée")) {
                // Statut "Annulée" -> Texte en ROUGE
                holder.tvStatus.setTextColor(Color.RED);

            } else if (item.status.equals("En cours")) {
                // Statut "En cours" -> Texte en VERT
                holder.tvStatus.setTextColor(Color.GREEN);

            }
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCarName;
            TextView tvDates;
            TextView tvFinalPrice;
            TextView tvStatus;
            Button btnRepeat;

            public ViewHolder(View itemView) {
                super(itemView);
                tvCarName = itemView.findViewById(R.id.tv_car_name_history);
                tvDates = itemView.findViewById(R.id.tv_dates_history);
                tvFinalPrice = itemView.findViewById(R.id.tv_final_price);
                tvStatus = itemView.findViewById(R.id.tv_status);
                btnRepeat = itemView.findViewById(R.id.btn_repeat_reservation);
            }
        }
    }

    private void selectButton(ImageButton button) {
        button.setSelected(true);
    }

    private void openProfil(){
        startActivity(new Intent(HistoryActivity.this, Profil.class));
    }

    private void openAccueil(){
        startActivity(new Intent(HistoryActivity.this, AccueilActivity.class));
    }
    private  void openFavoris (){
        startActivity(new Intent(HistoryActivity.this, Favoris.class));
    }
}
