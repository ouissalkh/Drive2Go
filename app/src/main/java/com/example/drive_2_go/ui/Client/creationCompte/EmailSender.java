package com.example.drive_2_go.ui.Client.creationCompte;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailSender {

    private static final String TAG = "EmailSender";

    //  BREVO API Configuration
   private static final String BREVO_API_KEY = ""; //Ajouter ici api
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String FROM_EMAIL = "drive2go.verify@gmail.com";
    private static final String FROM_NAME = "Drive2Go";

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface EmailCallback {
        void onSuccess();
        void onFailure(String error);
    }

    /**
     * Envoie un code de vérification par email
     */
    public void sendVerificationCode(String toEmail, String code, String userName, EmailCallback callback) {

        String subject = "Code de vérification Drive2Go";

        String htmlContent = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }" +
                ".container { background-color: white; max-width: 600px; margin: 0 auto; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                ".header { text-align: center; color: #2E7D32; }" +
                ".code-box { background-color: #E8F5E9; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; color: #2E7D32; border-radius: 8px; margin: 20px 0; letter-spacing: 5px; }" +
                ".footer { text-align: center; color: #666; font-size: 12px; margin-top: 30px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<h1 class='header'>🚗 Drive2Go</h1>" +
                "<h2>Bonjour " + userName + ",</h2>" +
                "<p>Merci de vous être inscrit sur <strong>Drive2Go</strong> !</p>" +
                "<p>Pour activer votre compte, veuillez utiliser le code de vérification ci-dessous :</p>" +
                "<div class='code-box'>" + code + "</div>" +
                "<p>Ce code est valable pendant <strong>15 minutes</strong>.</p>" +
                "<p>Si vous n'avez pas créé de compte, ignorez cet email.</p>" +
                "<div class='footer'>" +
                "<p>© 2025 Drive2Go - Location de voitures</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendEmail(toEmail, subject, htmlContent, callback);
    }

    /**
     * Méthode générique pour envoyer un email via Brevo
     */
    private void sendEmail(String toEmail, String subject, String htmlContent, EmailCallback callback) {

        executor.execute(() -> {
            try {
                // Créer la connexion HTTP
                URL url = new URL(BREVO_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("accept", "application/json");
                conn.setRequestProperty("api-key", BREVO_API_KEY);
                conn.setRequestProperty("content-type", "application/json");
                conn.setDoOutput(true);

                // Créer le JSON
                JSONObject json = new JSONObject();

                // Expéditeur
                JSONObject sender = new JSONObject();
                sender.put("name", FROM_NAME);
                sender.put("email", FROM_EMAIL);
                json.put("sender", sender);

                // Destinataire
                JSONObject recipient = new JSONObject();
                recipient.put("email", toEmail);
                org.json.JSONArray toArray = new org.json.JSONArray();
                toArray.put(recipient);
                json.put("to", toArray);

                // Sujet et contenu
                json.put("subject", subject);
                json.put("htmlContent", htmlContent);

                // Envoyer la requête
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(input, 0, input.length);
                os.close();

                // Lire la réponse
                int responseCode = conn.getResponseCode();

                if (responseCode == 201 || responseCode == 200) {
                    Log.d(TAG, "✅ Email envoyé avec succès à " + toEmail);
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    String error = "Code HTTP: " + responseCode;
                    Log.e(TAG, "❌ Erreur d'envoi : " + error);
                    mainHandler.post(() -> callback.onFailure(error));
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception : " + e.getMessage(), e);
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }
}