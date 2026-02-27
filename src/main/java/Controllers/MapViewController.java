package Controllers;

import Services.ORSMapService;
import Services.ORSMapService.DirectionsResult;
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

/**
 * Contrôleur pour afficher une carte avec la localisation d'une offre d'emploi
 * Utilise Leaflet dans un WebView JavaFX avec intégration OpenRouteService
 */
public class MapViewController {

    private ORSMapService orsService;
    private WebView webView;
    private WebEngine webEngine;

    // Position du candidat (optionnelle)
    private Double candidateLatitude;
    private Double candidateLongitude;

    public MapViewController() {
        this.orsService = new ORSMapService();
    }

    /**
     * Définit la position du candidat pour le calcul de distance
     */
    public void setCandidatePosition(double latitude, double longitude) {
        this.candidateLatitude = latitude;
        this.candidateLongitude = longitude;
    }

    /**
     * Affiche une fenêtre modale avec la carte de l'entreprise
     * @param companyLat Latitude de l'entreprise
     * @param companyLon Longitude de l'entreprise
     * @param locationName Nom du lieu (ex: "Paris, France")
     * @param jobTitle Titre de l'offre d'emploi
     */
    public void showMapDialog(double companyLat, double companyLon, String locationName, String jobTitle) {
        Stage mapStage = new Stage();
        mapStage.initModality(Modality.APPLICATION_MODAL);
        mapStage.setTitle("📍 Localisation - " + (jobTitle != null ? jobTitle : locationName));
        mapStage.setMinWidth(800);
        mapStage.setMinHeight(600);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f5f5f5;");

        // === EN-TÊTE ===
        VBox header = createHeader(locationName, companyLat, companyLon);
        root.setTop(header);

        // === ZONE CENTRALE: CARTE + INFO DISTANCE ===
        VBox centerContent = new VBox(0);

        // Info distance (si candidat positionné)
        HBox distanceBar = createDistanceBar(companyLat, companyLon);
        if (distanceBar != null) {
            centerContent.getChildren().add(distanceBar);
        }

        // WebView avec la carte Leaflet
        StackPane mapContainer = createMapContainer(companyLat, companyLon, locationName);
        VBox.setVgrow(mapContainer, Priority.ALWAYS);
        centerContent.getChildren().add(mapContainer);

        root.setCenter(centerContent);

        // === PIED DE PAGE ===
        HBox footer = createFooter(mapStage, companyLat, companyLon, locationName);
        root.setBottom(footer);

        Scene scene = new Scene(root, 900, 650);
        mapStage.setScene(scene);
        mapStage.show();

        // Charger la carte
        loadMap(companyLat, companyLon, locationName);
    }

    private VBox createHeader(String locationName, double lat, double lon) {
        VBox header = new VBox(8);
        header.setStyle("-fx-background-color: linear-gradient(to right, #2196F3, #1976D2); -fx-padding: 20;");
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("🏢 " + (locationName != null ? locationName : "Localisation de l'entreprise"));
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: white;");

        Label coords = new Label(String.format("📍 Coordonnées: %.6f, %.6f", lat, lon));
        coords.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.85);");

