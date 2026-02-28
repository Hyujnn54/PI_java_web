package Services.joboffers;

import Models.joboffers.ModerationResult;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

public class ModerationService {

    private String apiKey;
    private final HttpClient httpClient;

    public ModerationService() {
        this.httpClient = HttpClient.newHttpClient();
        loadApiKey();
    }

    private void loadApiKey() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("Le fichier config.properties n'a pas été trouvé");
                return;
            }
            Properties prop = new Properties();
            prop.load(input);
            this.apiKey = prop.getProperty("perspective.api.key");
            
            if (this.apiKey == null || this.apiKey.isEmpty()) {
                 System.err.println("La clé perspective.api.key n'est pas définie dans config.properties");
            }
        } catch (Exception ex) {
            System.err.println("Erreur lors du chargement de config.properties : " + ex.getMessage());
        }
    }

    /**
     * Call the Google Perspective API to analyze text for toxicity and other attributes.
     */
    public ModerationResult analyzeText(String text) {
        if (apiKey == null || apiKey.isEmpty()) {
             System.err.println("ModerationService: API Key est manquante. Impossible d'analyser le texte.");
             return null;
        }

        if (text == null || text.trim().isEmpty()) {
             return new ModerationResult(0, 0, 0, 0);
        }

        try {
            String url = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=" + apiKey;

            // Escaping text to prevent JSON errors
            String escapedText = text.replace("\\", "\\\\")
                                     .replace("\"", "\\\"")
                                     .replace("\n", " ")
                                     .replace("\r", "")
                                     .replace("\t", " ");

            String requestBody = "{"
                    + "\"comment\": {\"text\": \"" + escapedText + "\"}, "
                    + "\"languages\": [\"fr\"], "
                    + "\"requestedAttributes\": {"
                    + "  \"TOXICITY\": {}, "
                    + "  \"INSULT\": {}, "
                    + "  \"THREAT\": {}, "
                    + "  \"IDENTITY_ATTACK\": {}"
                    + "}"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseResponse(response.body());
            } else {
                System.err.println("Erreur API Perspective (Status code: " + response.statusCode() + "): " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'appel à l'API Perspective : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Helper to parse the Perspective API JSON response.
     */
    private ModerationResult parseResponse(String jsonBody) {
        try {
            JSONObject jsonResponse = new JSONObject(jsonBody);
            JSONObject attributeScores = jsonResponse.getJSONObject("attributeScores");

            double toxicity = extractScore(attributeScores, "TOXICITY");
            double insult = extractScore(attributeScores, "INSULT");
            double threat = extractScore(attributeScores, "THREAT");
            double identityAttack = extractScore(attributeScores, "IDENTITY_ATTACK");

            return new ModerationResult(toxicity, insult, threat, identityAttack);

        } catch (Exception e) {
             System.err.println("Erreur lors du parsing de la réponse JSON : " + e.getMessage());
             return null;
        }
    }

    private double extractScore(JSONObject attributeScores, String attributeName) {
         if (attributeScores.has(attributeName)) {
             return attributeScores.getJSONObject(attributeName)
                                   .getJSONObject("summaryScore")
                                   .getDouble("value");
         }
         return 0.0;
    }
}
