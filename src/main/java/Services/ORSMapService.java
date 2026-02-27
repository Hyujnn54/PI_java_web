package Services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Service de géolocalisation et directions utilisant l'API OpenRouteService
 * Avec support pour les directions (distance/durée réelles) et carte Leaflet
 */
public class ORSMapService {

    private static String API_KEY;
    private static String GEOCODE_URL;
    private static String AUTOCOMPLETE_URL;
    private static String DIRECTIONS_URL;

    static {
        loadConfiguration();
    }

    /**
     * Charge la configuration depuis config.properties
     */
    private static void loadConfiguration() {
        try (InputStream input = ORSMapService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                API_KEY = props.getProperty("ors.api.key");
                GEOCODE_URL = props.getProperty("ors.geocode.url", "https://api.openrouteservice.org/geocode/search");
                AUTOCOMPLETE_URL = props.getProperty("ors.autocomplete.url", "https://api.openrouteservice.org/geocode/autocomplete");
                DIRECTIONS_URL = props.getProperty("ors.directions.url", "https://api.openrouteservice.org/v2/directions/driving-car");
                System.out.println("✓ Configuration ORS chargée depuis config.properties");
            } else {
                // Fallback avec la clé par défaut
                API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImQ2ODk2YmExMTI4NTQ1NGZhMmNjNjYwODc0MDU4MjlmIiwiaCI6Im11cm11cjY0In0=";
                GEOCODE_URL = "https://api.openrouteservice.org/geocode/search";
                AUTOCOMPLETE_URL = "https://api.openrouteservice.org/geocode/autocomplete";
                DIRECTIONS_URL = "https://api.openrouteservice.org/v2/directions/driving-car";
                System.out.println("⚠ config.properties non trouvé, utilisation des valeurs par défaut");
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement config: " + e.getMessage());
            // Valeurs par défaut
            API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImQ2ODk2YmExMTI4NTQ1NGZhMmNjNjYwODc0MDU4MjlmIiwiaCI6Im11cm11cjY0In0=";
            GEOCODE_URL = "https://api.openrouteservice.org/geocode/search";
            AUTOCOMPLETE_URL = "https://api.openrouteservice.org/geocode/autocomplete";
            DIRECTIONS_URL = "https://api.openrouteservice.org/v2/directions/driving-car";
        }
    }

    /**
     * Représente un résultat de géocodage
     */
    public static class GeoLocation {
        private String displayName;
        private String city;
        private String country;
        private double latitude;
        private double longitude;

        public GeoLocation(String displayName, String city, String country, double latitude, double longitude) {
            this.displayName = displayName;
            this.city = city;
            this.country = country;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getDisplayName() { return displayName; }
        public String getCity() { return city; }
        public String getCountry() { return country; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }

        public String getFullLocation() {
            if (city != null && country != null) {
                return city + ", " + country;
            }
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Représente le résultat d'un calcul de directions
     */
    public static class DirectionsResult {
        private double distanceKm;
        private int durationMinutes;
        private String summary;
        private List<double[]> routeCoordinates; // Pour tracer la route sur la carte

        public DirectionsResult(double distanceKm, int durationMinutes) {
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
            this.routeCoordinates = new ArrayList<>();
        }

        public double getDistanceKm() { return distanceKm; }
        public int getDurationMinutes() { return durationMinutes; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public List<double[]> getRouteCoordinates() { return routeCoordinates; }
        public void setRouteCoordinates(List<double[]> coords) { this.routeCoordinates = coords; }

        public String getFormattedDistance() {
            if (distanceKm < 1) {
                return String.format("%.0f m", distanceKm * 1000);
            }
            return String.format("%.1f km", distanceKm);
        }

        public String getFormattedDuration() {
            if (durationMinutes < 60) {
                return durationMinutes + " min";
            }
            int hours = durationMinutes / 60;
            int mins = durationMinutes % 60;
            return hours + "h" + (mins > 0 ? String.format("%02d", mins) : "");
        }
    }

    /**
     * Géocode une adresse pour obtenir les coordonnées
     */
    public GeoLocation geocode(String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8.toString());
            String urlString = GEOCODE_URL + "?api_key=" + API_KEY + "&text=" + encodedAddress + "&size=1";

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }

                List<GeoLocation> results = parseGeoJsonResponse(response.toString());
                if (!results.isEmpty()) {
                    return results.get(0);
                }
            } else {
                System.err.println("Erreur geocode HTTP " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Erreur geocode: " + e.getMessage());
        }
        return null;
    }

    /**
     * Recherche des suggestions d'adresses avec autocomplete
     */
    public List<GeoLocation> autocomplete(String query) {
        List<GeoLocation> results = new ArrayList<>();
        if (query == null || query.trim().length() < 2) {
            return results;
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String urlString = AUTOCOMPLETE_URL + "?api_key=" + API_KEY +
                              "&text=" + encodedQuery +
                              "&boundary.country=FR,TN,BE,CH,CA" +
                              "&layers=locality,county,region" +
                              "&size=5";

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", API_KEY);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
                results = parseGeoJsonResponse(response.toString());
            }
        } catch (Exception e) {
            System.err.println("Erreur autocomplete: " + e.getMessage());
        }
        return results;
    }

