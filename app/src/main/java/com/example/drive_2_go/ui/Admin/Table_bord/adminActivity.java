package com.example.drive_2_go.ui.Admin.Table_bord;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.example.drive_2_go.R;
import com.example.drive_2_go.ui.Admin.ComposantCommunAdmin.BaseAdminActivity;
import com.example.drive_2_go.ui.Admin.Notifications_admin.NotificationAdminActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class adminActivity extends BaseAdminActivity {

    // Vues UI
    private TextView tvVehiculeDispo, tvReservationActives, tvUsersCount, tvRevenuMensuel, notificationBadge, tvFilterYearLabel;
    private BarChart barChartReservationsMois;
    private PieChart pieChartReservationsMarque;
    private LinearLayout btnFilterYear;

    // Firebase
    private FirebaseFirestore db;
    private ListenerRegistration pendingReservationsListener;

    // Constantes et Variables de filtrage
    private static final String TAG = "AdminDashboard";
    private static final String COLLECTION_RESERVATIONS = "reservations";
    private static final String COLLECTION_CARS = "cars";
    private static final String COLLECTION_USERS = "users";
    private int selectedYear = 2025; // Année par défaut

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        setupNavigation();

        db = FirebaseFirestore.getInstance();

        // 1. Initialisation des composants UI
        tvVehiculeDispo = findViewById(R.id.vehiculedispo);
        tvReservationActives = findViewById(R.id.reservationactives);
        tvUsersCount = findViewById(R.id.users);
        tvRevenuMensuel = findViewById(R.id.revenuemensulle);
        notificationBadge = findViewById(R.id.notificationBadge);
        barChartReservationsMois = findViewById(R.id.barChartReservationsMois);
        pieChartReservationsMarque = findViewById(R.id.pieChartReservationsMarque);
        btnFilterYear = findViewById(R.id.btnFilterYear);
        tvFilterYearLabel = findViewById(R.id.tvFilterYearLabel);

        // 2. Configuration du bouton de filtrage
        btnFilterYear.setOnClickListener(this::showYearPopup);

        // 3. Chargement initial des données
        loadDashboardStats();       // Compteurs simples
        loadMonthlyRevenue();      // Revenu du mois (acceptées)
        updateCharts(selectedYear); // Graphiques
        setupNotificationListener();

        // 4. Action clic Notifications
        findViewById(R.id.notificationContainer).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationAdminActivity.class));
            notificationBadge.setVisibility(View.GONE);
        });
    }

    // =========================================================
    // REVENUS DU MOIS COURANT (RÉSERVATIONS ACCEPTÉES)
    // =========================================================
    private void loadMonthlyRevenue() {
        tvRevenuMensuel.setText("Calcul...");

        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH); // Mois actuel (0-11)
        int currentYear = cal.get(Calendar.YEAR);   // Année actuelle

        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("status", "acceptée")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalRevenue = 0.0;

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        // On récupère la date de confirmation (ou startDate)
                        Date dateConf = document.getDate("timeConfirmationAdmin");

                        if (dateConf != null) {
                            cal.setTime(dateConf);
                            // On vérifie si c'est le même mois et la même année
                            if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {

                                // Récupération sécurisée du prix (Long ou Double)
                                Object priceObj = document.get("totalPrice");
                                if (priceObj instanceof Number) {
                                    totalRevenue += ((Number) priceObj).doubleValue();
                                }
                            }
                        }
                    }
                    tvRevenuMensuel.setText(String.format(Locale.FRENCH, "%.2f €", totalRevenue));
                })
                .addOnFailureListener(e -> tvRevenuMensuel.setText("0.00 €"));
    }

    // =========================================================
    // FILTRAGE PAR ANNÉE (BOUTON VIOLET)
    // =========================================================
    private void showYearPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("2024");
        popup.getMenu().add("2025");
        popup.getMenu().add("2026");

        popup.setOnMenuItemClickListener(item -> {
            selectedYear = Integer.parseInt(item.getTitle().toString());
            tvFilterYearLabel.setText(String.valueOf(selectedYear));
            updateCharts(selectedYear);
            return true;
        });
        popup.show();
    }

    private void updateCharts(int year) {
        loadMonthlyReservationsChart(year);
        loadReservationsByBrandChart(year);
    }

    // =========================================================
    // GRAPHIQUE BARRE (MENSUEL)
    // =========================================================
    private void loadMonthlyReservationsChart(int year) {
        final int[] monthlyCounts = new int[12];
        Arrays.fill(monthlyCounts, 0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        db.collection(COLLECTION_RESERVATIONS).whereEqualTo("status", "acceptée").get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String dateStr = doc.getString("startDate");
                        if (dateStr != null) {
                            try {
                                Date d = sdf.parse(dateStr);
                                cal.setTime(d);
                                if (cal.get(Calendar.YEAR) == year) {
                                    monthlyCounts[cal.get(Calendar.MONTH)]++;
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    displayBarChart(monthlyCounts);
                });
    }

    private void displayBarChart(int[] counts) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 12; i++) entries.add(new BarEntry(i, counts[i]));

        BarDataSet set = new BarDataSet(entries, "Réservations");
        set.setColor(Color.parseColor("#6366f1"));
        set.setValueTextSize(10f);
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (value > 0) ? String.valueOf((int) value) : "";
            }
        });

        barChartReservationsMois.setData(new BarData(set));
        barChartReservationsMois.getDescription().setEnabled(false);
        barChartReservationsMois.animateY(1000);

        XAxis xAxis = barChartReservationsMois.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Jan","Fév","Mar","Avr","Mai","Juin","Juil","Août","Sep","Oct","Nov","Déc"}));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        barChartReservationsMois.getAxisRight().setEnabled(false);
        barChartReservationsMois.getAxisLeft().setAxisMinimum(0f);
        barChartReservationsMois.invalidate();
    }

    // =========================================================
    // GRAPHIQUE PIE (POURCENTAGE PAR MARQUE)
    // =========================================================
    private void loadReservationsByBrandChart(int year) {
        Map<String, Integer> carCounts = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        db.collection(COLLECTION_RESERVATIONS).get().addOnSuccessListener(snapshot -> {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String dateStr = doc.getString("startDate");
                try {
                    Date d = sdf.parse(dateStr);
                    cal.setTime(d);
                    if (cal.get(Calendar.YEAR) == year) {
                        String carId = doc.getString("carId");
                        if (carId != null) carCounts.put(carId, carCounts.getOrDefault(carId, 0) + 1);
                    }
                } catch (Exception ignored) {}
            }

            db.collection(COLLECTION_CARS).get().addOnSuccessListener(carSnapshot -> {
                Map<String, Integer> brandCounts = new HashMap<>();
                for (DocumentSnapshot carDoc : carSnapshot.getDocuments()) {
                    if (carCounts.containsKey(carDoc.getId())) {
                        String brand = carDoc.getString("brand");
                        brandCounts.put(brand, brandCounts.getOrDefault(brand, 0) + carCounts.get(carDoc.getId()));
                    }
                }
                displayPieChart(brandCounts);
            });
        });
    }

    private void displayPieChart(Map<String, Integer> data) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            entries.add(new PieEntry(e.getValue().floatValue(), e.getKey()));
        }

        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(new int[]{Color.parseColor("#6366f1"), Color.parseColor("#a855f7"), Color.parseColor("#2dd4bf"), Color.parseColor("#f43f5e")});
        ds.setValueTextColor(Color.WHITE);
        ds.setValueTextSize(12f);
        ds.setSliceSpace(3f);

        PieData pieData = new PieData(ds);
        pieData.setValueFormatter(new PercentFormatter(pieChartReservationsMarque));

        pieChartReservationsMarque.setData(pieData);
        pieChartReservationsMarque.setUsePercentValues(true);
        pieChartReservationsMarque.getDescription().setEnabled(false);
        pieChartReservationsMarque.setDrawHoleEnabled(true);
        pieChartReservationsMarque.setHoleRadius(50f);
        pieChartReservationsMarque.animateY(1400);
        pieChartReservationsMarque.invalidate();
    }

    // =========================================================
    // STATISTIQUES GÉNÉRALES
    // =========================================================
    private void loadDashboardStats() {
        // Véhicules dispo / total
        db.collection(COLLECTION_CARS).get().addOnSuccessListener(s -> {
            long dispo = s.getDocuments().stream().filter(d -> Boolean.TRUE.equals(d.getBoolean("available"))).count();
            tvVehiculeDispo.setText(dispo + " / " + s.size());
        });

        // Réservations actives (Total acceptées)
        db.collection(COLLECTION_RESERVATIONS).whereEqualTo("status", "acceptée").get()
                .addOnSuccessListener(s -> tvReservationActives.setText(String.valueOf(s.size())));

        // Utilisateurs inscrits
        db.collection(COLLECTION_USERS).get().addOnSuccessListener(s -> tvUsersCount.setText(String.valueOf(s.size())));
    }

    private void setupNotificationListener() {
        pendingReservationsListener = db.collection(COLLECTION_RESERVATIONS).whereEqualTo("status", "En attente")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) notificationBadge.setVisibility(snapshots.size() > 0 ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingReservationsListener != null) pendingReservationsListener.remove();
    }
}