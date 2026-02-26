package Services;

import Models.Interview;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * SMS Service for sending interview reminders
 * Uses SMSMobileAPI - Simple HTTP-based SMS solution
 * Compatible with Tunisia
 */
public class SMSService {

    private static final Properties smsConfig = loadSMSConfiguration();

    // Configuration loaded from properties file
    private static final String API_URL = smsConfig.getProperty("sms.api.url", "");
    private static final String API_KEY = smsConfig.getProperty("sms.api.key", "");
    private static final String SENDER_NAME = smsConfig.getProperty("sms.sender.name", "TalentBridge");
    private static final String TEST_PHONE_NUMBER = smsConfig.getProperty("sms.test.recipient", "");
    private static final boolean SMS_ENABLED = Boolean.parseBoolean(smsConfig.getProperty("sms.enabled", "false"));

    /**
     * Load SMS configuration from properties file
     */
    private static Properties loadSMSConfiguration() {
        Properties props = new Properties();

        try (InputStream input = SMSService.class.getClassLoader()
                .getResourceAsStream("sms.properties")) {

            if (input == null) {
                System.out.println("⚠️  sms.properties not found. SMS service running in SIMULATION MODE.");
                System.out.println("💡 Tip: Copy sms.properties.template to sms.properties and configure SMSMobileAPI.");
                return props;
            }

            props.load(input);
            System.out.println("✅ SMS configuration loaded successfully from sms.properties");

            // Check if credentials are configured
            String apiUrl = props.getProperty("sms.api.url", "");
            String apiKey = props.getProperty("sms.api.key", "");
            boolean enabled = Boolean.parseBoolean(props.getProperty("sms.enabled", "false"));

            if (apiUrl.isEmpty() || apiKey.isEmpty() || apiKey.contains("YOUR_")) {
                System.out.println("⚠️  SMS API not configured - running in SIMULATION MODE");
            } else if (!enabled) {
                System.out.println("⚠️  SMS sending is DISABLED (sms.enabled=false) - SIMULATION MODE");
            } else {
                System.out.println("✅ SMS API configured - REAL SMS MODE");
                System.out.println("📱 Provider: SMSMOBILEAPI");
            }

        } catch (IOException e) {
            System.err.println("❌ Error loading SMS configuration: " + e.getMessage());
            System.out.println("💡 Using default values (simulation mode).");
        }

        return props;
    }

    /**
     * Send an SMS reminder for an upcoming interview
     *
     * @param interview The interview details
     * @param recipientPhone The recipient's phone number (format: +21612345678)
     */
    public static void sendInterviewReminder(Interview interview, String recipientPhone) {
        try {
            String messageBody = buildReminderSMSBody(interview);
            sendSMS(recipientPhone, messageBody);
            System.out.println("📱 SMS reminder sent successfully to: " + recipientPhone);
        } catch (Exception e) {
            System.err.println("❌ Failed to send SMS reminder: " + e.getMessage());
        }
    }

    /**
     * Build the SMS body for interview reminder
     */
    private static String buildReminderSMSBody(Interview interview) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

        StringBuilder body = new StringBuilder();
        body.append("🎯 RAPPEL ENTRETIEN - Talent Bridge\n\n");
        body.append("📅 ").append(interview.getScheduledAt().format(formatter)).append("\n");
        body.append("⏱️ Durée: ").append(interview.getDurationMinutes()).append(" min\n");
        body.append("📍 Mode: ").append(interview.getMode()).append("\n");

        if ("ONLINE".equals(interview.getMode()) && interview.getMeetingLink() != null) {
            body.append("🔗 Lien: ").append(interview.getMeetingLink()).append("\n");
        } else if ("ON_SITE".equals(interview.getMode()) && interview.getLocation() != null) {
            body.append("📍 Lieu: ").append(interview.getLocation()).append("\n");
        }

        body.append("\nBonne chance! 🍀");