        header.getChildren().addAll(title, coords);
        return header;
    }

    private HBox createDistanceBar(double companyLat, double companyLon) {
        if (candidateLatitude == null || candidateLongitude == null) {
            return null;
        }

        HBox bar = new HBox(20);
        bar.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 15 20;");
        bar.setAlignment(Pos.CENTER_LEFT);

        // Calcul de la distance et durée via API ORS Directions
        Label loadingLabel = new Label("⏳ Calcul de l'itinéraire en cours...");
        loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2e7d32;");
        bar.getChildren().add(loadingLabel);

        // Lancer le calcul en arrière-plan
        new Thread(() -> {
            DirectionsResult directions = orsService.getDirections(
                candidateLongitude, candidateLatitude,  // Départ: candidat [lon, lat]
                companyLon, companyLat                   // Arrivée: entreprise [lon, lat]
            );

            Platform.runLater(() -> {
                bar.getChildren().clear();

                if (directions != null) {
                    Label distanceLabel = new Label("🚗 Distance: " + directions.getFormattedDistance());
                    distanceLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #2e7d32;");

                    Label durationLabel = new Label("⏱️ Durée: " + directions.getFormattedDuration());
                    durationLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #1565c0;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label infoLabel = new Label("(en voiture)");
                    infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

                    bar.getChildren().addAll(distanceLabel, durationLabel, spacer, infoLabel);

                    // Mettre à jour la carte avec la route
                    updateMapWithRoute(companyLat, companyLon, directions);
                } else {
                    // Fallback: distance à vol d'oiseau
                    double distance = ORSMapService.calculateHaversineDistance(
                        candidateLatitude, candidateLongitude, companyLat, companyLon
                    );
                    int estimatedTime = (int) Math.round(distance / 50.0 * 60); // ~50 km/h moyenne

                    Label distanceLabel = new Label("📍 Distance: " + String.format("%.1f km", distance) + " (à vol d'oiseau)");
                    distanceLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #ff9800;");

                    Label durationLabel = new Label("⏱️ Durée estimée: ~" + estimatedTime + " min");
                    durationLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #666;");

                    bar.getChildren().addAll(distanceLabel, durationLabel);
                }
            });
        }).start();

        return bar;
    }

    private StackPane createMapContainer(double lat, double lon, String locationName) {
        StackPane container = new StackPane();
        container.setStyle("-fx-background-color: #e0e0e0;");

        webView = new WebView();
        webEngine = webView.getEngine();

        // Activer JavaScript
        webEngine.setJavaScriptEnabled(true);

        // Gérer les erreurs
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.FAILED) {
                System.err.println("Erreur chargement carte: " + webEngine.getLoadWorker().getException());
            }
        });

        // Message de chargement
        Label loadingLabel = new Label("🗺️ Chargement de la carte...");
        loadingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");

        container.getChildren().addAll(loadingLabel, webView);

        return container;
    }

    private void loadMap(double lat, double lon, String locationName) {
        String html;

        if (candidateLatitude != null && candidateLongitude != null) {
            // Carte avec position candidat (route sera ajoutée après)
            html = ORSMapService.generateMapHTML(lat, lon, locationName, candidateLatitude, candidateLongitude, null);
        } else {
            // Carte simple
            html = ORSMapService.generateSimpleMapHTML(lat, lon, locationName);
        }

        webEngine.loadContent(html);
    }

    private void updateMapWithRoute(double companyLat, double companyLon, DirectionsResult directions) {
        if (directions == null || webEngine == null) return;

        // Recharger la carte avec la route
        String html = ORSMapService.generateMapHTML(
            companyLat, companyLon,
            "Entreprise",
            candidateLatitude, candidateLongitude,
            directions
        );

        Platform.runLater(() -> webEngine.loadContent(html));
    }

    private HBox createFooter(Stage stage, double lat, double lon, String locationName) {
        HBox footer = new HBox(15);
        footer.setStyle("-fx-background-color: white; -fx-padding: 15 20; " +
                       "-fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");
        footer.setAlignment(Pos.CENTER_RIGHT);

        // Bouton ouvrir dans le navigateur
        Button btnBrowser = new Button("🌐 Ouvrir dans le navigateur");
        btnBrowser.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #333; -fx-font-size: 13px; " +
                          "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand; " +
                          "-fx-border-color: #ddd; -fx-border-radius: 6;");
        btnBrowser.setOnAction(e -> openInBrowser(lat, lon));

        // Bouton Google Maps
        Button btnGoogleMaps = new Button("🗺️ Google Maps");
        btnGoogleMaps.setStyle("-fx-background-color: #4285f4; -fx-text-fill: white; -fx-font-size: 13px; " +
                              "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnGoogleMaps.setOnAction(e -> openInGoogleMaps(lat, lon));

        // Bouton itinéraire (si candidat positionné)
        if (candidateLatitude != null && candidateLongitude != null) {
            Button btnItinerary = new Button("🚗 Itinéraire");
            btnItinerary.setStyle("-fx-background-color: #34a853; -fx-text-fill: white; -fx-font-size: 13px; " +
                                 "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
            btnItinerary.setOnAction(e -> openItinerary(lat, lon));
            footer.getChildren().add(btnItinerary);
        }

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton fermer
        Button btnClose = new Button("Fermer");
        btnClose.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #333; -fx-font-size: 13px; " +
                         "-fx-padding: 10 25; -fx-background-radius: 6; -fx-cursor: hand;");
        btnClose.setOnAction(e -> stage.close());

        footer.getChildren().addAll(btnBrowser, btnGoogleMaps, spacer, btnClose);
        return footer;
    }

    private void openInBrowser(double lat, double lon) {
        try {
            String url = String.format("https://www.openstreetmap.org/?mlat=%f&mlon=%f#map=15/%f/%f",
                                      lat, lon, lat, lon);
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            showError("Impossible d'ouvrir le navigateur: " + e.getMessage());
        }
    }

    private void openInGoogleMaps(double lat, double lon) {
        try {
            String url = String.format("https://www.google.com/maps?q=%f,%f", lat, lon);
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            showError("Impossible d'ouvrir Google Maps: " + e.getMessage());
        }
    }

    private void openItinerary(double destLat, double destLon) {
        if (candidateLatitude == null || candidateLongitude == null) {
            showError("Position du candidat non définie");
            return;
        }

        try {
            String url = String.format(
                "https://www.google.com/maps/dir/%f,%f/%f,%f",
                candidateLatitude, candidateLongitude, destLat, destLon
            );
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            showError("Impossible d'ouvrir l'itinéraire: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Méthode statique pour afficher rapidement une carte
     */
    public static void showMap(double lat, double lon, String locationName, String jobTitle) {
        MapViewController controller = new MapViewController();
        controller.showMapDialog(lat, lon, locationName, jobTitle);
    }

    /**
     * Méthode statique pour afficher une carte avec calcul de distance
     */
    public static void showMapWithDistance(double companyLat, double companyLon, String locationName,
                                           String jobTitle, double candidateLat, double candidateLon) {
        MapViewController controller = new MapViewController();
        controller.setCandidatePosition(candidateLat, candidateLon);
        controller.showMapDialog(companyLat, companyLon, locationName, jobTitle);
    }
}


