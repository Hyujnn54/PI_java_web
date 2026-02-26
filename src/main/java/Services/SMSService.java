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
    private static final String API_URL      = smsConfig.getProperty("sms.api.url", "");
    private static final String API_KEY      = smsConfig.getProperty("sms.api.key", "");
    private static final String SENDER_NAME  = smsConfig.getProperty("sms.sender.name", "TalentBridge");
    private static final String SENDER_PHONE = smsConfig.getProperty("sms.sender.phone", ""); // waphone param
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
        sendInterviewReminder(interview, recipientPhone, "");
    }

    public static void sendInterviewReminder(Interview interview, String recipientPhone, String candidateName) {
        try {
            String normalizedPhone = normalizePhone(recipientPhone);
            System.out.println("[SMSService] Normalized phone: " + recipientPhone + " -> " + normalizedPhone);
            String messageBody = buildReminderSMSBody(interview, candidateName);
            sendSMS(normalizedPhone, messageBody);
            System.out.println("[SMSService] SMS reminder sent to: " + normalizedPhone);
        } catch (Exception e) {
            System.err.println("[SMSService] Failed to send SMS reminder: " + e.getMessage());
        }
    }

    /**
     * Professional SMS body — clear, concise, no emoji clutter.
     */
    private static String buildReminderSMSBody(Interview interview, String candidateName) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a' HH:mm");
        String mode = "ONLINE".equals(interview.getMode()) ? "En ligne" : "Sur site";

        StringBuilder b = new StringBuilder();
        b.append("Talent Bridge - Rappel d'entretien\n");
        b.append("----------------------------------\n");
        if (candidateName != null && !candidateName.isBlank()) {
            b.append("Bonjour ").append(candidateName).append(",\n\n");
        }
        b.append("Votre entretien est prevu demain :\n");
        b.append("Date    : ").append(interview.getScheduledAt().format(fmt)).append("\n");
        b.append("Duree   : ").append(interview.getDurationMinutes()).append(" minutes\n");
        b.append("Format  : ").append(mode).append("\n");

        if ("ONLINE".equals(interview.getMode()) && interview.getMeetingLink() != null
                && !interview.getMeetingLink().isBlank()) {
            b.append("Lien    : ").append(interview.getMeetingLink()).append("\n");
        } else if ("ON_SITE".equals(interview.getMode()) && interview.getLocation() != null
                && !interview.getLocation().isBlank()) {
            b.append("Lieu    : ").append(interview.getLocation()).append("\n");
        }

        b.append("----------------------------------\n");
        b.append("Bonne chance !\nL'equipe Talent Bridge");
        return b.toString();
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
            System.out.println("[SMSService] Sending SMS...");
            System.out.println("[SMSService] FROM : +" + (SENDER_PHONE.isEmpty() ? "default phone" : SENDER_PHONE));
            System.out.println("[SMSService] TO   : " + toPhoneNumber);

            // Build URL — include waphone to force sending from the configured number
            StringBuilder urlBuilder = new StringBuilder(API_URL);
            urlBuilder.append("?apikey=").append(URLEncoder.encode(API_KEY, StandardCharsets.UTF_8));
            urlBuilder.append("&phone=").append(URLEncoder.encode(toPhoneNumber, StandardCharsets.UTF_8));
            urlBuilder.append("&message=").append(URLEncoder.encode(messageBody, StandardCharsets.UTF_8));
            if (!SENDER_PHONE.isEmpty()) {
                // waphone tells SMSMobileAPI which registered phone to send FROM
                urlBuilder.append("&waphone=").append(URLEncoder.encode(SENDER_PHONE, StandardCharsets.UTF_8));
                System.out.println("[SMSService] waphone=" + SENDER_PHONE + " (forced sender)");
            }

            String urlString = urlBuilder.toString();
            System.out.println("[SMSService] Request: " + API_URL + "?apikey=[hidden]&phone="
                + toPhoneNumber + "&waphone=" + SENDER_PHONE);

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
     * Test method — sends a real reminder SMS to the first upcoming interview found in the DB.
     * Never sends to a hardcoded number; always uses real candidate data.
     */
    public static void sendTestFromDatabase() {
        System.out.println("[SMSService] Looking for a real interview in the DB to test with...");
        try {
            java.util.List<Models.Interview> interviews = InterviewService.getAll();
            Models.Interview target = interviews.stream()
                .filter(i -> i.getId() != null && i.getScheduledAt() != null
                          && i.getScheduledAt().isAfter(java.time.LocalDateTime.now().minusHours(1)))
                .findFirst().orElse(null);

            if (target == null) {
                System.out.println("[SMSService] No upcoming interviews found in DB — nothing to test.");
                return;
            }

            String phone = getCandidatePhoneForApplication(target.getApplicationId());
            if (phone == null || phone.isBlank()) {
                System.out.println("[SMSService] No phone found for interview #" + target.getId());
                return;
            }

            String normalizedPhone = normalizePhone(phone);
            if (!isValidPhoneNumber(normalizedPhone)) {
                System.out.println("[SMSService] Phone '" + phone + "' -> '" + normalizedPhone
                    + "' is invalid — cannot send.");
                return;
            }

            System.out.println("[SMSService] Sending test reminder for interview #" + target.getId()
                + " to " + normalizedPhone);
            sendInterviewReminder(target, normalizedPhone);

        } catch (Exception e) {
            System.err.println("[SMSService] DB test failed: " + e.getMessage());
        }
    }

    /** Fetch the candidate phone from users table (real registered number, not CV field). */
    private static String getCandidatePhoneForApplication(Long applicationId) {
        if (applicationId == null) return null;
        // Use users.phone (registered phone), NOT job_application.phone (CV submission field)
        String sql = "SELECT u.phone FROM job_application ja " +
                     "JOIN users u ON ja.candidate_id = u.id WHERE ja.id = ?";
        try {
            java.sql.Connection conn = Utils.MyDatabase.getInstance().getConnection();
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, applicationId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("phone");
                }
            }
        } catch (Exception e) {
            System.err.println("[SMSService] DB lookup error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Validate phone number format - accepts international (+216...) and local 8-digit Tunisian numbers
     *
     * @param phoneNumber Phone number to validate
     * @return true if valid
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        String normalized = normalizePhone(phoneNumber);
        // International format: +XXXXXXXXXXX (10-15 digits after +)
        return normalized.matches("^\\+[1-9]\\d{9,14}$");
    }

    /**
     * Normalize a phone number to international format.
     * - Already international (+216...) -> kept as-is
     * - 8-digit local Tunisian number    -> +216XXXXXXXX prepended
     * - Strips spaces, dashes, dots
     */
    public static String normalizePhone(String phoneNumber) {
        if (phoneNumber == null) return "";
        // Remove spaces, dashes, dots, parentheses
        String cleaned = phoneNumber.replaceAll("[\\s\\-\\.\\(\\)]", "");
        if (cleaned.startsWith("+")) {
            return cleaned; // already international
        }
        if (cleaned.startsWith("00")) {
            return "+" + cleaned.substring(2); // 00216... -> +216...
        }
        // Tunisian local number: 8 digits starting with 2,4,5,7,9
        if (cleaned.matches("[2457-9]\\d{7}")) {
            return "+216" + cleaned;
        }
        // Return as-is — may still fail validation
        return cleaned;
    }
}


