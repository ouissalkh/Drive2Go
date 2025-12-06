package com.example.drive_2_go.ui.Client.description;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.Car;
import com.example.drive_2_go.ui.Client.accueil.AccueilActivity;
import com.example.drive_2_go.ui.Client.login.LoginActivity;
import com.example.drive_2_go.ui.main.MainActivity;
import com.example.drive_2_go.ui.reservation.ReservationFormActivity;

public class DescriptionCarActivity extends AppCompatActivity {
    private ImageButton backButton;
    private ImageButton favBtn;
    private Button btnLouer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.description_car); // Votre layout de description

        // 1. Initialisation du bouton de retour (Assurez-vous que cet ID existe dans description_car.xml)
        backButton = findViewById(R.id.backbtn);
        ImageButton favBtn = findViewById(R.id.favbtn);

        backButton.setOnClickListener(v -> {
            backToHome();
        });

        // Récupérer l'objet Car de l'Intent
        Car selectedCar = (Car) getIntent().getSerializableExtra("selected_car");
        // Au départ -> afficher icône selon favoris


        if (selectedCar != null) {
            // Vous pouvez maintenant utiliser selectedCar pour afficher les détails
            // Exemple :
            // TextView tvName = findViewById(R.id.tv_car_name);
            // tvName.setText(selectedCar.getName());
        }
        //pour aller a la forme de reservation
        btnLouer = findViewById(R.id.orderBtn);
        btnLouer.setOnClickListener(v -> {
            Intent intent = new Intent(DescriptionCarActivity.this, ReservationFormActivity.class);
            startActivity(intent);
            finish();
        });
    }
    private void backToHome(){
        startActivity(new Intent(DescriptionCarActivity.this, AccueilActivity.class));
    }

    private void updateFavIcon(ImageButton favBtn, boolean isFav) {
        if (isFav) {
            favBtn.setColorFilter(getResources().getColor(android.R.color.holo_orange_light));
        } else {
            favBtn.setColorFilter(getResources().getColor(android.R.color.white));
        }
    }


}