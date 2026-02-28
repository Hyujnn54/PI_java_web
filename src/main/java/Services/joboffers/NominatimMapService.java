package Services.joboffers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de géolocalisation utilisant Nominatim (OpenStreetMap) - 100% GRATUIT
 * Pas de clé API requise
 */
public class NominatimMapService {

    private static final String NOMINATIM_SEARCH_URL = "https://nominatim.openstreetmap.org/search";
    private static final String NOMINATIM_REVERSE_URL = "https://nominatim.openstreetmap.org/reverse";
    private static final String USER_AGENT = "TalentBridgeApp/1.0";

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
            if (city != null && !city.isEmpty()) {
                return country != null && !country.isEmpty() ? city + ", " + country : city;
            }
            return displayName;
        }

        @Override
        public String toString() {
            return getFullLocation();
        }
    }

    /**
     * Recherche des lieux par nom (autocomplete)
     */
    public List<GeoLocation> searchLocations(String query) {
        List<GeoLocation> results = new ArrayList<>();
        if (query == null || query.trim().length() < 2) return results;

        try {
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString());
            String urlStr = NOMINATIM_SEARCH_URL + "?q=" + encodedQuery +
                           "&format=json&limit=5&addressdetails=1";

            HttpURLConnection conn = createConnection(urlStr);

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                results = parseSearchResults(response);
            }
            conn.disconnect();

        } catch (Exception e) {
            System.err.println("Erreur recherche Nominatim: " + e.getMessage());
        }

        return results;
    }

    /**
     * Géocodage inverse (coordonnées -> adresse)
     */
    public GeoLocation reverseGeocode(double latitude, double longitude) {
        try {
            String urlStr = NOMINATIM_REVERSE_URL + "?lat=" + latitude + "&lon=" + longitude +
                           "&format=json&addressdetails=1";

            HttpURLConnection conn = createConnection(urlStr);

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                return parseReverseResult(response);
            }
            conn.disconnect();

        } catch (Exception e) {
            System.err.println("Erreur reverse geocoding: " + e.getMessage());
        }

        return null;
    }

    /**
     * Géocodage (adresse -> coordonnées)
     */
    public GeoLocation geocode(String address) {
        List<GeoLocation> results = searchLocations(address);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Calcule la distance entre deux points (formule Haversine)
     * @return distance en kilomètres
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Rayon de la Terre en km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Estime le temps de trajet en minutes (basé sur 50 km/h en moyenne)
     */
    public static int estimateTravelTime(double distanceKm) {
        return (int) Math.ceil(distanceKm / 50.0 * 60);
    }

    /**
     * Formate la distance pour l'affichage
     */
    public static String formatDistance(double distanceKm) {
        if (distanceKm < 1) {
            return String.format("%.0f m", distanceKm * 1000);
        }
        return String.format("%.1f km", distanceKm);
    }

    /**
     * Formate le temps de trajet pour l'affichage
     */
    public static String formatTravelTime(int minutes) {
        if (minutes < 60) {
            return minutes + " min";
        }
        int hours = minutes / 60;
        int mins = minutes % 60;
        return hours + "h" + (mins > 0 ? String.format("%02d", mins) : "");
    }

    /**
     * Génère le HTML pour une carte Leaflet interactive (sélection de localisation)
     */
    public static String generateMapPickerHtml(double initialLat, double initialLon) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Sélectionner une localisation</title>
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: 'Segoe UI', Arial, sans-serif; }
                    #map { height: 100vh; width: 100%; }
                    .search-box {
                        position: absolute; top: 10px; left: 50px; z-index: 1000;
                        background: white; padding: 10px; border-radius: 8px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.2);
                    }
                    .search-box input {
                        width: 280px; padding: 10px; border: 1px solid #ddd;
                        border-radius: 5px; font-size: 14px;
                    }
                    .search-results {
                        max-height: 200px; overflow-y: auto; margin-top: 5px;
                    }
                    .search-result {
                        padding: 8px 10px; cursor: pointer; border-bottom: 1px solid #eee;
                        font-size: 13px;
                    }
                    .search-result:hover { background: #f0f0f0; }
                    .info-box {
                        position: absolute; bottom: 20px; left: 50px; z-index: 1000;
                        background: white; padding: 15px; border-radius: 8px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.2); max-width: 350px;
                    }
                    .info-box h4 { color: #2c3e50; margin-bottom: 8px; }
                    .info-box p { color: #666; font-size: 13px; margin: 5px 0; }
                    .coords { font-family: monospace; color: #5BA3F5; }
                    .instruction {
                        position: absolute; top: 10px; right: 10px; z-index: 1000;
                        background: #5BA3F5; color: white; padding: 10px 15px;
                        border-radius: 8px; font-size: 13px;
                    }
                </style>
            </head>
            <body>
                <div id="map"></div>
                
                <div class="search-box">
                    <input type="text" id="searchInput" placeholder="🔍 Rechercher une ville..." 
                           onkeyup="searchLocation(this.value)">
                    <div id="searchResults" class="search-results"></div>
                </div>
                
                <div class="instruction">
                    📍 Cliquez sur la carte pour sélectionner
                </div>
                
                <div class="info-box" id="infoBox" style="display:none;">
                    <h4>📍 Localisation sélectionnée</h4>
                    <p id="selectedAddress">-</p>
                    <p class="coords">Lat: <span id="selectedLat">-</span>, Lon: <span id="selectedLon">-</span></p>
                </div>

                <script>
                    var map = L.map('map').setView([%f, %f], 6);
                    var marker = null;
                    var searchTimeout = null;

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);

                    // Clic sur la carte
                    map.on('click', function(e) {
                        setMarker(e.latlng.lat, e.latlng.lng);
                        reverseGeocode(e.latlng.lat, e.latlng.lng);
                    });

                    function setMarker(lat, lon) {
                        if (marker) map.removeLayer(marker);
                        marker = L.marker([lat, lon], {
                            icon: L.divIcon({
                                className: 'custom-marker',
                                html: '<div style="background:#5BA3F5;width:30px;height:30px;border-radius:50%%;border:3px solid white;box-shadow:0 2px 5px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center;color:white;font-size:16px;">📍</div>',
                                iconSize: [30, 30],
                                iconAnchor: [15, 15]
                            })
                        }).addTo(map);
                        
                        document.getElementById('selectedLat').textContent = lat.toFixed(6);
                        document.getElementById('selectedLon').textContent = lon.toFixed(6);
                        document.getElementById('infoBox').style.display = 'block';
                        
                        // Envoyer à Java
                        if (window.javaConnector) {
                            window.javaConnector.onLocationSelected(lat, lon, document.getElementById('selectedAddress').textContent);
                        }
                    }

                    function reverseGeocode(lat, lon) {
                        fetch('https://nominatim.openstreetmap.org/reverse?lat=' + lat + '&lon=' + lon + '&format=json')
                            .then(response => response.json())
                            .then(data => {
                                var address = data.display_name || 'Localisation inconnue';
                                // Simplifier l'adresse
                                var parts = address.split(',');
                                if (parts.length > 2) {
                                    address = parts[0].trim() + ', ' + parts[parts.length - 1].trim();
                                }
                                document.getElementById('selectedAddress').textContent = address;
                                
                                if (window.javaConnector) {
                                    window.javaConnector.onLocationSelected(lat, lon, address);
                                }
                            })
                            .catch(err => {
                                document.getElementById('selectedAddress').textContent = 'Lat: ' + lat.toFixed(4) + ', Lon: ' + lon.toFixed(4);
                            });
                    }

                    function searchLocation(query) {
                        if (searchTimeout) clearTimeout(searchTimeout);
                        if (query.length < 2) {
                            document.getElementById('searchResults').innerHTML = '';
                            return;
                        }
                        
                        searchTimeout = setTimeout(function() {
                            fetch('https://nominatim.openstreetmap.org/search?q=' + encodeURIComponent(query) + '&format=json&limit=5')
                                .then(response => response.json())
                                .then(data => {
                                    var html = '';
                                    data.forEach(function(item) {
                                        html += '<div class="search-result" onclick="selectSearchResult(' + 
                                                item.lat + ',' + item.lon + ',\\'' + item.display_name.replace(/'/g, "\\\\'") + '\\')">' +
                                                item.display_name + '</div>';
                                    });
                                    document.getElementById('searchResults').innerHTML = html;
                                });
                        }, 300);
                    }

                    function selectSearchResult(lat, lon, name) {
                        map.setView([lat, lon], 12);
                        setMarker(lat, lon);
                        document.getElementById('selectedAddress').textContent = name;
                        document.getElementById('searchResults').innerHTML = '';
                        document.getElementById('searchInput').value = '';
                        
                        if (window.javaConnector) {
                            window.javaConnector.onLocationSelected(lat, lon, name);
                        }
                    }
                </script>
            </body>
            </html>
            """.formatted(initialLat, initialLon);
    }

    /**
     * Génère le HTML pour afficher une offre sur la carte avec distance
     */
    public static String generateMapViewHtml(double offerLat, double offerLon, String offerAddress,
                                             Double candidateLat, Double candidateLon) {
        String distanceInfo = "";
        String candidateMarker = "";
        String routeLine = "";

        if (candidateLat != null && candidateLon != null) {
            double distance = calculateDistance(candidateLat, candidateLon, offerLat, offerLon);
            int travelTime = estimateTravelTime(distance);

            distanceInfo = """
                <div class="distance-box">
                    <h4>📏 Distance depuis votre position</h4>
                    <p class="distance-value">%s</p>
                    <p class="travel-time">🚗 Environ %s en voiture</p>
                </div>
                """.formatted(formatDistance(distance), formatTravelTime(travelTime));

            candidateMarker = """
                L.marker([%f, %f], {
                    icon: L.divIcon({
                        className: 'candidate-marker',
                        html: '<div style="background:#28a745;width:35px;height:35px;border-radius:50%%;border:3px solid white;box-shadow:0 2px 8px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center;font-size:18px;">🏠</div>',
                        iconSize: [35, 35],
                        iconAnchor: [17, 17]
                    })
                }).addTo(map).bindPopup('<b>Votre position</b>');
                """.formatted(candidateLat, candidateLon);

            routeLine = """
                L.polyline([[%f, %f], [%f, %f]], {
                    color: '#5BA3F5',
                    weight: 3,
                    opacity: 0.7,
                    dashArray: '10, 10'
                }).addTo(map);
                
                // Ajuster la vue pour montrer les deux points
                map.fitBounds([[%f, %f], [%f, %f]], {padding: [50, 50]});
                """.formatted(candidateLat, candidateLon, offerLat, offerLon,
                             candidateLat, candidateLon, offerLat, offerLon);
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Localisation de l'offre</title>
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: 'Segoe UI', Arial, sans-serif; }
                    #map { height: 100vh; width: 100%; }
                    .info-box {
                        position: absolute; top: 10px; left: 10px; z-index: 1000;
                        background: white; padding: 15px; border-radius: 10px;
                        box-shadow: 0 2px 15px rgba(0,0,0,0.2); max-width: 300px;
                    }
                    .info-box h3 { color: #2c3e50; margin-bottom: 10px; font-size: 16px; }
                    .info-box p { color: #666; font-size: 13px; line-height: 1.5; }
                    .distance-box {
                        position: absolute; bottom: 20px; left: 10px; z-index: 1000;
                        background: linear-gradient(135deg, #5BA3F5, #2196f3); 
                        color: white; padding: 15px 20px; border-radius: 10px;
                        box-shadow: 0 4px 15px rgba(91,163,245,0.4);
                    }
                    .distance-box h4 { font-size: 13px; opacity: 0.9; margin-bottom: 5px; }
                    .distance-value { font-size: 28px; font-weight: 700; }
                    .travel-time { font-size: 14px; opacity: 0.9; margin-top: 5px; }
                    .legend {
                        position: absolute; bottom: 20px; right: 10px; z-index: 1000;
                        background: white; padding: 10px 15px; border-radius: 8px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.15); font-size: 12px;
                    }
                    .legend-item { display: flex; align-items: center; margin: 5px 0; }
                    .legend-dot { width: 20px; height: 20px; border-radius: 50%%; margin-right: 8px; 
                                 display: flex; align-items: center; justify-content: center; font-size: 12px; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                
                <div class="info-box">
                    <h3>📍 Localisation de l'offre</h3>
                    <p>%s</p>
                </div>
                
                %s
                
                <div class="legend">
                    <div class="legend-item">
                        <div class="legend-dot" style="background:#dc3545;">💼</div>
                        <span>Lieu de travail</span>
                    </div>
                    %s
                </div>

                <script>
                    var map = L.map('map').setView([%f, %f], 13);

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '© OpenStreetMap'
                    }).addTo(map);

                    // Marqueur de l'offre
                    L.marker([%f, %f], {
                        icon: L.divIcon({
                            className: 'offer-marker',
                            html: '<div style="background:#dc3545;width:40px;height:40px;border-radius:50%%;border:3px solid white;box-shadow:0 3px 10px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center;font-size:20px;">💼</div>',
                            iconSize: [40, 40],
                            iconAnchor: [20, 20]
                        })
                    }).addTo(map).bindPopup('<b>%s</b>').openPopup();

                    %s
                    %s
                </script>
            </body>
            </html>
            """.formatted(
                offerAddress,
                distanceInfo,
                candidateLat != null ? "<div class=\"legend-item\"><div class=\"legend-dot\" style=\"background:#28a745;\">🏠</div><span>Votre position</span></div>" : "",
                offerLat, offerLon,
                offerLat, offerLon,
                offerAddress.replace("'", "\\'"),
                candidateMarker,
                routeLine
            );
    }

    // === Méthodes privées ===

    private HttpURLConnection createConnection(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        return conn;
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private List<GeoLocation> parseSearchResults(String json) {
        List<GeoLocation> results = new ArrayList<>();
        try {
            // Parse JSON array manuellement
            if (!json.startsWith("[")) return results;

            String[] items = json.substring(1, json.length() - 1).split("\\},\\{");
            for (String item : items) {
                if (!item.startsWith("{")) item = "{" + item;
                if (!item.endsWith("}")) item = item + "}";

                GeoLocation loc = parseGeoLocation(item);
                if (loc != null) results.add(loc);
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing: " + e.getMessage());
        }
        return results;
    }

    private GeoLocation parseReverseResult(String json) {
        return parseGeoLocation(json);
    }

    private GeoLocation parseGeoLocation(String json) {
        try {
            String displayName = extractJsonValue(json, "display_name");
            String latStr = extractJsonValue(json, "lat");
            String lonStr = extractJsonValue(json, "lon");

            if (latStr == null || lonStr == null) return null;

            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);

            // Extraire ville et pays de l'adresse
            String city = extractFromAddress(json, "city");
            if (city == null) city = extractFromAddress(json, "town");
            if (city == null) city = extractFromAddress(json, "village");
            if (city == null) city = extractFromAddress(json, "municipality");

            String country = extractFromAddress(json, "country");

            return new GeoLocation(displayName, city, country, lat, lon);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractJsonValue(String json, String key) {
        String pattern1 = "\"" + key + "\":\"";
        String pattern2 = "\"" + key + "\":";

        int idx = json.indexOf(pattern1);
        if (idx >= 0) {
            int start = idx + pattern1.length();
            int end = json.indexOf("\"", start);
            if (end > start) return json.substring(start, end);
        }

        idx = json.indexOf(pattern2);
        if (idx >= 0) {
            int start = idx + pattern2.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
                end++;
            }
            if (end > start) return json.substring(start, end);
        }

        return null;
    }

    private String extractFromAddress(String json, String key) {
        // Chercher dans la section "address"
        int addrIdx = json.indexOf("\"address\":");
        if (addrIdx < 0) return null;

        String addrSection = json.substring(addrIdx);
        int endIdx = addrSection.indexOf("}");
        if (endIdx > 0) {
            addrSection = addrSection.substring(0, endIdx + 1);
            return extractJsonValue(addrSection, key);
        }
        return null;
    }
}

