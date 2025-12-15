package com.example.drive_2_go.ui.Admin.addeditCar; // Vérifiez que c'est bien votre package

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Configuration de Cloudinary
       /* Map config = new HashMap();
        config.put("cloud_name", "datr9fmfp");
        config.put("api_key", "953344295627375");
        config.put("api_secret", "jPnIjBzEtR8Z2H6jLVbwNqCrhjc");
        // config.put("secure", true); // Optionnel, pour forcer HTTPS

        try {
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            // Déjà initialisé, on ignore
        }
    } */
}