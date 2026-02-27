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

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private static final String API_KEY = "AIzaSyAywUmsr4dSE-VJl_H8zvFf6Wp8283pWH8";

    public static String generateCoverLetter(String candidateName, String email, String phone,
                                             String jobTitle, String companyName, String experience,
                                             String education, java.util.List<String> skills, String cvContent) {
        try {
            String prompt = buildCoverLetterPrompt(candidateName, email, phone, jobTitle,
                    companyName, experience, education, skills, cvContent);

            String response = callGeminiAPI(prompt);

            if (response != null && !response.isEmpty()) {
                String result = extractCoverLetterFromGemini(response);
                if (result != null && !result.isEmpty()) {
                    System.out.println("Cover letter generated successfully using Gemini API");
                    return result;
                }
            }

        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
        }

        System.out.println("Falling back to local cover letter generation...");
        return LocalCoverLetterService.generateCoverLetter(candidateName, email, phone, jobTitle,
                companyName, experience, education, skills, cvContent);
    }

    private static String buildCoverLetterPrompt(String candidateName, String email, String phone,
                                                 String jobTitle, String companyName, String experience,
                                                 String education, java.util.List<String> skills, String cvContent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please write a professional cover letter for the following candidate applying for a job.\n\n");

        prompt.append("Candidate Information:\n");
        prompt.append("- Name: ").append(candidateName).append("\n");
        prompt.append("- Email: ").append(email).append("\n");
        prompt.append("- Phone: ").append(phone).append("\n");
        prompt.append("- Education: ").append(education).append("\n");
        prompt.append("- Experience: ").append(experience).append("\n");

        if (skills != null && !skills.isEmpty()) {
            prompt.append("- Skills: ");
            for (int i = 0; i < skills.size(); i++) {
                if (i > 0) prompt.append(", ");
                prompt.append(skills.get(i));
            }
            prompt.append("\n");
        }

        if (cvContent != null && !cvContent.isEmpty() && cvContent.length() > 50) {
            prompt.append("\nCV/Resume Content:\n");
            String limitedCvContent = cvContent.length() > 2000 ? cvContent.substring(0, 2000) + "..." : cvContent;
            prompt.append(limitedCvContent).append("\n");
        }

        prompt.append("\nJob Details:\n");
        prompt.append("- Position: ").append(jobTitle).append("\n");
        prompt.append("- Company: ").append(companyName).append("\n");

        prompt.append("\nPlease generate a compelling, professional cover letter (150-400 words).\n");
        prompt.append("Generate ONLY the cover letter text.");
        return prompt.toString();
    }

    private static String callGeminiAPI(String prompt) throws Exception {
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalStateException("GEMINI_API_KEY is missing.");
        }

        String urlWithKey = GEMINI_API_URL + "?key=" + API_KEY;
        URL url = new URL(urlWithKey);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);

        JSONObject requestBody = new JSONObject();

        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", prompt);
        parts.put(part);

        content.put("parts", parts);
        contents.put(content);

        requestBody.put("contents", contents);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 1024);
        requestBody.put("generationConfig", generationConfig);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            System.err.println("Gemini API error. Response code: " + responseCode);
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder errorBody = new StringBuilder();
                while ((line = errorReader.readLine()) != null) errorBody.append(line);
                System.err.println("Error response: " + errorBody);
            }
            return null;
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }
        return response.toString();
    }

    private static String extractCoverLetterFromGemini(String apiResponse) {
        try {
            JSONObject jsonResponse = new JSONObject(apiResponse);

            JSONArray candidates = jsonResponse.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.optJSONObject("content");
                if (content != null) {
                    JSONArray parts = content.optJSONArray("parts");
                    if (parts != null && parts.length() > 0) {
                        JSONObject firstPart = parts.getJSONObject(0);
                        String text = firstPart.optString("text", "");
                        return cleanContent(text);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Gemini API response: " + e.getMessage());
        }
        return null;
    }


    private static String cleanContent(String content) {
        if (content == null) return null;
        content = content.replaceAll("```[a-zA-Z]*\\n?", "");
        content = content.replaceAll("```", "");
        content = content.trim();
        return content.isEmpty() ? null : content;
    }
}

