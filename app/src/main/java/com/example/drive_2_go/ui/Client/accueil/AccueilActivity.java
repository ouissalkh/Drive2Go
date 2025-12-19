package com.example.drive_2_go.ui.Client.accueil;

import com.example.drive_2_go.R;
import com.example.drive_2_go.data.model.Car;
// IMPORTANT : Assurez-vous d'utiliser l'adaptateur CLIENT
import com.example.drive_2_go.ui.adapter.ClientCarAdapter;
import com.example.drive_2_go.ui.Client.favoris.Favoris;
import com.example.drive_2_go.ui.Client.history.HistoryActivity;
import com.example.drive_2_go.ui.Client.notification.NotificationClientActivity;
import com.example.drive_2_go.ui.Client.profil.Profil;
import com.example.drive_2_go.ui.adapter.BrandAdapter;

import java.util.Collections;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Imports Firebase Firestore
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AccueilActivity extends AppCompatActivity {

    private static final String TAG = "AccueilActivity";


    private RecyclerView rvBrands;
    // Déclaration des membres pour la liste de voitures
    private RecyclerView rvCarListings;
    private ClientCarAdapter carAdapter;
    private List<Car> carList; // Liste de données
    private List<Car> carListChercher;

    private ImageButton buttonProfil;
    private ImageButton buttonHome;
    private ImageButton buttonFavoris;
    private ImageButton buttonHistory;
    private ImageButton btnMenu;
    private ImageButton btn_notifications;
    private View notificationBadge;
    private Button btnFilter;
    private EditText etSearch;
    private androidx.cardview.widget.CardView contactInfoPanel;
    private androidx.cardview.widget.CardView filterPanel;
    private Button btnMoinsPlus;
    private Button btnPlusMoins;


    // Instance de Firebase Firestore
    private com.google.firebase.firestore.ListenerRegistration notificationListener;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId = "";

    @Override
    protected void onResume() {
        super.onResume();
        fetchUnreadNotificationCount();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_home); // Assurez-vous que c'est le bon nom de layout

        // Initialisation de Firebase
        db = FirebaseFirestore.getInstance();

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }


        // Initialisation des boutons de navigation et des vues
        buttonProfil = findViewById(R.id.buttonProfil);
        buttonHome = findViewById(R.id.buttonHome);
        buttonFavoris = findViewById(R.id.buttonFavoris);
        buttonHistory = findViewById(R.id.buttonHistory);
        btnMenu = findViewById(R.id.btn_menu);
        contactInfoPanel = findViewById(R.id.contact_info_panel);
        btnFilter = findViewById(R.id.btn_filter);
        filterPanel = findViewById(R.id.filter_panel);
        btn_notifications = findViewById(R.id.btn_notifications);
        notificationBadge = findViewById(R.id.notification_badge);
        //recherche
        etSearch = findViewById(R.id.et_search);
        //Initialisation des boutons de tri ---
        btnMoinsPlus = findViewById(R.id.moins_plus);
        btnPlusMoins = findViewById(R.id.plus_mois);

        // Configuration des écouteurs de la barre supérieure
        btnMenu.setOnClickListener(v -> toggleViewVisibility(contactInfoPanel));
        btnFilter.setOnClickListener(v -> toggleViewVisibility(filterPanel));
        btn_notifications.setOnClickListener(v -> {
            notificationBadge.setVisibility(View.GONE);

            // Marquer les notifs privées dans Firestore
            markNotificationsAsRead();

            // Mettre à jour le timestamp local pour les alertes globales
            long currentTime = System.currentTimeMillis() / 1000;
            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .edit()
                    .putLong("last_global_read_timestamp", currentTime)
                    .apply();

            // Redémarrer le listener pour qu'il prenne en compte le nouveau "lastRead"
            if (notificationListener != null) notificationListener.remove();
            startNotificationListener();

            startActivity(new Intent(AccueilActivity.this, NotificationClientActivity.class));
        });

        //filters
        btnMoinsPlus.setOnClickListener(v -> {
            sortCarsByPrice(true); // True pour Moins cher -> Plus cher
            toggleViewVisibility(filterPanel); // Cacher le panneau de filtre
        });

        btnPlusMoins.setOnClickListener(v -> {
            sortCarsByPrice(false); // False pour Plus cher -> Moins cher
            toggleViewVisibility(filterPanel); // Cacher le panneau de filtre
        });



        // Configuration de la barre de navigation inférieure
        buttonProfil.setOnClickListener(v -> openProfil());
        buttonFavoris.setOnClickListener(v -> openFavoris());
        buttonHome.setOnClickListener(v -> openAccueil());
        buttonHistory.setOnClickListener(v -> openHistory());
        selectButton(buttonHome);

        // RecyclerView des marques
        rvBrands = findViewById(R.id.rv_brands);
        // ... (Logique BrandAdapter existante)
        // Dans onCreate, dans la section "RecyclerView des marques":

        List<Integer> icons = Arrays.asList(
                R.drawable.audi_br, R.drawable.bmw_br, R.drawable.ford_br,
                R.drawable.mercedes_br, R.drawable.volkswagen, R.drawable.ic_renault, R.drawable.ic_peugeot, R.drawable.ic_toyota, R.drawable.ic_tesla
        );

        // Initialisation des noms de marques (doit correspondre à l'ordre des icônes!)
        List<String> brandNames = Arrays.asList("Audi", "BMW", "Ford", "Mercedes", "Volkswagen", "Renault", "Peugeot", "Toyota", "Tesla");

        // Assurez-vous que votre BrandAdapter est mis à jour pour prendre 'int position' si possible.
        // Si vous utilisez la version actuelle qui prend 'brandResId', vous devez le modifier.

        // --- NOUVELLE LOGIQUE DE CLIC DE MARQUE ---
        BrandAdapter brandAdapter = new BrandAdapter(icons, brandResId -> {
            // 1. Trouver l'index de la ressource cliquée
            int position = icons.indexOf(brandResId);

            if (position != -1 && position < brandNames.size()) {
                String selectedBrand = brandNames.get(position);

                // 2. Appeler la nouvelle fonction de filtrage
                filterCarsByBrand(selectedBrand);

                Toast.makeText(AccueilActivity.this, "Filtrage par : " + selectedBrand, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AccueilActivity.this, "Marque inconnue.", Toast.LENGTH_SHORT).show();
            }
        });

        rvBrands.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvBrands.setAdapter(brandAdapter);

        // --- Configuration du RecyclerView des Voitures ---
        rvCarListings = findViewById(R.id.rv_car_listings);
        carList = new ArrayList<>(); // Initialise la liste vide
        carListChercher = new ArrayList<>();

        // Initialise l'adaptateur avec la liste vide
        carAdapter = new ClientCarAdapter(this, carListChercher);
        rvCarListings.setLayoutManager(new LinearLayoutManager(this));
        rvCarListings.setAdapter(carAdapter);

        // Démarrer la récupération des données
        fetchCarListings();

        //fonction de recherche
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Non utilisé
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Non utilisé
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Appeler la méthode de filtrage avec le texte entré
                filterCars(editable.toString());
            }
        });

        // Dans onCreate, après l'initialisation des autres vues :

        LinearLayout idPhone = findViewById(R.id.id_phone);
        LinearLayout idEmail = findViewById(R.id.id_email);
        LinearLayout idAdresse = findViewById(R.id.id_adresse);

        // Click pour l'appel
        idPhone.setOnClickListener(v -> {
            String phoneNumber = "0636707122"; // Le numéro affiché
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(android.net.Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        });

        // Click pour l'email
        idEmail.setOnClickListener(v -> {
            String email = "admin@drive2go.com"; // Remplacez par le vrai mail
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(android.net.Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Demande d'information - Drive2Go");
            try {
                startActivity(Intent.createChooser(intent, "Envoyer un e-mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "Aucune application de messagerie installée.", Toast.LENGTH_SHORT).show();
            }
        });

        // Click pour l'adresse (Google Maps)
        idAdresse.setOnClickListener(v -> {
            String address = "Maison Drive2Go"; // Remplacez par l'adresse réelle
            android.net.Uri gmmIntentUri = android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(address));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps"); // Force l'ouverture dans Google Maps

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Si Google Maps n'est pas installé, ouvrir n'importe quelle app de cartes
                startActivity(new Intent(Intent.ACTION_VIEW, gmmIntentUri));
            }
        });
    }

    // Méthode pour basculer la visibilité d'une vue (pour btnMenu et btnFilter)
    private void toggleViewVisibility(View view) {
        if (view.getVisibility() == View.GONE) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * Récupère la liste des voitures depuis la collection "cars" de Firestore.
     */




    private void selectButton(ImageButton button) {
        // Réinitialiser tous les boutons avant de sélectionner
        buttonProfil.setSelected(false);
        buttonHome.setSelected(false);
        buttonFavoris.setSelected(false);
        buttonHistory.setSelected(false);

        button.setSelected(true);
    }

    private void openProfil(){
        startActivity(new Intent(AccueilActivity.this, Profil.class));
    }

    private void openFavoris(){
        startActivity(new Intent(AccueilActivity.this, Favoris.class));
    }
    private void openHistory(){
        startActivity(new Intent(AccueilActivity.this, HistoryActivity.class));
    }
    private void openAccueil(){
        startActivity(new Intent(AccueilActivity.this, AccueilActivity.class));
    }

    //les voitires recherchees:
    private void fetchCarListings() {
        db.collection("cars")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Vider la liste complète (source) et la liste affichée (filtrée)
                        carList.clear();
                        carListChercher.clear(); // <-- C'est maintenant sûr car elle est initialisée dans onCreate

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Car car = document.toObject(Car.class);
                                carList.add(car); // Liste complète/Originale
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur lors de la conversion du document Firestore en objet Car: " + e.getMessage(), e);
                            }
                        }

                        // Au démarrage, la liste affichée est la même que la liste complète
                        carListChercher.addAll(carList);

                        carAdapter.notifyDataSetChanged();

                        if (carListChercher.isEmpty()) {
                            Toast.makeText(this, "Aucune voiture disponible pour le moment.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.w(TAG, "Erreur lors de la récupération des documents: ", task.getException());
                        Toast.makeText(this, "Échec de la récupération des données : " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Ajouter cette méthode à la fin de la classe AccueilActivity

    /**
     * Filtre la liste des voitures en fonction du texte de recherche.
     * @param text La chaîne de caractères entrée dans la barre de recherche.
     */
    private void filterCars(String text) {
        // Convertir le texte en minuscules pour une recherche insensible à la casse
        String lowerCaseText = text.toLowerCase();

        // Nouvelle liste pour stocker les résultats du filtre
        List<Car> filteredList = new ArrayList<>();

        // Itérer sur la liste originale
        for (Car car : carList) {
            // Vérifier si le nom de la voiture (car.getName()) contient le texte entré
            // Assurez-vous que la classe Car a bien une méthode getName()
            if (car.getName() != null && car.getName().toLowerCase().contains(lowerCaseText)) {
                filteredList.add(car);
            }
        }

        // Mettre à jour la liste de l'adaptateur sans créer un nouvel adaptateur
        carListChercher.clear();
        carListChercher.addAll(filteredList);

        // Informer l'adaptateur du changement
        carAdapter.notifyDataSetChanged();

        if (filteredList.isEmpty() && !text.isEmpty()) {
            Toast.makeText(this, "Aucune voiture trouvée pour la recherche: " + text, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Trie la liste des voitures affichées (carListChercher) par prix.
     * @param ascending Si true, trie du prix le plus bas au plus haut (Moins cher -> Plus cher).
     */
    private void sortCarsByPrice(boolean ascending) {
        if (carListChercher.isEmpty()) {
            Toast.makeText(this, "Aucune voiture à trier.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tri de la liste actuellement affichée (carListChercher)
        Collections.sort(carListChercher, (car1, car2) -> {

            // ⚠️ REMPLACEZ car1.getPrice() et car2.getPrice() PAR VOTRE MÉTHODE CORRECTE.
            // La méthode doit retourner un type numérique (double, int, etc.).
            // Exemple ci-dessous utilisant getPrice() :
            double price1 = car1.getPrice();
            double price2 = car2.getPrice();

            if (price1 < price2) {
                return ascending ? -1 : 1; // Moins cher avant Plus cher si ascendant
            } else if (price1 > price2) {
                return ascending ? 1 : -1; // Plus cher avant Moins cher si descendant
            } else {
                return 0; // Prix égaux
            }
        });

        // Informer l'adaptateur que les données ont changé
        carAdapter.notifyDataSetChanged();
        Toast.makeText(this, ascending ? "Trié : Moins cher → Plus cher" : "Trié : Plus cher → Moins cher", Toast.LENGTH_SHORT).show();
    }


    /**
     * Filtre la liste des voitures pour n'afficher que celles correspondant à la marque sélectionnée.
     * @param brandName Le nom de la marque à filtrer (ex: "Audi").
     */
    private void filterCarsByBrand(String brandName) {
        if (brandName == null || brandName.isEmpty()) {
            // Si la marque est vide ou null, réafficher toutes les voitures.
            carListChercher.clear();
            carListChercher.addAll(carList);
        } else {
            // Convertir la marque sélectionnée en minuscules pour la comparaison
            String lowerCaseBrandName = brandName.toLowerCase();

            List<Car> filteredList = new ArrayList<>();

            // Itérer sur la liste originale (carList)
            for (Car car : carList) {

                // ⭐️ MODIFICATION IMPORTANTE : Utiliser car.getBrand()
                // Assurez-vous que l'attribut 'brand' est bien initialisé dans Firebase.
                String carBrand = car.getBrand();

                if (carBrand != null && carBrand.toLowerCase().contains(lowerCaseBrandName)) {
                    filteredList.add(car);
                }
            }

            // Mettre à jour la liste de l'adaptateur
            carListChercher.clear();
            carListChercher.addAll(filteredList);
        }

        // Informer l'adaptateur du changement
        carAdapter.notifyDataSetChanged();
    }

    /**
     * Assurez-vous d'initialiser mAuth et currentUserId dans onCreate
     * pour que cette méthode fonctionne.
     */
    private void checkUnreadNotifications() {
        if (currentUserId == null || currentUserId.isEmpty()) return;

        // Requête A : Réservations non lues
        Task<QuerySnapshot> unreadReservationsTask = db.collection("reservations")
                .whereEqualTo("userId", currentUserId)
                .whereIn("status", List.of("acceptée", "refusée"))
                .whereEqualTo("clientRead", false)
                .get();

        // Requête B : Alertes Spécifiques non lues
        Task<QuerySnapshot> unreadUserAlertsTask = db.collection("user_alerts")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("clientRead", false)
                .get();

        // Requête C : Alertes Globales (Nouvelles Voitures)
        Task<QuerySnapshot> globalAlertsTask = db.collection("global_alerts")
                .whereEqualTo("type", "New_Car_Added").get();

        // Combine les deux requêtes
        Tasks.whenAllSuccess(unreadReservationsTask, unreadUserAlertsTask)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Object> results = task.getResult();
                        int countA = ((QuerySnapshot) results.get(0)).size();
                        int countB = ((QuerySnapshot) results.get(1)).size();
                        int countC = ((QuerySnapshot) results.get(2)).size();

                        // Le badge apparaît si A > 0 OU B > 0 OU C > 0
                        updateNotificationBadge((countA + countB + countC) > 0);

                    } else {
                        Log.e(TAG, "Erreur lors de la vérification des notifications non lues: " + task.getException());
                    }
                });
    }

    /**
     * Met à jour la visibilité du badge de notification.
     * @param showBadge true pour afficher le cercle rouge, false pour le masquer.
     */
    private void updateNotificationBadge(boolean showBadge) {
        if (notificationBadge != null) {
            // Utiliser View.VISIBLE ou View.GONE selon la variable showBadge
            notificationBadge.setVisibility(showBadge ? View.VISIBLE : View.GONE);
            Log.d(TAG, "Notification badge set to: " + (showBadge ? "VISIBLE" : "GONE"));
        } else {
            Log.e(TAG, "notificationBadge est null. Vérifiez l'initialisation du findViewById.");
        }
    }

    /**
     * Récupère le nombre de notifications non lues (pour les collections qui ont un champ 'isRead').
     */
    private void fetchUnreadNotificationCount() {
        if (currentUserId.isEmpty()) {
            return;
        }

        // Récupérer le timestamp de la dernière lecture locale
        long lastRead = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getLong("last_global_read_timestamp", 0);

        Task<QuerySnapshot> resTask = db.collection("reservations")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("clientRead", false).get();

        Task<QuerySnapshot> userAlertsTask = db.collection("user_alerts")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("clientRead", false).get();

        Task<QuerySnapshot> globalAlertsTask = db.collection("global_alerts")
                .whereEqualTo("type", "New_Car_Added")
                .whereGreaterThan("timestamp", lastRead)
                .get();

        Tasks.whenAllSuccess(resTask, userAlertsTask, globalAlertsTask)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int total = 0;
                        for (Object res : task.getResult()) {
                            total += ((QuerySnapshot) res).size();
                        }
                        updateNotificationBadge(total > 0);
                    }
                });
    }

    private void startNotificationListener() {
        if (currentUserId == null || currentUserId.isEmpty()) return;

        // 1. Écoute des réservations personnelles
        db.collection("reservations")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("clientRead", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    // Si on a des réservations non lues, on affiche le badge
                    if (snapshots != null && !snapshots.isEmpty()) {
                        updateNotificationBadge(true);
                    } else {
                        // Si plus de réservations, on vérifie quand même les alertes globales
                        fetchUnreadNotificationCount();
                    }
                });

        // 2. Écoute des alertes globales avec filtre de temps
        long lastRead = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getLong("last_global_read_timestamp", 0);

        db.collection("global_alerts")
                .whereEqualTo("type", "New_Car_Added")
                .whereGreaterThan("timestamp", lastRead) // IMPORTANT : n'écouter que les NOUVELLES
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null && !snapshots.isEmpty()) {
                        updateNotificationBadge(true);
                        Log.d(TAG, "Nouvelle alerte globale détectée après le dernier check.");
                    }
                });
    }

    // Petite vérification rapide pour les alertes
    private void checkUserAlertsOnce() {
        db.collection("user_alerts")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("clientRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    updateNotificationBadge(!queryDocumentSnapshots.isEmpty());
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        startNotificationListener(); // Démarre l'écoute en temps réel
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (notificationListener != null) {
            notificationListener.remove(); // Arrête l'écoute quand l'app est en arrière-plan
        }
    }

    private void markNotificationsAsRead() {
        if (currentUserId.isEmpty()) return;

        // 1. Marquer les réservations comme lues
        db.collection("reservations")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("clientRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        db.collection("reservations").document(doc.getId()).update("clientRead", true);
                    }
                });

        // 2. Marquer les alertes utilisateurs comme lues
        db.collection("user_alerts")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("clientRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        db.collection("user_alerts").document(doc.getId()).update("clientRead", true);
                    }
                });

        // Note pour les alertes globales :
        // Si c'est global, vous devrez probablement gérer la lecture localement (SharedPreferences)
        // car on ne peut pas marquer "lu" pour tout le monde dans une collection partagée.
    }

}
