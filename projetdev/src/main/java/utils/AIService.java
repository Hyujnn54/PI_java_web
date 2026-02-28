package utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class AIService {

    private static final String API_KEY = ""; // USER: Add your Gemini or Cohere API key here
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    public interface AICallback {
        void onSuccess(String result);
        void onFailure(Throwable t);
    }

    /**
     * Generates a professional event description based on the title and type.
     */
    public static void generateEventDescription(String title, String eventType, AICallback callback) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            // High-quality mock for demonstration
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(1500); // Simulate network latency
                    String mockDescription = "Participez à notre événement '" + title + "' (" + eventType + "). " +
                            "Une occasion unique de réseauter, d'apprendre des experts du domaine et de propulser votre carrière vers de nouveaux sommets. " +
                            "Ne manquez pas cette opportunité d'enrichir vos compétences et d'élargir votre horizon professionnel dans un environnement stimulant.";
                    callback.onSuccess(mockDescription);
                } catch (InterruptedException e) {
                    callback.onFailure(e);
                }
            });
            return;
        }

        // Real API call logic (e.g., using Gemini API)
        // This is a placeholder for real implementation if the user provides a key
        callback.onFailure(new UnsupportedOperationException("API integration pending valid key configuration."));
    }

    /**
     * Generates a personalized insight for a candidate about an event.
     */
    public static void generateCareerInsight(String eventTitle, String eventDescription, String candidateProfile, AICallback callback) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(2000);
                    
                    // Simple "Dynamic" logic simulation
                    String strength = eventDescription != null && eventDescription.toLowerCase().contains("it") ? 
                        "développement technique" : "croissance professionnelle";
                    
                    String goal = eventTitle != null && eventTitle.toLowerCase().contains("forum") ?
                        "réseautage intensif avec des entreprises locales" : "acquisition de connaissances stratégiques";

                    String insight = "✨ Analyse Talent Bridge IA :\n\n" +
                            "En combinant votre parcours (" + candidateProfile + ") avec cet événement '" + eventTitle + "', j'ai identifié 3 points clés :\n\n" +
                            "1. **Adéquation Directe** : Votre expérience est un atout majeur pour '" + goal + "'.\n" +
                            "2. **Opportunité de Croissance** : La description mentionne des éléments clés pour votre " + strength + ".\n" +
                            "3. **Conseil IA** : Profitez de la section '" + (eventDescription != null && eventDescription.length() > 20 ? eventDescription.substring(0, 20) + "..." : "Détails") + "' pour préparer vos questions.";
                    
                    callback.onSuccess(insight);
                } catch (InterruptedException e) {
                    callback.onFailure(e);
                }
            });
            return;
        }
        
        callback.onFailure(new UnsupportedOperationException("API integration pending valid key configuration."));
    }
}
