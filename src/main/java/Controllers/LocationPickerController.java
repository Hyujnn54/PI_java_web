package Controllers;

import Services.NominatimMapService;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

/**
 * Contrôleur pour la sélection de localisation sur une carte interactive
 * Utilise Nominatim (OpenStreetMap) - 100% GRATUIT
 */
public class LocationPickerController {

    private WebView webView;
    private WebEngine webEngine;
    private Stage stage;
    private NominatimMapService mapService;

    // Résultat de la sélection
    private Double selectedLatitude = null;
    private Double selectedLongitude = null;
    private String selectedAddress = null;

    // Callback pour retourner le résultat
    private LocationSelectedCallback callback;

    // Labels pour afficher les infos
    private Label lblCoordinates;
    private Label lblAddress;
    private TextField txtSearch;

    public LocationPickerController() {
        this.mapService = new NominatimMapService();
    }

    /**
     * Interface de callback pour la sélection de localisation
     */
    public interface LocationSelectedCallback {
        void onLocationSelected(double latitude, double longitude, String address);
    }

    /**
     * Affiche le dialogue de sélection de localisation
     */
    public void showLocationPicker(LocationSelectedCallback callback) {
        this.callback = callback;

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("📍 Sélectionner la localisation sur la carte");
        stage.setMinWidth(900);
        stage.setMinHeight(700);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f5f5f5;");

        // En-tête avec barre de recherche
        VBox header = createHeader();
        root.setTop(header);

        // Carte au centre
        StackPane mapContainer = createMapContainer();
        root.setCenter(mapContainer);

        // Panneau d'information en bas
        VBox infoPanel = createInfoPanel();
        root.setBottom(infoPanel);

        Scene scene = new Scene(root, 900, 700);
        stage.setScene(scene);

        // Charger la carte
        loadInteractiveMap();

        stage.show();
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setStyle("-fx-background-color: linear-gradient(to right, #2196F3, #1976D2); -fx-padding: 15 20;");

        Label title = new Label("📍 Cliquez sur la carte pour sélectionner la localisation");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: white;");

        // Barre de recherche
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("Rechercher une ville ou adresse...");
        txtSearch.setStyle("-fx-pref-width: 400; -fx-padding: 10; -fx-font-size: 14px; -fx-background-radius: 20;");
        txtSearch.setOnAction(e -> searchLocation(txtSearch.getText()));

        Button btnSearch = new Button("🔍 Rechercher");
        btnSearch.setStyle("-fx-background-color: white; -fx-text-fill: #1976D2; -fx-font-weight: 600; " +
                          "-fx-padding: 10 20; -fx-background-radius: 20; -fx-cursor: hand;");
        btnSearch.setOnAction(e -> searchLocation(txtSearch.getText()));

        Button btnMyLocation = new Button("📌 Tunisie");
        btnMyLocation.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: 600; " +
                              "-fx-padding: 10 20; -fx-background-radius: 20; -fx-cursor: hand;");
        btnMyLocation.setOnAction(e -> centerOnDefaultLocation());

        searchBox.getChildren().addAll(txtSearch, btnSearch, btnMyLocation);

