package Services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service pour générer des messages d'avertissement via l'API Grok (xAI).
 * Used by the JobOffers module (warning system).
 */
public class GrokAIWarningService {

    private static final String API_KEY = "xai-BvO5mSs05cHXwQRM1qa8Z7lojgfAMS0I6Kc9Y1R5lQYSyHWO6eDq62ZZ0QsajWkyyyB6f41ZD4HmWOCU";
    private static final String API_URL = "https://api.x.ai/v1/chat/completions";

    /**
     * Génère un message d'avertissement professionnel basé sur la raison et les détails de l'offre
     */
    public String generateWarningMessage(String reason, String jobTitle, String jobDescription) {
        try {
            String prompt = buildPrompt(reason, jobTitle, jobDescription);
            return callGrokAPI(prompt);
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du message: " + e.getMessage());
            return getDefaultMessage(reason);
        }
    }

    private String buildPrompt(String reason, String jobTitle, String jobDescription) {
        return String.format(
            "Tu es un administrateur de plateforme d'emploi. Génère un message d'avertissement professionnel et courtois en français pour un recruteur. " +
            "Le message doit expliquer le problème et suggérer comment le corriger. " +
            "Sois direct mais respectueux. Maximum 3-4 phrases.\n\n" +
            "Raison du signalement: %s\n" +
            "Titre de l'offre: %s\n" +
            "Description de l'offre: %s\n\n" +
            "Génère uniquement le message, sans introduction ni signature.",
            reason,
            jobTitle != null ? jobTitle : "Non spécifié",
            jobDescription != null && jobDescription.length() > 200
                ? jobDescription.substring(0, 200) + "..."
                : (jobDescription != null ? jobDescription : "Non spécifiée")
        );
    }

    private String callGrokAPI(String prompt) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);

        String jsonBody = String.format(
            "{\"model\": \"grok-beta\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}], \"max_tokens\": 300, \"temperature\": 0.7}",
            escapeJson(prompt)
        );

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            return extractMessageFromResponse(response.toString());
        } else {
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line);
                }
            }
            System.err.println("Erreur API Grok (" + responseCode + "): " + errorResponse);
            throw new Exception("Erreur API: " + responseCode);
        }
    }

    private String extractMessageFromResponse(String jsonResponse) {
        try {
            int contentStart = jsonResponse.indexOf("\"content\":");
            if (contentStart == -1) return null;

            contentStart = jsonResponse.indexOf("\"", contentStart + 10) + 1;
            int contentEnd = jsonResponse.indexOf("\"", contentStart);

            while (contentEnd > 0 && jsonResponse.charAt(contentEnd - 1) == '\\') {
                contentEnd = jsonResponse.indexOf("\"", contentEnd + 1);
            }

            if (contentStart > 0 && contentEnd > contentStart) {
                String content = jsonResponse.substring(contentStart, contentEnd);
                content = content.replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                return content.trim();
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing réponse: " + e.getMessage());
        }
        return null;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Message par défaut si l'API échoue
     */
    private String getDefaultMessage(String reason) {
        return switch (reason) {
            case "Contenu inapproprié" ->
                "Votre offre contient du contenu qui ne respecte pas nos conditions d'utilisation. " +
                "Veuillez réviser le contenu et supprimer tout élément inapproprié.";
            case "Information trompeuse" ->
                "Certaines informations de votre offre semblent inexactes ou trompeuses. " +
                "Veuillez vérifier et corriger les détails pour assurer leur exactitude.";
            case "Discrimination" ->
                "Votre offre contient des critères qui pourraient être considérés comme discriminatoires. " +
                "Veuillez modifier votre offre pour qu'elle soit conforme aux lois sur l'égalité des chances.";
            case "Information incomplète" ->
                "Votre offre manque d'informations essentielles pour les candidats. " +
                "Veuillez compléter les détails du poste, les qualifications requises et les conditions.";
            case "Offre en double" ->
                "Cette offre semble être un doublon d'une offre existante. " +
                "Veuillez supprimer cette offre ou la modifier significativement.";
            case "Offre expirée non mise à jour" ->
                "Cette offre semble avoir dépassé sa date de validité. " +
                "Veuillez mettre à jour la date limite ou fermer l'offre si le poste est pourvu.";
            case "Spam" ->
                "Votre offre a été identifiée comme potentiellement indésirable. " +
                "Veuillez vous assurer que le contenu est pertinent et professionnel.";
            default ->
                "Un problème a été identifié avec votre offre d'emploi. " +
                "Veuillez la réviser et apporter les corrections nécessaires.";
        };
    }
}

