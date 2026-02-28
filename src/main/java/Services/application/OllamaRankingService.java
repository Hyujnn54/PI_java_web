package Services.application;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OllamaRankingService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private static final String MODEL = "llama3";
    private static final String API_KEY = "81eddfe408e945daa69c82ff3bbc2417.nNY2akMe9gLwFV7275Tf7hja";

    public record RankResult(int score, String rationale) {}

    public static RankResult rankApplication(
        String jobTitle,
        String jobDescription,
        List<String> offerSkills,
        String candidateName,
        String candidateExperience,
        String candidateEducation,
        List<String> candidateSkills,
        String coverLetter,
        String cvContent
    ) {
        try {
            String prompt = buildPrompt(jobTitle, jobDescription, offerSkills, candidateName,
                candidateExperience, candidateEducation, candidateSkills, coverLetter, cvContent);

            String response = callOllama(prompt);
            if (response != null && !response.isEmpty()) {
                RankResult parsed = parseResponse(response);
                if (parsed != null) {
                    return parsed;
                }
            }
        } catch (Exception e) {
            System.err.println("Ollama ranking failed: " + e.getMessage());
        }

        return heuristicRank(jobDescription, offerSkills, candidateSkills, coverLetter);
    }

    private static String buildPrompt(
        String jobTitle,
        String jobDescription,
        List<String> offerSkills,
        String candidateName,
        String candidateExperience,
        String candidateEducation,
        List<String> candidateSkills,
        String coverLetter,
        String cvContent
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an HR assistant ranking candidates for a job.");
        prompt.append(" Return JSON only: {\"score\": 0-100, \"rationale\": \"short reason (1-2 sentences)\"}.\n\n");

        prompt.append("Job Title: ").append(jobTitle).append("\n");
        if (jobDescription != null && !jobDescription.isEmpty()) {
            prompt.append("Job Description: ").append(jobDescription).append("\n");
        }
        if (offerSkills != null && !offerSkills.isEmpty()) {
            prompt.append("Required Skills: ").append(String.join(", ", offerSkills)).append("\n");
        }

        prompt.append("\nCandidate: ").append(candidateName).append("\n");
        if (candidateEducation != null && !candidateEducation.isEmpty()) {
            prompt.append("Education: ").append(candidateEducation).append("\n");
        }
        if (candidateExperience != null && !candidateExperience.isEmpty()) {
            prompt.append("Experience: ").append(candidateExperience).append("\n");
        }
        if (candidateSkills != null && !candidateSkills.isEmpty()) {
            prompt.append("Skills: ").append(String.join(", ", candidateSkills)).append("\n");
        }
        if (coverLetter != null && !coverLetter.isEmpty()) {
            prompt.append("Cover Letter Summary: ").append(limit(coverLetter, 800)).append("\n");
        }
        if (cvContent != null && !cvContent.isEmpty()) {
            prompt.append("CV Excerpt: ").append(limit(cvContent, 1200)).append("\n");
        }

        return prompt.toString();
    }

    private static String callOllama(String prompt) throws Exception {
        URL url = new URL(OLLAMA_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
        connection.setDoOutput(true);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("stream", false);
        requestBody.put("temperature", 0.2);

        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You score candidates for job fit.");
        messages.put(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.put(userMessage);

        requestBody.put("messages", messages);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            StringBuilder errorBody = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorBody.append(line);
                }
            }
            System.err.println("Ollama API error: " + responseCode + " - " + errorBody);
            return null;
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    private static RankResult parseResponse(String apiResponse) {
        try {
            JSONObject jsonResponse = new JSONObject(apiResponse);
            String content = "";

            if (jsonResponse.has("message")) {
                JSONObject message = jsonResponse.getJSONObject("message");
                content = message.optString("content", "");
            } else if (jsonResponse.has("response")) {
                content = jsonResponse.optString("response", "");
            }

            content = content == null ? "" : content.trim();
            if (content.isEmpty()) {
                return null;
            }

            // Try direct JSON parse from model output
            try {
                JSONObject parsed = new JSONObject(content);
                int score = clampScore(parsed.optInt("score", 0));
                String rationale = parsed.optString("rationale", "No rationale provided.");
                return new RankResult(score, trimRationale(rationale));
            } catch (Exception ignored) {
                // fall through to regex parsing
            }

            int score = clampScore(extractScore(content));
            String rationale = extractRationale(content);
            return new RankResult(score, trimRationale(rationale));

        } catch (Exception e) {
            System.err.println("Failed to parse Ollama response: " + e.getMessage());
            return null;
        }
    }

    private static int extractScore(String content) {
        Pattern p = Pattern.compile("(?i)score\\s*[:=]\\s*(\\d{1,3})");
        Matcher m = p.matcher(content);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private static String extractRationale(String content) {
        Pattern p = Pattern.compile("(?i)rationale\\s*[:=]\\s*(.*)");
        Matcher m = p.matcher(content);
        if (m.find()) {
            return m.group(1).trim();
        }
        return content;
    }

    private static RankResult heuristicRank(String jobDescription, List<String> offerSkills,
                                           List<String> candidateSkills, String coverLetter) {
        int score = 40;
        List<String> matched = new ArrayList<>();

        String description = jobDescription != null ? jobDescription.toLowerCase(Locale.ROOT) : "";
        String cover = coverLetter != null ? coverLetter.toLowerCase(Locale.ROOT) : "";

        if (offerSkills != null && candidateSkills != null) {
            for (String req : offerSkills) {
                for (String cand : candidateSkills) {
                    if (req.equalsIgnoreCase(cand) || cand.toLowerCase(Locale.ROOT).contains(req.toLowerCase(Locale.ROOT))) {
                        matched.add(req);
                        score += 10;
                        break;
                    }
                }
            }
        }

        if (candidateSkills != null) {
            for (String skill : candidateSkills) {
                String s = skill.toLowerCase(Locale.ROOT);
                if (!description.isEmpty() && description.contains(s)) {
                    score += 5;
                } else if (!cover.isEmpty() && cover.contains(s)) {
                    score += 3;
                }
            }
        }

        score = clampScore(score);
        String rationale = matched.isEmpty()
            ? "Baseline fit based on profile; limited direct skill matches found."
            : "Matched skills: " + String.join(", ", matched) + ".";

        return new RankResult(score, rationale);
    }

    private static int clampScore(int score) {
        if (score < 0) return 0;
        if (score > 100) return 100;
        return score;
    }

    private static String trimRationale(String rationale) {
        if (rationale == null || rationale.isEmpty()) {
            return "No rationale provided.";
        }
        return rationale.length() > 240 ? rationale.substring(0, 240) + "..." : rationale;
    }

    private static String limit(String text, int max) {
        if (text == null) return "";
        String trimmed = text.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) + "..." : trimmed;
    }
}

