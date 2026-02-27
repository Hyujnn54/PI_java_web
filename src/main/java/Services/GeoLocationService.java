package Services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de géolocalisation utilisant l'API OpenRouteService
 */
public class GeoLocationService {

    private static final String API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImQ2ODk2YmExMTI4NTQ1NGZhMmNjNjYwODc0MDU4MjlmIiwiaCI6Im11cm11cjY0In0=";
    private static final String GEOCODE_URL = "https://api.openrouteservice.org/geocode/search";
    private static final String AUTOCOMPLETE_URL = "https://api.openrouteservice.org/geocode/autocomplete";

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
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

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

                results = parseGeoJsonResponse(response.toString());
            }

        } catch (Exception e) {
            System.err.println("Erreur autocomplete: " + e.getMessage());
        }

        return results;
    }

    /**
     * Géocode une adresse pour obtenir les coordonnées
     */
    public GeoLocation geocode(String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8.toString());
            String urlString = GEOCODE_URL + "?api_key=" + API_KEY +
                              "&text=" + encodedAddress +
                              "&size=1";

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

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
            }

        } catch (Exception e) {
            System.err.println("Erreur geocode: " + e.getMessage());
        }

        return null;
    }

    /**
     * Calcule la distance entre deux points en km (formule de Haversine)
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
     * Estime le temps de trajet en minutes (vitesse moyenne 30 km/h en ville)
     */
    public static int estimateTravelTime(double distanceKm) {
        return (int) Math.round(distanceKm / 30.0 * 60);
    }

    /**
     * Formate la distance pour l'affichage
     */
    public static String formatDistance(double distanceKm) {
        if (distanceKm < 1) {
            return String.format("%.0f m", distanceKm * 1000);
        } else {
            return String.format("%.1f km", distanceKm);
        }
    }

    /**
     * Formate le temps de trajet pour l'affichage
     */
    public static String formatTravelTime(int minutes) {
        if (minutes < 60) {
            return minutes + " min";
        } else {
            int hours = minutes / 60;
            int mins = minutes % 60;
            return hours + "h" + (mins > 0 ? String.format("%02d", mins) : "");
        }
    }

    /**
     * Parse la réponse GeoJSON de l'API
     */
    private List<GeoLocation> parseGeoJsonResponse(String json) {
        List<GeoLocation> results = new ArrayList<>();

        try {
            // Parser les features du GeoJSON
            int featuresStart = json.indexOf("\"features\":");
            if (featuresStart == -1) return results;

            int arrayStart = json.indexOf("[", featuresStart);
            int arrayEnd = findMatchingBracket(json, arrayStart);

            if (arrayStart == -1 || arrayEnd == -1) return results;

            String featuresArray = json.substring(arrayStart, arrayEnd + 1);

            // Extraire chaque feature
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
            // Extraire les coordonnées
            int coordStart = feature.indexOf("\"coordinates\":");
            if (coordStart == -1) return null;

            int coordArrayStart = feature.indexOf("[", coordStart);
            int coordArrayEnd = feature.indexOf("]", coordArrayStart);

            String coordStr = feature.substring(coordArrayStart + 1, coordArrayEnd);
            String[] coords = coordStr.split(",");

            double longitude = Double.parseDouble(coords[0].trim());
            double latitude = Double.parseDouble(coords[1].trim());

            // Extraire le nom
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
}

