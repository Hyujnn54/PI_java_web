import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestPerspective {
    public static void main(String[] args) throws Exception {
        String apiKey = "AIzaSyCkCmkY0G-dGxfFnlpjr1pAlm4wNSNq5ro";
        String text = "Ingénieur Logiciel - je veux pas les femmes travail chez moi";
        
        String url = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=" + apiKey;
        String requestBody = "{"
                + "\"comment\": {\"text\": \"" + text + "\"}, "
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

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());
        System.out.println("Response: " + response.body());
    }
}