        return body.toString();
    }

    /**
     * Send an SMS using SMSMobileAPI
     *
     * @param toPhoneNumber Recipient phone number (format: +21612345678)
     * @param messageBody SMS message content
     */
    private static void sendSMS(String toPhoneNumber, String messageBody) {
        // Check if configured
        if (!SMS_ENABLED || API_URL.isEmpty() || API_KEY.isEmpty() || API_KEY.contains("YOUR_")) {
            System.out.println("\n=== SMS SIMULATION MODE ===");
            System.out.println("⚠️  No SMS sent - SMSMobileAPI not configured!");
            System.out.println("De: " + SENDER_NAME);
            System.out.println("À: " + toPhoneNumber);
            System.out.println("Message:\n" + messageBody);
            System.out.println("============================\n");
            System.out.println("💡 Pour envoyer de vrais SMS:");
            System.out.println("   1. Vérifiez votre email de SMSMobileAPI");
            System.out.println("   2. Copiez sms.properties.template vers sms.properties");
            System.out.println("   3. Configurez votre API URL et API Key");
            System.out.println("   4. Définissez sms.enabled=true");
            return;
        }

        // Send real SMS via SMSMobileAPI
        try {
            System.out.println("📱 Envoi d'un SMS réel via SMSMobileAPI à: " + toPhoneNumber);
            System.out.println("🔑 API Key (first 10 chars): " + API_KEY.substring(0, Math.min(10, API_KEY.length())) + "...");

            // Encode parameters for URL
            String encodedMessage = URLEncoder.encode(messageBody, StandardCharsets.UTF_8);
            String encodedPhone = URLEncoder.encode(toPhoneNumber, StandardCharsets.UTF_8);
            String encodedKey = URLEncoder.encode(API_KEY, StandardCharsets.UTF_8);

            // Build GET URL with parameters (using correct parameter names from SMSMobileAPI)
            // Their API uses: apikey, phone, message
            String urlString = String.format(
                "%s?apikey=%s&phone=%s&message=%s",
                API_URL,
                encodedKey,
                encodedPhone,
                encodedMessage
            );

            System.out.println("🔍 Requête GET: " + API_URL);
            System.out.println("📝 Paramètres: apikey=[hidden]&phone=" + toPhoneNumber + "&message=[" + messageBody.length() + " chars]");

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "TalentBridge/1.0");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            // Read response
            int responseCode = conn.getResponseCode();
            BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8
                )
            );

            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Check response for success
            String responseStr = response.toString();
            System.out.println("📊 Réponse API: " + responseStr);
            System.out.println("📊 Code HTTP: " + responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                // Check for success - error code "0" or "00" means success
                if (responseStr.contains("\"error\":\"0\"") ||
                    responseStr.contains("\"error\":0") ||
                    responseStr.contains("\"error\":\"00\"") ||
                    responseStr.contains("\"sent\":\"1\"") ||
                    responseStr.contains("\"success\":true") ||
                    responseStr.contains("\"status\":\"success\"")) {
                    System.out.println("✅ SMS envoyé avec succès via SMSMobileAPI!");
                } else if (responseStr.contains("api_missing") || responseStr.contains("\"error\":\"60\"")) {
                    System.err.println("❌ ERREUR 60: Paramètre API manquant");
                    System.err.println("💡 L'API ne reçoit pas le paramètre 'key' correctement");
                    System.err.println("💡 SOLUTION: Ouvrez le navigateur DevTools (F12)");
                    System.err.println("💡 Allez sur leur site web et envoyez un test SMS");
                    System.err.println("💡 Dans Network tab, cherchez 'Query String Parameters'");
                    System.err.println("💡 Copiez les NOMS EXACTS des paramètres (key/apikey/api?)");
                } else if (responseStr.contains("\"error\"")) {
                    System.err.println("❌ Échec de l'envoi du SMS (erreur API)");
                    System.err.println("💡 Réponse: " + responseStr);
                } else {
                    System.out.println("⚠️ Réponse inattendue - vérifiez manuellement");
                }
            } else {
                System.err.println("❌ Échec de l'envoi du SMS. Code HTTP: " + responseCode);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi du SMS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send interview status update SMS
     *
     * @param recipientPhone Recipient phone number
     * @param status Interview status (ACCEPTED/REJECTED/PENDING)
     */
    public static void sendInterviewStatusUpdate(String recipientPhone, String status) {
        try {
            String messageBody;
            switch (status.toUpperCase()) {
                case "ACCEPTED":
                    messageBody = "✅ Talent Bridge: Votre entretien a été ACCEPTÉ! Vous recevrez bientôt plus de détails.";
                    break;
                case "REJECTED":
                    messageBody = "❌ Talent Bridge: Malheureusement, votre candidature n'a pas été retenue cette fois.";
                    break;
                case "PENDING":
                    messageBody = "⏳ Talent Bridge: Votre entretien est en cours d'évaluation. Nous vous tiendrons informé.";
                    break;
                default:
                    messageBody = "🎯 Talent Bridge: Mise à jour du statut de votre entretien: " + status;
            }

            sendSMS(recipientPhone, messageBody);
            System.out.println("📱 SMS de mise à jour de statut envoyé avec succès à: " + recipientPhone);
        } catch (Exception e) {
            System.err.println("❌ Échec de l'envoi du SMS de mise à jour: " + e.getMessage());
        }
    }

    /**
     * Send application status update SMS
     *
     * @param recipientPhone Recipient phone number
     * @param jobTitle Job title
     * @param status Application status
     */
    public static void sendApplicationStatusUpdate(String recipientPhone, String jobTitle, String status) {
        try {
            String messageBody = String.format(
                "🎯 Talent Bridge\n\nMise à jour de votre candidature pour: %s\n\nStatut: %s\n\nConsultez votre compte pour plus de détails.",
                jobTitle, status
            );

            sendSMS(recipientPhone, messageBody);
            System.out.println("📱 SMS de statut de candidature envoyé avec succès à: " + recipientPhone);
        } catch (Exception e) {
            System.err.println("❌ Échec de l'envoi du SMS de statut: " + e.getMessage());
        }
    }

    /**
     * Test method to send a test SMS immediately
     */
    public static void sendTestSMS() {
        try {
            String messageBody = "🎯 Test SMS - Talent Bridge\n\n" +
                "Si vous recevez ce message, le service SMS fonctionne correctement!\n\n" +
                "Cordialement,\nL'équipe Talent Bridge";

            sendSMS(TEST_PHONE_NUMBER, messageBody);
            System.out.println("✅ Test SMS envoyé avec succès!");
        } catch (Exception e) {
            System.err.println("❌ Échec de l'envoi du test SMS: " + e.getMessage());
        }
    }

    /**
     * Validate phone number format
     *
     * @param phoneNumber Phone number to validate
     * @return true if valid format (+21612345678)
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        // Basic validation: starts with + and has 10-15 digits
        return phoneNumber.matches("^\\+[1-9]\\d{9,14}$");
    }
}

