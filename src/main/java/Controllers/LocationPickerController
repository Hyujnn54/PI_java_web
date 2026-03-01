package Controllers;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class LocationPickerController {

    @FXML private WebView webView;
    @FXML private Button btnSelect;
    @FXML private Label lblHint;

    private Double selectedLat;
    private Double selectedLng;

    private java.util.function.BiConsumer<Double, Double> onPicked;

    // ✅ Move bridge to a field so it's not Garbage Collected
    private final JsBridge bridge = new JsBridge();

    public void setOnPicked(java.util.function.BiConsumer<Double, Double> onPicked) {
        this.onPicked = onPicked;
    }

    @FXML
    public void initialize() {
        btnSelect.setDisable(true);
        lblHint.setText("Loading map...");

        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");

                // ✅ Attach the persistent bridge object
                window.setMember("javaApp", bridge);

                // ✅ Signal to JS that bridge is ready
                engine.executeScript("setBridgeReady();");

                System.out.println("✅ Bridge attached + setBridgeReady called");

                Platform.runLater(() -> lblHint.setText("Click on the map to select a point"));
            }
        });

        engine.loadContent(buildHtml());
    }

    /**
     * Bridge class must be public for WebView to access its methods.
     */
    public class JsBridge {
        public void onMapClick(double lat, double lng) {
            selectedLat = lat;
            selectedLng = lng;

            System.out.println("✅ JS called Java: " + lat + ", " + lng);

            Platform.runLater(() -> {
                btnSelect.setDisable(false);
                btnSelect.setText("Select ✅");
                lblHint.setText(String.format("Selected: %.6f, %.6f", lat, lng));
            });
        }
    }

    @FXML
    private void handleSelect() {
        if (selectedLat == null || selectedLng == null) {
            lblHint.setText("Pick a point first.");
            return;
        }

        System.out.println("✅ SELECT pressed: " + selectedLat + ", " + selectedLng);

        if (onPicked != null) {
            onPicked.accept(selectedLat, selectedLng);
            // Close the window after selection
            webView.getScene().getWindow().hide();
        } else {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(String.format("%.6f, %.6f", selectedLat, selectedLng));
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            lblHint.setText("Copied to clipboard ✅");
        }
    }

    @FXML
    private void handleCancel() {
        webView.getScene().getWindow().hide();
    }

    private String buildHtml() {
        return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>
    html, body { height: 100%; margin: 0; padding: 0; overflow: hidden; }
    #map { height: 100%; width: 100%; }
  </style>
</head>
<body>
  <div id="map"></div>

<script>
  var map;
  var marker = null;
  var clickBound = false;

  function initMap() {
    // Initial view of Tunisia
    map = L.map('map').setView([36.8065, 10.1815], 6);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap'
    }).addTo(map);
  }

  // Called by Java when the bridge is ready
  function setBridgeReady() {
    if (map && !clickBound) {
      clickBound = true;
      map.on('click', function(e) {
        var lat = e.latlng.lat;
        var lng = e.latlng.lng;

        if (marker) map.removeLayer(marker);
        marker = L.marker([lat, lng]).addTo(map);

        // Call the Java bridge
        if (window.javaApp && window.javaApp.onMapClick) {
            window.javaApp.onMapClick(lat, lng);
        } else {
            console.error("Java bridge not found!");
        }
      });
    }
  }

  initMap();
</script>
</body>
</html>
""";
    }
}