package com.example.drive_2_go.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageUtils {

    private static final String TAG = "ImageUtils";
    private static final String CAR_IMAGES_DIR = "car_images"; // Dossier privé

    /**
     * Copie l'image depuis l'URI (Galerie) vers le stockage interne privé de l'application
     * Retourne le chemin absolu du fichier créé.
     */
    public static String copyImageToInternalStorage(Context context, Uri sourceUri, String fileName) {
        try {
            // 1. Récupère ou crée le dossier "car_images" dans les fichiers privés de l'app
            File directory = new File(context.getFilesDir(), CAR_IMAGES_DIR);
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    Log.e(TAG, "Impossible de créer le dossier d'images");
                    return null;
                }
            }

            // 2. Crée le fichier de destination vide
            File destinationFile = new File(directory, fileName + ".jpg");

            // 3. Copie le contenu du flux d'entrée (source) vers le flux de sortie (destination)
            copyFile(context, sourceUri, destinationFile);

            // 4. Retourne le chemin complet (ex: /data/user/0/com.app/files/car_images/car_123.jpg)
            return destinationFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la sauvegarde de l'image", e);
            return null;
        }
    }

    /**
     * Crée un nom de fichier unique basé sur le timestamp actuel
     */
    public static String createUniqueFileName() {
        return "car_" + System.currentTimeMillis();
    }

    /**
     * Supprime physiquement l'image du stockage interne
     */
    public static boolean deleteImage(String imagePath) {
        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    return imageFile.delete();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur suppression image", e);
        }
        return false;
    }

    // Méthode interne pour gérer les flux (Streams)
    private static void copyFile(Context context, Uri sourceUri, File destinationFile) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = context.getContentResolver().openInputStream(sourceUri);
            outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[4096]; // Tampon de 4Ko
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } finally {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
        }
    }
}