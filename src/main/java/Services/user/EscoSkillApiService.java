package Services.user;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EscoSkillApiService {
    private static final String BASE = "https://ec.europa.eu/esco/api";
    private final HttpClient http = HttpClient.newHttpClient();

    public List<String> suggestSkills(String query, String language, int limit) throws Exception {
        if (query == null) return List.of();
        String q = query.trim();
        if (q.length() < 2) return List.of();

        String url = BASE + "/suggest2"
                + "?text=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                + "&language=" + URLEncoder.encode(language, StandardCharsets.UTF_8)
                + "&type=skill"
                + "&limit=" + limit
                + "&offset=0"
                + "&alt=true";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 400) {
            throw new RuntimeException("ESCO suggest failed (" + res.statusCode() + "): " + res.body());
        }

        JSONObject root = new JSONObject(res.body());
        List<String> out = new ArrayList<>();

        // Try _embedded.results
        if (root.has("_embedded")) {
            JSONObject embedded = root.getJSONObject("_embedded");
            if (embedded.has("results")) {
                extractLabels(embedded.getJSONArray("results"), out, language);
            }
        }

        // Fallback: top-level results
        if (root.has("results")) {
            extractLabels(root.getJSONArray("results"), out, language);
        }

        return out.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(limit)
                .toList();
    }

    private void extractLabels(JSONArray arr, List<String> out, String language) {
        for (int i = 0; i < arr.length(); i++) {
            JSONObject node = arr.optJSONObject(i);
            if (node == null) continue;

            // preferredLabel.{language}
            if (node.has("preferredLabel")) {
                Object pl = node.get("preferredLabel");
                if (pl instanceof JSONObject plObj && plObj.has(language)) {
                    String v = plObj.optString(language, "").trim();
                    if (!v.isEmpty()) out.add(v);
                }
            }

            // title
            String title = node.optString("title", "").trim();
            if (!title.isEmpty()) out.add(title);

            // searchHit
            String hit = node.optString("searchHit", "").trim();
            if (!hit.isEmpty()) out.add(hit);
        }
    }
}