    /**
     * Calcule la route entre deux points via l'API ORS Directions
     * @param startLon Longitude du point de départ
     * @param startLat Latitude du point de départ
     * @param endLon Longitude du point d'arrivée
     * @param endLat Latitude du point d'arrivée
     * @return DirectionsResult avec distance et durée réelles, ou null en cas d'erreur
     */
    public DirectionsResult getDirections(double startLon, double startLat, double endLon, double endLat) {
        try {
            URL url = new URL(DIRECTIONS_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json, application/geo+json");
            connection.setRequestProperty("Authorization", API_KEY);
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            // Corps de la requête JSON - format ORS: [longitude, latitude]
            String jsonBody = String.format(
                "{\"coordinates\":[[%f,%f],[%f,%f]],\"geometry\":true}",
                startLon, startLat, endLon, endLat
            );

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
                return parseDirectionsResponse(response.toString());
            } else {
                // Lire l'erreur
                StringBuilder error = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        error.append(line);
                    }
                }
                System.err.println("Erreur directions HTTP " + responseCode + ": " + error);
            }
        } catch (Exception e) {
            System.err.println("Erreur directions: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Parse la réponse de l'API Directions
     */
    private DirectionsResult parseDirectionsResponse(String json) {
        try {
            // Extraire la distance (en mètres)
            double distanceMeters = extractJsonNumber(json, "distance");
            // Extraire la durée (en secondes)
            double durationSeconds = extractJsonNumber(json, "duration");

            double distanceKm = distanceMeters / 1000.0;
            int durationMinutes = (int) Math.round(durationSeconds / 60.0);

            DirectionsResult result = new DirectionsResult(distanceKm, durationMinutes);

            // Extraire le résumé si disponible
            String summary = extractJsonString(json, "summary");
            if (summary != null) {
                result.setSummary(summary);
            }

            // Extraire les coordonnées de la route pour l'affichage
            List<double[]> routeCoords = extractRouteCoordinates(json);
            result.setRouteCoordinates(routeCoords);

            return result;
        } catch (Exception e) {
            System.err.println("Erreur parsing directions: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extrait les coordonnées de la route depuis la réponse GeoJSON
     */
    private List<double[]> extractRouteCoordinates(String json) {
        List<double[]> coordinates = new ArrayList<>();
        try {
            // Chercher l'array de coordinates dans la géométrie
            int geomStart = json.indexOf("\"geometry\"");
            if (geomStart == -1) return coordinates;

            int coordStart = json.indexOf("\"coordinates\":", geomStart);
            if (coordStart == -1) return coordinates;

            int arrayStart = json.indexOf("[[", coordStart);
            int arrayEnd = json.indexOf("]]", arrayStart);

            if (arrayStart == -1 || arrayEnd == -1) return coordinates;

            String coordsStr = json.substring(arrayStart + 1, arrayEnd + 1);

            // Parser les paires de coordonnées
            int pos = 0;
            while (pos < coordsStr.length()) {
                int pairStart = coordsStr.indexOf("[", pos);
                if (pairStart == -1) break;

                int pairEnd = coordsStr.indexOf("]", pairStart);
                if (pairEnd == -1) break;

                String pair = coordsStr.substring(pairStart + 1, pairEnd);
                String[] parts = pair.split(",");
                if (parts.length >= 2) {
                    double lon = Double.parseDouble(parts[0].trim());
                    double lat = Double.parseDouble(parts[1].trim());
                    coordinates.add(new double[]{lat, lon}); // Leaflet utilise [lat, lon]
                }

                pos = pairEnd + 1;
            }
        } catch (Exception e) {
            System.err.println("Erreur extraction coordonnées route: " + e.getMessage());
        }
        return coordinates;
    }

    /**
     * Génère le HTML pour afficher une carte Leaflet avec marker et route optionnelle
     */
    public static String generateMapHTML(double lat, double lon, String locationName,
                                         Double candidateLat, Double candidateLon,
                                         DirectionsResult directions) {
        StringBuilder routeJS = new StringBuilder();

        // Si on a des coordonnées de route, les ajouter
        if (directions != null && !directions.getRouteCoordinates().isEmpty()) {
            routeJS.append("var routeCoords = [");
            List<double[]> coords = directions.getRouteCoordinates();
            for (int i = 0; i < coords.size(); i++) {
                double[] coord = coords.get(i);
                routeJS.append(String.format("[%f, %f]", coord[0], coord[1]));
                if (i < coords.size() - 1) routeJS.append(",");
            }
            routeJS.append("];\n");
            routeJS.append("var routeLine = L.polyline(routeCoords, {color: '#3388ff', weight: 4, opacity: 0.8}).addTo(map);\n");
            routeJS.append("map.fitBounds(routeLine.getBounds(), {padding: [50, 50]});\n");
        }

        // Ajouter marker pour le candidat si disponible
        String candidateMarkerJS = "";
        if (candidateLat != null && candidateLon != null) {
            candidateMarkerJS = String.format(
                "var candidateIcon = L.divIcon({className: 'candidate-marker', html: '🏠', iconSize: [30, 30], iconAnchor: [15, 30]});\n" +
                "L.marker([%f, %f], {icon: candidateIcon}).addTo(map).bindPopup('<b>Votre position</b>');\n",
                candidateLat, candidateLon
            );
        }

        // Info de distance si disponible
        String distanceInfo = "";
        if (directions != null) {
            distanceInfo = String.format(
                "<div style='position:absolute;bottom:20px;left:20px;background:white;padding:12px 18px;" +
                "border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,0.15);z-index:1000;'>" +
                "<span style=\"font-size:14px;color:#333;\">🚗 <b>%s</b> • ⏱️ <b>%s</b></span></div>",
                directions.getFormattedDistance(), directions.getFormattedDuration()
            );
        }

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Localisation - %s</title>
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { height: 100%%; width: 100%%; }
                    #map { height: 100%%; width: 100%%; }
                    .company-marker { font-size: 24px; text-align: center; }
                    .candidate-marker { font-size: 20px; text-align: center; }
                    .leaflet-popup-content { font-family: 'Segoe UI', sans-serif; }
                    .popup-title { font-weight: 700; font-size: 14px; color: #2c3e50; margin-bottom: 5px; }
                    .popup-address { font-size: 12px; color: #666; }
                    .popup-coords { font-size: 10px; color: #999; margin-top: 5px; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                %s
                <script>
                    var map = L.map('map').setView([%f, %f], 14);
                    
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);
                    
                    // Marker de l'entreprise
                    var companyIcon = L.divIcon({
                        className: 'company-marker',
                        html: '🏢',
                        iconSize: [30, 30],
                        iconAnchor: [15, 30]
                    });
                    
                    var marker = L.marker([%f, %f], {icon: companyIcon}).addTo(map);
                    marker.bindPopup(
                        '<div class="popup-title">%s</div>' +
                        '<div class="popup-coords">📍 %f, %f</div>'
                    ).openPopup();
                    
                    // Marker du candidat
                    %s
                    
                    // Route
                    %s
                </script>
            </body>
            </html>
            """,
            escapeHtml(locationName),
            distanceInfo,
            lat, lon,
            lat, lon,
            escapeHtml(locationName),
            lat, lon,
            candidateMarkerJS,
            routeJS.toString()
        );
    }

    /**
     * Génère une carte simple sans route
     */
    public static String generateSimpleMapHTML(double lat, double lon, String locationName) {
        return generateMapHTML(lat, lon, locationName, null, null, null);
    }

    // =============== MÉTHODES UTILITAIRES ===============

    private List<GeoLocation> parseGeoJsonResponse(String json) {
        List<GeoLocation> results = new ArrayList<>();
        try {
            int featuresStart = json.indexOf("\"features\":");
            if (featuresStart == -1) return results;

            int arrayStart = json.indexOf("[", featuresStart);
            int arrayEnd = findMatchingBracket(json, arrayStart);

            if (arrayStart == -1 || arrayEnd == -1) return results;

            String featuresArray = json.substring(arrayStart, arrayEnd + 1);

            int featureStart = 0;
            while ((featureStart = featuresArray.indexOf("{\"type\":\"Feature\"", featureStart)) != -1) {
                int featureEnd = findMatchingBrace(featuresArray, featureStart);
                if (featureEnd == -1) break;

                String feature = featuresArray.substring(featureStart, featureEnd + 1);
                GeoLocation location = parseFeature(feature);
                if (location != null) {
                    results.add(location);
                }

                featureStart = featureEnd + 1;
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing GeoJSON: " + e.getMessage());
        }
        return results;
    }

    private GeoLocation parseFeature(String feature) {
        try {
            int coordStart = feature.indexOf("\"coordinates\":");
            if (coordStart == -1) return null;

            int coordArrayStart = feature.indexOf("[", coordStart);
            int coordArrayEnd = feature.indexOf("]", coordArrayStart);

            String coordStr = feature.substring(coordArrayStart + 1, coordArrayEnd);
            String[] coords = coordStr.split(",");

            double longitude = Double.parseDouble(coords[0].trim());
            double latitude = Double.parseDouble(coords[1].trim());

            String label = extractJsonString(feature, "label");
            String city = extractJsonString(feature, "locality");
            if (city == null) {
                city = extractJsonString(feature, "name");
            }
            String country = extractJsonString(feature, "country");

            if (label == null) {
                label = city != null ? city : "Unknown";
            }

            return new GeoLocation(label, city, country, latitude, longitude);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int valueStart = json.indexOf("\"", keyIndex + searchKey.length());
        if (valueStart == -1) return null;

        int valueEnd = json.indexOf("\"", valueStart + 1);
        if (valueEnd == -1) return null;

        return json.substring(valueStart + 1, valueEnd);
    }

    private double extractJsonNumber(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return 0;

        int valueStart = keyIndex + searchKey.length();
        // Sauter les espaces
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        int valueEnd = valueStart;
        while (valueEnd < json.length()) {
            char c = json.charAt(valueEnd);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
            valueEnd++;
        }

        try {
            return Double.parseDouble(json.substring(valueStart, valueEnd));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int findMatchingBracket(String str, int start) {
        if (start < 0 || str.charAt(start) != '[') return -1;
        int count = 1;
        for (int i = start + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '[') count++;
            else if (c == ']') count--;
            if (count == 0) return i;
        }
        return -1;
    }

    private int findMatchingBrace(String str, int start) {
        if (start < 0 || str.charAt(start) != '{') return -1;
        int count = 1;
        for (int i = start + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '{') count++;
            else if (c == '}') count--;
            if (count == 0) return i;
        }
        return -1;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * Calcule la distance à vol d'oiseau (Haversine) - utilisé en fallback
     */
    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}


