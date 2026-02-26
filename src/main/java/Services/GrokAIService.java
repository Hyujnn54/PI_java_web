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
 * Service for generating cover letters using OpenAI ChatGPT API
 */
public class GrokAIService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "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";
    private static final String MODEL = "gpt-3.5-turbo";

    /**
     * Generate a personalized cover letter using OpenAI ChatGPT API
     * Falls back to local generation if API fails
     */
    public static String generateCoverLetter(String candidateName, String email, String phone,
                                            String jobTitle, String companyName, String experience,
                                            String education, java.util.List<String> skills, String cvContent) {
        try {
            // Build the prompt for ChatGPT
            String prompt = buildCoverLetterPrompt(candidateName, email, phone, jobTitle,
                                                   companyName, experience, education, skills, cvContent);

            // Call OpenAI API
            String response = callOpenAIAPI(prompt);

            // If API call successful, extract and return the cover letter
            if (response != null && !response.isEmpty()) {
                String result = extractCoverLetter(response);
                if (result != null && !result.isEmpty()) {
                    System.out.println("Cover letter generated successfully using OpenAI API");
                    return result;
                }
            }

        } catch (Exception e) {
            System.err.println("Error calling OpenAI API: " + e.getMessage());
        }

        // Fallback: Generate locally if API fails
        System.out.println("Falling back to local cover letter generation...");
        return LocalCoverLetterService.generateCoverLetter(candidateName, email, phone, jobTitle,
                                                          companyName, experience, education, skills, cvContent);
    }

    /**
     * Build the prompt for ChatGPT to generate a cover letter
     */
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
            // Limit CV content to avoid token limits
            String limitedCvContent = cvContent.length() > 2000 ? cvContent.substring(0, 2000) + "..." : cvContent;
            prompt.append(limitedCvContent).append("\n");
        }

        prompt.append("\nJob Details:\n");
        prompt.append("- Position: ").append(jobTitle).append("\n");
        prompt.append("- Company: ").append(companyName).append("\n");

        prompt.append("\nPlease generate a compelling, professional cover letter (150-400 words) that:\n");
        prompt.append("1. Introduces the candidate and expresses interest in the position\n");
        prompt.append("2. Highlights relevant skills and experience matching the job\n");
        prompt.append("3. Demonstrates knowledge of the company\n");
        prompt.append("4. Includes a call to action and closing\n");
        prompt.append("5. Is personalized and unique\n");
        prompt.append("\nGenerate ONLY the cover letter text, without any additional explanation or formatting.");

        return prompt.toString();
    }

    /**
     * Call the OpenAI ChatGPT API to generate content
     */
    private static String callOpenAIAPI(String prompt) throws Exception {
        URL url = new URL(OPENAI_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set up the request
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
        connection.setDoOutput(true);

        // Build the request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1024);

        JSONArray messages = new JSONArray();

        // Add system message for better results
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a professional cover letter writer. Write compelling and personalized cover letters based on candidate information and skills.");
        messages.put(systemMessage);

        // Add user message
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.put(userMessage);

        requestBody.put("messages", messages);

        // Send the request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Read the response
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            System.err.println("OpenAI API error. Response code: " + responseCode);
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                String errorLine;
                StringBuilder errorBody = new StringBuilder();
                while ((errorLine = errorReader.readLine()) != null) {
                    errorBody.append(errorLine);
                }
                System.err.println("Error response: " + errorBody);
            }
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

    /**
     * Extract the cover letter text from the API response
     */
    private static String extractCoverLetter(String apiResponse) {
        try {
            if (apiResponse == null || apiResponse.isEmpty()) {
                return null;
            }

            // Parse the JSON response
            JSONObject jsonResponse = new JSONObject(apiResponse);

            // Get the choices array
            JSONArray choices = jsonResponse.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject message = firstChoice.optJSONObject("message");

                if (message != null) {
                    String content = message.optString("content", "");
                    // Clean up the content
                    return cleanContent(content);
                }
            }

        } catch (Exception e) {
            System.err.println("Error parsing API response: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Clean up the generated content
     */
    private static String cleanContent(String content) {
        if (content == null) {
            return null;
        }

        // Remove markdown code blocks if present
        content = content.replaceAll("```[a-zA-Z]*\\n?", "");
        content = content.replaceAll("```", "");

        // Trim whitespace
        content = content.trim();

        return content.isEmpty() ? null : content;
    }
}