        header.getChildren().addAll(title, searchBox);
        return header;
    }

    private StackPane createMapContainer() {
        StackPane container = new StackPane();
        container.setStyle("-fx-background-color: #e0e0e0;");

        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // Message de chargement
        Label loadingLabel = new Label("🗺️ Chargement de la carte...");
        loadingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");

        container.getChildren().addAll(loadingLabel, webView);

        return container;
    }

    private VBox createInfoPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color: white; -fx-padding: 15 20; " +
                      "-fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");

        // Informations sur la sélection
        HBox infoRow = new HBox(30);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        VBox coordsBox = new VBox(3);
        Label coordsTitle = new Label("Coordonnées:");
        coordsTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        lblCoordinates = new Label("Cliquez sur la carte...");
        lblCoordinates.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #333;");
        coordsBox.getChildren().addAll(coordsTitle, lblCoordinates);

        VBox addressBox = new VBox(3);
        Label addressTitle = new Label("Adresse:");
        addressTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        lblAddress = new Label("Aucune adresse sélectionnée");
        lblAddress.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #333;");
        lblAddress.setWrapText(true);
        lblAddress.setMaxWidth(500);
        addressBox.getChildren().addAll(addressTitle, lblAddress);
        HBox.setHgrow(addressBox, Priority.ALWAYS);

        infoRow.getChildren().addAll(coordsBox, addressBox);

        // Boutons d'action
        HBox buttonRow = new HBox(15);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #333; -fx-font-size: 14px; " +
                          "-fx-padding: 10 25; -fx-background-radius: 6; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> stage.close());

        Button btnConfirm = new Button("✓ Confirmer cette localisation");
        btnConfirm.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: 700; " +
                           "-fx-font-size: 14px; -fx-padding: 10 25; -fx-background-radius: 6; -fx-cursor: hand;");
        btnConfirm.setOnAction(e -> confirmSelection());

        buttonRow.getChildren().addAll(btnCancel, btnConfirm);

        panel.getChildren().addAll(infoRow, buttonRow);
        return panel;
    }

    private void loadInteractiveMap() {
        String html = generateInteractiveMapHTML();

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Créer un pont Java-JavaScript pour recevoir les clics
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaApp", new JavaBridge());
            }
        });

        webEngine.loadContent(html);
    }

    /**
     * Pont Java-JavaScript pour recevoir les événements de la carte
     */
    public class JavaBridge {
        public void onMapClick(double lat, double lng) {
            Platform.runLater(() -> {
                selectedLatitude = lat;
                selectedLongitude = lng;
                lblCoordinates.setText(String.format("%.6f, %.6f", lat, lng));
                lblAddress.setText("Recherche de l'adresse...");

                // Reverse geocoding pour obtenir l'adresse
                reverseGeocode(lat, lng);
            });
        }
    }

    private void reverseGeocode(double lat, double lng) {
        new Thread(() -> {
            try {
                NominatimMapService.GeoLocation location = mapService.reverseGeocode(lat, lng);

                Platform.runLater(() -> {
                    if (location != null) {
                        selectedAddress = location.getFullLocation();
                        lblAddress.setText(selectedAddress);
                    } else {
                        selectedAddress = String.format("%.4f, %.4f", lat, lng);
                        lblAddress.setText("Localisation: " + selectedAddress);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> lblAddress.setText("Erreur: " + e.getMessage()));
            }
        }).start();
    }

    private void searchLocation(String query) {
        if (query == null || query.trim().length() < 2) return;

        new Thread(() -> {
            try {
                var results = mapService.searchLocations(query);

                if (!results.isEmpty()) {
                    NominatimMapService.GeoLocation loc = results.get(0);

                    Platform.runLater(() -> {
                        selectedLatitude = loc.getLatitude();
                        selectedLongitude = loc.getLongitude();
                        selectedAddress = loc.getFullLocation();

                        lblCoordinates.setText(String.format("%.6f, %.6f", loc.getLatitude(), loc.getLongitude()));
                        lblAddress.setText(selectedAddress);

                        // Centrer la carte et placer un marqueur
                        webEngine.executeScript(String.format(
                            "setMarkerAndCenter(%f, %f);", loc.getLatitude(), loc.getLongitude()
                        ));
                    });
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Non trouvé");
                        alert.setContentText("Aucun résultat pour: " + query);
                        alert.show();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Erreur");
                    alert.setContentText("Erreur de recherche: " + e.getMessage());
                    alert.show();
                });
            }
        }).start();
    }


    private void centerOnDefaultLocation() {
        // Centrer sur Tunis par défaut
        webEngine.executeScript("setMarkerAndCenter(36.8065, 10.1815);");
        selectedLatitude = 36.8065;
        selectedLongitude = 10.1815;
        lblCoordinates.setText("36.806500, 10.181500");
        reverseGeocode(36.8065, 10.1815);
    }

    private void confirmSelection() {
        if (selectedLatitude == null || selectedLongitude == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Veuillez d'abord cliquer sur la carte pour sélectionner une localisation.");
            alert.showAndWait();
            return;
        }

        if (callback != null) {
            String address = selectedAddress != null ? selectedAddress :
                String.format("%.4f, %.4f", selectedLatitude, selectedLongitude);
            callback.onLocationSelected(selectedLatitude, selectedLongitude, address);
        }

        stage.close();
    }

    private String generateInteractiveMapHTML() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Sélection de localisation</title>
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { height: 100%; width: 100%; }
                    #map { height: 100%; width: 100%; cursor: crosshair; }
                    .custom-marker {
                        background: #f44336;
                        border: 3px solid white;
                        border-radius: 50%;
                        width: 20px;
                        height: 20px;
                        box-shadow: 0 2px 5px rgba(0,0,0,0.3);
                    }
                    .leaflet-popup-content {
                        font-family: 'Segoe UI', sans-serif;
                        font-size: 13px;
                    }
                    .click-instruction {
                        position: absolute;
                        top: 10px;
                        left: 50%;
                        transform: translateX(-50%);
                        background: rgba(33, 150, 243, 0.9);
                        color: white;
                        padding: 10px 20px;
                        border-radius: 20px;
                        font-family: 'Segoe UI', sans-serif;
                        font-size: 14px;
                        z-index: 1000;
                        pointer-events: none;
                    }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <div class="click-instruction">👆 Cliquez sur la carte pour sélectionner une localisation</div>
                
                <script>
                    // Initialiser la carte centrée sur la Tunisie
                    var map = L.map('map').setView([36.8065, 10.1815], 6);
                    
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);
                    
                    var currentMarker = null;
                    
                    // Icône personnalisée pour le marqueur
                    var redIcon = L.divIcon({
                        className: 'custom-marker',
                        iconSize: [20, 20],
                        iconAnchor: [10, 10]
                    });
                    
                    // Gestionnaire de clic sur la carte
                    map.on('click', function(e) {
                        var lat = e.latlng.lat;
                        var lng = e.latlng.lng;
                        
                        // Supprimer l'ancien marqueur
                        if (currentMarker) {
                            map.removeLayer(currentMarker);
                        }
                        
                        // Ajouter un nouveau marqueur
                        currentMarker = L.marker([lat, lng], {icon: redIcon}).addTo(map);
                        currentMarker.bindPopup('<b>Position sélectionnée</b><br>Lat: ' + lat.toFixed(6) + '<br>Lng: ' + lng.toFixed(6)).openPopup();
                        
                        // Envoyer les coordonnées à Java
                        if (window.javaApp) {
                            window.javaApp.onMapClick(lat, lng);
                        }
                    });
                    
                    // Fonction pour centrer la carte et placer un marqueur (appelée depuis Java)
                    function setMarkerAndCenter(lat, lng) {
                        map.setView([lat, lng], 14);
                        
                        if (currentMarker) {
                            map.removeLayer(currentMarker);
                        }
                        
                        currentMarker = L.marker([lat, lng], {icon: redIcon}).addTo(map);
                        currentMarker.bindPopup('<b>Position sélectionnée</b><br>Lat: ' + lat.toFixed(6) + '<br>Lng: ' + lng.toFixed(6)).openPopup();
                    }
                </script>
            </body>
            </html>
            """;
    }

    /**
     * Méthode statique pour ouvrir le sélecteur de localisation
     */
    public static void pickLocation(LocationSelectedCallback callback) {
        LocationPickerController picker = new LocationPickerController();
        picker.showLocationPicker(callback);
    }
}


