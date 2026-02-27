package Services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Service for generating cover letters using Google Gemini API
 */
public class GrokAIService {

    // ⚠️ Replace with a valid API key from https://aistudio.google.com/apikey
    private static final String API_KEY = "AIzaSyDHr-PkQZ2ZPGd-_JM_mCxEQ-_ijOtdeTc";

    // Models confirmed available in v1beta (from ListModels API)
    private static final String[] MODELS = {
        "gemini-2.0-flash",
        "gemini-2.0-flash-lite",
        "gemini-2.5-flash-lite",
        "gemini-2.5-pro"
    };

    public static String generateCoverLetter(String candidateName, String email, String phone,
                                             String jobTitle, String companyName, String experience,
                                             String education, java.util.List<String> skills, String cvContent) {
        String prompt = buildPrompt(candidateName, email, phone, jobTitle,
                companyName, experience, education, skills, cvContent);

        for (String model : MODELS) {
            try {
                System.out.println("Trying Gemini model: " + model);
                String result = callGeminiREST(model, prompt);
                if (result != null && !result.isEmpty()) {
                    System.out.println("Cover letter generated successfully using model: " + model);
                    return cleanContent(result);
                }
            } catch (Exception e) {
                System.err.println("Model " + model + " failed: " + e.getMessage());
            }
        }

        System.out.println("All Gemini models failed. Falling back to local generation...");
        return LocalCoverLetterService.generateCoverLetter(candidateName, email, phone, jobTitle,
                companyName, experience, education, skills, cvContent);
    }

    private static String buildPrompt(String candidateName, String email, String phone,
                                      String jobTitle, String companyName, String experience,
                                      String education, java.util.List<String> skills, String cvContent) {
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

    private static String callGeminiREST(String model, String prompt) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + API_KEY;

        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        // Build Gemini request body
        JSONObject part = new JSONObject();
        part.put("text", prompt);

        JSONArray parts = new JSONArray();
        parts.put(part);

        JSONObject content = new JSONObject();
        content.put("parts", parts);

        JSONArray contents = new JSONArray();
        contents.put(content);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 1024);

        JSONObject body = new JSONObject();
        body.put("contents", contents);
        body.put("generationConfig", generationConfig);

        // Send request
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
            System.err.println("Gemini HTTP " + code + " for model " + model + ": " + err);
            return null;
        }

        // Read response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) response.append(line);
        }

        // Parse response: candidates[0].content.parts[0].text
        JSONObject json = new JSONObject(response.toString());
        JSONArray candidates = json.optJSONArray("candidates");
        if (candidates != null && candidates.length() > 0) {
            JSONObject firstCandidate = candidates.getJSONObject(0);
            JSONObject contentObj = firstCandidate.optJSONObject("content");
            if (contentObj != null) {
                JSONArray partsArr = contentObj.optJSONArray("parts");
                if (partsArr != null && partsArr.length() > 0) {
                    return partsArr.getJSONObject(0).optString("text", "");
                }
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

        // Map display language names to MyMemory language codes
        String langCode = switch (targetLanguage.toLowerCase()) {
            case "french"  -> "fr";
            case "arabic"  -> "ar";
            case "english" -> "en";
            case "spanish" -> "es";
            case "german"  -> "de";
            default        -> targetLanguage.toLowerCase().substring(0, Math.min(2, targetLanguage.length()));
        };

        // First try Gemini (if key works)
        String prompt = "Translate the following cover letter to " + targetLanguage + ".\n"
                + "Output ONLY the translated cover letter text, nothing else. Do not add any explanation.\n\n"
                + coverLetter;
        for (String model : MODELS) {
            try {
                String result = callGeminiREST(model, prompt);
                if (result != null && !result.isEmpty()) {
                    System.out.println("Translation via Gemini successful (" + model + ")");
                    return cleanContent(result);
                }
            } catch (Exception ignored) {}
        }

        // Fallback: MyMemory free translation API (no key required)
        System.out.println("Gemini unavailable — falling back to MyMemory translation API");
        return translateWithMyMemory(coverLetter, langCode);
    }

    private static String translateWithMyMemory(String text, String targetLangCode) {
        try {
            // MyMemory supports up to 500 chars per call — split if needed
            if (text.length() <= 500) {
                return callMyMemory(text, targetLangCode);
            }

            // Split into chunks of ~480 chars at sentence boundaries
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
            return text; // return original if all fails
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
        if (code != 200) {
            throw new Exception("MyMemory HTTP " + code);
        }

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
