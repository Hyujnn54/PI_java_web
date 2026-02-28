package Services.application;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Service for generating / translating cover letters using Groq API.
 * Used by the Applications module.
 */
public class GrokAIService {

    private static final String GROQ_API_KEY = "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";
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";

    // Groq-hosted models, tried in order
    private static final String[] MODELS = {
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x7b-32768"
    };

    public static String generateCoverLetter(String candidateName, String email, String phone,
                                             String jobTitle, String companyName, String experience,
                                             String education, List<String> skills, String cvContent) {
        String prompt = buildPrompt(candidateName, email, phone, jobTitle,
                companyName, experience, education, skills, cvContent);

        for (String model : MODELS) {
            try {
                System.out.println("Trying Groq model: " + model);
                String result = callGroq(model, prompt);
                if (result != null && !result.isBlank()) {
                    System.out.println("Cover letter generated with Groq model: " + model);
                    return cleanContent(result);
                }
            } catch (Exception e) {
                System.err.println("Groq model " + model + " failed: " + e.getMessage());
            }
        }

        System.out.println("All Groq models failed – falling back to local generation.");
        return LocalCoverLetterService.generateCoverLetter(candidateName, email, phone, jobTitle,
                companyName, experience, education, skills, cvContent);
    }

    private static String buildPrompt(String candidateName, String email, String phone,
                                      String jobTitle, String companyName, String experience,
                                      String education, List<String> skills, String cvContent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional cover letter writer. Write a compelling, professional cover letter (200-400 words) for the following candidate applying to a job. Output ONLY the cover letter text, nothing else.\n\n");

        prompt.append("CANDIDATE:\n");
        prompt.append("Name: ").append(candidateName).append("\n");
        prompt.append("Email: ").append(email).append("\n");
        prompt.append("Phone: ").append(phone).append("\n");
        prompt.append("Education: ").append(education).append("\n");
        prompt.append("Experience: ").append(experience).append("\n");

        if (skills != null && !skills.isEmpty()) {
            prompt.append("Skills: ").append(String.join(", ", skills)).append("\n");
        }

        if (cvContent != null && cvContent.length() > 50) {
            String cv = cvContent.length() > 2000 ? cvContent.substring(0, 2000) + "..." : cvContent;
            prompt.append("CV Summary: ").append(cv).append("\n");
        }

        prompt.append("\nJOB:\n");
        prompt.append("Position: ").append(jobTitle).append("\n");
        prompt.append("Company: ").append(companyName).append("\n");

        return prompt.toString();
    }

    private static String callGroq(String model, String prompt) throws Exception {
        URL url = new URL(GROQ_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);

        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", "You are a professional assistant.");

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        JSONArray messages = new JSONArray();
        messages.put(systemMsg);
        messages.put(userMsg);

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 1024);
        body.put("stream", false);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            StringBuilder err = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) err.append(line);
            }
            System.err.println("Groq HTTP " + code + " for model " + model + ": " + err);
            return null;
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) response.append(line);
        }

        JSONObject json = new JSONObject(response.toString());
        JSONArray choices = json.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            if (message != null) {
                return message.optString("content", "");
            }
        }
        return null;
    }

    private static String cleanContent(String content) {
        if (content == null) return null;
        content = content.replaceAll("```[a-zA-Z]*\\n?", "").replaceAll("```", "").trim();
        return content.isEmpty() ? null : content;
    }

    public static String translateCoverLetter(String coverLetter, String targetLanguage) {
        if (coverLetter == null || coverLetter.trim().isEmpty()) return coverLetter;

        String prompt = "Translate the following cover letter to " + targetLanguage + ".\n"
                + "Output ONLY the translated cover letter text, with no preamble or explanation.\n\n"
                + coverLetter;

        for (String model : MODELS) {
            try {
                String result = callGroq(model, prompt);
                if (result != null && !result.isBlank()) {
                    System.out.println("Translation via Groq successful (" + model + ")");
                    return cleanContent(result);
                }
            } catch (Exception e) {
                System.err.println("Groq translation model " + model + " failed: " + e.getMessage());
            }
        }

        // Fallback: MyMemory free translation API
        System.out.println("Groq unavailable — falling back to MyMemory translation API");
        String langCode = switch (targetLanguage.toLowerCase()) {
            case "french"  -> "fr";
            case "arabic"  -> "ar";
            case "english" -> "en";
            case "spanish" -> "es";
            case "german"  -> "de";
            default        -> targetLanguage.toLowerCase().substring(0, Math.min(2, targetLanguage.length()));
        };
        return translateWithMyMemory(coverLetter, langCode);
    }

    private static String translateWithMyMemory(String text, String targetLangCode) {
        try {
            if (text.length() <= 500) {
                return callMyMemory(text, targetLangCode);
            }

            StringBuilder result = new StringBuilder();
            String[] sentences = text.split("(?<=[.!?])\\s+");
            StringBuilder chunk = new StringBuilder();

            for (String sentence : sentences) {
                if (chunk.length() + sentence.length() > 480 && chunk.length() > 0) {
                    String translated = callMyMemory(chunk.toString().trim(), targetLangCode);
                    result.append(translated).append(" ");
                    chunk = new StringBuilder();
                }
                chunk.append(sentence).append(" ");
            }
            if (chunk.length() > 0) {
                result.append(callMyMemory(chunk.toString().trim(), targetLangCode));
            }
            return result.toString().trim();
        } catch (Exception e) {
            System.err.println("MyMemory translation failed: " + e.getMessage());
            return text;
        }
    }

    private static String callMyMemory(String text, String targetLangCode) throws Exception {
        String encoded = java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);
        String urlStr = "https://api.mymemory.translated.net/get?q=" + encoded
                + "&langpair=en|" + targetLangCode;

        java.net.URL url = new java.net.URL(urlStr);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);

        int code = conn.getResponseCode();
        if (code != 200) throw new Exception("MyMemory HTTP " + code);

        java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) response.append(line);
        br.close();

        JSONObject json = new JSONObject(response.toString());
        JSONObject responseData = json.optJSONObject("responseData");
        if (responseData != null) {
            String translated = responseData.optString("translatedText", "");
            if (!translated.isEmpty()) return translated;
        }
        return text;
    }
}
