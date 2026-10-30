package controllers;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class MapViewController implements Initializable {

    @FXML
    private WebView webView;
    @FXML
    private Label statusLabel;
    @FXML
    private Button confirmBtn;

    private WebEngine webEngine;
    private String selectedAddress;
    private Consumer<String> onLocationConfirmed;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        webEngine = webView.getEngine();
        
        // Handle JS to Java bridge
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", new JavaConnector());
            }
        });

        // Load map.html from resources
        URL mapUrl = getClass().getResource("/GUI/map.html");
        if (mapUrl != null) {
            webEngine.load(mapUrl.toExternalForm());
        } else {
            statusLabel.setText("Erreur : Fichier map.html introuvable.");
        }
    }

    public void setOnLocationConfirmed(Consumer<String> callback) {
        this.onLocationConfirmed = callback;
    }

    @FXML
    private void handleConfirm() {
        if (selectedAddress != null && onLocationConfirmed != null) {
            onLocationConfirmed.accept(selectedAddress);
            handleCancel();
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) webView.getScene().getWindow();
        stage.close();
    }

    // Bridge class for JavaScript calls
    public class JavaConnector {
        public void onLocationSelected(double lat, double lng) {
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("Recherche de l'adresse...");
                confirmBtn.setDisable(true);
                
                // Perform reverse geocoding in a separate thread
                new Thread(() -> {
                    String address = reverseGeocode(lat, lng);
                    javafx.application.Platform.runLater(() -> {
                        selectedAddress = address;
                        statusLabel.setText(address != null ? address : "Adresse non trouvée.");
                        confirmBtn.setDisable(address == null);
                    });
                }).start();
            });
        }
    }

    private String reverseGeocode(double lat, double lng) {
        try {
            // Using Locale.US to ensure dots for decimals, matching Nominatim requirements
            String urlStr = String.format(Locale.US, "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&addressdetails=1", lat, lng);
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "TalentBridge-App/1.0 (rayanbenamor207@gmail.com)");

            if (conn.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Simple JSON parsing to extract display_name
                String json = response.toString();
                int start = json.indexOf("\"display_name\":\"") + 16;
                int end = json.indexOf("\"", start);
                if (start > 15 && end > start) {
                    return json.substring(start, end).replace("\\u00e9", "é").replace("\\u00e0", "à");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
